package gz.radar;

import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import android.content.res.Resources;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.VideoView;
import gz.util.X;

public class ViewXmlDumper {
	
	public static class XmlDumpResult {
		private Document document;
		private Map<String, WeakReference<View>> viewCache;
		public XmlDumpResult(Document document, Map<String, WeakReference<View>> viewCache) {
			this.document = document;
			this.viewCache = viewCache;
		}
		public Document getDocument() {
			return document;
		}
		public Map<String, WeakReference<View>> getViewCache() {
			return viewCache;
		}
	}

	public static XmlDumpResult viewToXml(View view) throws Exception {
		StringBuilder sb = new StringBuilder();
		Map<String, WeakReference<View>> viewCache = new HashMap<String, WeakReference<View>>();
		buildXml(view, sb, 0, viewCache);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
    	InputSource is = new InputSource(new StringReader(sb.toString()));
        org.w3c.dom.Document doc = builder.parse(is);
		return new XmlDumpResult(doc, viewCache);
	}
	
	private static boolean shouldSkip(View view) {
		if (view.getWidth() <= 0 || view.getHeight() <= 0) return true;

	    // 跳过完全空的容器
	    if (view instanceof ViewGroup) {
	        ViewGroup vg = (ViewGroup) view;
	        if (vg.getChildCount() == 0) {
	            return true;
	        }
	    }

	    // 可选：针对类名过滤
	    String className = view.getClass().getName();
	    if (className.contains("ViewStub") || className.contains("Space")) {
	        return true;
	    }
	    
	    // 可点击 OR 可选中 OR 可获取焦点 OR 包含文本，才被认为有意义
//	    boolean hasMeaning = view.isFocusable();
//	    if (view instanceof TextView) {
//	        String text = ((TextView) view).getText().toString();
//	        hasMeaning |= (text != null && !text.trim().isEmpty());
//	    }
//	    return hasMeaning;
	    return false;
	}


	private static void buildXml(View view, StringBuilder sb, int indent, Map<String, WeakReference<View>> viewCache) throws Exception {
		if (view.getVisibility() != View.VISIBLE || shouldSkip(view)) {
			return;
		}
		viewCache.put(String.valueOf(view.hashCode()), new WeakReference<>(view));
		String prefix = new String(new char[indent]).replace("\0", "  ");
		Map<String, String> attrs = new HashMap<String, String>();
		String className = view.getClass().getName();
		attrs.put("class_name", className);
		String idName = "no_id";
		int id = view.getId();
		if (id != View.NO_ID) {
			try {
				idName = view.getResources().getResourceEntryName(id);
			} catch (Resources.NotFoundException e) {
				idName = "id_" + id;
			}
			attrs.put("id", idName);
		}

		String text = "";
		if (view instanceof TextView) {
			text = ((TextView) view).getText().toString();
			attrs.put("text", text);
		}

		String bounds = view.getLeft() + "," + view.getTop() + "," + view.getRight() + ","
				+ view.getBottom();
		attrs.put("bounds", bounds);
		boolean isClickable = view.isClickable();
		attrs.put("is_clickable", String.valueOf(isClickable));
        if (isClickable) {
        	Object mListenerInfo  = X.invokeObject(view, "getListenerInfo");
        	if (mListenerInfo != null) {
        		Object mOnClickListener = X.getField(mListenerInfo, "mOnClickListener");
        		Object mOnLongClickListener = X.getField(mListenerInfo, "mOnLongClickListener");
        		if (mOnClickListener != null) {
        			String mOnClickListenerClassName = mOnClickListener.getClass().getName();
        			attrs.put("on_click_listener_classname", mOnClickListenerClassName);
        		}
        		if (mOnLongClickListener != null) {
        			String mOnLongClickListenerClassName = mOnLongClickListener.getClass().getName();
        			attrs.put("on_longclick_listener_classname", mOnLongClickListenerClassName);
        		}
        	}
        }
        attrs.put("is_visible", String.valueOf(view.getVisibility() == View.VISIBLE));
        attrs.put("is_enabled", String.valueOf(view.isEnabled()));
        attrs.put("is_focusable", String.valueOf(view.isFocusable()));
        attrs.put("is_focused", String.valueOf(view.isFocused()));
        attrs.put("is_horizontal_scroll_bar_enabled", String.valueOf(view.isHorizontalScrollBarEnabled()));
        attrs.put("is_long_clickable", String.valueOf(view.isLongClickable()));
        attrs.put("is_selected", String.valueOf(view.isSelected()));
        attrs.put("is_shown", String.valueOf(view.isShown()));
        attrs.put("width", String.valueOf(view.getWidth()));
        attrs.put("height", String.valueOf(view.getHeight()));
        attrs.put("hash_code", String.valueOf(view.hashCode()));
        String tagName = getNearestConcreteViewClass(view);
        String xml = mapToXmlElement(tagName, attrs, indent, view instanceof ViewGroup);
		sb.append(prefix).append(xml);
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				buildXml(group.getChildAt(i), sb, indent + 1, viewCache);
			}
			sb.append(prefix).append("</"+tagName+">\n");
		}
	}
	
	public static String mapToXmlElement(String tagName, Map<String, String> attrs, int indent, boolean isViewGroup) {
	    StringBuilder sb = new StringBuilder();
	    String prefix = new String(new char[indent]).replace("\0", "  ");
	    sb.append(prefix).append("<").append(tagName);

	    for (Map.Entry<String, String> entry : attrs.entrySet()) {
	        sb.append(" ")
	          .append(entry.getKey())
	          .append("=\"")
	          .append(escapeXml(entry.getValue()))
	          .append("\"");
	    }
	    if (isViewGroup) {
	    	sb.append(" >\n");
	    }else {
	    	sb.append(" />\n");
	    }
	    return sb.toString();
	}

	// 简单 XML 字符转义（必要时防止崩溃）
	private static String escapeXml(String input) {
		if (input == null || input.isEmpty())
			return "";
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
	
	public static String getNearestConcreteViewClass(View view) {
	    if (view == null) return "null";
	    // 更具体的控件先判断
	    if (view instanceof AutoCompleteTextView) return "AutoCompleteTextView";
	    if (view instanceof MultiAutoCompleteTextView) return "MultiAutoCompleteTextView";
	    if (view instanceof ImageButton) return "ImageButton";
	    if (view instanceof ImageView) return "ImageView";
	    if (view instanceof EditText) return "EditText";
	    if (view instanceof Button) return "Button";
	    if (view instanceof CheckBox) return "CheckBox";
	    if (view instanceof RadioButton) return "RadioButton";
	    if (view instanceof ToggleButton) return "ToggleButton";
	    if (view instanceof Switch) return "Switch";
	    if (view instanceof TextView) return "TextView";

	    if (view instanceof VideoView) return "VideoView";
	    if (view instanceof SurfaceView) return "SurfaceView";
	    if (view instanceof TextureView) return "TextureView";

	    if (view instanceof ListView) return "ListView";
	    
	    if (view instanceof GridView) return "GridView";
	    if (view instanceof ScrollView) return "ScrollView";
	    if (view instanceof HorizontalScrollView) return "HorizontalScrollView";

	    if (view instanceof SeekBar) return "SeekBar";
	    if (view instanceof RatingBar) return "RatingBar";
	    if (view instanceof ProgressBar) return "ProgressBar";

	    if (view instanceof WebView) return "WebView";
	    if (view instanceof SearchView) return "SearchView";
	    if (view instanceof DatePicker) return "DatePicker";
	    if (view instanceof TimePicker) return "TimePicker";
	    if (view instanceof NumberPicker) return "NumberPicker";
	    if (view instanceof CalendarView) return "CalendarView";
	    try {
	    	if (view instanceof android.support.v7.widget.RecyclerView) return "RecyclerView";
		} catch (NoClassDefFoundError e) {
		} catch (Exception ex) {
		}
	    // 最后 fallback 为 View 或其自定义类名
	    return view.getClass().getSimpleName();
	}
	
}
