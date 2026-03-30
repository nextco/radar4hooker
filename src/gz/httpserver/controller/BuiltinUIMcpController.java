package gz.httpserver.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestPostJson;

@HookerController("/hooker/mcp/ui/")
public class BuiltinUIMcpController {

	private final BuiltinUIServiceController uiController = new BuiltinUIServiceController();
	private final BuiltinAppInfoController appInfoController = new BuiltinAppInfoController();
	private final BuiltinMediaProjectionController mediaProjectionController = new BuiltinMediaProjectionController();

	@HookerRequestMapping(path = "tools", produces = Produces.AUTO, method = Method.GET)
	public Map<String, Object> tools() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("server", "hooker-ui-mcp");
		result.put("protocol", "simple-mcp-over-http");
		result.put("tools", buildTools());
		return result;
	}

	@HookerRequestMapping(path = "call", produces = Produces.AUTO, method = Method.POST)
	public Map<String, Object> call(@HookerRequestPostJson Map<String, Object> postJson) throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		String name = stringValue(postJson.get("name"));
		Map<String, Object> arguments = asMap(postJson.get("arguments"));
		if (name == null || name.length() == 0) {
			result.put("ok", false);
			result.put("error", "missing tool name");
			return result;
		}
		result.put("ok", true);
		result.put("tool", name);
		try {
			Object toolResult = callTool(name, arguments);
			result.put("result", toolResult);
		} catch (IllegalArgumentException e) {
			result.put("ok", false);
			result.put("error", e.getMessage());
		}
		return result;
	}

	private Object callTool(String name, Map<String, Object> arguments) throws Exception {
		if ("inspect_current_ui".equals(name)) {
			return uiController.inspect(stringValue(arguments.get("format"), "json"),
					stringValue(arguments.get("text_contains"), ""),
					stringValue(arguments.get("rect_limit"), "0,0,0,0"),
					stringValue(arguments.get("view_type"), ""),
					stringValue(arguments.get("class_name"), ""),
					stringValue(arguments.get("class_name_contains"), ""),
					intValue(arguments.get("is_image"), 0),
					intValue(arguments.get("is_edittext"), 0),
					intValue(arguments.get("is_listview"), 0),
					intValue(arguments.get("is_scrollview"), 0));
		}
		if ("inspect_overlay".equals(name)) {
			return uiController.inspect_overlay(
					stringValue(arguments.get("text_contains"), ""),
					stringValue(arguments.get("rect_limit"), "0,0,0,0"),
					stringValue(arguments.get("view_type"), ""),
					stringValue(arguments.get("class_name"), ""),
					stringValue(arguments.get("class_name_contains"), ""),
					intValue(arguments.get("is_image"), 0),
					intValue(arguments.get("is_edittext"), 0),
					intValue(arguments.get("is_listview"), 0),
					intValue(arguments.get("is_scrollview"), 0));
		}
		if ("click_view".equals(name)) {
			return uiController.click_by_id(required(arguments, "id"));
		}
		if ("long_click_view".equals(name)) {
			return uiController.long_click_view(required(arguments, "id"), longValue(arguments.get("duration_ms"), 800L));
		}
		if ("click_by_position".equals(name)) {
			return uiController.click_by_position(intValue(arguments.get("x"), 0), intValue(arguments.get("y"), 0));
		}
		if ("swipe_on_screen".equals(name)) {
			return uiController.swipe_on_screen(intValue(arguments.get("start_x"), 0),
					intValue(arguments.get("start_y"), 0),
					intValue(arguments.get("end_x"), 0),
					intValue(arguments.get("end_y"), 0),
					longValue(arguments.get("duration_ms"), 300L));
		}
		if ("swipe_view".equals(name)) {
			return uiController.swipe_view(required(arguments, "id"),
					stringValue(arguments.get("direction"), "up"),
					floatValue(arguments.get("distance_ratio"), 0.6f),
					longValue(arguments.get("duration_ms"), 300L));
		}
		if ("set_text".equals(name)) {
			return uiController.set_text(required(arguments, "id"), stringValue(arguments.get("text"), ""));
		}
		if ("send_search_action".equals(name)) {
			return uiController.send_search_action(required(arguments, "id"));
		}
		if ("focus_view".equals(name)) {
			return uiController.focus_on(required(arguments, "id"));
		}
		if ("go_back".equals(name)) {
			return uiController.back();
		}
		if ("go_home".equals(name)) {
			return uiController.home();
		}
		if ("finish_current_activity".equals(name)) {
			return uiController.finish_current_activity();
		}
		if ("try_to_dismiss_dialog".equals(name)) {
			return uiController.tryDismissByDialogFragment();
		}
		if ("swipe_view_pager".equals(name)) {
			return uiController.viewPageSwipe(required(arguments, "id"), stringValue(arguments.get("direction"), "next"));
		}
		if ("scroll_recycler_by".equals(name)) {
			return uiController.scrollRecyclerBy(required(arguments, "id"), intValue(arguments.get("x"), 0),
					intValue(arguments.get("y"), 0));
		}
		if ("scroll_recycler_to_position".equals(name)) {
			String id = required(arguments, "id");
			int position = intValue(arguments.get("position"), 0);
			boolean smooth = booleanValue(arguments.get("smooth"), false);
			if (smooth) {
				return uiController.smoothScrollRecyclerToPosition(id, position);
			}
			return uiController.scrollRecyclerToPosition(id, position);
		}
		if ("set_checked".equals(name)) {
			return uiController.set_checked(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("checked"), -1)));
		}
		if ("set_progress".equals(name)) {
			return uiController.set_progress(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("progress"), 0)));
		}
		if ("set_rating".equals(name)) {
			return uiController.set_rating(required(arguments, "id"), floatValue(arguments.get("rating"), 0f));
		}
		if ("spinner_set_selection".equals(name)) {
			return uiController.spinner_set_selection(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("position"), 0)));
		}
		if ("adapter_view_scroll_to_position".equals(name)) {
			return uiController.adapter_view_scroll_to_position(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("position"), 0)));
		}
		if ("adapter_view_click_position".equals(name)) {
			return uiController.adapter_view_click_position(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("position"), 0)));
		}
		if ("scroll_view_scroll_to".equals(name)) {
			return uiController.scroll_view_scroll_to(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("x"), 0)),
					Integer.valueOf(intValue(arguments.get("y"), 0)));
		}
		if ("scroll_view_scroll_by".equals(name)) {
			return uiController.scroll_view_scroll_by(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("x"), 0)),
					Integer.valueOf(intValue(arguments.get("y"), 0)));
		}
		if ("web_view_load_url".equals(name)) {
			return uiController.web_view_load_url(required(arguments, "id"), required(arguments, "url"));
		}
		if ("web_view_go_back".equals(name)) {
			return uiController.web_view_go_back(required(arguments, "id"));
		}
		if ("web_view_go_forward".equals(name)) {
			return uiController.web_view_go_forward(required(arguments, "id"));
		}
		if ("video_view_start".equals(name)) {
			return uiController.video_view_start(required(arguments, "id"));
		}
		if ("video_view_pause".equals(name)) {
			return uiController.video_view_pause(required(arguments, "id"));
		}
		if ("video_view_seek_to".equals(name)) {
			return uiController.video_view_seek_to(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("msec"), 0)));
		}
		if ("drawer_open".equals(name)) {
			return uiController.drawer_open(required(arguments, "id"));
		}
		if ("drawer_close".equals(name)) {
			return uiController.drawer_close(required(arguments, "id"));
		}
		if ("tab_layout_select".equals(name)) {
			return uiController.tab_layout_select(required(arguments, "id"), Integer.valueOf(intValue(arguments.get("position"), 0)));
		}
		if ("view_stub_inflate".equals(name)) {
			return uiController.view_stub_inflate(required(arguments, "id"));
		}
		if ("get_app_info".equals(name)) {
			return appInfoController.app_info();
		}
		if ("request_media_projection_permission".equals(name)) {
			return mediaProjectionController.requestPermission();
		}
		if ("get_media_projection_status".equals(name)) {
			return mediaProjectionController.status();
		}
		if ("capture_media_projection_screenshot".equals(name)) {
			return mediaProjectionController.screenshotForMcp();
		}
		if ("get_screen_info".equals(name)) {
			return uiController.screen_info();
		}
		if ("get_activity_stack".equals(name)) {
			return uiController.activity_stack();
		}
		if ("get_shared_prefs".equals(name)) {
			return appInfoController.shared_prefs();
		}
		if ("get_databases".equals(name)) {
			return appInfoController.databases();
		}
		if ("read_database_table".equals(name)) {
			return appInfoController.read_table(required(arguments, "database"), required(arguments, "table"),
					intValue(arguments.get("limit"), 100));
		}
		throw new IllegalArgumentException("unknown tool: " + name);
	}

	private List<Map<String, Object>> buildTools() {
		List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>();
		String actionVerifyHint = "执行后应立即调用 capture_media_projection_screenshot（底层接口 /hooker/mediaprojection/screenshot）观察界面变化，确认操作是否成功。";
		tools.add(tool("inspect_current_ui", "获取当前界面的重要 View 列表和 hooker_id，支持按文本、矩形区域、视图类型、类名以及常见控件类别过滤。",
				schema(prop("format", "string", false, "返回格式，建议 json"),
						prop("text_contains", "string", false, "按 text/hint/contentDescription 做包含匹配，大小写不敏感"),
						prop("rect_limit", "string", false, "屏幕区域过滤，格式 left,top,right,bottom"),
						prop("view_type", "string", false, "按视图类型精确匹配，例如 TextView、EditText、RecyclerView"),
						prop("class_name", "string", false, "按完整类名精确匹配，例如 android.widget.TextView"),
						prop("class_name_contains", "string", false, "按完整类名包含匹配"),
						prop("is_image", "integer", false, "为 1 时只返回 ImageView / ImageButton"),
						prop("is_edittext", "integer", false, "为 1 时只返回 EditText"),
						prop("is_listview", "integer", false, "为 1 时只返回 ListView / GridView / RecyclerView"),
						prop("is_scrollview", "integer", false, "为 1 时只返回 ScrollView / HorizontalScrollView"))));
		tools.add(tool("inspect_overlay", "截取当前界面并将 inspect 匹配到的控件 screen_rectangle 直接画框到图片上，返回 overlay 图片地址，便于模型按图理解布局。",
				schema(prop("text_contains", "string", false, "按 text/hint/contentDescription 做包含匹配，大小写不敏感"),
						prop("rect_limit", "string", false, "屏幕区域过滤，格式 left,top,right,bottom"),
						prop("view_type", "string", false, "按视图类型精确匹配，例如 TextView、EditText、RecyclerView"),
						prop("class_name", "string", false, "按完整类名精确匹配，例如 android.widget.TextView"),
						prop("class_name_contains", "string", false, "按完整类名包含匹配"),
						prop("is_image", "integer", false, "为 1 时只返回 ImageView / ImageButton"),
						prop("is_edittext", "integer", false, "为 1 时只返回 EditText"),
						prop("is_listview", "integer", false, "为 1 时只返回 ListView / GridView / RecyclerView"),
						prop("is_scrollview", "integer", false, "为 1 时只返回 ScrollView / HorizontalScrollView"))));
		tools.add(tool("click_view", "按 hooker_id 或资源 id 点击控件。" + actionVerifyHint,
				schema(prop("id", "string", true, "inspect 返回的 hooker_id 或 view id"))));
		tools.add(tool("long_click_view", "按 hooker_id 或资源 id 长按控件。" + actionVerifyHint,
				schema(prop("id", "string", true, "inspect 返回的 hooker_id 或 view id"),
						prop("duration_ms", "integer", false, "长按时长，默认 800ms"))));
		tools.add(tool("click_by_position", "按屏幕坐标点击控件。" + actionVerifyHint,
				schema(prop("x", "integer", true, "屏幕 x 坐标"),
						prop("y", "integer", true, "屏幕 y 坐标"))));
		tools.add(tool("swipe_on_screen", "按起止坐标在屏幕上滑动。" + actionVerifyHint,
				schema(prop("start_x", "integer", true, "起点 x 坐标"),
						prop("start_y", "integer", true, "起点 y 坐标"),
						prop("end_x", "integer", true, "终点 x 坐标"),
						prop("end_y", "integer", true, "终点 y 坐标"),
						prop("duration_ms", "integer", false, "滑动时长，默认 300ms"))));
		tools.add(tool("swipe_view", "在指定控件区域内部按方向滑动。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标控件 id"),
						prop("direction", "string", false, "up/down/left/right，默认 up"),
						prop("distance_ratio", "number", false, "滑动距离占控件可用区域的比例，默认 0.6"),
						prop("duration_ms", "integer", false, "滑动时长，默认 300ms"))));
		tools.add(tool("set_text", "给 TextView/EditText 设置文本。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标控件 id"), prop("text", "string", false, "要设置的文本"))));
		tools.add(tool("send_search_action", "对 EditText 触发搜索事件。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 EditText id"))));
		tools.add(tool("focus_view", "让控件获取焦点。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标控件 id"))));
		tools.add(tool("go_back", "执行系统返回。" + actionVerifyHint, schema()));
		tools.add(tool("go_home", "回到系统桌面。" + actionVerifyHint, schema()));
		tools.add(tool("finish_current_activity", "关闭当前顶部 Activity。" + actionVerifyHint, schema()));
		tools.add(tool("try_to_dismiss_dialog", "尝试强制关闭当前界面上的阻断性弹窗，适合处理没有关闭按钮的升级弹窗或营销弹窗。" + actionVerifyHint, schema()));
		tools.add(tool("swipe_view_pager", "控制 ViewPager/ViewPager2 向前或向后翻页。" + actionVerifyHint,
				schema(prop("id", "string", true, "pager 控件 id"), prop("direction", "string", false, "next 或 prev"))));
		tools.add(tool("scroll_recycler_by", "按偏移滚动 RecyclerView。" + actionVerifyHint,
				schema(prop("id", "string", true, "RecyclerView id"), prop("x", "integer", false, "横向偏移"),
						prop("y", "integer", false, "纵向偏移"))));
		tools.add(tool("scroll_recycler_to_position", "滚动 RecyclerView 到指定 position。" + actionVerifyHint,
				schema(prop("id", "string", true, "RecyclerView id"), prop("position", "integer", true, "目标位置"),
						prop("smooth", "boolean", false, "是否平滑滚动"))));
		tools.add(tool("set_checked", "设置或切换 CheckBox、RadioButton、ToggleButton、Switch 等选中状态。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标控件 id"),
						prop("checked", "integer", false, "0 表示 false，1 表示 true；不传时切换当前状态"))));
		tools.add(tool("set_progress", "设置 ProgressBar/SeekBar 当前进度。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标控件 id"),
						prop("progress", "integer", true, "目标进度值"))));
		tools.add(tool("set_rating", "设置 RatingBar 当前评分。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 RatingBar id"),
						prop("rating", "number", true, "目标评分值"))));
		tools.add(tool("spinner_set_selection", "设置 Spinner 选中项。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 Spinner id"),
						prop("position", "integer", true, "目标位置"))));
		tools.add(tool("adapter_view_scroll_to_position", "滚动 ListView/GridView 等 AdapterView 到指定位置。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 AdapterView id"),
						prop("position", "integer", true, "目标位置"))));
		tools.add(tool("adapter_view_click_position", "点击 ListView/GridView 等 AdapterView 的指定位置。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 AdapterView id"),
						prop("position", "integer", true, "目标位置"))));
		tools.add(tool("scroll_view_scroll_to", "滚动 ScrollView/HorizontalScrollView/NestedScrollView 到指定坐标。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标滚动容器 id"),
						prop("x", "integer", false, "目标 x 坐标"),
						prop("y", "integer", false, "目标 y 坐标"))));
		tools.add(tool("scroll_view_scroll_by", "按偏移量滚动 ScrollView/HorizontalScrollView/NestedScrollView。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标滚动容器 id"),
						prop("x", "integer", false, "横向偏移"),
						prop("y", "integer", false, "纵向偏移"))));
		tools.add(tool("web_view_load_url", "让 WebView 加载指定 URL。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 WebView id"),
						prop("url", "string", true, "要加载的 URL"))));
		tools.add(tool("web_view_go_back", "让 WebView 返回上一页。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 WebView id"))));
		tools.add(tool("web_view_go_forward", "让 WebView 前进到下一页。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 WebView id"))));
		tools.add(tool("video_view_start", "开始播放 VideoView。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 VideoView id"))));
		tools.add(tool("video_view_pause", "暂停 VideoView。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 VideoView id"))));
		tools.add(tool("video_view_seek_to", "设置 VideoView 播放进度。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 VideoView id"),
						prop("msec", "integer", true, "目标毫秒位置"))));
		tools.add(tool("drawer_open", "打开 DrawerLayout 抽屉。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 DrawerLayout id"))));
		tools.add(tool("drawer_close", "关闭 DrawerLayout 抽屉。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 DrawerLayout id"))));
		tools.add(tool("tab_layout_select", "切换 TabLayout 到指定 tab。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 TabLayout id"),
						prop("position", "integer", true, "目标 tab 下标"))));
		tools.add(tool("view_stub_inflate", "触发 ViewStub inflate。" + actionVerifyHint,
				schema(prop("id", "string", true, "目标 ViewStub id"))));
		tools.add(tool("get_app_info", "获取当前 App 的基础信息、组件、权限、签名和常见目录。", schema()));
		tools.add(tool("get_screen_info", "获取当前屏幕和应用窗口的基础显示信息，包括像素尺寸、density、方向和旋转角度。", schema()));
		tools.add(tool("get_activity_stack", "获取当前进程内 Activity 栈信息，包括顶部 Activity、title、paused、stopped 状态。", schema()));
		tools.add(tool("request_media_projection_permission", "发起 MediaProjection 动态授权请求，设备上会弹系统确认框。", schema()));
		tools.add(tool("get_media_projection_status", "查询 MediaProjection 当前授权状态。", schema()));
		tools.add(tool("capture_media_projection_screenshot", "使用 MediaProjection 截取整屏图片；默认返回 JPEG，可通过底层接口指定 format=jpeg|png、quality=0-100。", schema()));
		tools.add(tool("get_shared_prefs", "读取当前 App 的 shared_prefs 内容。", schema()));
		tools.add(tool("get_databases", "列出当前 App 的数据库、表和字段结构。", schema()));
		tools.add(tool("read_database_table", "读取指定数据库表的数据，适合排查本地缓存和业务状态。",
				schema(prop("database", "string", true, "数据库文件名，例如 xxx.db"),
						prop("table", "string", true, "表名"),
						prop("limit", "integer", false, "最多读取多少行，默认 100，最大 1000"))));
		return tools;
	}

	private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
		Map<String, Object> tool = new LinkedHashMap<String, Object>();
		tool.put("name", name);
		tool.put("description", description);
		tool.put("input_schema", inputSchema);
		return tool;
	}

	private Map<String, Object> schema(Map<String, Object>... props) {
		Map<String, Object> schema = new LinkedHashMap<String, Object>();
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		List<String> required = new ArrayList<String>();
		for (Map<String, Object> prop : props) {
			properties.put(stringValue(prop.get("name")), prop);
			if (booleanValue(prop.get("required"), false)) {
				required.add(stringValue(prop.get("name")));
			}
		}
		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", required);
		return schema;
	}

	private Map<String, Object> prop(String name, String type, boolean required, String description) {
		Map<String, Object> prop = new LinkedHashMap<String, Object>();
		prop.put("name", name);
		prop.put("type", type);
		prop.put("required", required);
		prop.put("description", description);
		return prop;
	}

	private String required(Map<String, Object> arguments, String key) {
		String value = stringValue(arguments.get(key));
		if (value == null || value.length() == 0) {
			throw new IllegalArgumentException("missing argument: " + key);
		}
		return value;
	}

	private Map<String, Object> asMap(Object value) {
		if (value instanceof Map) {
			return (Map<String, Object>) value;
		}
		return new LinkedHashMap<String, Object>();
	}

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private String stringValue(Object value, String defaultValue) {
		String result = stringValue(value);
		return result == null || result.length() == 0 ? defaultValue : result;
	}

	private int intValue(Object value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private boolean booleanValue(Object value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		return Boolean.parseBoolean(String.valueOf(value));
	}

	private float floatValue(Object value, float defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).floatValue();
		}
		try {
			return Float.parseFloat(String.valueOf(value));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private long longValue(Object value, long defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (Exception e) {
			return defaultValue;
		}
	}
}
