package gz.radar;

import java.util.HashMap;
import java.util.Map;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import gz.util.X;

public class ViewXmlDumper {

	public static String viewToXml(View view) throws Exception {
		StringBuilder sb = new StringBuilder();
		buildXml(view, sb, 0);
		return sb.toString();
	}

	private static void buildXml(View view, StringBuilder sb, int indent) throws Exception {
		if (view.getVisibility() != View.VISIBLE) {
			return;
		}
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

		String bounds = view.getLeft() + "," + view.getTop() + "][" + view.getRight() + ","
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
        String xml = mapToXmlElement("view", attrs, indent);
		sb.append(prefix).append(xml);
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				buildXml(group.getChildAt(i), sb, indent + 1);
			}
		}
		sb.append(prefix).append("</view>\n");
	}
	
	public static String mapToXmlElement(String tagName, Map<String, String> attrs, int indent) {
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

	    sb.append(" >\n");
	    return sb.toString();
	}

	// 简单 XML 字符转义（必要时防止崩溃）
	private static String escapeXml(String input) {
		if (input == null || input.isEmpty())
			return "";
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

}
