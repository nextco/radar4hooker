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
			return uiController.inspect(stringValue(arguments.get("format"), "json"));
		}
		if ("click_view".equals(name)) {
			return uiController.click_by_id(required(arguments, "id"));
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
		tools.add(tool("inspect_current_ui", "获取当前界面结构和 hooker_id。",
				schema(prop("format", "string", false, "返回格式，建议 json"))));
		tools.add(tool("click_view", "按 hooker_id 或资源 id 点击控件。",
				schema(prop("id", "string", true, "inspect 返回的 hooker_id 或 view id"))));
		tools.add(tool("set_text", "给 TextView/EditText 设置文本。",
				schema(prop("id", "string", true, "目标控件 id"), prop("text", "string", false, "要设置的文本"))));
		tools.add(tool("send_search_action", "对 EditText 触发搜索事件。",
				schema(prop("id", "string", true, "目标 EditText id"))));
		tools.add(tool("focus_view", "让控件获取焦点。",
				schema(prop("id", "string", true, "目标控件 id"))));
		tools.add(tool("go_back", "执行系统返回。", schema()));
		tools.add(tool("go_home", "回到系统桌面。", schema()));
		tools.add(tool("try_to_dismiss_dialog", "尝试强制关闭当前界面上的阻断性弹窗，适合处理没有关闭按钮的升级弹窗或营销弹窗。", schema()));
		tools.add(tool("swipe_view_pager", "控制 ViewPager/ViewPager2 向前或向后翻页。",
				schema(prop("id", "string", true, "pager 控件 id"), prop("direction", "string", false, "next 或 prev"))));
		tools.add(tool("scroll_recycler_by", "按偏移滚动 RecyclerView。",
				schema(prop("id", "string", true, "RecyclerView id"), prop("x", "integer", false, "横向偏移"),
						prop("y", "integer", false, "纵向偏移"))));
		tools.add(tool("scroll_recycler_to_position", "滚动 RecyclerView 到指定 position。",
				schema(prop("id", "string", true, "RecyclerView id"), prop("position", "integer", true, "目标位置"),
						prop("smooth", "boolean", false, "是否平滑滚动"))));
		tools.add(tool("get_app_info", "获取当前 App 的基础信息、组件、权限、签名和常见目录。", schema()));
		tools.add(tool("request_media_projection_permission", "发起 MediaProjection 动态授权请求，设备上会弹系统确认框。", schema()));
		tools.add(tool("get_media_projection_status", "查询 MediaProjection 当前授权状态。", schema()));
		tools.add(tool("capture_media_projection_screenshot", "使用 MediaProjection 截取整屏 PNG；调用前需要先完成授权。", schema()));
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
}
