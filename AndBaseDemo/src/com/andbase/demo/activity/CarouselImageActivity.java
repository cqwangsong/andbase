package com.andbase.demo.activity;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import com.ab.activity.AbActivity;
import com.ab.view.carousel.CarouselAdapter;
import com.ab.view.carousel.CarouselAdapter.OnItemClickListener;
import com.ab.view.carousel.CarouselAdapter.OnItemSelectedListener;
import com.ab.view.carousel.CarouselImageAdapter;
import com.ab.view.carousel.CarouselImageView;
import com.andbase.R;
/**
 * 
 * Copyright (c) 2012 All rights reserved
 * 名称：CarouselImageActivity.java 
 * 描述：图片适配的旋转木马
 * @author zhaoqp
 * @date：2013-8-23 下午2:06:48
 * @version v1.0
 */
public class CarouselImageActivity extends AbActivity {
	
	private CarouselImageView carousel = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setAbContentView(R.layout.carousel_image);
		
		carousel = (CarouselImageView) findViewById(R.id.carousel);
		
		List<Drawable> mDrawables = new ArrayList<Drawable>();
		mDrawables.add(this.getResources().getDrawable(R.drawable.icon1));
		mDrawables.add(this.getResources().getDrawable(R.drawable.icon2));
		mDrawables.add(this.getResources().getDrawable(R.drawable.icon3));
		mDrawables.add(this.getResources().getDrawable(R.drawable.icon4));
		mDrawables.add(this.getResources().getDrawable(R.drawable.icon5));
		mDrawables.add(this.getResources().getDrawable(R.drawable.icon6));
		
		//不支持的动态添加dapter.notifyDataSetChanged();增强滑动的流畅
		
		CarouselImageAdapter adapter = new CarouselImageAdapter(this,mDrawables,true);
		carousel.setAdapter(adapter);
		
		carousel.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(CarouselAdapter<?> parent, View view,
					int position, long id) {
				showToast("Click Position=" + position);
				
			}

		});
		
		carousel.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(CarouselAdapter<?> parent, View view,
					int position, long id) {
				showToast("Selected Position=" + position);
			}

			@Override
			public void onNothingSelected(CarouselAdapter<?> parent) {
			}
			
		});
		
		
	}

}
