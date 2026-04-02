#!/usr/bin/env node
const fs = require("node:fs");
const path = require("node:path");
const process = require("node:process");
const { pathToFileURL } = require("node:url");

const DEFAULT_BASE_URL = "http://10.112.101.249:8080";
const DEFAULT_PROTOCOL_VERSION = "2024-11-05";
const DEFAULT_SDK_ROOT = "/Users/stephen256/JSReverser-MCP/node_modules/@modelcontextprotocol/sdk/dist/esm";

function schema(...props) {
  const properties = {};
  const required = [];
  for (const prop of props) {
    properties[prop.name] = prop;
    if (prop.required) {
      required.push(prop.name);
    }
  }
  return {
    type: "object",
    properties,
    required,
    additionalProperties: false,
  };
}

function prop(name, type, required, description) {
  return { name, type, required, description };
}

function parseCliArgs(argv) {
  const result = {
    baseUrl: process.env.HOOKER_BASE_URL || DEFAULT_BASE_URL,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--baseUrl") {
      result.baseUrl = argv[index + 1] || result.baseUrl;
      index += 1;
    }
  }
  return result;
}

function writeLog(text) {
  fs.writeSync(process.stderr.fd, `${String(text).replace(/\s+$/, "")}\n`);
}

async function loadSdk() {
  const sdkRoot = process.env.MCP_SDK_ROOT || DEFAULT_SDK_ROOT;
  const serverIndex = path.join(sdkRoot, "server/index.js");
  const stdioPath = path.join(sdkRoot, "server/stdio.js");
  const typesPath = path.join(sdkRoot, "types.js");
  for (const requiredPath of [serverIndex, stdioPath, typesPath]) {
    if (!fs.existsSync(requiredPath)) {
      throw new Error(`official MCP SDK not found: ${requiredPath}`);
    }
  }
  const [serverMod, stdioMod, typesMod] = await Promise.all([
    import(pathToFileURL(serverIndex).href),
    import(pathToFileURL(stdioPath).href),
    import(pathToFileURL(typesPath).href),
  ]);
  return {
    Server: serverMod.Server,
    StdioServerTransport: stdioMod.StdioServerTransport,
    ListToolsRequestSchema: typesMod.ListToolsRequestSchema,
    CallToolRequestSchema: typesMod.CallToolRequestSchema,
    PingRequestSchema: typesMod.PingRequestSchema,
  };
}

const TOOLS = [
  {
    name: "inspect_current_ui",
    description: "获取当前界面的重要 View 列表和 hooker_id，支持按文本、矩形区域、视图类型、类名以及常见控件类别过滤。",
    inputSchema: schema(
      prop("format", "string", false, "返回格式，建议 json"),
      prop("text_contains", "string", false, "按 text/hint/contentDescription 做包含匹配，大小写不敏感"),
      prop("rect_limit", "string", false, "屏幕区域过滤，格式 left,top,right,bottom"),
      prop("view_type", "string", false, "按视图类型精确匹配，例如 TextView、EditText、RecyclerView"),
      prop("class_name", "string", false, "按完整类名精确匹配，例如 android.widget.TextView"),
      prop("class_name_contains", "string", false, "按完整类名包含匹配"),
      prop("is_image", "integer", false, "为 1 时只返回 ImageView / ImageButton"),
      prop("is_edittext", "integer", false, "为 1 时只返回 EditText"),
      prop("is_listview", "integer", false, "为 1 时只返回 ListView / GridView / RecyclerView"),
      prop("is_scrollview", "integer", false, "为 1 时只返回 ScrollView / HorizontalScrollView")
    ),
  },
  {
    name: "inspect_overlay",
    description: "截取当前界面并将 inspect 匹配到的控件 screen_rectangle 直接画框到图片上，返回 overlay 图片地址，便于模型按图理解布局。",
    inputSchema: schema(
      prop("text_contains", "string", false, "按 text/hint/contentDescription 做包含匹配，大小写不敏感"),
      prop("rect_limit", "string", false, "屏幕区域过滤，格式 left,top,right,bottom"),
      prop("view_type", "string", false, "按视图类型精确匹配，例如 TextView、EditText、RecyclerView"),
      prop("class_name", "string", false, "按完整类名精确匹配，例如 android.widget.TextView"),
      prop("class_name_contains", "string", false, "按完整类名包含匹配"),
      prop("is_image", "integer", false, "为 1 时只返回 ImageView / ImageButton"),
      prop("is_edittext", "integer", false, "为 1 时只返回 EditText"),
      prop("is_listview", "integer", false, "为 1 时只返回 ListView / GridView / RecyclerView"),
      prop("is_scrollview", "integer", false, "为 1 时只返回 ScrollView / HorizontalScrollView")
    ),
  },
  { name: "click_view", description: "按 hooker_id 或资源 id 点击控件。", inputSchema: schema(prop("id", "string", true, "inspect 返回的 hooker_id 或 view id")) },
  { name: "long_click_view", description: "按 hooker_id 或资源 id 长按控件。", inputSchema: schema(prop("id", "string", true, "inspect 返回的 hooker_id 或 view id"), prop("duration_ms", "integer", false, "长按时长，默认 800ms")) },
  { name: "click_by_position", description: "按屏幕坐标点击控件。", inputSchema: schema(prop("x", "integer", true, "屏幕 x 坐标"), prop("y", "integer", true, "屏幕 y 坐标")) },
  { name: "swipe_on_screen", description: "按起止坐标在屏幕上滑动。", inputSchema: schema(prop("start_x", "integer", true, "起点 x 坐标"), prop("start_y", "integer", true, "起点 y 坐标"), prop("end_x", "integer", true, "终点 x 坐标"), prop("end_y", "integer", true, "终点 y 坐标"), prop("duration_ms", "integer", false, "滑动时长，默认 300ms")) },
  { name: "swipe_view", description: "在指定控件区域内部按方向滑动。", inputSchema: schema(prop("id", "string", true, "目标控件 id"), prop("direction", "string", false, "up/down/left/right，默认 up"), prop("distance_ratio", "number", false, "滑动距离占控件可用区域的比例，默认 0.6"), prop("duration_ms", "integer", false, "滑动时长，默认 300ms")) },
  { name: "set_text", description: "给 TextView/EditText 设置文本，底层走 JSON POST，避免 GET 参数中文乱码。", inputSchema: schema(prop("id", "string", true, "目标控件 id"), prop("text", "string", false, "要设置的文本")) },
  { name: "send_search_action", description: "对 EditText 触发搜索事件。", inputSchema: schema(prop("id", "string", true, "目标 EditText id")) },
  { name: "focus_view", description: "让控件获取焦点。", inputSchema: schema(prop("id", "string", true, "目标控件 id")) },
  { name: "go_back", description: "执行系统返回。", inputSchema: schema() },
  { name: "go_home", description: "回到系统桌面。", inputSchema: schema() },
  { name: "finish_current_activity", description: "关闭当前顶部 Activity。", inputSchema: schema() },
  { name: "try_to_dismiss_dialog", description: "尝试强制关闭当前界面上的阻断性弹窗，适合处理没有关闭按钮的升级弹窗或营销弹窗。", inputSchema: schema() },
  { name: "swipe_view_pager", description: "控制 ViewPager/ViewPager2 向前或向后翻页。", inputSchema: schema(prop("id", "string", true, "pager 控件 id"), prop("direction", "string", false, "next 或 prev")) },
  { name: "scroll_recycler_by", description: "按偏移滚动 RecyclerView。", inputSchema: schema(prop("id", "string", true, "RecyclerView id"), prop("x", "integer", false, "横向偏移"), prop("y", "integer", false, "纵向偏移")) },
  { name: "scroll_recycler_to_position", description: "滚动 RecyclerView 到指定 position。", inputSchema: schema(prop("id", "string", true, "RecyclerView id"), prop("position", "integer", true, "目标位置"), prop("smooth", "boolean", false, "是否平滑滚动")) },
  { name: "set_checked", description: "设置或切换 CheckBox、RadioButton、ToggleButton、Switch 等选中状态。", inputSchema: schema(prop("id", "string", true, "目标控件 id"), prop("checked", "integer", false, "0 表示 false，1 表示 true；不传时切换当前状态")) },
  { name: "set_progress", description: "设置 ProgressBar/SeekBar 当前进度。", inputSchema: schema(prop("id", "string", true, "目标控件 id"), prop("progress", "integer", true, "目标进度值")) },
  { name: "set_rating", description: "设置 RatingBar 当前评分。", inputSchema: schema(prop("id", "string", true, "目标 RatingBar id"), prop("rating", "number", true, "目标评分值")) },
  { name: "spinner_set_selection", description: "设置 Spinner 选中项。", inputSchema: schema(prop("id", "string", true, "目标 Spinner id"), prop("position", "integer", true, "目标位置")) },
  { name: "adapter_view_scroll_to_position", description: "滚动 ListView/GridView 等 AdapterView 到指定位置。", inputSchema: schema(prop("id", "string", true, "目标 AdapterView id"), prop("position", "integer", true, "目标位置")) },
  { name: "adapter_view_click_position", description: "点击 ListView/GridView 等 AdapterView 的指定位置。", inputSchema: schema(prop("id", "string", true, "目标 AdapterView id"), prop("position", "integer", true, "目标位置")) },
  { name: "scroll_view_scroll_to", description: "滚动 ScrollView/HorizontalScrollView/NestedScrollView 到指定坐标。", inputSchema: schema(prop("id", "string", true, "目标滚动容器 id"), prop("x", "integer", false, "目标 x 坐标"), prop("y", "integer", false, "目标 y 坐标")) },
  { name: "scroll_view_scroll_by", description: "按偏移量滚动 ScrollView/HorizontalScrollView/NestedScrollView。", inputSchema: schema(prop("id", "string", true, "目标滚动容器 id"), prop("x", "integer", false, "横向偏移"), prop("y", "integer", false, "纵向偏移")) },
  { name: "web_view_load_url", description: "让 WebView 加载指定 URL。", inputSchema: schema(prop("id", "string", true, "目标 WebView id"), prop("url", "string", true, "要加载的 URL")) },
  { name: "web_view_go_back", description: "让 WebView 返回上一页。", inputSchema: schema(prop("id", "string", true, "目标 WebView id")) },
  { name: "web_view_go_forward", description: "让 WebView 前进到下一页。", inputSchema: schema(prop("id", "string", true, "目标 WebView id")) },
  { name: "video_view_start", description: "开始播放 VideoView。", inputSchema: schema(prop("id", "string", true, "目标 VideoView id")) },
  { name: "video_view_pause", description: "暂停 VideoView。", inputSchema: schema(prop("id", "string", true, "目标 VideoView id")) },
  { name: "video_view_seek_to", description: "设置 VideoView 播放进度。", inputSchema: schema(prop("id", "string", true, "目标 VideoView id"), prop("msec", "integer", true, "目标毫秒位置")) },
  { name: "drawer_open", description: "打开 DrawerLayout 抽屉。", inputSchema: schema(prop("id", "string", true, "目标 DrawerLayout id")) },
  { name: "drawer_close", description: "关闭 DrawerLayout 抽屉。", inputSchema: schema(prop("id", "string", true, "目标 DrawerLayout id")) },
  { name: "tab_layout_select", description: "切换 TabLayout 到指定 tab。", inputSchema: schema(prop("id", "string", true, "目标 TabLayout id"), prop("position", "integer", true, "目标 tab 下标")) },
  { name: "view_stub_inflate", description: "触发 ViewStub inflate。", inputSchema: schema(prop("id", "string", true, "目标 ViewStub id")) },
  { name: "get_app_info", description: "获取当前 App 的基础信息、组件、权限、签名和常见目录。", inputSchema: schema() },
  { name: "get_screen_info", description: "获取当前屏幕和应用窗口的基础显示信息，包括像素尺寸、density、方向和旋转角度。", inputSchema: schema() },
  { name: "get_activity_stack", description: "获取当前进程内 Activity 栈信息，包括顶部 Activity、title、paused、stopped 状态。", inputSchema: schema() },
  { name: "request_media_projection_permission", description: "发起 MediaProjection 动态授权请求，设备上会弹系统确认框。", inputSchema: schema() },
  { name: "get_media_projection_status", description: "查询 MediaProjection 当前授权状态。", inputSchema: schema() },
  { name: "capture_media_projection_screenshot", description: "使用 MediaProjection 截取整屏图片；默认返回 JPEG，可通过底层接口指定 format=jpeg|png、quality=0-100。", inputSchema: schema() },
  { name: "get_shared_prefs", description: "读取当前 App 的 shared_prefs 内容。", inputSchema: schema() },
  { name: "get_databases", description: "列出当前 App 的数据库、表和字段结构。", inputSchema: schema() },
  { name: "read_database_table", description: "读取指定数据库表的数据，适合排查本地缓存和业务状态。", inputSchema: schema(prop("database", "string", true, "数据库文件名，例如 xxx.db"), prop("table", "string", true, "表名"), prop("limit", "integer", false, "最多读取多少行，默认 100，最大 1000")) },
];

class HookerUiBridge {
  constructor(options = {}) {
    this.baseUrl = (options.baseUrl || DEFAULT_BASE_URL).replace(/\/+$/, "");
    this.serverName = "hooker-ui-mcp-bridge";
    this.serverVersion = "0.2.0";
    this.tools = new Map(TOOLS.map((tool) => [tool.name, tool]));
    writeLog(`server boot: name=${this.serverName} version=${this.serverVersion} base_url=${this.baseUrl} pid=${process.pid}`);
  }

  async callTool(name, arguments_) {
    if (!this.tools.has(name)) {
      throw new Error(`unknown tool: ${name}`);
    }
    if (name === "inspect_current_ui") {
      return this.httpGet("/hooker/ui/inspect", {
        format: this.string(arguments_.format, "json"),
        text_contains: this.string(arguments_.text_contains, ""),
        rect_limit: this.string(arguments_.rect_limit, "0,0,0,0"),
        view_type: this.string(arguments_.view_type, ""),
        class_name: this.string(arguments_.class_name, ""),
        class_name_contains: this.string(arguments_.class_name_contains, ""),
        is_image: this.int(arguments_.is_image, 0),
        is_edittext: this.int(arguments_.is_edittext, 0),
        is_listview: this.int(arguments_.is_listview, 0),
        is_scrollview: this.int(arguments_.is_scrollview, 0),
      });
    }
    if (name === "inspect_overlay") {
      return this.httpGet("/hooker/ui/inspect_overlay", {
        text_contains: this.string(arguments_.text_contains, ""),
        rect_limit: this.string(arguments_.rect_limit, "0,0,0,0"),
        view_type: this.string(arguments_.view_type, ""),
        class_name: this.string(arguments_.class_name, ""),
        class_name_contains: this.string(arguments_.class_name_contains, ""),
        is_image: this.int(arguments_.is_image, 0),
        is_edittext: this.int(arguments_.is_edittext, 0),
        is_listview: this.int(arguments_.is_listview, 0),
        is_scrollview: this.int(arguments_.is_scrollview, 0),
      });
    }
    if (name === "click_view") return this.httpGet("/hooker/ui/click_by_id", { id: this.required(arguments_, "id") });
    if (name === "long_click_view") return this.httpGet("/hooker/ui/long_click_view", { id: this.required(arguments_, "id"), duration_ms: this.int(arguments_.duration_ms, 800) });
    if (name === "click_by_position") return this.httpGet("/hooker/ui/click_by_position", { x: this.requiredInt(arguments_, "x"), y: this.requiredInt(arguments_, "y") });
    if (name === "swipe_on_screen") return this.httpGet("/hooker/ui/swipe_on_screen", { start_x: this.requiredInt(arguments_, "start_x"), start_y: this.requiredInt(arguments_, "start_y"), end_x: this.requiredInt(arguments_, "end_x"), end_y: this.requiredInt(arguments_, "end_y"), duration_ms: this.int(arguments_.duration_ms, 300) });
    if (name === "swipe_view") return this.httpGet("/hooker/ui/swipe_view", { id: this.required(arguments_, "id"), direction: this.string(arguments_.direction, "up"), distance_ratio: this.float(arguments_.distance_ratio, 0.6), duration_ms: this.int(arguments_.duration_ms, 300) });
    if (name === "set_text") return this.httpPost("/hooker/ui/set_text_json", { id: this.required(arguments_, "id"), text: this.string(arguments_.text, "") });
    if (name === "send_search_action") return this.httpGet("/hooker/ui/send_search_action", { id: this.required(arguments_, "id") });
    if (name === "focus_view") return this.httpGet("/hooker/ui/focus_on", { id: this.required(arguments_, "id") });
    if (name === "go_back") return this.httpGet("/hooker/ui/back");
    if (name === "go_home") return this.httpGet("/hooker/ui/home");
    if (name === "finish_current_activity") return this.httpGet("/hooker/ui/finish_current_activity");
    if (name === "try_to_dismiss_dialog") return this.httpGet("/hooker/ui/try_to_dismiss_dialog");
    if (name === "swipe_view_pager") return this.httpGet("/hooker/ui/view_page_swipe", { id: this.required(arguments_, "id"), direction: this.string(arguments_.direction, "next") });
    if (name === "scroll_recycler_by") return this.httpGet("/hooker/ui/recycler_view_scroll_by", { id: this.required(arguments_, "id"), x: this.int(arguments_.x, 0), y: this.int(arguments_.y, 0) });
    if (name === "scroll_recycler_to_position") return this.httpGet(this.bool(arguments_.smooth, false) ? "/hooker/ui/smooth_scroll_recycler_to_position" : "/hooker/ui/scroll_recycler_to_position", { id: this.required(arguments_, "id"), position: this.requiredInt(arguments_, "position") });
    if (name === "set_checked") return this.httpGet("/hooker/ui/set_checked", { id: this.required(arguments_, "id"), checked: this.int(arguments_.checked, -1) });
    if (name === "set_progress") return this.httpGet("/hooker/ui/set_progress", { id: this.required(arguments_, "id"), progress: this.requiredInt(arguments_, "progress") });
    if (name === "set_rating") return this.httpGet("/hooker/ui/set_rating", { id: this.required(arguments_, "id"), rating: this.requiredFloat(arguments_, "rating") });
    if (name === "spinner_set_selection") return this.httpGet("/hooker/ui/spinner_set_selection", { id: this.required(arguments_, "id"), position: this.requiredInt(arguments_, "position") });
    if (name === "adapter_view_scroll_to_position") return this.httpGet("/hooker/ui/adapter_view_scroll_to_position", { id: this.required(arguments_, "id"), position: this.requiredInt(arguments_, "position") });
    if (name === "adapter_view_click_position") return this.httpGet("/hooker/ui/adapter_view_click_position", { id: this.required(arguments_, "id"), position: this.requiredInt(arguments_, "position") });
    if (name === "scroll_view_scroll_to") return this.httpGet("/hooker/ui/scroll_view_scroll_to", { id: this.required(arguments_, "id"), x: this.int(arguments_.x, 0), y: this.int(arguments_.y, 0) });
    if (name === "scroll_view_scroll_by") return this.httpGet("/hooker/ui/scroll_view_scroll_by", { id: this.required(arguments_, "id"), x: this.int(arguments_.x, 0), y: this.int(arguments_.y, 0) });
    if (name === "web_view_load_url") return this.httpGet("/hooker/ui/web_view_load_url", { id: this.required(arguments_, "id"), url: this.required(arguments_, "url") });
    if (name === "web_view_go_back") return this.httpGet("/hooker/ui/web_view_go_back", { id: this.required(arguments_, "id") });
    if (name === "web_view_go_forward") return this.httpGet("/hooker/ui/web_view_go_forward", { id: this.required(arguments_, "id") });
    if (name === "video_view_start") return this.httpGet("/hooker/ui/video_view_start", { id: this.required(arguments_, "id") });
    if (name === "video_view_pause") return this.httpGet("/hooker/ui/video_view_pause", { id: this.required(arguments_, "id") });
    if (name === "video_view_seek_to") return this.httpGet("/hooker/ui/video_view_seek_to", { id: this.required(arguments_, "id"), msec: this.requiredInt(arguments_, "msec") });
    if (name === "drawer_open") return this.httpGet("/hooker/ui/drawer_open", { id: this.required(arguments_, "id") });
    if (name === "drawer_close") return this.httpGet("/hooker/ui/drawer_close", { id: this.required(arguments_, "id") });
    if (name === "tab_layout_select") return this.httpGet("/hooker/ui/tab_layout_select", { id: this.required(arguments_, "id"), position: this.requiredInt(arguments_, "position") });
    if (name === "view_stub_inflate") return this.httpGet("/hooker/ui/view_stub_inflate", { id: this.required(arguments_, "id") });
    if (name === "get_app_info") return this.httpGet("/hooker/appinfo");
    if (name === "request_media_projection_permission") return this.httpGet("/hooker/mediaprojection/request_permission");
    if (name === "get_media_projection_status") return this.httpGet("/hooker/mediaprojection/status");
    if (name === "capture_media_projection_screenshot") return this.captureMediaProjectionScreenshot();
    if (name === "get_screen_info") return this.httpGet("/hooker/ui/screen_info");
    if (name === "get_activity_stack") return this.httpGet("/hooker/ui/activity_stack");
    if (name === "get_shared_prefs") return this.httpGet("/hooker/appinfo/shared_prefs");
    if (name === "get_databases") return this.httpGet("/hooker/appinfo/databases");
    if (name === "read_database_table") return this.httpGet("/hooker/appinfo/read_table", { database: this.required(arguments_, "database"), table: this.required(arguments_, "table"), limit: this.int(arguments_.limit, 100) });
    throw new Error(`unknown tool: ${name}`);
  }

  async captureMediaProjectionScreenshot() {
    const status = await this.httpGet("/hooker/mediaprojection/status");
    const hasPermission = Boolean(status.hasPermission || status.authorized || status.granted);
    if (!hasPermission) {
      return {
        ok: false,
        authorized: false,
        error: "MediaProjection permission not granted",
        status,
        endpoint: "/hooker/mediaprojection/screenshot",
      };
    }
    const metadata = await this.httpGet("/hooker/ui/screen_info");
    return {
      ok: true,
      authorized: true,
      content_type: "image/jpeg",
      stream: true,
      message: "Call /hooker/mediaprojection/screenshot to receive JPEG bytes directly. Optional params: format=jpeg|png, quality=0-100.",
      endpoint: "/hooker/mediaprojection/screenshot",
      metadata,
    };
  }

  async httpGet(pathname, query) {
    const cleaned = {};
    if (query) {
      for (const [key, value] of Object.entries(query)) {
        if (value !== null && value !== undefined) {
          cleaned[key] = value;
        }
      }
    }
    return this.httpJson("GET", pathname, Object.keys(cleaned).length ? cleaned : null, null);
  }

  async httpPost(pathname, payload) {
    return this.httpJson("POST", pathname, null, payload);
  }

  async httpJson(method, pathname, params, jsonPayload) {
    const url = new URL(this.baseUrl + pathname);
    if (params) {
      for (const [key, value] of Object.entries(params)) {
        url.searchParams.set(key, String(value));
      }
    }
    const startedAt = Date.now();
    writeLog(`http start: method=${method} url=${url.toString()} params=${params ? JSON.stringify(params, Object.keys(params).sort()) : "null"} json=${jsonPayload ? JSON.stringify(jsonPayload, Object.keys(jsonPayload).sort()) : "null"}`);
    try {
      const response = await fetch(url, {
        method,
        headers: jsonPayload ? { "Content-Type": "application/json" } : undefined,
        body: jsonPayload ? JSON.stringify(jsonPayload) : undefined,
        signal: AbortSignal.timeout(60000),
      });
      const elapsedMs = Date.now() - startedAt;
      const raw = await response.text();
      if (!response.ok) {
        writeLog(`http error: method=${method} url=${url.toString()} status=${response.status} elapsed_ms=${elapsedMs} body=${raw}`);
        throw new Error(`remote HTTP ${response.status}: ${raw}`);
      }
      writeLog(`http done: method=${method} url=${url.toString()} status=${response.status} elapsed_ms=${elapsedMs}`);
      if (!raw) {
        return {};
      }
      return JSON.parse(raw);
    } catch (error) {
      if (String(error.message || "").startsWith("remote HTTP ")) {
        throw error;
      }
      const elapsedMs = Date.now() - startedAt;
      writeLog(`http exception: method=${method} url=${url.toString()} elapsed_ms=${elapsedMs} error=${error}`);
      throw new Error(`remote connection failed: ${error.message || error}`);
    }
  }

  required(arguments_, key) {
    const value = arguments_[key];
    if (value === null || value === undefined || String(value) === "") {
      throw new Error(`missing argument: ${key}`);
    }
    return String(value);
  }

  string(value, defaultValue) {
    if (value === null || value === undefined) return defaultValue;
    const text = String(value);
    return text === "" ? defaultValue : text;
  }

  int(value, defaultValue) {
    if (value === null || value === undefined) return defaultValue;
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? defaultValue : parsed;
  }

  requiredInt(arguments_, key) {
    if (!(key in arguments_) || arguments_[key] === null || String(arguments_[key]) === "") {
      throw new Error(`missing argument: ${key}`);
    }
    return Number.parseInt(arguments_[key], 10);
  }

  float(value, defaultValue) {
    if (value === null || value === undefined) return defaultValue;
    const parsed = Number.parseFloat(value);
    return Number.isNaN(parsed) ? defaultValue : parsed;
  }

  requiredFloat(arguments_, key) {
    if (!(key in arguments_) || arguments_[key] === null || String(arguments_[key]) === "") {
      throw new Error(`missing argument: ${key}`);
    }
    return Number.parseFloat(arguments_[key]);
  }

  bool(value, defaultValue) {
    if (value === null || value === undefined) return defaultValue;
    if (typeof value === "boolean") return value;
    return String(value).toLowerCase() === "true";
  }
}

async function main() {
  const options = parseCliArgs(process.argv.slice(2));
  writeLog("sdk load: starting");
  const { Server, StdioServerTransport, ListToolsRequestSchema, CallToolRequestSchema, PingRequestSchema } = await loadSdk();
  writeLog("sdk load: completed");
  const bridge = new HookerUiBridge(options);
  const server = new Server(
    {
      name: bridge.serverName,
      version: bridge.serverVersion,
    },
    {
      capabilities: {
        tools: {},
      },
      instructions: "Hooker UI MCP bridge for Android UI automation over HTTP.",
    }
  );

  server.setRequestHandler(PingRequestSchema, async () => {
    writeLog("mcp recv: method=ping");
    return {};
  });

  server.setRequestHandler(ListToolsRequestSchema, async () => {
    writeLog(`mcp recv: method=tools/list`);
    writeLog(`tools/list: count=${TOOLS.length}`);
    return { tools: JSON.parse(JSON.stringify(TOOLS)) };
  });

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const name = request.params.name;
    const arguments_ = request.params.arguments || {};
    writeLog(`mcp recv: method=tools/call id=${request.id}`);
    writeLog(`tools/call: name=${name} args=${JSON.stringify(arguments_, Object.keys(arguments_).sort())}`);
    try {
      const remote = await bridge.callTool(name, arguments_);
      return {
        content: [{ type: "text", text: JSON.stringify(remote) }],
        structuredContent: remote,
        isError: !Boolean(remote.ok ?? true),
      };
    } catch (error) {
      const remote = {
        ok: false,
        tool: name,
        error: String(error.message || error),
      };
      return {
        content: [{ type: "text", text: JSON.stringify(remote) }],
        structuredContent: remote,
        isError: true,
      };
    }
  });

  server.oninitialized = () => {
    writeLog(`mcp initialized: protocol=${DEFAULT_PROTOCOL_VERSION}`);
  };

  const transport = new StdioServerTransport();
  writeLog("transport connect: starting");
  await server.connect(transport);
  process.stdin.resume();
  writeLog("transport connect: completed");
}

main().catch((error) => {
  writeLog(`fatal: ${error && error.stack ? error.stack : error}`);
  process.exit(1);
});
