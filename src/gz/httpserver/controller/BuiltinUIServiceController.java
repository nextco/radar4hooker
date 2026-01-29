package gz.httpserver.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
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
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager.widget.PagerAdapter;
import gz.com.alibaba.fastjson.JSON;
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
		AndroidUI.performClick(view);
		return "ok";
	}

	@HookerRequestMapping(path = "show_toast", produces = Produces.AUTO)
	public String show_toast(@HookerRequestParam(name = "text", defaultValue = "牛逼不？") String text) throws Exception {
		AndroidUI.showToast(text);
		return "ok";
	}
	
	@HookerRequestMapping(path = "view_page_swipe", produces = Produces.AUTO)
	public String viewPageSwipe(@HookerRequestParam(name = "id") String id, @HookerRequestParam(name = "direction", defaultValue = "next") String direction) throws Exception {
		View view = findViewById(id);
		if (Class.forName("androidx.viewpager.widget.ViewPager").isInstance(view)) {
			androidx.viewpager.widget.ViewPager viewPager = (androidx.viewpager.widget.ViewPager) view;
			if (direction.equals("next")) {
				AndroidUI.swipeToNext(viewPager);
			}else {
				AndroidUI.swipeToPrev(viewPager);
			}
		}else if (Class.forName("androidx.viewpager2.widget.ViewPager2").isInstance(view)) {
			androidx.viewpager2.widget.ViewPager2 viewPager = (androidx.viewpager2.widget.ViewPager2) view;
			viewPager.post(new Runnable() {
				@Override
				public void run() {
					int cur = viewPager.getCurrentItem();
					if (direction.equals("next")) {
						viewPager.setCurrentItem(cur - 1, true);
					}else {
						viewPager.setCurrentItem(cur + 1, true);
					}
				}
			});
		}
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

	@HookerRequestMapping(path = "try_to_dismiss_dialog", produces = Produces.AUTO)
	public String tryDismissByDialogFragment() throws Exception {
		AndroidUI2.tryDismissByDialogFragment();
		return "ok";
	}
	
	@HookerRequestMapping(path = "rv_scroll_by", produces = Produces.AUTO)
	public String rvscrollBy(@HookerRequestParam(name = "id") String id, @HookerRequestParam(name = "x") Integer x, @HookerRequestParam(name = "y") Integer y)
			throws Exception {
		androidx.recyclerview.widget.RecyclerView rv = (RecyclerView) findViewById(id);
		AndroidUI.scrollBy(rv, x, y);
		return "ok";
	}
	
	@HookerRequestMapping(path = "rv_scroll_to_position", produces = Produces.AUTO)
	public String rvscrollToPosition(@HookerRequestParam(name = "id") String id, @HookerRequestParam(name = "position", defaultValue = "0") Integer position)
			throws Exception {
		androidx.recyclerview.widget.RecyclerView rv = (RecyclerView) findViewById(id);
		AndroidUI.scrollToPosition(rv, position);
		return "ok";
	}
	
	
	@HookerRequestMapping(path = "rv_smooth_scroll_to_position", produces = Produces.AUTO)
	public String rvsmoothScrollToPosition(@HookerRequestParam(name = "id") String id, @HookerRequestParam(name = "position", defaultValue = "0") Integer position)
			throws Exception {
		androidx.recyclerview.widget.RecyclerView rv = (RecyclerView) findViewById(id);
		AndroidUI.smoothScrollToPosition(rv, position);
		return "ok";
	}

	@HookerRequestMapping(path = "lookup_important_views", produces = Produces.AUTO, method = Method.GET)
	public Object lookup_important_views(@HookerRequestParam(name = "format", defaultValue = "html") String format) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
		Activity activity = Android.getTopActivity();
		View rootView = activity.getWindow().getDecorView();
		List<View> results = AndroidUI2.collectImportantViews(rootView);
		for (View view : results) {
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
					viewInfo.put("image_url", "/file?filename="+tmpImageFile.getAbsolutePath());
				}
			}
			
			if (view instanceof ImageButton) {
				ImageButton imageButton = (ImageButton) view;
				viewInfo.put("image_button_content_description", imageButton.getContentDescription());
			}
			
			if (AndroidUI2.isViewPager(view)) {
				androidx.viewpager.widget.ViewPager viewPager = (androidx.viewpager.widget.ViewPager) view;
				viewInfo.put("view_page_current_item", viewPager.getCurrentItem());
				viewInfo.put("view_page_is_enabled", viewPager.isEnabled());
				viewInfo.put("view_page_super_class", "androidx.viewpager.widget.ViewPager");
				PagerAdapter adapter = viewPager.getAdapter();
				int count = adapter != null ? adapter.getCount() : 0;
				viewInfo.put("view_page_count", count);
			}
			
			if (AndroidUI2.isViewPager2(view)) {
				androidx.viewpager2.widget.ViewPager2 viewPager = (androidx.viewpager2.widget.ViewPager2) view;
				viewInfo.put("view_page_current_item", viewPager.getCurrentItem());
				viewInfo.put("view_page_is_enabled", viewPager.isEnabled());
				viewInfo.put("view_page_super_class", "androidx.viewpager2.widget.ViewPager2");
			}
			
			if (AndroidUI2.isRecyclerView(view)) {
				androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) view;
				RecyclerView.Adapter adapter = rv.getAdapter();
				if (adapter != null) {
				    String adapterClass = adapter.getClass().getName();
				    int itemCount = adapter.getItemCount();

				    // 非常重要的信息
				    viewInfo.put("adapter_class", adapterClass);
				    viewInfo.put("item_count", itemCount);
				}
				int childCount = rv.getChildCount();
				viewInfo.put("visible_child_count", childCount);
				
				RecyclerView.LayoutManager lm = rv.getLayoutManager();
				if (lm != null) {
				    String lmClass = lm.getClass().getName();
				    viewInfo.put("layout_manager", lmClass);

				    if (lm instanceof LinearLayoutManager) {
				        LinearLayoutManager llm = (LinearLayoutManager) lm;
				        viewInfo.put("orientation",
				                llm.getOrientation() == LinearLayoutManager.VERTICAL ? "vertical" : "horizontal");
				        viewInfo.put("first_visible_position", llm.findFirstVisibleItemPosition());
				        viewInfo.put("last_visible_position", llm.findLastVisibleItemPosition());
				    } else if (lm instanceof GridLayoutManager) {
				        GridLayoutManager glm = (GridLayoutManager) lm;
				        viewInfo.put("span_count", glm.getSpanCount());
				        viewInfo.put("first_visible_position", glm.findFirstVisibleItemPosition());
				        viewInfo.put("last_visible_position", glm.findLastVisibleItemPosition());
				    } else if (lm instanceof StaggeredGridLayoutManager) {
				        StaggeredGridLayoutManager sgm = (StaggeredGridLayoutManager) lm;
				        int[] first = sgm.findFirstVisibleItemPositions(null);
				        int[] last = sgm.findLastVisibleItemPositions(null);
				        viewInfo.put("first_visible_positions", Arrays.toString(first));
				        viewInfo.put("last_visible_positions", Arrays.toString(last));
				    }
				}

			}
			
			if (view instanceof CheckBox) {
				CheckBox checkBox = (CheckBox) view;
				// 1. 状态信息
			    boolean isChecked = checkBox.isChecked();
			    boolean isEnabled = checkBox.isEnabled();
			    boolean isClickable = checkBox.isClickable();

			    viewInfo.put("type", "CheckBox");
			    viewInfo.put("checked", isChecked);
			    viewInfo.put("enabled", isEnabled);
			    viewInfo.put("clickable", isClickable);

			    // 2. 文本信息（很多 CheckBox 都有文字）
			    CharSequence text = checkBox.getText();
			    if (text != null) {
			        viewInfo.put("text", text.toString());
			    }

			    // 3. contentDescription（有些 App 靠这个表达语义）
			    CharSequence cd = checkBox.getContentDescription();
			    if (cd != null) {
			        viewInfo.put("content_description", cd.toString());
			    }

			    // 4. 是否有监听器（逆向时非常重要）

			    CompoundButton.OnCheckedChangeListener onCheckedChangeListener =
			            xView.getOnCheckedChangeListener();
			    if (onCheckedChangeListener != null) {
			        viewInfo.put("on_checked_change_listener_clazz",
			                onCheckedChangeListener.getClass().getName());
			    }
			}
			
			if (view instanceof RadioButton) {
				
			}
			
			views.add(viewInfo);
		}
		if (format.equals("html")) {
			//将views转成html
			return renderViewsHtml(activity, views);
		}else {
			result.put("top_activity", activity.getClass().getName());
			result.put("title", activity.getTitle());
			result.put("views", views);
			return result;
		}
	}
	
	private String renderViewsHtml(Activity activity, List<Map<String, Object>> views) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("<html><head><meta charset='utf-8'/>")
	      .append("<style>")
	      .append("body{font-family:monospace;padding:12px}")
	      .append(".row{padding:8px 6px;border-bottom:1px solid #ddd;display:flex;gap:10px;align-items:flex-start}")
	      .append(".img{width:64px;height:64px;object-fit:contain;border:1px solid #ccc;border-radius:6px;background:#fafafa}")
	      .append(".kv{white-space:pre-wrap;word-break:break-word}")
	      .append(".tag{display:inline-block;padding:1px 6px;border:1px solid #bbb;border-radius:10px;margin-right:6px;font-size:12px}")
	      .append(".json{white-space:pre-wrap;word-break:break-word;font-size:12px;opacity:0.9;border:1px dashed #ccc;padding:6px;border-radius:6px;max-width:520px}")
	      .append("</style></head><body>");

	    sb.append("<h3>TopActivity: ")
	      .append(escapeHtml(activity.getClass().getName()))
	      .append(" | Title: ")
	      .append(escapeHtml(String.valueOf(activity.getTitle())))
	      .append(" | Views: ")
	      .append(views.size())
	      .append("</h3>");

	    for (Map<String, Object> v : views) {
	        String clazz = asString(v.get("class"));
	        String id = asString(v.get("id"));
	        String hookerId = asString(v.get("hooker_id"));
	        String text = asString(v.get("text"));
	        String hint = asString(v.get("hint_text"));
	        String focused = String.valueOf(v.get("is_focused"));

	        String clickL = asString(v.get("on_click_listener_clazz"));
	        String longClickL = asString(v.get("on_long_click_listener_clazz"));
	        String editorL = asString(v.get("on_editor_action_listener_clazz"));
	        String focusL = asString(v.get("on_focus_change_listener_clazz"));
	        String cd = asString(v.get("image_button_content_description"));
	        String imageUrl = asString(v.get("image_url"));

	        sb.append("<div class='row'>");

	        // JSON 展示（一定要转义）
	        String jsonText = JSON.toJSONString(v);
	        sb.append("<pre class='json'>").append(escapeHtml(jsonText)).append("</pre>");
	        
	        // 图片展示（用统一 img 样式类）
	        if (imageUrl != null && !imageUrl.isEmpty()) {
	            sb.append("<a href='").append(escapeHtml(imageUrl)).append("' target='_blank'>")
	              .append("<img class='img' src='").append(escapeHtml(imageUrl)).append("'/></a>");
	        } else {
	            sb.append("<div class='img'></div>");
	        }

	        sb.append("<div class='kv'>")
	          .append("<span class='tag'>focused=").append(escapeHtml(focused)).append("</span>")
	          .append("<span class='tag'>hooker_id=").append(escapeHtml(hookerId)).append("</span>")
	          .append("<div><b>class</b>: ").append(escapeHtml(clazz)).append("</div>");

	        if (id != null && !id.isEmpty()) sb.append("<div><b>id</b>: ").append(escapeHtml(id)).append("</div>");
	        if (text != null && !text.isEmpty()) sb.append("<div><b>text</b>: ").append(escapeHtml(text)).append("</div>");
	        if (hint != null && !hint.isEmpty()) sb.append("<div><b>hint</b>: ").append(escapeHtml(hint)).append("</div>");
	        if (cd != null && !cd.isEmpty()) sb.append("<div><b>contentDesc</b>: ").append(escapeHtml(cd)).append("</div>");

	        if (clickL != null && !clickL.isEmpty()) sb.append("<div><b>onClick</b>: ").append(escapeHtml(clickL)).append("</div>");
	        if (longClickL != null && !longClickL.isEmpty()) sb.append("<div><b>onLongClick</b>: ").append(escapeHtml(longClickL)).append("</div>");
	        if (editorL != null && !editorL.isEmpty()) sb.append("<div><b>onEditorAction</b>: ").append(escapeHtml(editorL)).append("</div>");
	        if (focusL != null && !focusL.isEmpty()) sb.append("<div><b>onFocusChange</b>: ").append(escapeHtml(focusL)).append("</div>");

	        sb.append("</div></div>");
	    }

	    sb.append("</body></html>");
	    return sb.toString();
	}

	private String asString(Object o) {
	    return o == null ? "" : String.valueOf(o);
	}

	private String escapeHtml(String s) {
	    if (s == null) return "";
	    return s.replace("&", "&amp;")
	            .replace("<", "&lt;")
	            .replace(">", "&gt;")
	            .replace("\"", "&quot;")
	            .replace("'", "&#39;");
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
