package gz.httpserver.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.mustang.MustangServlet;
import gz.radar.Android;
import gz.util.MediaProjectionScreenshotManager;

@HookerController("/hooker/mediaprojection/")
public class BuiltinMediaProjectionController {

	@HookerRequestMapping(path = "request_permission", produces = Produces.AUTO, method = Method.GET)
	public Map<String, Object> requestPermission() throws Exception {
		return MediaProjectionScreenshotManager.getInstance().requestPermission();
	}

	@HookerRequestMapping(path = "status", produces = Produces.AUTO, method = Method.GET)
	public Map<String, Object> status() {
		return MediaProjectionScreenshotManager.getInstance().getStatus();
	}

	@HookerRequestMapping(path = "screenshot", produces = Produces.AUTO, method = Method.GET)
	public Response screenshot(@HookerRequestParam(name = "format", defaultValue = "jpeg") String format,
			@HookerRequestParam(name = "quality", defaultValue = "70") Integer quality,
			MustangServlet servlet) throws Exception {
		MediaProjectionScreenshotManager manager = MediaProjectionScreenshotManager.getInstance();
		if (!manager.hasPermission()) {
			return servlet.newForbidden("MediaProjection permission not granted. Call /hooker/mediaprojection/request_permission first.");
		}
		String normalizedFormat = normalizeFormat(format);
		byte[] imageBytes = manager.captureScreenshot(normalizedFormat, quality == null ? 70 : quality.intValue());
		Response response = servlet.newFixedLengthResponse(imageBytes, contentTypeForFormat(normalizedFormat));
		Map<String, Object> metadata = collectScreenshotMetadata();
		addMetadataHeaders(response, metadata);
		return response;
	}

	public Map<String, Object> screenshotForMcp() throws Exception {
		MediaProjectionScreenshotManager manager = MediaProjectionScreenshotManager.getInstance();
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("authorized", Boolean.valueOf(manager.hasPermission()));
		if (!manager.hasPermission()) {
			result.put("ok", false);
			result.put("error", "MediaProjection permission not granted");
			result.put("status", manager.getStatus());
			result.put("endpoint", "/hooker/mediaprojection/screenshot");
			return result;
		}
		result.put("ok", true);
		result.put("content_type", "image/jpeg");
		result.put("stream", true);
		result.put("message", "Call /hooker/mediaprojection/screenshot to receive JPEG bytes directly. Optional params: format=jpeg|png, quality=0-100.");
		result.put("endpoint", "/hooker/mediaprojection/screenshot");
		result.put("metadata", collectScreenshotMetadata());
		return result;
	}

	private String normalizeFormat(String format) {
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

	private String contentTypeForFormat(String format) {
		return "png".equals(format) ? "image/png" : "image/jpeg";
	}

	private Map<String, Object> collectScreenshotMetadata() throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		Activity activity = Android.getTopActivity();
		if (activity == null) {
			result.put("ok", false);
			result.put("error", "top activity is null");
			return result;
		}
		DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
		Display display = activity.getWindowManager().getDefaultDisplay();
		Point displaySize = new Point();
		Point realSize = new Point();
		try {
			display.getSize(displaySize);
		} catch (Throwable e) {
			displaySize.set(metrics.widthPixels, metrics.heightPixels);
		}
		try {
			display.getRealSize(realSize);
		} catch (Throwable e) {
			realSize.set(displaySize.x, displaySize.y);
		}
		View decorView = activity.getWindow().getDecorView();
		Map<String, Object> appWindow = new LinkedHashMap<String, Object>();
		appWindow.put("width", Integer.valueOf(decorView.getWidth()));
		appWindow.put("height", Integer.valueOf(decorView.getHeight()));
		result.put("ok", true);
		result.put("top_activity", activity.getClass().getName());
		result.put("display_width", Integer.valueOf(displaySize.x));
		result.put("display_height", Integer.valueOf(displaySize.y));
		result.put("real_display_width", Integer.valueOf(realSize.x));
		result.put("real_display_height", Integer.valueOf(realSize.y));
		result.put("density", Float.valueOf(metrics.density));
		result.put("density_dpi", Integer.valueOf(metrics.densityDpi));
		result.put("scaled_density", Float.valueOf(metrics.scaledDensity));
		result.put("orientation", orientationName(activity.getResources().getConfiguration().orientation));
		result.put("rotation", Integer.valueOf(display.getRotation()));
		result.put("app_window", appWindow);
		result.put("image_width", Integer.valueOf(realSize.x));
		result.put("image_height", Integer.valueOf(realSize.y));
		return result;
	}

	private void addMetadataHeaders(Response response, Map<String, Object> metadata) {
		if (response == null || metadata == null) {
			return;
		}
		addHeader(response, "X-Hooker-Image-Width", metadata.get("image_width"));
		addHeader(response, "X-Hooker-Image-Height", metadata.get("image_height"));
		addHeader(response, "X-Hooker-Display-Width", metadata.get("display_width"));
		addHeader(response, "X-Hooker-Display-Height", metadata.get("display_height"));
		addHeader(response, "X-Hooker-Real-Display-Width", metadata.get("real_display_width"));
		addHeader(response, "X-Hooker-Real-Display-Height", metadata.get("real_display_height"));
		addHeader(response, "X-Hooker-Rotation", metadata.get("rotation"));
		addHeader(response, "X-Hooker-Orientation", metadata.get("orientation"));
	}

	private void addHeader(Response response, String key, Object value) {
		if (value != null) {
			response.addHeader(key, String.valueOf(value));
		}
	}

	private String orientationName(int orientation) {
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			return "portrait";
		}
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			return "landscape";
		}
		if (orientation == Configuration.ORIENTATION_SQUARE) {
			return "square";
		}
		return "undefined";
	}
}
