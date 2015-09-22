package com.sky;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sky.ListImagePopupWindow.onDirSelectListener;
import com.sky.picselect.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private GridView mGridView = null;

	private List<String> mImgs = null;
	private ImageAdapter mImageAdapter = null;

	private RelativeLayout mBottomLy = null;
	private TextView mDirName = null;
	private TextView mDirCount = null;

	private File mCurrentDir = null;
	private int mMaxCount;

	private ListImagePopupWindow mDirPopupWindow;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (-1 == msg.what) {
				mProgressDialog.dismiss();
				// 绑定数据到View
				data2View();
				initDirpopupWindow();
			}
		}

	};

	private ProgressDialog mProgressDialog = null;

	private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		initView();
		initDatas();
		initEvent();
	}

	protected void initDirpopupWindow() {
		mDirPopupWindow = new ListImagePopupWindow(this, mFolderBeans);
		mDirPopupWindow.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss() {
				lightOn();
			}
		});
		mDirPopupWindow.setonDirSelectListener(new onDirSelectListener() {

			@Override
			public void onSelected(FolderBean folderBean) {
				mCurrentDir = new File(folderBean.getDir());
				mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String filename) {
						if (filename.endsWith(".jpg")
								|| filename.endsWith(".png")
								|| filename.endsWith(".jpeg")) {
							return true;
						}

						return false;
					}
				}));
				
				mImageAdapter  = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
				mGridView.setAdapter(mImageAdapter);
				
				mDirCount.setText(mImgs.size()+"");
				mDirName.setText(folderBean.getName());
				
				mDirPopupWindow.dismiss();
			}
		});

	}

	protected void lightOn() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 1.0f;
		getWindow().setAttributes(lp);
	}

	protected void data2View() {
		if (null == mCurrentDir) {
			showToast("未找到任何图片！");
			return;
		}
		mImgs = Arrays.asList(mCurrentDir.list());
		mImageAdapter = new ImageAdapter(this, mImgs,
				mCurrentDir.getAbsolutePath());
		mGridView.setAdapter(mImageAdapter);

		mDirCount.setText(mMaxCount + "");
		mDirName.setText(mCurrentDir.getName());
	}

	private void initEvent() {
		mBottomLy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
				mDirPopupWindow.showAsDropDown(mBottomLy, 0, 0);
				lightOff();

			}
		});
	}

	protected void lightOff() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 0.3f;
		getWindow().setAttributes(lp);
	}

	private void initDatas() {
		/**
		 * 利用ContentProvider 遍历所有图片
		 */
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			showToast("存储卡不可用！");
			return;
		}
		mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
		new Thread() {
			public void run() {
				Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				ContentResolver cr = MainActivity.this.getContentResolver();
				Cursor cursor = cr.query(mImgUri, null,
						MediaStore.Images.Media.MIME_TYPE + " = ? or "
								+ MediaStore.Images.Media.MIME_TYPE + " = ? ",
						new String[] { "image/jpeg", "image/png" },
						MediaStore.Images.Media.DATE_MODIFIED);

				Set<String> mDirPaths = new HashSet<String>();
				while (cursor.moveToNext()) {
					String path = cursor.getString(cursor
							.getColumnIndex(MediaStore.Images.Media.DATA));
					File parentFile = new File(path).getParentFile();
					if (null == parentFile) {
						continue;
					}

					String dirPath = parentFile.getAbsolutePath();
					FolderBean folderBean = null;
					if (mDirPaths.contains(dirPath)) {
						continue;
					} else {
						mDirPaths.add(dirPath);
						folderBean = new FolderBean();
						folderBean.setDir(dirPath);
						folderBean.setFirstImgPath(path);
					}
					if (null == parentFile.list()) {
						continue;
					}
					int picSize = parentFile.list(new FilenameFilter() {

						@Override
						public boolean accept(File dir, String filename) {
							if (filename.endsWith(".jpg")
									|| filename.endsWith(".png")
									|| filename.endsWith(".jpeg")) {
								return true;
							}
							return false;
						}

					}).length;
					folderBean.setCount(picSize);
					mFolderBeans.add(folderBean);

					if (picSize > mMaxCount) {
						mMaxCount = picSize;
						mCurrentDir = parentFile;
					}
				}
				cursor.close();
				mHandler.sendEmptyMessage(-1);// 通知扫描完成
			};
		}.start();

	}

	private void showToast(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	private void initView() {
		mGridView = (GridView) findViewById(R.id.id_gridview);
		mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
		mDirName = (TextView) findViewById(R.id.id_dir_name);
		mDirCount = (TextView) findViewById(R.id.id_dir_count);

	}

}
