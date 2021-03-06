
package com.andbase.demo.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.ab.task.AbTaskCallback;
import com.ab.task.AbTaskItem;
import com.ab.task.AbTaskPool;
import com.ab.view.listener.AbOnListViewListener;
import com.ab.view.pullview.AbPullListView;
import com.andbase.R;
import com.andbase.demo.adapter.ImageListAdapter;
import com.andbase.global.MyApplication;


public class Fragment2 extends Fragment {
	
	private MyApplication application;
	private Activity mActivity = null;
	private List<Map<String, Object>> list = null;
	private List<Map<String, Object>> newList = null;
	private AbPullListView mAbPullListView = null;
	private int currentPage = 1;
	private AbTaskPool mAbTaskPool = null;
	private ArrayList<String> mPhotoList = new ArrayList<String>();
	private ImageListAdapter myListViewAdapter = null;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) { 
		 mActivity = this.getActivity();
		 application = (MyApplication) mActivity.getApplication();
		 
		 View view = inflater.inflate(R.layout.pull_list, null);
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i3/13215035600700175/T1C2mzXthaXXXXXXXX_!!0-item_pic.jpg_230x230.jpg");  
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i2/13215025617307680/T1AQqAXqpeXXXXXXXX_!!0-item_pic.jpg_230x230.jpg");
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i1/13215035569460099/T16GuzXs0cXXXXXXXX_!!0-item_pic.jpg_230x230.jpg");
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i2/13215023694438773/T1lImmXElhXXXXXXXX_!!0-item_pic.jpg_230x230.jpg");
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i3/13215023521330093/T1BWuzXrhcXXXXXXXX_!!0-item_pic.jpg_230x230.jpg");  
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i4/13215035563144015/T1Q.eyXsldXXXXXXXX_!!0-item_pic.jpg_230x230.jpg");  
		 mPhotoList.add("http://img01.taobaocdn.com/bao/uploaded/i3/13215023749568975/T1UKWCXvpXXXXXXXXX_!!0-item_pic.jpg_230x230.jpg"); 
		 mAbTaskPool = AbTaskPool.getInstance();
	     //获取ListView对象
         mAbPullListView = (AbPullListView)view.findViewById(R.id.mListView);
         //关闭加载更多功能
         mAbPullListView.setPullLoadEnable(false);
         //ListView数据
    	 list = new ArrayList<Map<String, Object>>();
    	
    	 //使用自定义的Adapter
    	 myListViewAdapter = new ImageListAdapter(mActivity, list,R.layout.list_items,
				new String[] { "itemsIcon", "itemsTitle","itemsText" }, new int[] { R.id.itemsIcon,
						R.id.itemsTitle,R.id.itemsText });
    	 mAbPullListView.setAdapter(myListViewAdapter);
    	 //item被点击事件
    	 mAbPullListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
			}
    	 });

    	 //定义两种查询的事件
    	 final AbTaskItem item1 = new AbTaskItem();
		 item1.callback = new AbTaskCallback() {

			@Override
			public void update() {
				list.clear();
				if(newList!=null && newList.size()>0){
	                list.addAll(newList);
	                myListViewAdapter.notifyDataSetChanged();
	                newList.clear();
   		    	}
				mAbPullListView.stopRefresh();
			}

			@Override
			public void get() {
	   		    try {
	   		    	Thread.sleep(1000);
	   		    	currentPage = 1;
	   		    	newList = new ArrayList<Map<String, Object>>();
	   		    	Map<String, Object> map = null;
	   		    	
	   		    	for (int i = 0; i < 10; i++) {
	   		    		map = new HashMap<String, Object>();
	   					map.put("itemsIcon",mPhotoList.get(new Random().nextInt(mPhotoList.size())));
		   		    	map.put("itemsTitle", "item"+i);
		   		    	map.put("itemsText", "item..."+i);
		   		    	newList.add(map);
	   				}
	   		    } catch (Exception e) {
	   		    }
		  };
		};
		
		
		
		mAbPullListView.setAbOnListViewListener(new AbOnListViewListener(){

			@Override
			public void onRefresh() {
				mAbTaskPool.execute(item1);
			}

			@Override
			public void onLoadMore() {
			}
			
		});
		
    	//第一次下载数据
		mAbTaskPool.execute(item1);
	    
		return view;
	} 

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

}

