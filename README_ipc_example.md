# Hooker UI IPC Minimal Example

这是给外部 App 调用 `radar4hooker` UI 自动化 Binder 服务的最小示例。

前提：

- 目标进程里已经启动了 `radar4hooker`
- `HookerWebServerBoot.startDefaultHttpServer()` 或 `scanAndStartHttpServer(...)` 已执行
- Binder 服务已经通过 `HookerUiAutomationService.ensureStarted()` 注册
- 外部 App 已经拿到 `xradar.jar`，并能访问：
  - `gz.ipc.HookerUiAutomationClient`
  - `gz.ipc.HookerUiAutomationContract`

已支持的 IPC 能力分组：

- 基础导航：`back` `home` `finish_current_activity`
- UI 检查：`activity_stack` `screen_info` `inspect` `inspect_overlay`
- 交互：`click_by_id` `click_by_text` `click_by_position` `long_click_view`
- 滑动与分页：`swipe_on_screen` `swipe_view` `view_page_swipe`
- 输入：`set_text` `focus_on` `send_search_action`
- 状态控制：`set_checked` `set_progress` `set_rating` `spinner_set_selection`
- 容器滚动：`recycler_view_scroll_by` `scroll_recycler_to_position`
- 容器滚动：`smooth_scroll_recycler_to_position` `adapter_view_scroll_to_position`
- 容器滚动：`adapter_view_click_position` `scroll_view_scroll_to` `scroll_view_scroll_by`
- 特殊控件：`web_view_load_url` `web_view_go_back` `web_view_go_forward`
- 特殊控件：`video_view_start` `video_view_pause` `video_view_seek_to`
- 特殊控件：`drawer_open` `drawer_close` `tab_layout_select` `view_stub_inflate`
- 系统能力：`try_to_dismiss_dialog` `start_activity` `show_toast`
- 截图能力：`media_projection_status` `media_projection_request_permission` `media_projection_capture_screenshot`

## 1. 最小调用示例

```java
import android.os.Bundle;
import gz.ipc.HookerUiAutomationClient;
import gz.ipc.HookerUiAutomationContract;

public final class HookerUiDemo {

    public static void runInspect() throws Exception {
        Bundle args = new Bundle();
        args.putString("format", "json");
        args.putString("text_contains", "搜索");

        Bundle result = HookerUiAutomationClient.call(
                HookerUiAutomationContract.ACTION_INSPECT,
                args
        );

        boolean ok = result.getBoolean(HookerUiAutomationContract.KEY_OK, false);
        if (!ok) {
            throw new IllegalStateException(result.getString(HookerUiAutomationContract.KEY_ERROR));
        }

        String json = result.getString(HookerUiAutomationContract.KEY_RESULT_JSON);
        System.out.println("inspect result = " + json);
    }

    public static void runClickByPosition() throws Exception {
        Bundle args = new Bundle();
        args.putInt("x", 1003);
        args.putInt("y", 126);

        Bundle result = HookerUiAutomationClient.call(
                HookerUiAutomationContract.ACTION_CLICK_BY_POSITION,
                args
        );

        boolean ok = result.getBoolean(HookerUiAutomationContract.KEY_OK, false);
        if (!ok) {
            throw new IllegalStateException(result.getString(HookerUiAutomationContract.KEY_ERROR));
        }

        String json = result.getString(HookerUiAutomationContract.KEY_RESULT_JSON);
        System.out.println("click result = " + json);
    }
}
```

## 2. 常见动作示例

### 返回

```java
Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_BACK,
        null
);
```

### 获取当前 Activity 栈

```java
Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_ACTIVITY_STACK,
        null
);
String json = result.getString(HookerUiAutomationContract.KEY_RESULT_JSON);
```

### 设置文本

```java
Bundle args = new Bundle();
args.putString("id", "hooker_dc767665");
args.putString("text", "美伊最新进展");

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_SET_TEXT,
        args
);
```

### 触发搜索

```java
Bundle args = new Bundle();
args.putString("id", "hooker_dc767665");

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_SEND_SEARCH_ACTION,
        args
);
```

### 屏幕滑动

```java
Bundle args = new Bundle();
args.putInt("start_x", 540);
args.putInt("start_y", 1600);
args.putInt("end_x", 540);
args.putInt("end_y", 650);
args.putLong("duration_ms", 300L);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_SWIPE_ON_SCREEN,
        args
);
```

### 按文本点击

```java
Bundle args = new Bundle();
args.putString("text", "直播");
args.putBoolean("text_equeal", true);
args.putBoolean("visible", true);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_CLICK_BY_TEXT,
        args
);
```

### RecyclerView 滚动

```java
Bundle args = new Bundle();
args.putString("id", "hooker_list_id");
args.putInt("y", 1200);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_RECYCLER_VIEW_SCROLL_BY,
        args
);
```

### 切换 TabLayout

```java
Bundle args = new Bundle();
args.putString("id", "hooker_tab_layout");
args.putInt("position", 1);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_TAB_LAYOUT_SELECT,
        args
);
```

### 启动 Activity

```java
Bundle args = new Bundle();
args.putString("action", "android.intent.action.VIEW");
args.putString("data_uri", "https://www.example.com");
args.putBoolean("browsable", true);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_START_ACTIVITY,
        args
);
```

### 查询 MediaProjection 授权状态

```java
Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_STATUS,
        null
);
String json = result.getString(HookerUiAutomationContract.KEY_RESULT_JSON);
```

### 发起 MediaProjection 授权

```java
Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_REQUEST_PERMISSION,
        null
);
```

### 通过 IPC 直接抓取截图

```java
Bundle args = new Bundle();
args.putString("format", "jpeg");
args.putInt("quality", 70);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_CAPTURE_SCREENSHOT,
        args
);

boolean ok = result.getBoolean(HookerUiAutomationContract.KEY_OK, false);
byte[] imageBytes = result.getByteArray(HookerUiAutomationContract.KEY_RESULT_BYTES);
String contentType = result.getString(HookerUiAutomationContract.KEY_CONTENT_TYPE);
String metadataJson = result.getString(HookerUiAutomationContract.KEY_METADATA_JSON);
```

### 把 `result_bytes` 写成 JPEG 文件

```java
import java.io.File;
import java.io.FileOutputStream;

Bundle args = new Bundle();
args.putString("format", "jpeg");
args.putInt("quality", 70);

Bundle result = HookerUiAutomationClient.call(
        HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_CAPTURE_SCREENSHOT,
        args
);

boolean ok = result.getBoolean(HookerUiAutomationContract.KEY_OK, false);
if (!ok) {
    throw new IllegalStateException(result.getString(HookerUiAutomationContract.KEY_ERROR));
}

byte[] imageBytes = result.getByteArray(HookerUiAutomationContract.KEY_RESULT_BYTES);
if (imageBytes == null || imageBytes.length == 0) {
    throw new IllegalStateException("empty screenshot bytes");
}

File outFile = new File(context.getExternalFilesDir(null), "hooker_capture.jpg");
FileOutputStream fos = new FileOutputStream(outFile);
try {
    fos.write(imageBytes);
    fos.flush();
} finally {
    fos.close();
}

System.out.println("saved screenshot to: " + outFile.getAbsolutePath());
```

注意：

- 截图字节直接通过 Binder 返回，受 Binder 事务大小限制影响
- 大图或高质量 PNG 可能超过限制
- 默认更建议用 `jpeg + 适中 quality`

## 3. 返回值约定

成功时：

- `ok = true`
- 结构化返回放在 `result_json`
- 纯文本返回放在 `result_text`
- 二进制返回放在 `result_bytes`
- `result_type` 为 `json` 或 `text`

截图这类二进制结果还会附带：

- `content_type`
- `metadata_json`

失败时：

- `ok = false`
- `error` 是完整异常堆栈
- `error_class` 是异常类名

示例：

```java
boolean ok = result.getBoolean(HookerUiAutomationContract.KEY_OK, false);
if (!ok) {
    String error = result.getString(HookerUiAutomationContract.KEY_ERROR);
    String errorClass = result.getString(HookerUiAutomationContract.KEY_ERROR_CLASS);
}
```

## 4. 建议调用顺序

对外部 App 来说，推荐这样使用：

1. 先 `ACTION_ACTIVITY_STACK` 确认当前页面
2. 再 `ACTION_INSPECT` 获取新的 `hooker_id`
3. 再执行 `click` / `swipe` / `set_text`
4. 动作后再次 `inspect` 或 `activity_stack` 验证结果

不要长时间缓存 `hooker_id`，界面一变化它就可能失效。
