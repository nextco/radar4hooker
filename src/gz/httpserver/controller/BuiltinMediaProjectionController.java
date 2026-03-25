package gz.httpserver.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.mustang.MustangServlet;
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
	public Response screenshot(MustangServlet servlet) throws Exception {
		MediaProjectionScreenshotManager manager = MediaProjectionScreenshotManager.getInstance();
		if (!manager.hasPermission()) {
			return servlet.newForbidden("MediaProjection permission not granted. Call /hooker/mediaprojection/request_permission first.");
		}
		byte[] pngBytes = manager.captureScreenshot();
		return servlet.newFixedLengthResponse(pngBytes, "image/png");
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
		result.put("content_type", "image/png");
		result.put("stream", true);
		result.put("message", "Call /hooker/mediaprojection/screenshot to receive PNG bytes directly.");
		result.put("endpoint", "/hooker/mediaprojection/screenshot");
		return result;
	}
}
