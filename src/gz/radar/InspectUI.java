package gz.radar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.graphics.Rect;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VideoView;
import gz.util.Logger;

public class InspectUI {

	private static Logger logger = new Logger(InspectUI.class);
	
	private static String getBounds(View view) {
		Rect rect = new Rect();
		boolean visible = view.getGlobalVisibleRect(rect);
		if (!visible) {
			int[] location = new int[2];
			view.getLocationOnScreen(location);
			rect.set(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
		}
		return "[" + rect.left + "," + rect.top + "][" + rect.right + "," + rect.bottom + "]";
	}
	
	public static boolean isProbablyVisibleToUser(View view) {
	      if (view == null) {
	          return false;
	      }
	      if (view.getVisibility() != View.VISIBLE) {
	          return false;
	      }
	      if (view.getAlpha() <= 0f) {
	          return false;
	      }
	      if (view.getWidth() <= 0 || view.getHeight() <= 0) {
	          return false;
	      }
	      if (!view.isAttachedToWindow()) {
	          return false;
	      }
	      Rect rect = new Rect();
	      if (!view.getGlobalVisibleRect(rect)) {
	          return false;
	      }
	      return rect.width() > 0 && rect.height() > 0;
	  }
	
	public static String getViewType(View view) {
		if (view == null) {
			return "Unknown";
		}
		if (view instanceof ImageButton) {
			return "ImageButton";
		}
		if (view instanceof RadioButton) {
			return "RadioButton";
		}
		if (view instanceof ToggleButton) {
			return "ToggleButton";
		}
		if (view instanceof Switch) {
			return "Switch";
		}
		if (view instanceof CheckedTextView) {
			return "CheckedTextView";
		}
		if (view instanceof AutoCompleteTextView) {
			return "AutoCompleteTextView";
		}
		if (view instanceof EditText) {
			return "EditText";
		}
		if (view instanceof Button) {
			return "Button";
		}
		if (view instanceof ImageView) {
			return "ImageView";
		}
		if (view instanceof WebView) {
			return "WebView";
		}
		if (view instanceof SurfaceView) {
			return "SurfaceView";
		}
		if (view instanceof TextureView) {
			return "TextureView";
		}
		if (isInstanceOf(view, "androidx.recyclerview.widget.RecyclerView")
				|| isInstanceOf(view, "android.support.v7.widget.RecyclerView")) {
			return "RecyclerView";
		}
		if (isInstanceOf(view, "androidx.viewpager.widget.ViewPager")
				|| isInstanceOf(view, "android.support.v4.view.ViewPager")) {
			return "ViewPager";
		}
		if (isInstanceOf(view, "androidx.viewpager2.widget.ViewPager2")) {
			return "ViewPager2";
		}
		if (view instanceof SeekBar) {
			return "SeekBar";
		}
		if (view instanceof RatingBar) {
			return "RatingBar";
		}
		if (view instanceof ProgressBar) {
			return "ProgressBar";
		}
		if (view instanceof VideoView) {
			return "VideoView";
		}
		if (view instanceof GridView) {
			return "GridView";
		}
		if (view instanceof ListView) {
			return "ListView";
		}
		if (view instanceof AbsListView) {
			return "AbsListView";
		}
		if (view instanceof ScrollView) {
			return "ScrollView";
		}
		if (view instanceof CompoundButton) {
			return "CompoundButton";
		}
		if (view instanceof TextView) {
			return "TextView";
		}
		if (view instanceof ViewGroup) {
			return "ViewGroup";
		}
		return view.getClass().getSimpleName();
	}

	public static List<View> collectImportantViews(Activity activity, String textContains, String rectLimit, String viewType,
			String className, String classNameContains, int isImage, int isEditText, int isListView,
			int isScrollView) {
		Set<View> result = new LinkedHashSet<View>();
		List<View> roots = collectWindowRoots(activity);
		for (View root : roots) {
			traverse(root, result, normalizeContainsValue(textContains), parseRectLimit(rectLimit),
					normalizeContainsValue(viewType), normalizeContainsValue(className),
					normalizeContainsValue(classNameContains), isImage == 1, isEditText == 1, isListView == 1,
					isScrollView == 1);
		}
		return new ArrayList<View>(result);
	}

	private static List<View> collectWindowRoots(Activity activity) {
		LinkedHashSet<View> roots = new LinkedHashSet<View>();
		if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
			roots.add(activity.getWindow().getDecorView());
		}
		try {
			Class<?> wmgClass = Class.forName("android.view.WindowManagerGlobal");
			Object wmg = invokeNoArgStatic(wmgClass, "getInstance");
			Object viewsValue = readField(wmg, "mViews");
			addWindowRoots(roots, viewsValue);
		} catch (Throwable e) {
			logger.warn(e);
		}
		if (roots.isEmpty() && activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
			roots.add(activity.getWindow().getDecorView());
		}
		return new ArrayList<View>(roots);
	}

	private static void addWindowRoots(Set<View> out, Object viewsValue) {
		if (viewsValue == null) {
			return;
		}
		if (viewsValue instanceof List) {
			List<?> views = (List<?>) viewsValue;
			for (Object item : views) {
				addWindowRoot(out, item);
			}
			return;
		}
		if (viewsValue instanceof View[]) {
			for (View view : (View[]) viewsValue) {
				addWindowRoot(out, view);
			}
		}
	}

	private static void addWindowRoot(Set<View> out, Object value) {
		if (!(value instanceof View)) {
			return;
		}
		View view = (View) value;
		if (!view.isAttachedToWindow() || view.getVisibility() != View.VISIBLE) {
			return;
		}
		out.add(view);
	}

	private static void traverse(View v, Set<View> out, String normalizedTextContains, Rect limitRect,
			String normalizedViewType, String normalizedClassName, String normalizedClassNameContains,
			boolean requireImage, boolean requireEditText, boolean requireListView, boolean requireScrollView) {
		if (v == null || !isProbablyVisibleToUser(v))
			return;
		if (v.getVisibility() != View.VISIBLE)
			return;
		
		if (isImportantView(v) && matchesTextContains(v, normalizedTextContains)
				&& matchesRectLimit(v, limitRect)
				&& matchesViewType(v, normalizedViewType)
				&& matchesClassName(v, normalizedClassName, normalizedClassNameContains)
				&& matchesSpecialFlags(v, requireImage, requireEditText, requireListView, requireScrollView)) {
	        addIfAbsent(out, v);
	    }

		if (isSuspiciousVideoContainer(v) && matchesTextContains(v, normalizedTextContains)
				&& matchesRectLimit(v, limitRect)
				&& matchesViewType(v, normalizedViewType)
				&& matchesClassName(v, normalizedClassName, normalizedClassNameContains)
				&& matchesSpecialFlags(v, requireImage, requireEditText, requireListView, requireScrollView)) {
			addIfAbsent(out, v);
		}

		if (isVideoLikeView(v)) {
			collectVideoAncestors(v, out, normalizedTextContains, limitRect, normalizedViewType,
					normalizedClassName, normalizedClassNameContains, requireImage, requireEditText, requireListView,
					requireScrollView);
		}

		if (v instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) v;
			for (int i = 0; i < group.getChildCount(); i++) {
				traverse(group.getChildAt(i), out, normalizedTextContains, limitRect, normalizedViewType,
						normalizedClassName, normalizedClassNameContains, requireImage, requireEditText,
						requireListView, requireScrollView);
			}
		}
	}

	private static boolean matchesTextContains(View view, String normalizedTextContains) {
		if (normalizedTextContains == null || normalizedTextContains.isEmpty()) {
			return true;
		}
		if (containsNormalized(view.getContentDescription(), normalizedTextContains)) {
			return true;
		}
		if (view instanceof TextView) {
			TextView textView = (TextView) view;
			return containsNormalized(textView.getText(), normalizedTextContains)
					|| containsNormalized(textView.getHint(), normalizedTextContains);
		}
		return false;
	}

	private static boolean containsNormalized(CharSequence value, String normalizedNeedle) {
		if (value == null || normalizedNeedle == null || normalizedNeedle.isEmpty()) {
			return false;
		}
		String normalizedHaystack = normalizeContainsValue(value);
		return !normalizedHaystack.isEmpty() && normalizedHaystack.contains(normalizedNeedle);
	}

	private static String normalizeContainsValue(CharSequence value) {
		if (value == null) {
			return "";
		}
		return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
	}

	private static boolean matchesRectLimit(View view, Rect limitRect) {
		if (limitRect == null) {
			return true;
		}
		Rect viewRect = new Rect();
		boolean visible = view.getGlobalVisibleRect(viewRect);
		if (!visible) {
			int[] location = new int[2];
			view.getLocationOnScreen(location);
			viewRect.set(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
		}
		return Rect.intersects(limitRect, viewRect);
	}

	private static boolean matchesViewType(View view, String normalizedViewType) {
		if (normalizedViewType == null || normalizedViewType.isEmpty()) {
			return true;
		}
		return normalizeContainsValue(getViewType(view)).equals(normalizedViewType);
	}

	private static boolean matchesClassName(View view, String normalizedClassName, String normalizedClassNameContains) {
		String normalizedActualClassName = normalizeContainsValue(view.getClass().getName());
		if (normalizedClassName != null && !normalizedClassName.isEmpty()
				&& !normalizedActualClassName.equals(normalizedClassName)) {
			return false;
		}
		if (normalizedClassNameContains != null && !normalizedClassNameContains.isEmpty()
				&& !normalizedActualClassName.contains(normalizedClassNameContains)) {
			return false;
		}
		return true;
	}

	private static boolean matchesSpecialFlags(View view, boolean requireImage, boolean requireEditText,
			boolean requireListView, boolean requireScrollView) {
		if (requireImage && !(view instanceof ImageView || view instanceof ImageButton)) {
			return false;
		}
		if (requireEditText && !(view instanceof EditText)) {
			return false;
		}
		if (requireListView && !(view instanceof ListView || view instanceof GridView || isRecyclerView(view))) {
			return false;
		}
		if (requireScrollView && !(view instanceof ScrollView || view instanceof HorizontalScrollView)) {
			return false;
		}
		return true;
	}

	private static Rect parseRectLimit(String rectLimit) {
		if (rectLimit == null) {
			return null;
		}
		String trimmed = rectLimit.trim();
		if (trimmed.isEmpty() || "0,0,0,0".equals(trimmed)) {
			return null;
		}
		String[] parts = trimmed.split(",");
		if (parts.length != 4) {
			logger.warn("invalid rect_limit: " + rectLimit);
			return null;
		}
		try {
			int left = Integer.parseInt(parts[0].trim());
			int top = Integer.parseInt(parts[1].trim());
			int right = Integer.parseInt(parts[2].trim());
			int bottom = Integer.parseInt(parts[3].trim());
			if (left == 0 && top == 0 && right == 0 && bottom == 0) {
				return null;
			}
			return new Rect(Math.min(left, right), Math.min(top, bottom), Math.max(left, right), Math.max(top, bottom));
		} catch (NumberFormatException e) {
			logger.warn("invalid rect_limit: " + rectLimit);
			logger.warn(e);
			return null;
		}
	}
	
	private static boolean isImportantView(View v) {
	    return v instanceof Button
	            || v instanceof TextView
	            || v instanceof EditText
	            || v instanceof ImageView
	            || v instanceof ImageButton
	            || v instanceof CheckBox
	            || v instanceof RadioButton
	            || v instanceof ToggleButton
	            || v instanceof SeekBar
	            || v instanceof ProgressBar
	            || v instanceof RatingBar
	            || isSwitch(v)
	            || isViewPager(v)
	            || isViewPager2(v)
	            || isRecyclerView(v)
	            || v instanceof ListView
	            || v instanceof GridView
	            || v instanceof ScrollView
	            || v instanceof HorizontalScrollView
	            || isWebView(v)
	            || isMaterialView(v)
	            || isToolbar(v)
		            || v instanceof VideoView
		            || v instanceof SurfaceView
		            || v instanceof TextureView;
	}

	private static boolean isVideoLikeView(View v) {
		return v instanceof SurfaceView
				|| v instanceof TextureView
				|| v instanceof VideoView
				|| isWebView(v)
				|| containsAny(v.getClass().getName().toLowerCase(), "video", "player", "render", "surface", "texture");
	}

	private static void collectVideoAncestors(View view, Set<View> out, String normalizedTextContains, Rect limitRect,
			String normalizedViewType, String normalizedClassName, String normalizedClassNameContains,
			boolean requireImage, boolean requireEditText, boolean requireListView, boolean requireScrollView) {
		View current = view;
		int depth = 0;
		while (current != null && current.getParent() instanceof View && depth < 5) {
			View parent = (View) current.getParent();
			if (isSuspiciousVideoContainer(parent) && matchesTextContains(parent, normalizedTextContains)
					&& matchesRectLimit(parent, limitRect)
					&& matchesViewType(parent, normalizedViewType)
					&& matchesClassName(parent, normalizedClassName, normalizedClassNameContains)
					&& matchesSpecialFlags(parent, requireImage, requireEditText, requireListView, requireScrollView)) {
				addIfAbsent(out, parent);
			}
			current = parent;
			depth++;
		}
	}

	private static boolean isSuspiciousVideoContainer(View v) {
		if (!(v instanceof ViewGroup)) {
			return false;
		}
		String className = v.getClass().getName().toLowerCase();
		boolean nameMatched = containsAny(className,
				"video", "feed", "player", "pinch", "pager", "swipe", "scroll", "container", "cover");
		boolean sizeMatched = v.getWidth() >= 300 && v.getHeight() >= 300;
		boolean scrollCandidate = isViewPager(v) || isViewPager2(v) || isRecyclerView(v)
				|| v instanceof ScrollView || v instanceof HorizontalScrollView
				|| containsAny(className, "recyclerview", "viewpager");
		return (nameMatched && sizeMatched) || scrollCandidate;
	}

	private static boolean containsAny(String text, String... keywords) {
		if (text == null || keywords == null) {
			return false;
		}
		for (String keyword : keywords) {
			if (keyword != null && text.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private static void addIfAbsent(Set<View> out, View view) {
		if (view == null) {
			return;
		}
		out.add(view);
	}
	
	private static boolean isInstanceOf(View v, String clz) {
	    try {
	        return Class.forName(clz).isInstance(v);
	    } catch (Throwable e) {
	        return false;
	    }
	}

	public static boolean isMaterialView(View v) {
	    return isInstanceOf(v, "com.google.android.material.tabs.TabLayout")
	        || isInstanceOf(v, "com.google.android.material.bottomnavigation.BottomNavigationView")
	        || isInstanceOf(v, "com.google.android.material.floatingactionbutton.FloatingActionButton");
	}

	public static boolean isToolbar(View v) {
	    return isInstanceOf(v, "androidx.appcompat.widget.Toolbar");
	}

	public static boolean isSwitch(View v) {
		try {
			if (v instanceof android.widget.Switch)
				return true;
			return Class.forName("androidx.appcompat.widget.SwitchCompat").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}

	public static boolean isViewPager(View v) {
		try {
			return Class.forName("androidx.viewpager.widget.ViewPager").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}
	
	public static boolean isViewPager2(View v) {
		try {
			return Class.forName("androidx.viewpager2.widget.ViewPager2").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}

	public static boolean isRecyclerView(View v) {
		try {
			return Class.forName("androidx.recyclerview.widget.RecyclerView").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}

	public static boolean isWebView(View v) {
		if (v instanceof android.webkit.WebView)
			return true;

		// 兼容某些 App 的自定义 WebView（抖音/微信等）
		try {
			if (v.getClass().getName().toLowerCase().contains("webview")) {
				return true;
			}
		} catch (Throwable ignore) {
		}
		return false;
	}

	public static void tryDismissByDialogFragment() throws Exception {
		Activity top = Android.getTopActivity();
		if (top == null)
			return;
		top.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (dismissAndroidXDialogFragment(top))
					return;
				dismissPlatformDialogFragment(top);
			}
		});
	}
	
	static Field findField(Class<?> clz, String name) throws NoSuchFieldException {
	    Class<?> c = clz;
	    while (c != null) {
	        try {
	            Field f = c.getDeclaredField(name);
	            f.setAccessible(true);
	            return f;
	        } catch (NoSuchFieldException ignored) {
	            c = c.getSuperclass();
	        }
	    }
	    throw new NoSuchFieldException("No field " + name + " in " + clz);
	}

	static Method findMethod(Class<?> clz, String name, Class<?>... params) throws NoSuchMethodException {
	    Class<?> c = clz;
	    while (c != null) {
	        try {
	            Method m = c.getDeclaredMethod(name, params);
	            m.setAccessible(true);
	            return m;
	        } catch (NoSuchMethodException ignored) {
	            c = c.getSuperclass();
	        }
	    }
	    throw new NoSuchMethodException("No method " + name + " in " + clz);
	}

	static Object readField(Object target, String name) throws Exception {
		if (target == null) {
			return null;
		}
		return findField(target.getClass(), name).get(target);
	}

	static Object invokeNoArgStatic(Class<?> clz, String name) throws Exception {
		return findMethod(clz, name).invoke(null);
	}

	@SuppressWarnings("unchecked")
	static List<Object> tryGetFragmentsByFields(Object fm) {
	    try {
	        // 1) 新版本：fm.mFragmentStore.mAdded
	        Field storeF = null;
	        try { storeF = findField(fm.getClass(), "mFragmentStore"); } catch (Throwable ignored) {}

	        if (storeF != null) {
	            Object store = storeF.get(fm);
	            if (store != null) {
	                try {
	                    Field addedF = findField(store.getClass(), "mAdded");
	                    Object added = addedF.get(store);
	                    if (added instanceof List) return (List<Object>) added;
	                } catch (Throwable ignored) {}
	            }
	        }

	        // 2) 老版本：fm.mAdded
	        try {
	            Field addedF2 = findField(fm.getClass(), "mAdded");
	            Object added2 = addedF2.get(fm);
	            if (added2 instanceof List) return (List<Object>) added2;
	        } catch (Throwable ignored) {}

	    } catch (Throwable ignored) {}

	    return null;
	}
	
	@SuppressWarnings("unchecked")
	static List<Object> tryGetFragmentsByMethod(Object fm) {
	    try {
	        Method m = findMethod(fm.getClass(), "getFragments");
	        Object r = m.invoke(fm);
	        return (List<Object>) r;
	    } catch (Throwable ignored) {
	        return null;
	    }
	}
	
	// AndroidX：dismiss 顶层 DialogFragment
	public static boolean dismissAndroidXDialogFragment(Activity a) {
	    try {
	        Class<?> faClz = Class.forName("androidx.fragment.app.FragmentActivity");
	        if (!faClz.isInstance(a)) return false;
	        Object fm = faClz.getMethod("getSupportFragmentManager").invoke(a);
	        if (fm == null) return false;
	        List<Object> fragments = tryGetFragmentsByMethod(fm);
	        if (fragments == null) fragments = tryGetFragmentsByFields(fm);
	        if (fragments == null || fragments.isEmpty()) return false;
	        Class<?> dfClz = Class.forName("androidx.fragment.app.DialogFragment");
	        // 从后往前找：更接近“顶层”
	        for (int i = fragments.size() - 1; i >= 0; i--) {
	            Object f = fragments.get(i);
	            if (f == null) continue;
	            if (dfClz.isInstance(f)) {
	                // 优先 dismissAllowingStateLoss，避免状态保存异常
	                try {
	                    f.getClass().getMethod("dismissAllowingStateLoss").invoke(f);
	                } catch (NoSuchMethodException e) {
	                    // 极少数版本只有 dismiss()
	                    f.getClass().getMethod("dismiss").invoke(f);
	                }
	                return true;
	            }
	        }
	    } catch (Throwable e) {
	        logger.warn(e);
	    }
	    return false;
	}


	// 原生 DialogFragment
	public static boolean dismissPlatformDialogFragment(Activity a) {
		try {
			Object fm = a.getFragmentManager();
			java.util.List<?> fragments = (java.util.List<?>) fm.getClass().getMethod("getFragments").invoke(fm);
			if (fragments == null)
				return false;

			Class<?> dfClz = Class.forName("android.app.DialogFragment");
			for (int i = fragments.size() - 1; i >= 0; i--) {
				Object f = fragments.get(i);
				if (f != null && dfClz.isInstance(f)) {
					android.app.DialogFragment df = null;
					dfClz.getMethod("dismissAllowingStateLoss").invoke(f);
					return true;
				}
			}
		} catch (Throwable e) {
			logger.warn(e);
		}
		return false;
	}
	

}
