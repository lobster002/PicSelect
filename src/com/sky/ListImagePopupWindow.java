package com.sky;

import java.util.List;

import com.sky.Util.ImageLoader;
import com.sky.picselect.R;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class ListImagePopupWindow extends PopupWindow implements
		OnTouchListener {

	private int mWidth;
	private int mHeight;
	private View mConvertView;
	private ListView mListView;

	private List<FolderBean> mDatas;

	public interface onDirSelectListener {
		void onSelected(FolderBean folderBean);
	};

	public onDirSelectListener mListener;

	public void setonDirSelectListener(onDirSelectListener mListener) {
		this.mListener = mListener;
	}

	public ListImagePopupWindow(Context context, List<FolderBean> datas) {
		caculateWidthAndHeight(context);
		mConvertView = LayoutInflater.from(context).inflate(
				R.layout.popup_main, null);
		mDatas = datas;
		setContentView(mConvertView);
		setWidth(mWidth);
		setHeight(mHeight);
		setFocusable(true);
		setTouchable(true);
		setOutsideTouchable(true);
		setBackgroundDrawable(new BitmapDrawable());
		setTouchInterceptor(this);
		initViews(context);
		initEvent();
	}

	private void initEvent() {

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(null != mListener){
					mListener.onSelected(mDatas.get(position));
				}
			}
		});
	}

	private void initViews(Context c) {
		mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
		mListView.setAdapter(new ListDirAdapter(c, mDatas));
	}

	private void caculateWidthAndHeight(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		mWidth = outMetrics.widthPixels;
		mHeight = outMetrics.heightPixels * 7 / 10;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
			dismiss();
			return true;
		}
		return false;
	}

	private class ListDirAdapter extends ArrayAdapter<FolderBean> {

		private LayoutInflater mInflater;
		private List<FolderBean> mDatas;

		public ListDirAdapter(Context context, List<FolderBean> objects) {
			super(context, 0, objects);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (null == convertView) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.item_popup_main,
						parent, false);
				holder.mImage = (ImageView) convertView
						.findViewById(R.id.id_id_dir_item_img);
				holder.mDirName = (TextView) convertView
						.findViewById(R.id.id_dir_item_name);
				holder.mDirCount = (TextView) convertView
						.findViewById(R.id.id_dir_item_count);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			FolderBean bean = getItem(position);
			holder.mImage.setImageResource(R.drawable.no_pic);
			ImageLoader.getInstance().LoadImage(bean.getFirstImgPath(),
					holder.mImage);
			holder.mDirName.setText(bean.getName());
			holder.mDirCount.setText(bean.getCount() + "");
			return convertView;
		}
	}

	private class ViewHolder {
		ImageView mImage;
		TextView mDirName;
		TextView mDirCount;
	}

}
