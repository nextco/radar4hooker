package gz.util;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.Display;
import gz.radar.Android;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MediaProjectionScreenshotManager {

	private static final String FRAGMENT_TAG = "hooker_media_projection_permission_fragment";
	private static final int REQUEST_CODE = 3621;
	private static final Logger logger = new Logger(MediaProjectionScreenshotManager.class);
	private static final MediaProjectionScreenshotManager INSTANCE = new MediaProjectionScreenshotManager();

	private volatile int resultCode = Activity.RESULT_CANCELED;
	private volatile Intent resultData;
	private volatile boolean requesting;
	private volatile long grantedAt;

	public static MediaProjectionScreenshotManager getInstance() {
		return INSTANCE;
	}

	public synchronized boolean hasPermission() {
		return resultData != null && resultCode == Activity.RESULT_OK;
	}

	public synchronized Map<String, Object> requestPermission() throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			result.put("ok", false);
			result.put("supported", false);
			result.put("error", "MediaProjection requires Android 5.0+");
			return result;
		}
		if (hasPermission()) {
			result.put("ok", true);
			result.put("supported", true);
			result.put("authorized", true);
			result.put("requesting", false);
			result.put("granted_at", Long.valueOf(grantedAt));
			return result;
		}
		final Activity activity = Android.getTopActivity();
		if (activity == null) {
			result.put("ok", false);
			result.put("supported", true);
			result.put("error", "top activity not found");
			return result;
		}
		if (requesting) {
			result.put("ok", true);
			result.put("supported", true);
			result.put("authorized", false);
			result.put("requesting", true);
			return result;
		}
		runOnUiThreadAndWait(activity, new Runnable() {
			@Override
			public void run() {
				PermissionFragment fragment = ensurePermissionFragment(activity);
				requesting = true;
				fragment.startRequest();
			}
		});
		result.put("ok", true);
		result.put("supported", true);
		result.put("authorized", false);
		result.put("requesting", true);
		result.put("message", "permission dialog started");
		return result;
	}

	public synchronized Map<String, Object> getStatus() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("supported", Boolean.valueOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP));
		result.put("authorized", Boolean.valueOf(hasPermission()));
		result.put("requesting", Boolean.valueOf(requesting));
		result.put("granted_at", Long.valueOf(grantedAt));
		return result;
	}

	public byte[] captureScreenshot() throws Exception {
		return captureScreenshot("jpeg", 70);
	}

	public byte[] captureScreenshot(String format, int quality) throws Exception {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			throw new IllegalStateException("MediaProjection requires Android 5.0+");
		}
		if (!hasPermission()) {
			throw new IllegalStateException("MediaProjection permission not granted");
		}
		final Activity activity = Android.getTopActivity();
		if (activity == null) {
			throw new IllegalStateException("top activity not found");
		}
		final DisplayMetrics metrics = new DisplayMetrics();
		runOnUiThreadAndWait(activity, new Runnable() {
			@Override
			public void run() {
				Display display = activity.getWindowManager().getDefaultDisplay();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					display.getRealMetrics(metrics);
				} else {
					display.getMetrics(metrics);
				}
			}
		});
		int width = Math.max(1, metrics.widthPixels);
		int height = Math.max(1, metrics.heightPixels);
		int densityDpi = Math.max(1, metrics.densityDpi);
		Intent permissionData = new Intent(resultData);
		MediaProjectionManager projectionManager = (MediaProjectionManager) Android.getApplication()
				.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		if (projectionManager == null) {
			throw new IllegalStateException("MediaProjectionManager unavailable");
		}
		MediaProjection mediaProjection = projectionManager.getMediaProjection(resultCode, permissionData);
		if (mediaProjection == null) {
			throw new IllegalStateException("failed to create MediaProjection");
		}
		ImageReader imageReader = null;
		VirtualDisplay virtualDisplay = null;
		HandlerThread handlerThread = null;
		try {
			imageReader = ImageReader.newInstance(width, height, 0x1, 2);
			handlerThread = new HandlerThread("hooker-media-projection");
			handlerThread.start();
			Handler handler = new Handler(handlerThread.getLooper());
			final AtomicReference<Image> imageRef = new AtomicReference<Image>();
			final CountDownLatch latch = new CountDownLatch(1);
			imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
				@Override
				public void onImageAvailable(ImageReader reader) {
					Image image = null;
					try {
						image = reader.acquireLatestImage();
						if (image == null) {
							return;
						}
						if (imageRef.compareAndSet(null, image)) {
							latch.countDown();
							return;
						}
					} catch (Throwable t) {
						logger.warn(t);
					}
					if (image != null) {
						image.close();
					}
				}
			}, handler);
			virtualDisplay = mediaProjection.createVirtualDisplay("hooker-screen-capture", width, height, densityDpi,
					DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, handler);
			if (!latch.await(3, TimeUnit.SECONDS)) {
				throw new IllegalStateException("wait MediaProjection image timeout");
			}
			Image image = imageRef.get();
			if (image == null) {
				throw new IllegalStateException("empty screenshot image");
			}
			try {
				return toImageBytes(image, width, height, format, quality);
			} finally {
				image.close();
			}
		} finally {
			if (virtualDisplay != null) {
				virtualDisplay.release();
			}
			if (imageReader != null) {
				imageReader.close();
			}
			mediaProjection.stop();
			if (handlerThread != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
					handlerThread.quitSafely();
				} else {
					handlerThread.quit();
				}
			}
		}
	}

	private byte[] toImageBytes(Image image, int width, int height, String format, int quality) throws Exception {
		Image.Plane[] planes = image.getPlanes();
		if (planes == null || planes.length == 0) {
			throw new IllegalStateException("image planes empty");
		}
		ByteBuffer buffer = planes[0].getBuffer();
		int pixelStride = planes[0].getPixelStride();
		int rowStride = planes[0].getRowStride();
		int rowPadding = rowStride - pixelStride * width;
		Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
		bitmap.copyPixelsFromBuffer(buffer);
		Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			Bitmap.CompressFormat compressFormat = resolveCompressFormat(format);
			int resolvedQuality = normalizeQuality(quality);
			croppedBitmap.compress(compressFormat, resolvedQuality, baos);
			return baos.toByteArray();
		} finally {
			croppedBitmap.recycle();
			bitmap.recycle();
			baos.close();
		}
	}

	private Bitmap.CompressFormat resolveCompressFormat(String format) {
		if (format == null) {
			return Bitmap.CompressFormat.JPEG;
		}
		String normalized = format.trim().toLowerCase();
		if ("png".equals(normalized)) {
			return Bitmap.CompressFormat.PNG;
		}
		if ("jpg".equals(normalized) || "jpeg".equals(normalized)) {
			return Bitmap.CompressFormat.JPEG;
		}
		return Bitmap.CompressFormat.JPEG;
	}

	private int normalizeQuality(int quality) {
		if (quality < 0) {
			return 0;
		}
		if (quality > 100) {
			return 100;
		}
		return quality;
	}

	private synchronized void onPermissionResult(int code, Intent data) {
		requesting = false;
		if (code == Activity.RESULT_OK && data != null) {
			resultCode = code;
			resultData = new Intent(data);
			grantedAt = System.currentTimeMillis();
			logger.info("MediaProjection permission granted");
			return;
		}
		resultCode = Activity.RESULT_CANCELED;
		resultData = null;
		grantedAt = 0L;
		logger.warn("MediaProjection permission denied");
	}

	private PermissionFragment ensurePermissionFragment(Activity activity) {
		FragmentManager fragmentManager = activity.getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG);
		if (fragment instanceof PermissionFragment) {
			return (PermissionFragment) fragment;
		}
		PermissionFragment permissionFragment = new PermissionFragment();
		fragmentManager.beginTransaction().add(permissionFragment, FRAGMENT_TAG).commitAllowingStateLoss();
		fragmentManager.executePendingTransactions();
		return permissionFragment;
	}

	private void removePermissionFragment(Fragment fragment) {
		try {
			FragmentManager fragmentManager = fragment.getFragmentManager();
			if (fragmentManager != null) {
				fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
				fragmentManager.executePendingTransactions();
			}
		} catch (Throwable t) {
			logger.warn(t);
		}
	}

	private void runOnUiThreadAndWait(Activity activity, final Runnable runnable) throws Exception {
		if (Thread.currentThread() == activity.getMainLooper().getThread()) {
			runnable.run();
			return;
		}
		final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
		final CountDownLatch latch = new CountDownLatch(1);
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (Throwable t) {
					errorRef.set(t);
				} finally {
					latch.countDown();
				}
			}
		});
		if (!latch.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("runOnUiThread timeout");
		}
		Throwable t = errorRef.get();
		if (t != null) {
			if (t instanceof Exception) {
				throw (Exception) t;
			}
			throw new RuntimeException(t);
		}
	}

	public static class PermissionFragment extends Fragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}

		public void startRequest() {
			Activity activity = getActivity();
			if (activity == null) {
				MediaProjectionScreenshotManager.getInstance().onPermissionResult(Activity.RESULT_CANCELED, null);
				return;
			}
			MediaProjectionManager projectionManager = (MediaProjectionManager) activity
					.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
			if (projectionManager == null) {
				MediaProjectionScreenshotManager.getInstance().onPermissionResult(Activity.RESULT_CANCELED, null);
				return;
			}
			startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if (requestCode != REQUEST_CODE) {
				return;
			}
			MediaProjectionScreenshotManager manager = MediaProjectionScreenshotManager.getInstance();
			manager.onPermissionResult(resultCode, data);
			manager.removePermissionFragment(this);
		}
	}
}
