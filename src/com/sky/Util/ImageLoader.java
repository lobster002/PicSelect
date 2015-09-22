package com.sky.Util;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

public class ImageLoader {

	private static ImageLoader mInstance = null;

	private LruCache<String, Bitmap> mLruCache = null;// �������

	private ExecutorService mThreadPool = null;// �̳߳�
	private static final int DEFAULT_THREAD_COUNT = 1;// Ĭ���߳���

	private Semaphore mSemaphoreThreadPool;

	private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

	private LinkedList<Runnable> mTaskQueue = null;// ������У�����
	private Thread mPoolThread = null;// ��̨��ѯ�߳�
	private Handler mPoolThreadHandler = null;
	private Handler mUIHandler = null;

	private ImageLoader(int count) {
		// ����ģʽ���������ⲿ���ù��캯�� ����������
		init(count);
	}

	private void init(int count) {
		// ��ʼ������

		mPoolThread = new Thread() {// ��̨��ѯ�߳�

			@Override
			public void run() {
				Looper.prepare();
				mPoolThreadHandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// �̳߳�ȡ��һ������ȥִ��
//						mThreadPool.execute(getTask());
						mThreadPool.execute(mTaskQueue.removeLast());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				};
				mSemaphorePoolThreadHandler.release();
				Looper.loop();
			};
		};
		mPoolThread.start();
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheMemory = maxMemory / 8;
		mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();
			}
		};
		// �����̳߳�
		mThreadPool = Executors.newFixedThreadPool(count); //����һ�������ù̶��߳������̳߳�
		mTaskQueue = new LinkedList<Runnable>(); //���������

		mSemaphoreThreadPool = new Semaphore(count);
	}

//	private Runnable getTask() {
//		return mTaskQueue.removeLast(); //�Ӻ���ǰ����
////		return mTaskQueue.removeFirst();  //��ǰ�������
//	}

	/*
	 * ����pathΪImageView����ͼƬ
	 */
	public void LoadImage(final String path, final ImageView imageview) {
		imageview.setTag(path);
		if (null == mUIHandler) {
			mUIHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					// ��ȡ�õ���ͼƬ��ΪiamgeView�ص�����ͼƬ
					ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
					Bitmap bm = holder.bitmap;
					ImageView imageView = holder.imageView;
					String path = holder.path;

					if (imageView.getTag().toString().equals(path)) {
						imageView.setImageBitmap(bm);
					}
				};
			};
		}

		Bitmap bm = getBitmapFromCache(path);
		if (null != bm) {
			Message message = Message.obtain();
			ImgBeanHolder holder = new ImgBeanHolder();
			holder.bitmap = bm;
			holder.path = path;
			holder.imageView = imageview;
			message.obj = holder;
			mUIHandler.sendMessage(message);
		} else {
			addTasks(new Runnable() {
				public void run() {
					// ���ز�ѹ��ͼƬ
					ImageSize imageSize = getImageViewSize(imageview);
					Bitmap bm = decodeSampleBitmapFromPath(path,
							imageSize.Width, imageSize.Height);

					addBitmapTruCache(path, bm);

					Message message = Message.obtain();
					ImgBeanHolder holder = new ImgBeanHolder();
					holder.bitmap = bm;
					holder.path = path;
					holder.imageView = imageview;
					message.obj = holder;
					mUIHandler.sendMessage(message);
					mSemaphoreThreadPool.release();
				}
			});
		}
	}

	protected void addBitmapTruCache(String path, Bitmap bm) {
		if (null == getBitmapFromCache(path) && null != bm) {
			mLruCache.put(path, bm);
		}
	}

	protected Bitmap decodeSampleBitmapFromPath(String path, int width,
			int height) {
		// ѹ��ͼƬ
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// ��ȡͼƬ��ߣ��������ص��ڴ�
		BitmapFactory.decodeFile(path, options);

		options.inSampleSize = caculeteInSampleSize(options, width, height);
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);
		return bitmap;
	}

	private int caculeteInSampleSize(Options options, int reqWidth,
			int reqHeight) {
		int width = options.outWidth;
		int height = options.outHeight;
		int inSampleSize = 1;
		if (width > reqWidth || height > reqHeight) {
			int widthRadio = Math.round(width * 1.0f / reqWidth);
			int heightRadio = Math.round(height * 1.0f / reqHeight);
			inSampleSize = Math.max(widthRadio, heightRadio);
		}
		return inSampleSize;
	}

	protected ImageSize getImageViewSize(ImageView imageview) {
		ImageSize imageSize = new ImageSize();
		android.view.ViewGroup.LayoutParams lp = imageview.getLayoutParams();
		int width = imageview.getWidth();// ��ȡImageView��ʵ�ʿ��
		if (width <= 0) {
			width = lp.width;// ��ȡImageView��Layout�������Ŀ��
		}
		if (width <= 0) {
			width = getImageFieldValue(imageview, "mMaxWidth");
		}
		if (width <= 0) {
			width = imageview.getContext().getResources().getDisplayMetrics().widthPixels;
		}

		int height = imageview.getHeight();
		if (height <= 0) {
			height = lp.height;// ��ȡImageView��Layout�������Ŀ��
		}
		if (height <= 0) {
			height = getImageFieldValue(imageview, "mMaxHeight");
		}
		if (height <= 0) {
			height = imageview.getContext().getResources().getDisplayMetrics().widthPixels;
		}

		imageSize.Width = width;
		imageSize.Height = height;

		return imageSize;
	}

	private static int getImageFieldValue(Object object, String fieldName) {
		int value = 0;
		Field field;
		try {
			field = ImageView.class.getDeclaredField(fieldName);
			field.setAccessible(true);

			int filedValue = field.getInt(object);

			if (filedValue > 0 && filedValue < Integer.MAX_VALUE) {
				value = filedValue;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;
	}

	private synchronized void addTasks(Runnable runnable) {
		mTaskQueue.add(runnable);
		if (null == mPoolThreadHandler) {
			try {
				mSemaphorePoolThreadHandler.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mPoolThreadHandler.sendEmptyMessage(-1);
	}

	protected class ImageSize {
		public int Height;
		public int Width;
	};

	private class ImgBeanHolder {
		public Bitmap bitmap = null;
		ImageView imageView = null;
		String path = null;
	}

	private Bitmap getBitmapFromCache(String key) {
		// ����Key��path���ڻ�������ͼƬ
		return mLruCache.get(key);
	}

	public static ImageLoader getInstance(int count) {

		// ���߳�ͬ������
		if (null == mInstance) {
			synchronized (ImageLoader.class) {
				if (null == mInstance) {
					mInstance = new ImageLoader(count);
				}
			}
		}
		return mInstance;
	}

	public static ImageLoader getInstance() {

		// ���߳�ͬ������
		if (null == mInstance) {
			synchronized (ImageLoader.class) {
				if (null == mInstance) {
					mInstance = new ImageLoader(DEFAULT_THREAD_COUNT);
				}
			}
		}
		return mInstance;
	}
}
