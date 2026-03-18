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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VideoView;
import android.webkit.WebView;
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
	
	@HookerRequestMapping(path = "set_checked", produces = Produces.AUTO)
	public String set_checked(@HookerRequestParam(name = "id") String id)
			throws Exception {
		ToggleButton tb  = (ToggleButton) findViewById(id);
		tb.post(new Runnable() {
			
			@Override
			public void run() {
				boolean checked = tb.isChecked();
				tb.setChecked(!checked);
			}
		});
		return "ok";
	}
	
	@HookerRequestMapping(path = "set_progress", produces = Produces.AUTO)
	public String set_progress(@HookerRequestParam(name = "id") String id, @HookerRequestParam(name = "progress", defaultValue = "0") Integer progress)
			throws Exception {
		SeekBar seekbar  = (SeekBar) findViewById(id);
		seekbar.post(new Runnable() {
			
			@Override
			public void run() {
				seekbar.setProgress(progress);
			}
		});
		return "ok";
	}

	@HookerRequestMapping(path = "inspect", produces = Produces.AUTO, method = Method.GET)
	public Object inspect(@HookerRequestParam(name = "format", defaultValue = "html") String format) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
		Activity activity = Android.getTopActivity();
		View rootView = activity.getWindow().getDecorView();
		List<View> results = AndroidUI2.collectImportantViews(rootView);
		for (View view : results) {
			Map<String, Object> viewInfo = new HashMap<String, Object>();
			viewInfo.put("is_focused", view.isFocused());
			viewInfo.put("class", view.getClass().getName());
			viewInfo.put("visibility", getVisibilityName(view.getVisibility()));
			viewInfo.put("is_enabled", view.isEnabled());
			viewInfo.put("is_clickable", view.isClickable());
			viewInfo.put("is_long_clickable", view.isLongClickable());
			viewInfo.put("is_selected", view.isSelected());
			viewInfo.put("is_shown", view.isShown());
			viewInfo.put("alpha", view.getAlpha());
			viewInfo.put("width", view.getWidth());
			viewInfo.put("height", view.getHeight());
			viewInfo.put("x", view.getX());
			viewInfo.put("y", view.getY());
			viewInfo.put("content_description", safeToString(view.getContentDescription()));
			viewInfo.put("parent_class", view.getParent() == null ? null : view.getParent().getClass().getName());
			XView xView = new XView(view);
			if (view.getId() != View.NO_ID) {
				viewInfo.put("id", resolveViewIdName(activity, view.getId()));
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
			if (view instanceof Button) {
				Button button = (Button) view;
				viewInfo.put("type", "Button");
				viewInfo.put("button_text", safeToString(button.getText()));
			}
			if (view instanceof EditText) {
				EditText editText = (EditText) view;
				String hint = safeToString(editText.getHint());
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
				try {
					if (saveImageButtonToFile(imageView, tmpImageFile)) {
						viewInfo.put("image_url", "/file?filename="+tmpImageFile.getAbsolutePath());
					}
				} catch (Exception e) {
					logger.warn(e);
				}
			}
			
			if (view instanceof ImageButton) {
				ImageButton imageButton = (ImageButton) view;
				viewInfo.put("image_button_content_description", imageButton.getContentDescription());
			}

			if (view instanceof ProgressBar) {
				ProgressBar progressBar = (ProgressBar) view;
				viewInfo.put("progress_bar_progress", progressBar.getProgress());
				viewInfo.put("progress_bar_max", progressBar.getMax());
				viewInfo.put("progress_bar_indeterminate", progressBar.isIndeterminate());
			}

			if (view instanceof RatingBar) {
				RatingBar ratingBar = (RatingBar) view;
				viewInfo.put("rating", ratingBar.getRating());
				viewInfo.put("rating_max", ratingBar.getMax());
				viewInfo.put("rating_num_stars", ratingBar.getNumStars());
				viewInfo.put("rating_step_size", ratingBar.getStepSize());
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

			if (view instanceof Spinner) {
				Spinner spinner = (Spinner) view;
				viewInfo.put("type", "Spinner");
				viewInfo.put("spinner_selected_position", spinner.getSelectedItemPosition());
				viewInfo.put("spinner_selected_item", safeToString(spinner.getSelectedItem()));
				viewInfo.put("spinner_prompt", safeToString(spinner.getPrompt()));
				if (spinner.getAdapter() != null) {
					viewInfo.put("spinner_adapter_class", spinner.getAdapter().getClass().getName());
					viewInfo.put("spinner_item_count", spinner.getAdapter().getCount());
				}
			}

			if (view instanceof ListView) {
				ListView listView = (ListView) view;
				viewInfo.put("type", "ListView");
				fillAdapterViewInfo(viewInfo, listView.getAdapter());
				viewInfo.put("first_visible_position", listView.getFirstVisiblePosition());
				viewInfo.put("last_visible_position", listView.getLastVisiblePosition());
				viewInfo.put("checked_item_position", listView.getCheckedItemPosition());
			}

			if (view instanceof GridView) {
				GridView gridView = (GridView) view;
				viewInfo.put("type", "GridView");
				fillAdapterViewInfo(viewInfo, gridView.getAdapter());
				viewInfo.put("first_visible_position", gridView.getFirstVisiblePosition());
				viewInfo.put("last_visible_position", gridView.getLastVisiblePosition());
				viewInfo.put("grid_num_columns", gridView.getNumColumns());
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
				RadioButton rb = (RadioButton) view;

			    viewInfo.put("type", "RadioButton");
			    viewInfo.put("checked", rb.isChecked());
			    viewInfo.put("enabled", rb.isEnabled());
			    viewInfo.put("clickable", rb.isClickable());

			    // 文本
			    CharSequence text = rb.getText();
			    if (text != null) {
			        viewInfo.put("text", text.toString());
			    }

			    // 语义描述
			    CharSequence cd = rb.getContentDescription();
			    if (cd != null) {
			        viewInfo.put("content_description", cd.toString());
			    }

			    // RadioGroup 信息（非常重要）
			    ViewParent parent = rb.getParent();
			    if (parent instanceof RadioGroup) {
			        RadioGroup group = (RadioGroup) parent;
			        viewInfo.put("radio_group_id", group.getId());
			        viewInfo.put("radio_group_class", group.getClass().getName());

			        // 当前组中选中的 RadioButton
			        int checkedId = group.getCheckedRadioButtonId();
			        viewInfo.put("radio_group_checked_id", checkedId);

			        // 组内 RadioButton 数量
			        int count = group.getChildCount();
			        viewInfo.put("radio_group_child_count", count);
			    }

			    CompoundButton.OnCheckedChangeListener checkedListener =
			            xView.getOnCheckedChangeListener();
			    if (checkedListener != null) {
			        viewInfo.put("on_checked_change_listener_clazz",
			                checkedListener.getClass().getName());
			    }
			}
			
			if (view instanceof ToggleButton) {
				ToggleButton toggleButton = (ToggleButton) view;
				 // 当前状态
			    boolean checked = toggleButton.isChecked();
			    viewInfo.put("checked", checked);
			    // 文本
			    CharSequence textOn = toggleButton.getTextOn();
			    viewInfo.put("textOn", textOn);
			    CharSequence textOff = toggleButton.getTextOff();
			    viewInfo.put("textOff", textOff);
			    CharSequence currentText = toggleButton.getText();
			    viewInfo.put("currentText", currentText);
			    CompoundButton.OnCheckedChangeListener checkedListener =
			            xView.getOnCheckedChangeListener();
			    if (checkedListener != null) {
			        viewInfo.put("on_checked_change_listener_clazz",
			                checkedListener.getClass().getName());
			    }
			}

			fillSwitchLikeInfo(viewInfo, view, xView);
			
			if (view instanceof SeekBar) {
				SeekBar seekBar = (SeekBar) view;
				int progress = seekBar.getProgress();     // 当前值
			    int max = seekBar.getMax();               // 最大值
			    int min = 0;                              // 老版本 Android 默认是 0
			    viewInfo.put("seek_bar_progress", progress);
			    viewInfo.put("seek_bar_max", max);
			    viewInfo.put("seek_bar_min", min);
			    OnSeekBarChangeListener onSeekBarChangeListener = xView.getSeekBarListener();
			    if (onSeekBarChangeListener != null) {
			    	viewInfo.put("on_seek_bar_change_listener_clazz",
			    			onSeekBarChangeListener.getClass().getName());
			    }
			}

			if (view instanceof ScrollView) {
				ScrollView scrollView = (ScrollView) view;
				viewInfo.put("type", "ScrollView");
				viewInfo.put("scroll_y", scrollView.getScrollY());
				viewInfo.put("scroll_x", scrollView.getScrollX());
			}

			if (view instanceof HorizontalScrollView) {
				HorizontalScrollView horizontalScrollView = (HorizontalScrollView) view;
				viewInfo.put("type", "HorizontalScrollView");
				viewInfo.put("scroll_y", horizontalScrollView.getScrollY());
				viewInfo.put("scroll_x", horizontalScrollView.getScrollX());
			}

				if (view instanceof WebView) {
					WebView webView = (WebView) view;
					viewInfo.put("type", "WebView");
					fillWebViewInfo(viewInfo, webView);
				}

			if (view instanceof VideoView) {
				VideoView videoView = (VideoView) view;
				viewInfo.put("type", "VideoView");
				viewInfo.put("video_current_position", videoView.getCurrentPosition());
				viewInfo.put("video_duration", videoView.getDuration());
				viewInfo.put("video_is_playing", videoView.isPlaying());
			}

			if (view instanceof TextureView) {
				TextureView textureView = (TextureView) view;
				viewInfo.put("type", "TextureView");
				viewInfo.put("texture_is_available", textureView.isAvailable());
			}

			if (view instanceof ViewStub) {
				ViewStub viewStub = (ViewStub) view;
				viewInfo.put("type", "ViewStub");
				viewInfo.put("layout_resource", viewStub.getLayoutResource());
				viewInfo.put("inflated_id", viewStub.getInflatedId());
			}

			fillReflectiveViewInfo(viewInfo, view);

			if (view instanceof ViewGroup) {
				ViewGroup viewGroup = (ViewGroup) view;
				viewInfo.put("child_count", viewGroup.getChildCount());
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
	    int clickableCount = 0;
	    int focusedCount = 0;
	    int imageCount = 0;
	    for (Map<String, Object> view : views) {
	    	if (Boolean.TRUE.equals(view.get("is_clickable"))) {
	    		clickableCount++;
	    	}
	    	if (Boolean.TRUE.equals(view.get("is_focused"))) {
	    		focusedCount++;
	    	}
	    	if (!asString(view.get("image_url")).isEmpty()) {
	    		imageCount++;
	    	}
	    }
	    sb.append("<html><head><meta charset='utf-8'/>")
	      .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
	      .append("<style>")
	      .append("body{margin:0;background:#f4f6f8;color:#18212b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}")
	      .append(".page{max-width:1200px;margin:0 auto;padding:18px}")
	      .append(".hero{background:linear-gradient(135deg,#0f172a,#1d4ed8);color:#fff;border-radius:18px;padding:20px 22px;box-shadow:0 20px 40px rgba(15,23,42,.18)}")
	      .append(".hero h1{margin:0 0 8px;font-size:24px}")
	      .append(".hero-sub{opacity:.85;font-size:13px;line-height:1.6}")
	      .append(".stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin-top:16px}")
	      .append(".stat{background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.16);border-radius:14px;padding:12px 14px}")
	      .append(".stat-label{font-size:12px;opacity:.8;text-transform:uppercase;letter-spacing:.08em}")
	      .append(".stat-value{margin-top:6px;font-size:22px;font-weight:700}")
	      .append(".views{display:grid;gap:14px;margin-top:18px}")
	      .append(".card{background:#fff;border:1px solid #dde3ea;border-radius:18px;box-shadow:0 10px 24px rgba(15,23,42,.06);overflow:hidden}")
	      .append(".card-head{display:flex;justify-content:space-between;gap:10px;align-items:flex-start;padding:16px 18px 10px;border-bottom:1px solid #eef2f6}")
	      .append(".card-title{min-width:0}")
	      .append(".card-title h2{margin:0;font-size:16px;line-height:1.4;word-break:break-word}")
	      .append(".card-sub{margin-top:6px;color:#526071;font-size:12px;word-break:break-word}")
	      .append(".badges{text-align:right}")
	      .append(".badge{display:inline-block;padding:4px 10px;border-radius:999px;font-size:12px;font-weight:600;margin-left:6px;margin-bottom:6px}")
	      .append(".badge-blue{background:#dbeafe;color:#1d4ed8}")
	      .append(".badge-green{background:#dcfce7;color:#15803d}")
	      .append(".badge-amber{background:#fef3c7;color:#b45309}")
	      .append(".badge-slate{background:#e2e8f0;color:#334155}")
	      .append(".card-body{padding:16px 18px 18px}")
	      .append(".content-grid{display:grid;grid-template-columns:minmax(0,1.25fr) minmax(320px,.95fr);gap:14px;align-items:start}")
	      .append(".left-col,.right-col{min-width:0}")
	      .append(".topline{display:grid;grid-template-columns:minmax(0,1fr) 104px;gap:16px;align-items:start}")
	      .append(".preview{width:104px;height:104px;border-radius:14px;border:1px solid #d7dee7;background:#f8fafc;display:flex;align-items:center;justify-content:center;overflow:hidden}")
	      .append(".preview img{width:100%;height:100%;object-fit:contain;background:#fff}")
	      .append(".empty-preview{font-size:12px;color:#94a3b8;text-align:center;padding:8px}")
	      .append(".primary{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:8px 18px}")
	      .append(".field{min-width:0}")
	      .append(".label{display:block;font-size:11px;font-weight:700;letter-spacing:.08em;color:#64748b;text-transform:uppercase;margin-bottom:4px}")
	      .append(".value{font-size:14px;line-height:1.5;word-break:break-word}")
	      .append(".sections{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;margin-top:14px}")
	      .append(".panel{background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:12px 14px}")
	      .append(".panel h3{margin:0 0 10px;font-size:13px;color:#0f172a}")
	      .append(".meta-line{margin:0 0 8px;font-size:13px;line-height:1.5;word-break:break-word}")
	      .append(".action-stack{display:grid;gap:10px;margin-top:14px}")
	      .append(".action-bar{display:flex;justify-content:flex-end;gap:10px;flex-wrap:wrap}")
	      .append(".action-btn{border:none;border-radius:12px;padding:10px 14px;font-size:13px;font-weight:700;cursor:pointer;transition:transform .15s ease,box-shadow .15s ease}")
	      .append(".action-btn:hover{transform:translateY(-1px);box-shadow:0 8px 18px rgba(29,78,216,.18)}")
	      .append(".action-btn:disabled{opacity:.45;cursor:not-allowed;transform:none;box-shadow:none}")
	      .append(".click-btn{background:#1d4ed8;color:#fff}")
	      .append(".search-btn{background:#0f766e;color:#fff}")
	      .append(".text-form{display:grid;grid-template-columns:minmax(0,1fr) auto auto;gap:10px;align-items:center}")
	      .append(".text-input{width:100%;border:1px solid #cbd5e1;border-radius:12px;padding:10px 12px;font-size:13px;background:#fff;color:#0f172a}")
	      .append(".text-input:focus{outline:none;border-color:#1d4ed8;box-shadow:0 0 0 3px rgba(29,78,216,.12)}")
	      .append(".raw-panel{background:#0f172a;color:#dbeafe;border:1px solid #1e293b;border-radius:14px;padding:12px 14px}")
	      .append(".raw-panel h3{margin:0 0 10px;font-size:13px;color:#bfdbfe}")
	      .append(".raw-grid{display:grid;gap:8px}")
	      .append(".raw-item{padding:8px 10px;background:rgba(15,23,42,.38);border:1px solid rgba(148,163,184,.18);border-radius:10px}")
	      .append(".raw-key{display:block;font-size:11px;font-weight:700;letter-spacing:.08em;color:#93c5fd;text-transform:uppercase;margin-bottom:4px}")
	      .append(".raw-value{font-size:12px;line-height:1.55;word-break:break-word;white-space:pre-wrap}")
	      .append(".raw-empty{font-size:12px;color:#94a3b8}")
	      .append(".action-result{font-size:12px;text-align:right;color:#64748b;min-height:18px;margin-top:8px}")
	      .append("@media (max-width:900px){.content-grid{grid-template-columns:1fr}}")
	      .append("@media (max-width:640px){.text-form{grid-template-columns:1fr}.action-bar{justify-content:stretch}.action-btn{width:100%}}")
	      .append("@media (max-width:700px){.card-head{display:block}.badges{text-align:left;margin-top:10px}.topline{grid-template-columns:1fr}.preview{width:100%;height:160px}}")
	      .append("</style>")
	      .append("<script>")
	      .append("function clickView(id,btn){if(!id||!btn){return;}var card=btn.closest('.card');var result=card?card.querySelector('.action-result'):null;btn.disabled=true;if(result){result.textContent='clicking...';}fetch('/hooker/ui/click_by_id?id='+encodeURIComponent(id)).then(function(resp){return resp.text();}).then(function(text){if(result){result.textContent='click result: '+text;}}).catch(function(err){if(result){result.textContent='click failed: '+err;}}).finally(function(){btn.disabled=false;});}")
	      .append("function setTextValue(id,btn){if(!id||!btn){return;}var card=btn.closest('.card');var result=card?card.querySelector('.action-result'):null;var input=card?card.querySelector('.text-input'):null;if(!input){return;}btn.disabled=true;if(result){result.textContent='setting text...';}fetch('/hooker/ui/set_text?id='+encodeURIComponent(id)+'&text='+encodeURIComponent(input.value||'')).then(function(resp){return resp.text();}).then(function(text){if(result){result.textContent='set text result: '+text;}}).catch(function(err){if(result){result.textContent='set text failed: '+err;}}).finally(function(){btn.disabled=false;});}")
	      .append("function sendSearchAction(id,btn){if(!id||!btn){return;}var card=btn.closest('.card');var result=card?card.querySelector('.action-result'):null;btn.disabled=true;if(result){result.textContent='sending search...';}fetch('/hooker/ui/send_search_action?id='+encodeURIComponent(id)).then(function(resp){return resp.text();}).then(function(text){if(result){result.textContent='search action result: '+text;}}).catch(function(err){if(result){result.textContent='search action failed: '+err;}}).finally(function(){btn.disabled=false;});}")
	      .append("</script></head><body><div class='page'>");

	    sb.append("<section class='hero'>")
	      .append("<h1>UI Inspect</h1>")
	      .append("<div class='hero-sub'><div><b>Top Activity</b>: ")
	      .append(escapeHtml(activity.getClass().getName()))
	      .append("</div><div><b>Title</b>: ")
	      .append(escapeHtml(String.valueOf(activity.getTitle())))
	      .append("</div></div>")
	      .append("<div class='stats'>")
	      .append(renderStat("Views", String.valueOf(views.size())))
	      .append(renderStat("Clickable", String.valueOf(clickableCount)))
	      .append(renderStat("Focused", String.valueOf(focusedCount)))
	      .append(renderStat("Images", String.valueOf(imageCount)))
	      .append("</div></section>");

	    sb.append("<section class='views'>");
	    for (Map<String, Object> v : views) {
	    	String clazz = asString(v.get("class"));
	    	String type = asString(v.get("type"));
	    	String title = !type.isEmpty() ? type : simpleClassName(clazz);
	    	String id = asString(v.get("id"));
	    	String hookerId = asString(v.get("hooker_id"));
	    	String text = asString(v.get("text"));
	    	String hint = asString(v.get("hint_text"));
	    	String superClass = asString(v.get("super_class"));
	    	String contentDescription = asString(v.get("content_description"));
	    	String imageButtonContentDescription = asString(v.get("image_button_content_description"));
	    	String listenerSummary = joinNonEmpty(", ",
	    			asString(v.get("on_click_listener_clazz")),
	    			asString(v.get("on_long_click_listener_clazz")),
	    			asString(v.get("on_editor_action_listener_clazz")),
	    			asString(v.get("on_focus_change_listener_clazz")),
	    			asString(v.get("on_checked_change_listener_clazz")),
	    			asString(v.get("on_seek_bar_change_listener_clazz")));
	    	String imageUrl = asString(v.get("image_url"));
	    	String statusBadge = asString(v.get("visibility"));
	    	if (statusBadge.isEmpty()) {
	    		statusBadge = "UNKNOWN";
	    	}
	    	boolean isClickable = Boolean.TRUE.equals(v.get("is_clickable"));
	    	boolean isEditText = EditText.class.getName().equals(superClass) || EditText.class.getName().equals(clazz);
	    	boolean isImageView = viewClassMatches(clazz, ImageView.class) || viewClassMatches(clazz, ImageButton.class);
	    	boolean isTextView = viewClassMatches(clazz, TextView.class);
	    	sb.append("<article class='card'>")
	    	  .append("<div class='card-head'><div class='card-title'>")
	    	  .append("<h2>").append(escapeHtml(title)).append("</h2>")
	    	  .append("<div class='card-sub'>").append(escapeHtml(clazz)).append("</div>");
	    	if (!id.isEmpty()) {
	    		sb.append("<div class='card-sub'>id: ").append(escapeHtml(id)).append("</div>");
	    	}
	    	sb.append("</div><div class='badges'>")
		    	  .append(renderBadge(statusBadge, "badge-blue"))
		    	  .append(renderBooleanBadge("focused", Boolean.TRUE.equals(v.get("is_focused")), "badge-amber"))
		    	  .append(renderBooleanBadge("clickable", isClickable, "badge-green"))
		    	  .append("</div></div>")
	    	  .append("<div class='card-body'><div class='content-grid'><div class='left-col'><div class='topline'><div class='primary'>");

	    	appendField(sb, "hooker_id", hookerId);
	    	appendField(sb, "text", text);
	    	appendField(sb, "hint", hint);
	    	appendField(sb, "content", !contentDescription.isEmpty() ? contentDescription : imageButtonContentDescription);
	    	appendField(sb, "parent", asString(v.get("parent_class")));
	    	appendField(sb, "bounds", buildBoundsText(v));
	    	appendField(sb, "alpha", asString(v.get("alpha")));
	    	appendField(sb, "selected", asString(v.get("is_selected")));
	    	appendField(sb, "shown", asString(v.get("is_shown")));
	    	appendField(sb, "enabled", asString(v.get("is_enabled")));
	    	sb.append("</div>");

	    	if (!imageUrl.isEmpty()) {
	    		sb.append("<a class='preview' href='").append(escapeHtml(imageUrl)).append("' target='_blank'>")
	    		  .append("<img src='").append(escapeHtml(imageUrl)).append("'/></a>");
	    	} else {
	    		sb.append("<div class='preview'><div class='empty-preview'>No Preview</div></div>");
	    	}
	    	sb.append("</div><div class='sections'>");

	    	sb.append("<section class='panel'><h3>Interaction</h3>");
	    	appendMetaLine(sb, "long clickable", asString(v.get("is_long_clickable")));
	    	appendMetaLine(sb, "listeners", listenerSummary);
	    	appendMetaLine(sb, "adapter", firstNonEmpty(asString(v.get("adapter_class")), asString(v.get("spinner_adapter_class"))));
	    	appendMetaLine(sb, "item count", firstNonEmpty(asString(v.get("item_count")), asString(v.get("spinner_item_count"))));
	    	appendMetaLine(sb, "checked", asString(v.get("checked")));
	    	appendMetaLine(sb, "progress", buildProgressText(v));
	    	appendMetaLine(sb, "scroll", buildScrollText(v));
	    	sb.append("</section>");

	    	sb.append("<section class='panel'><h3>View Details</h3>");
	    	appendMetaLine(sb, "web", buildWebText(v));
	    	appendMetaLine(sb, "video", buildVideoText(v));
	    	appendMetaLine(sb, "view pager", buildViewPagerText(v));
	    	appendMetaLine(sb, "list range", buildRangeText(v));
	    	appendMetaLine(sb, "child count", asString(v.get("child_count")));
	    	appendMetaLine(sb, "extra", buildExtraText(v));
	    	sb.append("</section>");
	    	sb.append("</div>");
	    	sb.append("<div class='action-stack'>");
	    	if (isEditText && !hookerId.isEmpty()) {
	    		sb.append("<div class='text-form'><input class='text-input' type='text' value='")
	    		  .append(escapeHtml(text))
	    		  .append("' placeholder='")
	    		  .append(escapeHtml(hint.isEmpty() ? "input text" : hint))
	    		  .append("'/><button class='action-btn click-btn' onclick=\"setTextValue('")
	    		  .append(escapeJs(hookerId))
	    		  .append("', this)\">Set Text</button><button class='action-btn search-btn' onclick=\"sendSearchAction('")
	    		  .append(escapeJs(hookerId))
	    		  .append("', this)\">Search</button></div>");
	    	}
	    	if ((isClickable || isImageView || isTextView) && !hookerId.isEmpty()) {
	    		sb.append("<div class='action-bar'><button class='action-btn click-btn' onclick=\"clickView('")
	    		  .append(escapeJs(hookerId))
	    		  .append("', this)\">Click</button></div>");
	    	}
	    	sb.append("<div class='action-result'></div></div>");
	    	sb.append("</div><div class='right-col'><aside class='raw-panel'><h3>Raw Fields</h3>")
	    	  .append(renderRawFieldsHtml(v))
	    	  .append("</aside></div></div></div></article>");
	    }
	    sb.append("</section></div></body></html>");
	    return sb.toString();
	}

	private String asString(Object o) {
	    return o == null ? "" : String.valueOf(o);
	}

	private String renderStat(String label, String value) {
		return "<div class='stat'><div class='stat-label'>" + escapeHtml(label) + "</div><div class='stat-value'>"
				+ escapeHtml(value) + "</div></div>";
	}

	private String renderBadge(String text, String clazz) {
		return "<span class='badge " + clazz + "'>" + escapeHtml(text) + "</span>";
	}

	private String renderBooleanBadge(String label, boolean value, String clazz) {
		if (!value) {
			return "";
		}
		return renderBadge(label, clazz);
	}

	private void appendField(StringBuilder sb, String label, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		sb.append("<div class='field'><span class='label'>")
		  .append(escapeHtml(label))
		  .append("</span><div class='value'>")
		  .append(escapeHtml(value))
		  .append("</div></div>");
	}

	private void appendMetaLine(StringBuilder sb, String label, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		sb.append("<div class='meta-line'><span class='label'>")
		  .append(escapeHtml(label))
		  .append("</span><span class='value'>")
		  .append(escapeHtml(value))
		  .append("</span></div>");
	}

	private void fillWebViewInfo(Map<String, Object> viewInfo, WebView webView) {
		try {
			Map<String, Object> webInfo = runOnMainThread(new ValueCallable<Map<String, Object>>() {
				@Override
				public Map<String, Object> call() {
					Map<String, Object> data = new HashMap<String, Object>();
					data.put("web_url", webView.getUrl());
					data.put("web_title", webView.getTitle());
					data.put("web_progress", webView.getProgress());
					data.put("web_can_go_back", webView.canGoBack());
					data.put("web_can_go_forward", webView.canGoForward());
					data.put("web_content_height", webView.getContentHeight());
					return data;
				}
			});
			viewInfo.putAll(webInfo);
		} catch (Exception e) {
			logger.warn(e);
			viewInfo.put("web_error", e.getClass().getName());
		}
	}

	private <T> T runOnMainThread(ValueCallable<T> callable) throws Exception {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			return callable.call();
		}
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<T> resultRef = new AtomicReference<T>();
		AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
		Activity activity = Android.getTopActivity();
		if (activity == null) {
			return callable.call();
		}
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					resultRef.set(callable.call());
				} catch (Throwable e) {
					errorRef.set(e);
				} finally {
					latch.countDown();
				}
			}
		});
		latch.await();
		if (errorRef.get() != null) {
			Throwable error = errorRef.get();
			if (error instanceof Exception) {
				throw (Exception) error;
			}
			throw new RuntimeException(error);
		}
		return resultRef.get();
	}

	private interface ValueCallable<T> {
		T call() throws Exception;
	}

	private String simpleClassName(String clazz) {
		if (clazz == null || clazz.isEmpty()) {
			return "";
		}
		int index = clazz.lastIndexOf('.');
		return index >= 0 ? clazz.substring(index + 1) : clazz;
	}

	private boolean viewClassMatches(String className, Class<?> targetClass) {
		if (className == null || className.isEmpty() || targetClass == null) {
			return false;
		}
		try {
			return targetClass.isAssignableFrom(Class.forName(className));
		} catch (Throwable e) {
			return className.equals(targetClass.getName());
		}
	}

	private String buildBoundsText(Map<String, Object> viewInfo) {
		String width = asString(viewInfo.get("width"));
		String height = asString(viewInfo.get("height"));
		String x = asString(viewInfo.get("x"));
		String y = asString(viewInfo.get("y"));
		if (width.isEmpty() && height.isEmpty() && x.isEmpty() && y.isEmpty()) {
			return "";
		}
		return joinNonEmpty(" | ",
				width.isEmpty() || height.isEmpty() ? "" : width + " x " + height,
				x.isEmpty() || y.isEmpty() ? "" : "x=" + x + ", y=" + y);
	}

	private String buildProgressText(Map<String, Object> viewInfo) {
		String progress = firstNonEmpty(asString(viewInfo.get("progress_bar_progress")), asString(viewInfo.get("seek_bar_progress")));
		String max = firstNonEmpty(asString(viewInfo.get("progress_bar_max")), asString(viewInfo.get("seek_bar_max")));
		String rating = asString(viewInfo.get("rating"));
		if (!rating.isEmpty()) {
			return joinNonEmpty(" | ", "rating=" + rating, "stars=" + asString(viewInfo.get("rating_num_stars")));
		}
		if (progress.isEmpty() && max.isEmpty()) {
			return "";
		}
		return progress + (max.isEmpty() ? "" : " / " + max);
	}

	private String buildScrollText(Map<String, Object> viewInfo) {
		String scrollX = asString(viewInfo.get("scroll_x"));
		String scrollY = asString(viewInfo.get("scroll_y"));
		if (scrollX.isEmpty() && scrollY.isEmpty()) {
			return "";
		}
		return joinNonEmpty(", ", scrollX.isEmpty() ? "" : "x=" + scrollX, scrollY.isEmpty() ? "" : "y=" + scrollY);
	}

	private String buildWebText(Map<String, Object> viewInfo) {
		String url = asString(viewInfo.get("web_url"));
		String title = asString(viewInfo.get("web_title"));
		String progress = asString(viewInfo.get("web_progress"));
		return joinNonEmpty(" | ",
				title.isEmpty() ? "" : title,
				url,
				progress.isEmpty() ? "" : "progress=" + progress + "%");
	}

	private String buildVideoText(Map<String, Object> viewInfo) {
		String current = asString(viewInfo.get("video_current_position"));
		String duration = asString(viewInfo.get("video_duration"));
		String playing = asString(viewInfo.get("video_is_playing"));
		if (current.isEmpty() && duration.isEmpty() && playing.isEmpty()) {
			return "";
		}
		return joinNonEmpty(" | ",
				current.isEmpty() ? "" : "current=" + current,
				duration.isEmpty() ? "" : "duration=" + duration,
				playing.isEmpty() ? "" : "playing=" + playing);
	}

	private String buildViewPagerText(Map<String, Object> viewInfo) {
		String current = asString(viewInfo.get("view_page_current_item"));
		String count = asString(viewInfo.get("view_page_count"));
		String superClass = asString(viewInfo.get("view_page_super_class"));
		if (current.isEmpty() && count.isEmpty() && superClass.isEmpty()) {
			return "";
		}
		return joinNonEmpty(" | ",
				superClass,
				current.isEmpty() ? "" : "current=" + current,
				count.isEmpty() ? "" : "count=" + count);
	}

	private String buildRangeText(Map<String, Object> viewInfo) {
		String first = firstNonEmpty(asString(viewInfo.get("first_visible_position")), asString(viewInfo.get("first_visible_positions")));
		String last = firstNonEmpty(asString(viewInfo.get("last_visible_position")), asString(viewInfo.get("last_visible_positions")));
		if (first.isEmpty() && last.isEmpty()) {
			return "";
		}
		return joinNonEmpty(" | ",
				first.isEmpty() ? "" : "first=" + first,
				last.isEmpty() ? "" : "last=" + last);
	}

	private String buildExtraText(Map<String, Object> viewInfo) {
		return joinNonEmpty(" | ",
				asString(viewInfo.get("layout_manager")),
				asString(viewInfo.get("orientation")),
				asString(viewInfo.get("spinner_selected_item")),
				asString(viewInfo.get("textOn")),
				asString(viewInfo.get("currentText")),
				asString(viewInfo.get("texture_is_available")));
	}

	private String firstNonEmpty(String... values) {
		if (values == null) {
			return "";
		}
		for (String value : values) {
			if (value != null && !value.isEmpty()) {
				return value;
			}
		}
		return "";
	}

	private String joinNonEmpty(String separator, String... values) {
		if (values == null || values.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			if (value == null || value.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(separator);
			}
			sb.append(value);
		}
		return sb.toString();
	}

	private String escapeHtml(String s) {
	    if (s == null) return "";
	    return s.replace("&", "&amp;")
	            .replace("<", "&lt;")
	            .replace(">", "&gt;")
	            .replace("\"", "&quot;")
	            .replace("'", "&#39;");
	}

	private String escapeJs(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("'", "\\'");
	}

	private String renderRawFieldsHtml(Map<String, Object> viewInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='raw-grid'>");
		List<String> keys = new ArrayList<String>(viewInfo.keySet());
		java.util.Collections.sort(keys);
		for (String key : keys) {
			Object value = viewInfo.get(key);
			sb.append("<div class='raw-item'><span class='raw-key'>")
			  .append(escapeHtml(key))
			  .append("</span><div class='raw-value'>")
			  .append(escapeHtml(formatRawValue(value)))
			  .append("</div></div>");
		}
		if (keys.isEmpty()) {
			sb.append("<div class='raw-empty'>No raw fields</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String formatRawValue(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof String || value instanceof Number || value instanceof Boolean) {
			return String.valueOf(value);
		}
		return JSON.toJSONString(value, true);
	}

	private String safeToString(Object obj) {
		return obj == null ? null : String.valueOf(obj);
	}

	private String getVisibilityName(int visibility) {
		if (visibility == View.VISIBLE) {
			return "VISIBLE";
		}
		if (visibility == View.INVISIBLE) {
			return "INVISIBLE";
		}
		if (visibility == View.GONE) {
			return "GONE";
		}
		return String.valueOf(visibility);
	}

	private void fillAdapterViewInfo(Map<String, Object> viewInfo, ListAdapter adapter) {
		if (adapter == null) {
			return;
		}
		viewInfo.put("adapter_class", adapter.getClass().getName());
		viewInfo.put("item_count", adapter.getCount());
	}

	private void fillSwitchLikeInfo(Map<String, Object> viewInfo, View view, XView xView) {
		if (!isInstanceOf(view, "android.widget.Switch")
				&& !isInstanceOf(view, "androidx.appcompat.widget.SwitchCompat")
				&& !isInstanceOf(view, "android.widget.SwitchButton")) {
			return;
		}
		viewInfo.put("type", "Switch");
		if (view instanceof CompoundButton) {
			CompoundButton compoundButton = (CompoundButton) view;
			viewInfo.put("checked", compoundButton.isChecked());
			viewInfo.put("text", safeToString(compoundButton.getText()));
			CompoundButton.OnCheckedChangeListener checkedListener = xView.getOnCheckedChangeListener();
			if (checkedListener != null) {
				viewInfo.put("on_checked_change_listener_clazz", checkedListener.getClass().getName());
			}
		}
		Object textOn = invokeNoArg(view, "getTextOn");
		Object textOff = invokeNoArg(view, "getTextOff");
		if (textOn != null) {
			viewInfo.put("textOn", safeToString(textOn));
		}
		if (textOff != null) {
			viewInfo.put("textOff", safeToString(textOff));
		}
	}

	private void fillReflectiveViewInfo(Map<String, Object> viewInfo, View view) {
		if (isInstanceOf(view, "androidx.core.widget.NestedScrollView")) {
			viewInfo.put("type", "NestedScrollView");
			viewInfo.put("scroll_y", invokeNoArg(view, "getScrollY"));
			viewInfo.put("scroll_x", invokeNoArg(view, "getScrollX"));
		}
		if (isInstanceOf(view, "androidx.drawerlayout.widget.DrawerLayout")) {
			viewInfo.put("type", "DrawerLayout");
			viewInfo.put("drawer_open", invokeIntArg(view, "isDrawerOpen", 8388611));
		}
		if (isInstanceOf(view, "com.google.android.material.tabs.TabLayout")) {
			viewInfo.put("type", "TabLayout");
			viewInfo.put("tab_count", invokeNoArg(view, "getTabCount"));
			viewInfo.put("selected_tab_position", invokeNoArg(view, "getSelectedTabPosition"));
		}
		if (isInstanceOf(view, "androidx.fragment.app.FragmentContainerView")) {
			viewInfo.put("type", "FragmentContainerView");
		}
		if (isInstanceOf(view, "androidx.compose.ui.platform.ComposeView")) {
			viewInfo.put("type", "ComposeView");
		}
		if (isInstanceOf(view, "android.view.SurfaceView")) {
			viewInfo.put("type", "SurfaceView");
		}
		if (isInstanceOf(view, "com.google.android.gms.maps.MapView")) {
			viewInfo.put("type", "MapView");
		}
	}

	private boolean isInstanceOf(Object obj, String className) {
		try {
			return Class.forName(className).isInstance(obj);
		} catch (Exception e) {
			return false;
		}
	}

	private Object invokeNoArg(Object obj, String methodName) {
		try {
			return obj.getClass().getMethod(methodName).invoke(obj);
		} catch (Exception e) {
			return null;
		}
	}

	private Object invokeIntArg(Object obj, String methodName, int arg) {
		try {
			return obj.getClass().getMethod(methodName, int.class).invoke(obj, Integer.valueOf(arg));
		} catch (Exception e) {
			return null;
		}
	}

	private String resolveViewIdName(Activity activity, int viewId) {
		try {
			return activity.getResources().getResourceEntryName(viewId);
		} catch (Exception e) {
			return "0x" + Integer.toHexString(viewId);
		}
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
	    	if (bitmap == null) {
	    		return false;
	    	}
		    FileOutputStream fos = new FileOutputStream(outFile);
		    try {
		    	boolean compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
		    	fos.flush();
		    	return compressed;
		    } finally {
		    	fos.close();
		    }
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
