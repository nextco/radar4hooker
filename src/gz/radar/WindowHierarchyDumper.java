package gz.radar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceView;
import android.view.TextureView;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
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

public class WindowHierarchyDumper {

	public static class DumpResult {
		private final String packageName;
		private final String activityClassName;
		private final long dumpedAt;
		private final Map<String, Object> hierarchy;

		public DumpResult(String packageName, String activityClassName, long dumpedAt, Map<String, Object> hierarchy) {
			this.packageName = packageName;
			this.activityClassName = activityClassName;
			this.dumpedAt = dumpedAt;
			this.hierarchy = hierarchy;
		}

		public String toXml() {
			StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			sb.append("<hierarchy");
			appendXmlAttribute(sb, "package", packageName);
			appendXmlAttribute(sb, "activity", activityClassName);
			appendXmlAttribute(sb, "dumped-at", String.valueOf(dumpedAt));
			sb.append(">\n");
			appendNodeXml(sb, hierarchy, 1);
			sb.append("</hierarchy>\n");
			return sb.toString();
		}

		public Map<String, Object> toJsonObject() {
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put("package", packageName);
			result.put("activity", activityClassName);
			result.put("dumped_at", Long.valueOf(dumpedAt));
			result.put("hierarchy", hierarchy);
			return result;
		}
	}

	public static DumpResult dumpCurrentWindow() throws Exception {
		final Activity activity = Android.getTopActivity();
		if (activity == null) {
			throw new IllegalStateException("top activity not found");
		}
		final AtomicReference<Map<String, Object>> treeRef = new AtomicReference<Map<String, Object>>();
		final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
		final CountDownLatch latch = new CountDownLatch(1);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					View decorView = activity.getWindow().getDecorView();
					treeRef.set(dumpNode(decorView, 0, activity.getPackageName()));
				} catch (Throwable t) {
					errorRef.set(t);
				} finally {
					latch.countDown();
				}
			}
		};
		if (Looper.myLooper() == Looper.getMainLooper()) {
			task.run();
		} else {
			activity.runOnUiThread(task);
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("dump window hierarchy timeout");
			}
		}
		if (errorRef.get() != null) {
			Throwable t = errorRef.get();
			if (t instanceof Exception) {
				throw (Exception) t;
			}
			throw new RuntimeException(t);
		}
		return new DumpResult(activity.getPackageName(), activity.getClass().getName(), System.currentTimeMillis(), treeRef.get());
	}

	private static Map<String, Object> dumpNode(View view, int index, String packageName) {
		Map<String, Object> node = new LinkedHashMap<String, Object>();
		node.put("index", Integer.valueOf(index));
		node.put("class", view.getClass().getName());
		node.put("view-type", getViewType(view));
		node.put("package", packageName);
		node.put("resource-id", getResourceId(view));
		node.put("text", getText(view));
		node.put("content-desc", getContentDescription(view));
		node.put("checkable", Boolean.valueOf(view instanceof Checkable));
		node.put("checked", Boolean.valueOf(isChecked(view)));
		node.put("clickable", Boolean.valueOf(view.isClickable()));
		node.put("enabled", Boolean.valueOf(view.isEnabled()));
		node.put("focusable", Boolean.valueOf(view.isFocusable()));
		node.put("focused", Boolean.valueOf(view.isFocused()));
		node.put("scrollable", Boolean.valueOf(canScroll(view)));
		node.put("long-clickable", Boolean.valueOf(view.isLongClickable()));
		node.put("password", Boolean.valueOf(isPassword(view)));
		node.put("selected", Boolean.valueOf(view.isSelected()));
		node.put("visible-to-user", Boolean.valueOf(view.getVisibility() == View.VISIBLE && view.isShown()));
		node.put("bounds", getBounds(view));
		node.put("child-count", Integer.valueOf(view instanceof ViewGroup ? ((ViewGroup) view).getChildCount() : 0));
		List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				View child = group.getChildAt(i);
				children.add(dumpNode(child, i, packageName));
			}
		}
		node.put("children", children);
		return node;
	}

	private static String getResourceId(View view) {
		int id = view.getId();
		if (id == View.NO_ID) {
			return "";
		}
		try {
			Resources resources = view.getResources();
			return resources.getResourcePackageName(id) + ":id/" + resources.getResourceEntryName(id);
		} catch (Exception e) {
			return String.valueOf(id);
		}
	}

	private static String getText(View view) {
		if (view instanceof TextView) {
			CharSequence text = ((TextView) view).getText();
			return text == null ? "" : String.valueOf(text);
		}
		return "";
	}

	private static String getContentDescription(View view) {
		CharSequence contentDescription = view.getContentDescription();
		return contentDescription == null ? "" : String.valueOf(contentDescription);
	}

	private static boolean isChecked(View view) {
		if (view instanceof Checkable) {
			return ((Checkable) view).isChecked();
		}
		return false;
	}

	private static boolean canScroll(View view) {
		return view.canScrollHorizontally(-1) || view.canScrollHorizontally(1) || view.canScrollVertically(-1)
				|| view.canScrollVertically(1);
	}

	private static boolean isPassword(View view) {
		if (!(view instanceof TextView)) {
			return false;
		}
		int inputType = ((TextView) view).getInputType();
		int variation = inputType & InputType.TYPE_MASK_VARIATION;
		return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
				|| variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
				|| variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
	}

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

	private static void appendNodeXml(StringBuilder sb, Map<String, Object> node, int indent) {
		indent(sb, indent);
		sb.append("<node");
		appendXmlAttribute(sb, "index", stringValue(node.get("index")));
		appendXmlAttribute(sb, "view-type", stringValue(node.get("view-type")));
		appendXmlAttribute(sb, "text", stringValue(node.get("text")));
		appendXmlAttribute(sb, "resource-id", stringValue(node.get("resource-id")));
		appendXmlAttribute(sb, "class", stringValue(node.get("class")));
		appendXmlAttribute(sb, "package", stringValue(node.get("package")));
		appendXmlAttribute(sb, "content-desc", stringValue(node.get("content-desc")));
		appendXmlAttribute(sb, "checkable", stringValue(node.get("checkable")));
		appendXmlAttribute(sb, "checked", stringValue(node.get("checked")));
		appendXmlAttribute(sb, "clickable", stringValue(node.get("clickable")));
		appendXmlAttribute(sb, "enabled", stringValue(node.get("enabled")));
		appendXmlAttribute(sb, "focusable", stringValue(node.get("focusable")));
		appendXmlAttribute(sb, "focused", stringValue(node.get("focused")));
		appendXmlAttribute(sb, "scrollable", stringValue(node.get("scrollable")));
		appendXmlAttribute(sb, "long-clickable", stringValue(node.get("long-clickable")));
		appendXmlAttribute(sb, "password", stringValue(node.get("password")));
		appendXmlAttribute(sb, "selected", stringValue(node.get("selected")));
		appendXmlAttribute(sb, "visible-to-user", stringValue(node.get("visible-to-user")));
		appendXmlAttribute(sb, "bounds", stringValue(node.get("bounds")));
		List<Map<String, Object>> children = castChildren(node.get("children"));
		if (children.isEmpty()) {
			sb.append(" />\n");
			return;
		}
		sb.append(">\n");
		for (Map<String, Object> child : children) {
			appendNodeXml(sb, child, indent + 1);
		}
		indent(sb, indent);
		sb.append("</node>\n");
	}

	private static List<Map<String, Object>> castChildren(Object value) {
		List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
		if (value instanceof List) {
			List list = (List) value;
			for (Object item : list) {
				if (item instanceof Map) {
					children.add((Map<String, Object>) item);
				}
			}
		}
		return children;
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

	private static boolean isInstanceOf(View view, String className) {
		try {
			return Class.forName(className).isInstance(view);
		} catch (Throwable t) {
			return false;
		}
	}

	private static void appendXmlAttribute(StringBuilder sb, String name, String value) {
		sb.append(" ").append(name).append("=\"").append(escapeXml(value)).append("\"");
	}

	private static String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static String escapeXml(String input) {
		if (input == null || input.length() == 0) {
			return "";
		}
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private static void indent(StringBuilder sb, int indent) {
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
	}
}
