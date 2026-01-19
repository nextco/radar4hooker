package gz.httpserver.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.annotation.HookerRequestPostJson;
import gz.radar.Android;
import gz.radar.AndroidUI;
import gz.radar.AndroidUI2;
import gz.util.Logger;
import gz.util.XView;

@HookerController("/hooker/ui/")
public class BuiltinUIServiceController {

	private Logger logger = new Logger(BuiltinUIServiceController.class);
	
	@HookerRequestMapping(path = "finish_current_activity", produces = Produces.AUTO)
	public String finish_current_activity() throws Exception {
		AndroidUI.finishCurrentActivity();
		return "ok";
	}

	@HookerRequestMapping(path = "back", produces = Produces.AUTO)
	public String back() throws Exception {
		AndroidUI.back();
		return "ok";
	}

	@HookerRequestMapping(path = "home", produces = Produces.AUTO)
	public String home() throws Exception {
		AndroidUI.home();
		return "ok";
	}

	@HookerRequestMapping(path = "click_by_text", produces = Produces.AUTO)
	public String click_by_text(@HookerRequestParam(name = "text") String text,
			@HookerRequestParam(name = "text_equeal", defaultValue = "false") boolean mustBeTextEqueal,
			@HookerRequestParam(name = "visible", defaultValue = "false") boolean mustBeVisible) throws Exception {
		AndroidUI.clickByText(text, mustBeTextEqueal, mustBeVisible);
		return "ok";
	}

	@HookerRequestMapping(path = "click_by_id", produces = Produces.AUTO)
	public String click_by_id(@HookerRequestParam(name = "id") String id) throws Exception {
		View view = findViewById(id);
		logger.info("view: " + view.getClass().getName());
		if (view != null && view.isClickable()) {
			final View clickableView = view;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					clickableView.performClick();
				}
			};
			logger.info("click_by_id: " + id);
			clickableView.post(runnable);
		}
		return "ok";
	}

	@HookerRequestMapping(path = "show_toast", produces = Produces.AUTO)
	public String show_toast(@HookerRequestParam(name = "text", defaultValue = "牛逼不？") String text) throws Exception {
		AndroidUI.showToast(text);
		return "ok";
	}

	@HookerRequestMapping(path = "start_activity", produces = Produces.AUTO, method = Method.POST)
	public String start_activity(@HookerRequestPostJson Map<String, Object> postJson) throws Exception {
		String className = (String) postJson.get("class_name"); // 可选，显式启动
		Map<String, Object> extrasMap = (Map<String, Object>) postJson.get("extras"); // 可选参数
		String type = (String) postJson.get("type"); // 可选 MIME type
		String action = (String) postJson.get("action"); // 可选 Action，例如 "android.intent.action.VIEW"
		String dataUri = (String) postJson.get("data_uri"); // 可选 Data URI
		Boolean browsable = (Boolean) postJson.getOrDefault("browsable", false); // 是否加 CATEGORY_BROWSABLE
		Context context = Android.getApplication();
		Intent intent;
		if (className != null && !className.isEmpty()) {
			// 显式启动
			intent = new Intent();
			intent.setClassName(context, className);
		} else {
			// 隐式启动
			intent = new Intent();
		}
		// 设置 Action
		if (action != null)
			intent.setAction(action);
		// 设置 Data
		if (dataUri != null)
			intent.setData(Uri.parse(dataUri));
		// 设置 MIME type
		if (type != null)
			intent.setType(type);
		// 设置 extras
		if (extrasMap != null) {
			Bundle bundle = new Bundle();
			for (Map.Entry<String, Object> entry : extrasMap.entrySet()) {
				Object v = entry.getValue();
				String k = entry.getKey();
				if (v instanceof String)
					bundle.putString(k, (String) v);
				else if (v instanceof Integer)
					bundle.putInt(k, (Integer) v);
				else if (v instanceof Boolean)
					bundle.putBoolean(k, (Boolean) v);
				else if (v instanceof Long)
					bundle.putLong(k, (Long) v);
				else if (v instanceof Float)
					bundle.putFloat(k, (Float) v);
				else if (v instanceof Double)
					bundle.putDouble(k, (Double) v);
				// 还可以继续支持 Parcelable / Serializable
			}
			intent.putExtras(bundle);
		}
		// 设置浏览器调用标记
		if (browsable)
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
		// FLAG: 如果不是 Activity Context，需要加 NEW_TASK
		if (!(context instanceof Activity)) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		context.startActivity(intent);
		return "ok";
	}

	private Map<String, WeakReference<View>> viewCache = new HashMap<String, WeakReference<View>>();

	private static String random3Letters() {
		String letters = "abcdefghijklmnopqrstuvwxyz";
		Random random = new Random();
		StringBuilder sb = new StringBuilder(3);
		for (int i = 0; i < 3; i++) {
			sb.append(letters.charAt(random.nextInt(letters.length())));
		}
		return sb.toString();
	}

	@HookerRequestMapping(path = "show_toast", produces = Produces.AUTO)
	public String show_toast_bak(@HookerRequestParam(name = "text", defaultValue = "牛逼不？") String text)
			throws Exception {
		AndroidUI.showToast(text);
		return "ok";
	}

	@HookerRequestMapping(path = "try_dismiss_by_dialog_fragment", produces = Produces.AUTO)
	public String tryDismissByDialogFragment() throws Exception {
		AndroidUI2.tryDismissByDialogFragment();
		return "ok";
	}

	@HookerRequestMapping(path = "lookup_important_views", produces = Produces.AUTO, method = Method.GET)
	public List<Map<String, Object>> lookup_important_views() throws Exception {
		List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
		Activity activity = Android.getTopActivity();
		View rootView = activity.getWindow().getDecorView();
		List<View> results = AndroidUI2.collectImportantViews(rootView);
		for (View view : results) {
			if (view.getVisibility() != View.VISIBLE) {
				continue;
			}
			Map<String, Object> viewInfo = new HashMap<String, Object>();
			viewInfo.put("is_focused", view.isFocused());
			viewInfo.put("class", view.getClass().getName());
			XView xView = new XView(view);
			if (view.getId() != View.NO_ID) {
				String name = activity.getResources().getResourceEntryName(view.getId());
				viewInfo.put("id", name);
			}
			String random3Letters = "hooker_" + random3Letters();
			viewCache.put(random3Letters, new WeakReference<>(view));
			viewInfo.put("hooker_id", random3Letters);
			TextView.OnClickListener onClickListener = xView.getOnClickListener();
			if (onClickListener != null) {
				viewInfo.put("on_click_listener_clazz", onClickListener.getClass().getName());
			}
			TextView.OnLongClickListener onLongClickListener = xView.getOnLongClickListener();
			if (onLongClickListener != null) {
				viewInfo.put("on_long_click_listener_clazz", onLongClickListener.getClass().getName());
			}
			if (view instanceof TextView) {
				TextView textView = (TextView) view;
				viewInfo.put("text", textView.getText().toString());
			}
			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				String hint = editText.getHint().toString();
				viewInfo.put("super_class", EditText.class.getName());
				viewInfo.put("hint_text", hint);

				TextView.OnEditorActionListener onEditorActionListener = xView.getOnEditorActionListener();
				if (onEditorActionListener != null) {
					viewInfo.put("on_editor_action_listener_clazz", onEditorActionListener.getClass().getName());
				}
				TextView.OnFocusChangeListener onFocusChangeListener = xView.getOnFocusChangeListener();
				if (onFocusChangeListener != null) {
					viewInfo.put("on_focus_change_listener_clazz", onFocusChangeListener.getClass().getName());
				}

			}
			if (view instanceof ImageView) {
				ImageView imageView = (ImageView) view;
				File tmpImageFile = new File(BuiltinFileServiceController.tempFileDir.getAbsolutePath()+ "/" + UUID.randomUUID().toString() + ".jpg");
				if (saveImageButtonToFile(imageView, tmpImageFile)) {
					viewInfo.put("imgae_url", "/file?filename="+tmpImageFile.getAbsolutePath());
				}
			}
			
			if (view instanceof ImageButton) {
				ImageButton imageButton = (ImageButton) view;
				
				
				viewInfo.put("image_button_content_description", imageButton.getContentDescription());
				
			}
			views.add(viewInfo);
		}
		return views;
	}

	@HookerRequestMapping(path = "set_text", produces = Produces.AUTO, method = Method.GET)
	public String set_text(@HookerRequestParam(name = "id") String id, @HookerRequestParam(name = "text") String text)
			throws Exception {
		View view = findViewById(id);
		if (view != null && view instanceof TextView) {
			TextView textView = (TextView) view;
			textView.post(new Runnable() {

				@Override
				public void run() {
					textView.setText(text);
				}
			});
		}
		return "ok";
	}

	@HookerRequestMapping(path = "send_search_action", produces = Produces.AUTO, method = Method.GET)
	public String send_search_action(@HookerRequestParam(name = "id") String id) throws Exception {
		View view = findViewById(id);
		if (view != null && view instanceof EditText) {
			EditText editText = (EditText) view;
			// 发送搜索事件
			editText.post(new Runnable() {
				
				@Override
				public void run() {
					try {
						editText.requestFocus();
						InputMethodManager imm = (InputMethodManager) Android.getApplication().getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
						editText.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
					} catch (Exception e) {
						logger.warn(e);
					}
					
				}
			});
		}
		return "ok";
	}

	@HookerRequestMapping(path = "focus_on", produces = Produces.AUTO, method = Method.GET)
	public Map<String, Object> focus_on(@HookerRequestParam(name = "id") String id) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		View view = findViewById(id);
		if (view == null) {
			result.put("ok", false);
			result.put("code", 404); // 找不到控件
			result.put("msg", "Not found view");
			return result;
		}
		// 把代码放到主线程里、并且会在 View 已经附着并完成布局流程之后执行
		view.post(new Runnable() {

			@Override
			public void run() {
				view.requestFocus();
			}
		});
		result.put("ok", true);
		result.put("code", 200);
		result.put("msg", "success");
		return result;
	}

	private View findViewById(String id) throws Exception {
		View view = null;
		if (id.startsWith("hooker_")) {
			view = viewCache.get(id).get();
		} else {
			view = AndroidUI.findViewByIdName(id);
		}
		return view;
	}
	
	private static boolean saveImageButtonToFile(ImageView imageView, File outFile) throws Exception {
	    Drawable drawable = imageView.getDrawable();
	    if (drawable == null) {
	        return false;
	    }
	    int w = drawable.getIntrinsicWidth();
	    int h = drawable.getIntrinsicHeight();
	    if (w >= 40 && h >= 40) {
	    	Bitmap bitmap = drawableToBitmap(drawable);
		    FileOutputStream fos = new FileOutputStream(outFile);
		    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		    fos.flush();
		    fos.close();
		    return true;
	    }
	    return false;
	}
	
	public static Bitmap drawableToBitmap(Drawable drawable) {
	    if (drawable instanceof BitmapDrawable) {
	        return ((BitmapDrawable) drawable).getBitmap();
	    }

	    int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
	    int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;

	    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap);
	    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
	    drawable.draw(canvas);
	    return bitmap;
	}



}
