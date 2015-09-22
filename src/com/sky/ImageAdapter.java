package com.sky;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sky.Util.ImageLoader;
import com.sky.picselect.R;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	private static Set<String> mSelectImg = new HashSet<String>();
	private String mDirPath = null;
	private List<String> mImgPaths = null;
	private LayoutInflater mInflater = null;

	private int mScreenWidth;

	public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
		this.mDirPath = dirPath;
		this.mImgPaths = mDatas;
		mInflater = LayoutInflater.from(context);
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		mScreenWidth = outMetrics.widthPixels;
	}

	@Override
	public int getCount() {
		return mImgPaths.size();
	}

	@Override
	public Object getItem(int position) {
		return mImgPaths.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		final ViewHolder viewHolder;
		if (null == convertView) {
			convertView = mInflater.inflate(R.layout.item_gridview, parent,
					false);
			viewHolder = new ViewHolder();
			viewHolder.mImag = (ImageView) convertView
					.findViewById(R.id.id_item_img);
			viewHolder.mSelect = (ImageButton) convertView
					.findViewById(R.id.id_item_select);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.mImag.setImageResource(R.drawable.no_pic);
		viewHolder.mSelect.setImageResource(R.drawable.uncheck);
		viewHolder.mImag.setColorFilter(null);

		viewHolder.mImag.setMaxWidth(mScreenWidth / 3);
		// GridView 一行显示3个，为了节省内存，提前压缩为屏幕三分之一宽度

		ImageLoader.getInstance(3).LoadImage(
				mDirPath + "/" + mImgPaths.get(position), viewHolder.mImag);
		final String filePath = mDirPath + "/" + mImgPaths.get(position);
		viewHolder.mImag.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mSelectImg.contains(filePath)) {
					mSelectImg.remove(filePath);
					viewHolder.mImag.setColorFilter(null);
					viewHolder.mSelect.setImageResource(R.drawable.uncheck);
				} else {
					mSelectImg.add(filePath);
					viewHolder.mImag.setColorFilter(Color
							.parseColor("#88000000"));
					viewHolder.mSelect.setImageResource(R.drawable.checked);
				}
				// notifyDataSetChanged(); 会闪屏
			}
		});

		if (mSelectImg.contains(filePath)) {
			viewHolder.mImag.setColorFilter(Color.parseColor("#88000000"));
			viewHolder.mSelect.setImageResource(R.drawable.checked);
		}
		return convertView;
	}

	private class ViewHolder {
		public ImageView mImag = null;
		public ImageButton mSelect = null;
	}

}