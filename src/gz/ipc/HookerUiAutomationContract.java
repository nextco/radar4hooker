package gz.ipc;

import android.os.IBinder;

public final class HookerUiAutomationContract {

	public static final String SERVICE_NAME = "hooker_ui_automation";
	public static final String DESCRIPTOR = "gz.ipc.HookerUiAutomation";
	public static final int TRANSACTION_CALL = IBinder.FIRST_CALL_TRANSACTION;

	public static final String KEY_ACTION = "action";
	public static final String KEY_ARGS = "args";
	public static final String KEY_RESULT_JSON = "result_json";
	public static final String KEY_RESULT_TEXT = "result_text";
	public static final String KEY_RESULT_TYPE = "result_type";
	public static final String KEY_RESULT_BYTES = "result_bytes";
	public static final String KEY_CONTENT_TYPE = "content_type";
	public static final String KEY_METADATA_JSON = "metadata_json";
	public static final String KEY_OK = "ok";
	public static final String KEY_ERROR = "error";
	public static final String KEY_ERROR_CLASS = "error_class";

	public static final String RESULT_TYPE_JSON = "json";
	public static final String RESULT_TYPE_TEXT = "text";
	public static final String RESULT_TYPE_BYTES = "bytes";

	public static final String ACTION_BACK = "back";
	public static final String ACTION_HOME = "home";
	public static final String ACTION_FINISH_CURRENT_ACTIVITY = "finish_current_activity";
	public static final String ACTION_SCREEN_INFO = "screen_info";
	public static final String ACTION_ACTIVITY_STACK = "activity_stack";
	public static final String ACTION_INSPECT = "inspect";
	public static final String ACTION_INSPECT_OVERLAY = "inspect_overlay";
	public static final String ACTION_CLICK_BY_ID = "click_by_id";
	public static final String ACTION_CLICK_BY_TEXT = "click_by_text";
	public static final String ACTION_LONG_CLICK_VIEW = "long_click_view";
	public static final String ACTION_CLICK_BY_POSITION = "click_by_position";
	public static final String ACTION_SWIPE_ON_SCREEN = "swipe_on_screen";
	public static final String ACTION_SWIPE_VIEW = "swipe_view";
	public static final String ACTION_VIEW_PAGE_SWIPE = "view_page_swipe";
	public static final String ACTION_SET_TEXT = "set_text";
	public static final String ACTION_SEND_SEARCH_ACTION = "send_search_action";
	public static final String ACTION_FOCUS_ON = "focus_on";
	public static final String ACTION_SET_CHECKED = "set_checked";
	public static final String ACTION_SET_PROGRESS = "set_progress";
	public static final String ACTION_SET_RATING = "set_rating";
	public static final String ACTION_SPINNER_SET_SELECTION = "spinner_set_selection";
	public static final String ACTION_RECYCLER_VIEW_SCROLL_BY = "recycler_view_scroll_by";
	public static final String ACTION_SCROLL_RECYCLER_TO_POSITION = "scroll_recycler_to_position";
	public static final String ACTION_SMOOTH_SCROLL_RECYCLER_TO_POSITION = "smooth_scroll_recycler_to_position";
	public static final String ACTION_ADAPTER_VIEW_SCROLL_TO_POSITION = "adapter_view_scroll_to_position";
	public static final String ACTION_ADAPTER_VIEW_CLICK_POSITION = "adapter_view_click_position";
	public static final String ACTION_SCROLL_VIEW_SCROLL_TO = "scroll_view_scroll_to";
	public static final String ACTION_SCROLL_VIEW_SCROLL_BY = "scroll_view_scroll_by";
	public static final String ACTION_WEB_VIEW_LOAD_URL = "web_view_load_url";
	public static final String ACTION_WEB_VIEW_GO_BACK = "web_view_go_back";
	public static final String ACTION_WEB_VIEW_GO_FORWARD = "web_view_go_forward";
	public static final String ACTION_VIDEO_VIEW_START = "video_view_start";
	public static final String ACTION_VIDEO_VIEW_PAUSE = "video_view_pause";
	public static final String ACTION_VIDEO_VIEW_SEEK_TO = "video_view_seek_to";
	public static final String ACTION_DRAWER_OPEN = "drawer_open";
	public static final String ACTION_DRAWER_CLOSE = "drawer_close";
	public static final String ACTION_TAB_LAYOUT_SELECT = "tab_layout_select";
	public static final String ACTION_VIEW_STUB_INFLATE = "view_stub_inflate";
	public static final String ACTION_TRY_DISMISS_DIALOG = "try_to_dismiss_dialog";
	public static final String ACTION_START_ACTIVITY = "start_activity";
	public static final String ACTION_SHOW_TOAST = "show_toast";
	public static final String ACTION_MEDIA_PROJECTION_STATUS = "media_projection_status";
	public static final String ACTION_MEDIA_PROJECTION_REQUEST_PERMISSION = "media_projection_request_permission";
	public static final String ACTION_MEDIA_PROJECTION_CAPTURE_SCREENSHOT = "media_projection_capture_screenshot";

	private HookerUiAutomationContract() {
	}
}
