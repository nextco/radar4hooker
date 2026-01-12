package gz.httpserver.controller;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.annotation.HookerRequestPostJson;
import gz.radar.Android;
import gz.radar.AndroidUI;
import gz.util.Logger;
import gz.util.XView;

@HookerController("/hooker/ui/")
public class BuiltinUIServiceController {
	
	private Logger logger = new Logger(BuiltinUIServiceController.class);

	@HookerRequestMapping(path="finish_current_activity", produces = Produces.AUTO)
	public String finish_current_activity() throws Exception {
		AndroidUI.finishCurrentActivity();
		return "ok";
	}
	
	@HookerRequestMapping(path="back", produces = Produces.AUTO)
	public String back() throws Exception {
		AndroidUI.back();
		return "ok";
	}
	
	@HookerRequestMapping(path="home", produces = Produces.AUTO)
	public String home() throws Exception {
		AndroidUI.home();
		return "ok";
	}
	
	@HookerRequestMapping(path="click_by_text", produces = Produces.AUTO)
	public String click_by_text(@HookerRequestParam(name = "text") String text, @HookerRequestParam(name = "text_equeal", defaultValue = "false") boolean mustBeTextEqueal, @HookerRequestParam(name = "visible", defaultValue = "false") boolean mustBeVisible) throws Exception {
		AndroidUI.clickByText(text, mustBeTextEqueal, mustBeVisible);
		return "ok";
	}
	
	@HookerRequestMapping(path="click_by_id", produces = Produces.AUTO)
	public String click_by_id(@HookerRequestParam(name = "id") int id) throws Exception {
		AndroidUI.clickById(id);
		return "ok";
	}
	
	
	@HookerRequestMapping(path="show_toast", produces = Produces.AUTO)
	public String show_toast(@HookerRequestParam(name = "text", defaultValue = "牛逼不？") String text) throws Exception {
		AndroidUI.showToast(text);
		return "ok";
	}
	
	@HookerRequestMapping(path="start_activity", produces = Produces.AUTO, method = Method.POST)
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
	    if (action != null) intent.setAction(action);
	    // 设置 Data
	    if (dataUri != null) intent.setData(Uri.parse(dataUri));
	    // 设置 MIME type
	    if (type != null) intent.setType(type);
	    // 设置 extras
	    if (extrasMap != null) {
	        Bundle bundle = new Bundle();
	        for (Map.Entry<String, Object> entry : extrasMap.entrySet()) {
	            Object v = entry.getValue();
	            String k = entry.getKey();
	            if (v instanceof String) bundle.putString(k, (String)v);
	            else if (v instanceof Integer) bundle.putInt(k, (Integer)v);
	            else if (v instanceof Boolean) bundle.putBoolean(k, (Boolean)v);
	            else if (v instanceof Long) bundle.putLong(k, (Long)v);
	            else if (v instanceof Float) bundle.putFloat(k, (Float)v);
	            else if (v instanceof Double) bundle.putDouble(k, (Double)v);
	            // 还可以继续支持 Parcelable / Serializable
	        }
	        intent.putExtras(bundle);
	    }
	    // 设置浏览器调用标记
	    if (browsable) intent.addCategory(Intent.CATEGORY_BROWSABLE);
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
	
	@HookerRequestMapping(path="lookup_important_views", produces = Produces.AUTO, method = Method.GET)
	public List<Map<String, Object>> lookup_important_views() throws Exception {
		List<Map<String, Object>> views = new ArrayList<Map<String,Object>>();
		Activity activity = Android.getTopActivity();
		View rootView = activity.getWindow().getDecorView();
		List<View> results = collectImportantViews(rootView);
		for (View view : results) {
			Map<String, Object> viewInfo = new HashMap<String, Object>();
			XView xView = new XView(view);
			if (view.getId() != View.NO_ID) {
				String name = activity.getResources().getResourceEntryName(view.getId());
				viewInfo.put("id", name);
			}else {
				String random3Letters = random3Letters();
				viewCache.put(random3Letters, new WeakReference<>(view));
				viewInfo.put("id", random3Letters);
			}
			TextView.OnClickListener onClickListener = xView.getOnClickListener();
			if (onClickListener != null) {
				viewInfo.put("onClickListenerClazz", onClickListener.getClass().getName());
			}
			TextView.OnLongClickListener onLongClickListener = xView.getOnLongClickListener();
			if (onLongClickListener != null) {
				viewInfo.put("onLongClickListenerClazz", onLongClickListener.getClass().getName());
			}
			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				String textViewText = editText.getText().toString().trim();
				viewInfo.put("text", textViewText);
				TextView.OnEditorActionListener onEditorActionListener = xView.getOnEditorActionListener();
				if (onEditorActionListener != null) {
					viewInfo.put("onEditorActionListenerClazz", onEditorActionListener.getClass().getName());
				}
				TextView.OnFocusChangeListener onFocusChangeListener = xView.getOnFocusChangeListener();
				if (onFocusChangeListener != null) {
					viewInfo.put("onFocusChangeListenerClazz", onFocusChangeListener.getClass().getName());
				}
				
			}
			views.add(viewInfo);
		}
		return views;
	}
	
	@HookerRequestMapping(path="focus_on", produces = Produces.AUTO, method = Method.GET)
	public Map<String, Object> focus_on(@HookerRequestParam(name = "id") String id) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		View view = AndroidUI.findViewByIdName(id);
		if (view == null) {
			result.put("ok", false);
			result.put("code", 404); //找不到控件
			result.put("msg", "Not found view");
			return result;
		}
		//把代码放到主线程里、并且会在 View 已经附着并完成布局流程之后执行
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
	
	
	
	public static List<View> collectImportantViews(View root) {
	    List<View> result = new ArrayList<>();
	    traverse(root, result);
	    return result;
	}

	private static void traverse(View v, List<View> out) {
	    if (v == null) return;
	    if (v.getVisibility() != View.VISIBLE) return;
	    // ===== 重要控件判定 =====
	    if (v instanceof Button
	            || v instanceof TextView
	            || v instanceof ImageView
	            || v instanceof ImageButton
	            || v instanceof CheckBox
	            || v instanceof SeekBar
	            || isSwitch(v)
	            || isViewPager(v)
	            || isRecyclerView(v)
	            || isWebView(v)) {

	        out.add(v);
	    }

	    // ===== 继续递归子 View =====
	    if (v instanceof ViewGroup) {
	        ViewGroup group = (ViewGroup) v;
	        for (int i = 0; i < group.getChildCount(); i++) {
	            traverse(group.getChildAt(i), out);
	        }
	    }
	}
	
	private static boolean isSwitch(View v) {
	    try {
	        if (v instanceof android.widget.Switch) return true;
	        return Class.forName("androidx.appcompat.widget.SwitchCompat").isInstance(v);
	    } catch (Throwable ignore) {}
	    return false;
	}
	
	private static boolean isViewPager(View v) {
	    try {
	        return Class.forName("androidx.viewpager.widget.ViewPager").isInstance(v)
	                || Class.forName("androidx.viewpager2.widget.ViewPager2").isInstance(v);
	    } catch (Throwable ignore) {}
	    return false;
	}
	
	private static boolean isRecyclerView(View v) {
	    try {
	        return Class.forName("androidx.recyclerview.widget.RecyclerView").isInstance(v);
	    } catch (Throwable ignore) {}
	    return false;
	}

	private static boolean isWebView(View v) {
	    if (v instanceof android.webkit.WebView) return true;

	    // 兼容某些 App 的自定义 WebView（抖音/微信等）
	    try {
	        if (v.getClass().getName().toLowerCase().contains("webview")) {
	            return true;
	        }
	    } catch (Throwable ignore) {}
	    return false;
	}

	
}
