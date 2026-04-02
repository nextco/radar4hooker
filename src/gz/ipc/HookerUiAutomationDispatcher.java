package gz.ipc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import gz.com.alibaba.fastjson.JSON;
import gz.httpserver.controller.BuiltinMediaProjectionController;
import gz.httpserver.controller.BuiltinUIServiceController;
import gz.util.MediaProjectionScreenshotManager;

public final class HookerUiAutomationDispatcher {

	private final BuiltinUIServiceController controller = new BuiltinUIServiceController();
	private final BuiltinMediaProjectionController mediaProjectionController = new BuiltinMediaProjectionController();

	public Bundle dispatch(String action, Bundle args) {
		Bundle safeArgs = args == null ? new Bundle() : args;
		try {
			Object result = execute(action, safeArgs);
			if (result instanceof Bundle) {
				return (Bundle) result;
			}
			return buildSuccessBundle(result);
		} catch (Throwable throwable) {
			return buildErrorBundle(throwable);
		}
	}

	private Object execute(String action, Bundle args) throws Exception {
		if (HookerUiAutomationContract.ACTION_BACK.equals(action)) {
			return controller.back();
		}
		if (HookerUiAutomationContract.ACTION_HOME.equals(action)) {
			return controller.home();
		}
		if (HookerUiAutomationContract.ACTION_FINISH_CURRENT_ACTIVITY.equals(action)) {
			return controller.finish_current_activity();
		}
		if (HookerUiAutomationContract.ACTION_SCREEN_INFO.equals(action)) {
			return controller.screen_info();
		}
		if (HookerUiAutomationContract.ACTION_ACTIVITY_STACK.equals(action)) {
			return controller.activity_stack();
		}
		if (HookerUiAutomationContract.ACTION_INSPECT.equals(action)) {
			return controller.inspect(
					stringArg(args, "format", "json"),
					stringArg(args, "text_contains", ""),
					stringArg(args, "rect_limit", "0,0,0,0"),
					stringArg(args, "view_type", ""),
					stringArg(args, "class_name", ""),
					stringArg(args, "class_name_contains", ""),
					intArg(args, "is_image", 0),
					intArg(args, "is_edittext", 0),
					intArg(args, "is_listview", 0),
					intArg(args, "is_scrollview", 0)
			);
		}
		if (HookerUiAutomationContract.ACTION_INSPECT_OVERLAY.equals(action)) {
			return controller.inspect_overlay(
					stringArg(args, "text_contains", ""),
					stringArg(args, "rect_limit", "0,0,0,0"),
					stringArg(args, "view_type", ""),
					stringArg(args, "class_name", ""),
					stringArg(args, "class_name_contains", ""),
					intArg(args, "is_image", 0),
					intArg(args, "is_edittext", 0),
					intArg(args, "is_listview", 0),
					intArg(args, "is_scrollview", 0)
			);
		}
		if (HookerUiAutomationContract.ACTION_CLICK_BY_ID.equals(action)) {
			return controller.click_by_id(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_CLICK_BY_TEXT.equals(action)) {
			return controller.click_by_text(
					requiredStringArg(args, "text"),
					booleanArg(args, "text_equeal", false),
					booleanArg(args, "visible", false)
			);
		}
		if (HookerUiAutomationContract.ACTION_LONG_CLICK_VIEW.equals(action)) {
			return controller.long_click_view(requiredStringArg(args, "id"), longArg(args, "duration_ms", 800L));
		}
		if (HookerUiAutomationContract.ACTION_CLICK_BY_POSITION.equals(action)) {
			return controller.click_by_position(intArg(args, "x", 0), intArg(args, "y", 0));
		}
		if (HookerUiAutomationContract.ACTION_SWIPE_ON_SCREEN.equals(action)) {
			return controller.swipe_on_screen(
					intArg(args, "start_x", 0),
					intArg(args, "start_y", 0),
					intArg(args, "end_x", 0),
					intArg(args, "end_y", 0),
					longArg(args, "duration_ms", 300L)
			);
		}
		if (HookerUiAutomationContract.ACTION_SWIPE_VIEW.equals(action)) {
			return controller.swipe_view(
					requiredStringArg(args, "id"),
					stringArg(args, "direction", "up"),
					floatArg(args, "distance_ratio", 0.6f),
					longArg(args, "duration_ms", 300L)
			);
		}
		if (HookerUiAutomationContract.ACTION_VIEW_PAGE_SWIPE.equals(action)) {
			return controller.viewPageSwipe(
					requiredStringArg(args, "id"),
					stringArg(args, "direction", "next")
			);
		}
		if (HookerUiAutomationContract.ACTION_SET_TEXT.equals(action)) {
			Map<String, Object> body = new HashMap<String, Object>();
			body.put("id", requiredStringArg(args, "id"));
			body.put("text", stringArg(args, "text", ""));
			return controller.set_text_json(body);
		}
		if (HookerUiAutomationContract.ACTION_SEND_SEARCH_ACTION.equals(action)) {
			return controller.send_search_action(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_FOCUS_ON.equals(action)) {
			return controller.focus_on(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_SET_CHECKED.equals(action)) {
			return controller.set_checked(
					requiredStringArg(args, "id"),
					args.containsKey("checked") ? Integer.valueOf(intArg(args, "checked", -1)) : null
			);
		}
		if (HookerUiAutomationContract.ACTION_SET_PROGRESS.equals(action)) {
			return controller.set_progress(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "progress", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_SET_RATING.equals(action)) {
			return controller.set_rating(
					requiredStringArg(args, "id"),
					floatArg(args, "rating", 0f)
			);
		}
		if (HookerUiAutomationContract.ACTION_SPINNER_SET_SELECTION.equals(action)) {
			return controller.spinner_set_selection(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "position", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_RECYCLER_VIEW_SCROLL_BY.equals(action)) {
			return controller.scrollRecyclerBy(
					requiredStringArg(args, "id"),
					args.containsKey("x") ? Integer.valueOf(intArg(args, "x", 0)) : null,
					args.containsKey("y") ? Integer.valueOf(intArg(args, "y", 0)) : null
			);
		}
		if (HookerUiAutomationContract.ACTION_SCROLL_RECYCLER_TO_POSITION.equals(action)) {
			return controller.scrollRecyclerToPosition(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "position", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_SMOOTH_SCROLL_RECYCLER_TO_POSITION.equals(action)) {
			return controller.smoothScrollRecyclerToPosition(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "position", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_ADAPTER_VIEW_SCROLL_TO_POSITION.equals(action)) {
			return controller.adapter_view_scroll_to_position(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "position", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_ADAPTER_VIEW_CLICK_POSITION.equals(action)) {
			return controller.adapter_view_click_position(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "position", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_SCROLL_VIEW_SCROLL_TO.equals(action)) {
			return controller.scroll_view_scroll_to(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "x", 0)),
					Integer.valueOf(intArg(args, "y", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_SCROLL_VIEW_SCROLL_BY.equals(action)) {
			return controller.scroll_view_scroll_by(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "x", 0)),
					Integer.valueOf(intArg(args, "y", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_WEB_VIEW_LOAD_URL.equals(action)) {
			return controller.web_view_load_url(
					requiredStringArg(args, "id"),
					requiredStringArg(args, "url")
			);
		}
		if (HookerUiAutomationContract.ACTION_WEB_VIEW_GO_BACK.equals(action)) {
			return controller.web_view_go_back(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_WEB_VIEW_GO_FORWARD.equals(action)) {
			return controller.web_view_go_forward(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_VIDEO_VIEW_START.equals(action)) {
			return controller.video_view_start(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_VIDEO_VIEW_PAUSE.equals(action)) {
			return controller.video_view_pause(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_VIDEO_VIEW_SEEK_TO.equals(action)) {
			return controller.video_view_seek_to(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "msec", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_DRAWER_OPEN.equals(action)) {
			return controller.drawer_open(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_DRAWER_CLOSE.equals(action)) {
			return controller.drawer_close(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_TAB_LAYOUT_SELECT.equals(action)) {
			return controller.tab_layout_select(
					requiredStringArg(args, "id"),
					Integer.valueOf(intArg(args, "position", 0))
			);
		}
		if (HookerUiAutomationContract.ACTION_VIEW_STUB_INFLATE.equals(action)) {
			return controller.view_stub_inflate(requiredStringArg(args, "id"));
		}
		if (HookerUiAutomationContract.ACTION_TRY_DISMISS_DIALOG.equals(action)) {
			return controller.tryDismissByDialogFragment();
		}
		if (HookerUiAutomationContract.ACTION_START_ACTIVITY.equals(action)) {
			return controller.start_activity(buildStartActivityBody(args));
		}
		if (HookerUiAutomationContract.ACTION_SHOW_TOAST.equals(action)) {
			return controller.show_toast(stringArg(args, "text", "牛逼不？"));
		}
		if (HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_STATUS.equals(action)) {
			return mediaProjectionController.status();
		}
		if (HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_REQUEST_PERMISSION.equals(action)) {
			return mediaProjectionController.requestPermission();
		}
		if (HookerUiAutomationContract.ACTION_MEDIA_PROJECTION_CAPTURE_SCREENSHOT.equals(action)) {
			return captureMediaProjectionScreenshot(args);
		}
		throw new IllegalArgumentException("Unsupported action: " + action);
	}

	private Bundle buildSuccessBundle(Object result) {
		Bundle bundle = new Bundle();
		bundle.putBoolean(HookerUiAutomationContract.KEY_OK, extractOk(result, true));
		if (result instanceof String) {
			bundle.putString(HookerUiAutomationContract.KEY_RESULT_TYPE, HookerUiAutomationContract.RESULT_TYPE_TEXT);
			bundle.putString(HookerUiAutomationContract.KEY_RESULT_TEXT, (String) result);
			return bundle;
		}
		bundle.putString(HookerUiAutomationContract.KEY_RESULT_TYPE, HookerUiAutomationContract.RESULT_TYPE_JSON);
		bundle.putString(HookerUiAutomationContract.KEY_RESULT_JSON, JSON.toJSONString(result));
		return bundle;
	}

	private Bundle buildErrorBundle(Throwable throwable) {
		Bundle bundle = new Bundle();
		bundle.putBoolean(HookerUiAutomationContract.KEY_OK, false);
		bundle.putString(HookerUiAutomationContract.KEY_ERROR, buildErrorMessage(throwable));
		bundle.putString(HookerUiAutomationContract.KEY_ERROR_CLASS, throwable.getClass().getName());
		return bundle;
	}

	private String buildErrorMessage(Throwable throwable) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		printWriter.flush();
		return stringWriter.toString();
	}

	private boolean extractOk(Object result, boolean defaultValue) {
		if (!(result instanceof Map)) {
			return defaultValue;
		}
		Object ok = ((Map<?, ?>) result).get("ok");
		return ok instanceof Boolean ? ((Boolean) ok).booleanValue() : defaultValue;
	}

	private String requiredStringArg(Bundle args, String key) {
		String value = args.getString(key);
		if (value == null || value.length() == 0) {
			throw new IllegalArgumentException("Missing required arg: " + key);
		}
		return value;
	}

	private String stringArg(Bundle args, String key, String defaultValue) {
		String value = args.getString(key);
		return value == null ? defaultValue : value;
	}

	private int intArg(Bundle args, String key, int defaultValue) {
		return args.containsKey(key) ? args.getInt(key) : defaultValue;
	}

	private long longArg(Bundle args, String key, long defaultValue) {
		return args.containsKey(key) ? args.getLong(key) : defaultValue;
	}

	private float floatArg(Bundle args, String key, float defaultValue) {
		return args.containsKey(key) ? args.getFloat(key) : defaultValue;
	}

	private boolean booleanArg(Bundle args, String key, boolean defaultValue) {
		return args.containsKey(key) ? args.getBoolean(key) : defaultValue;
	}

	private Map<String, Object> buildStartActivityBody(Bundle args) {
		Map<String, Object> body = new HashMap<String, Object>();
		putIfPresent(body, "class_name", args, "class_name");
		putIfPresent(body, "type", args, "type");
		putIfPresent(body, "action", args, "action");
		putIfPresent(body, "data_uri", args, "data_uri");
		if (args.containsKey("browsable")) {
			body.put("browsable", Boolean.valueOf(args.getBoolean("browsable")));
		}
		Bundle extrasBundle = args.getBundle("extras");
		if (extrasBundle != null) {
			body.put("extras", bundleToMap(extrasBundle));
		}
		return body;
	}

	private void putIfPresent(Map<String, Object> target, String targetKey, Bundle args, String bundleKey) {
		if (!args.containsKey(bundleKey)) {
			return;
		}
		target.put(targetKey, args.get(bundleKey));
	}

	private Map<String, Object> bundleToMap(Bundle bundle) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (String key : bundle.keySet()) {
			Object value = bundle.get(key);
			if (value instanceof Bundle) {
				map.put(key, bundleToMap((Bundle) value));
			} else {
				map.put(key, value);
			}
		}
		return map;
	}

	private Bundle captureMediaProjectionScreenshot(Bundle args) throws Exception {
		String format = stringArg(args, "format", "jpeg");
		int quality = intArg(args, "quality", 70);
		MediaProjectionScreenshotManager manager = MediaProjectionScreenshotManager.getInstance();
		if (!manager.hasPermission()) {
			Bundle bundle = new Bundle();
			bundle.putBoolean(HookerUiAutomationContract.KEY_OK, false);
			bundle.putString(HookerUiAutomationContract.KEY_ERROR, "MediaProjection permission not granted");
			bundle.putString(HookerUiAutomationContract.KEY_RESULT_TYPE, HookerUiAutomationContract.RESULT_TYPE_JSON);
			bundle.putString(HookerUiAutomationContract.KEY_RESULT_JSON, JSON.toJSONString(mediaProjectionController.screenshotForMcp()));
			return bundle;
		}

		String normalizedFormat = normalizeImageFormat(format);
		byte[] imageBytes = manager.captureScreenshot(normalizedFormat, quality);
		Bundle bundle = new Bundle();
		bundle.putBoolean(HookerUiAutomationContract.KEY_OK, true);
		bundle.putString(HookerUiAutomationContract.KEY_RESULT_TYPE, HookerUiAutomationContract.RESULT_TYPE_BYTES);
		bundle.putByteArray(HookerUiAutomationContract.KEY_RESULT_BYTES, imageBytes);
		bundle.putString(HookerUiAutomationContract.KEY_CONTENT_TYPE, "png".equals(normalizedFormat) ? "image/png" : "image/jpeg");
		bundle.putString(HookerUiAutomationContract.KEY_METADATA_JSON, JSON.toJSONString(mediaProjectionController.screenshotForMcp()));
		return bundle;
	}

	private String normalizeImageFormat(String format) {
		if (format == null) {
			return "jpeg";
		}
		String normalized = format.trim().toLowerCase();
		if ("png".equals(normalized)) {
			return "png";
		}
		if ("jpg".equals(normalized) || "jpeg".equals(normalized)) {
			return "jpeg";
		}
		return "jpeg";
	}
}
