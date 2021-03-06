package com.example.renxin.shujia.SortListView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.example.renxin.shujia.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudySortListMainActivity extends Activity {
	private ListView sortListView;
	private StudySideBar sideBar;
	private TextView dialog;
	private StudySortAdapter adapter;
	private StudyClearEditText mClearEditText;
    private boolean multichoice;
    ArrayList<Integer> indexList;
	
	/**
	 * 汉字转换成拼音的类
	 */
	private StudyCharacterParser characterParser;
	private List<StudySortModel> SourceDateList;

	/**
	 * 根据拼音来排列ListView里面的数据类
	 */
	private StudyPinyinComparator pinyinComparator;

    //result code
    public static int RESULT_FALSE=1, RESULT_OK=0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.study_sort_list_activity_main);
		initViews();
	}


	private void initViews() {

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);

        //得到父Activity的参数
        Intent intent = getIntent();
        setTitle(intent.getStringExtra("title"));
        String[] data = intent.getStringArrayExtra("data");
        multichoice = intent.getBooleanExtra("multichoice", false);
        indexList = intent.getIntegerArrayListExtra("index_list");

        //是否需要返回index

        final int groupIndex = intent.getIntExtra("groupIndex", 0);
        final int itemIndex = intent.getIntExtra("itemIndex", 0);

		RelativeLayout	back = (RelativeLayout) findViewById(R.id.ImageButton_back);
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				setResult(RESULT_FALSE , intent);
				StudySortListMainActivity.this.finish();
			}
		});

		//实例化汉字转拼音类
		characterParser = StudyCharacterParser.getInstance();

		pinyinComparator = new StudyPinyinComparator();

		sideBar = (StudySideBar) findViewById(R.id.sidrbar);
		dialog = (TextView) findViewById(R.id.dialog);
		sideBar.setTextView(dialog);

		//设置右侧触摸监听
		sideBar.setOnTouchingLetterChangedListener(new StudySideBar.OnTouchingLetterChangedListener() {

			@Override
			public void onTouchingLetterChanged(String s) {
				//该字母首次出现的位置
				int position = adapter.getPositionForSection(s.charAt(0));
				if(position != -1){
					sortListView.setSelection(position);
				}

			}
		});

        //------------------------------点击监听·返回------------------------------
		sortListView = (ListView) findViewById(R.id.country_lvcountry);
		sortListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				//这里要利用adapter.getItem(position)来获取当前position所对应的对象

                //单选
                if(!multichoice) {
                    Intent intent = new Intent();
                    String str = ((StudySortModel) adapter.getItem(position)).getName();

                    intent.putExtra("data", str);
                    intent.putExtra("groupIndex", groupIndex);
                    intent.putExtra("itemIndex", itemIndex);
                    setResult(RESULT_OK, intent);
                    finish();
                }

                //多选
                else{
					StudySortModel sortModel = (StudySortModel) adapter.getItem(position);
                    sortModel.setChecked(!sortModel.isChecked());
                    adapter.notifyDataSetChanged();
                }
			}
		});

		SourceDateList = filledData(data);

		// 根据a-z进行排序源数据
		Collections.sort(SourceDateList, pinyinComparator);

        //设置checkbox
        if(multichoice) {
            for (int i = 0; i < indexList.size(); i++) {
                SourceDateList.get(indexList.get(i)).setChecked(true);
            }
        }


        adapter = new StudySortAdapter(this, SourceDateList, multichoice);
		sortListView.setAdapter(adapter);


		mClearEditText = (StudyClearEditText) findViewById(R.id.filter_edit);

		//根据输入框输入值的改变来过滤搜索
		mClearEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
				filterData(s.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}


	/**
	 * ?ListView???????
	 * @param date
	 * @return
	 */
	private List<StudySortModel> filledData(String [] date){
		List<StudySortModel> mSortList = new ArrayList<StudySortModel>();

		for(int i=0; i<date.length; i++){
			StudySortModel sortModel = new StudySortModel();
			sortModel.setName(date[i]);
            sortModel.setChecked(false);
			//汉字转换成拼音
			String pinyin = characterParser.getSelling(date[i]);
			String sortString = pinyin.substring(0, 1).toUpperCase();

			// 正则表达式，判断首字母是否是英文字母
			if(sortString.matches("[A-Z]")){
				sortModel.setSortLetters(sortString.toUpperCase());
			}else{
				sortModel.setSortLetters("#");
			}

			mSortList.add(sortModel);
		}

		return mSortList;

	}

	/**
	 * 根据输入框中的值来过滤数据并更新ListView
	 * @param filterStr
	 */
	private void filterData(String filterStr){
		List<StudySortModel> filterDateList = new ArrayList<StudySortModel>();

		if(TextUtils.isEmpty(filterStr)){
			filterDateList = SourceDateList;
		}else{
			filterDateList.clear();
			for(StudySortModel sortModel : SourceDateList){
				final String name = sortModel.getName();
				if(name.indexOf(filterStr.toString()) != -1 || characterParser.getSelling(name).startsWith(filterStr.toString())){
					filterDateList.add(sortModel);
				}
			}
		}

		// 根据a-z进行排序
		Collections.sort(filterDateList, pinyinComparator);
		adapter.updateListView(filterDateList);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(multichoice)
            getMenuInflater().inflate(R.menu.crm_study_menu_sort_list, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent = new Intent();

        if (id == android.R.id.home) {
			setResult(RESULT_FALSE, intent);
			StudySortListMainActivity.this.finish();
		} else if (id == R.id.sortlist_ok) {

			//返回多个选项
			intent.putExtra("data", getMultiChoice());
			setResult(RESULT_OK, intent);
			finish();
		}

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){

            Intent intent = new Intent();
            setResult(RESULT_FALSE , intent);
			StudySortListMainActivity.this.finish();
        }

        return super.onKeyDown(keyCode, event);
    }

    //返回多个选项
    public ArrayList<Object> getMultiChoice(){
        ArrayList<String> strList = new ArrayList<String>();
        ArrayList<Integer> indexList = new ArrayList<Integer>();
		StudySortModel model;

        for(int i=0, j=0; i<SourceDateList.size(); i++){
            model = SourceDateList.get(i);
            if(model.isChecked()) {
                strList.add(model.getName());
                indexList.add(i);
            }
        }

        ArrayList<Object> list = new ArrayList<Object>();
        list.add(strList);
        list.add(indexList);

        return list;
    }

}
