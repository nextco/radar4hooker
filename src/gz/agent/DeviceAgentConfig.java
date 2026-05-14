package gz.agent;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import gz.radar.Android;

public class DeviceAgentConfig {

	public boolean enabled;
	public String wsUrl;
	public String deviceId;
	public String token;
	public String app;
	public String packageName;
	public int localPort;
	public int heartbeatSec;
	public int connectTimeoutMs;
	public int retryMinMs;
	public int retryMaxMs;
	public int invokeTimeoutMs;
	public int maxResponseBodyBytes;
	public List<String> allowedPathPrefixes = new ArrayList<String>();

	public boolean isEnabled() {
		return enabled && wsUrl != null && wsUrl.length() > 0;
	}

	public static DeviceAgentConfig fromSystemProperties(int port) throws Exception {
		DeviceAgentConfig config = new DeviceAgentConfig();
		Context context = Android.getApplication();
		String packageName = context != null ? context.getPackageName() : "";
		config.enabled = parseBoolean(System.getProperty("hooker.remote.enabled"), false);
		config.wsUrl = trim(System.getProperty("hooker.remote.ws_url"));
		config.token = trim(System.getProperty("hooker.remote.token"));
		config.app = defaultIfEmpty(trim(System.getProperty("hooker.remote.app")), packageName);
		config.packageName = defaultIfEmpty(trim(System.getProperty("hooker.remote.package_name")), packageName);
		config.localPort = port;
		config.heartbeatSec = parseInt(System.getProperty("hooker.remote.heartbeat_sec"), 20);
		config.connectTimeoutMs = parseInt(System.getProperty("hooker.remote.connect_timeout_ms"), 10000);
		config.retryMinMs = parseInt(System.getProperty("hooker.remote.retry_min_ms"), 1000);
		config.retryMaxMs = parseInt(System.getProperty("hooker.remote.retry_max_ms"), 30000);
		config.invokeTimeoutMs = parseInt(System.getProperty("hooker.remote.invoke_timeout_ms"), 15000);
		config.maxResponseBodyBytes = parseInt(System.getProperty("hooker.remote.max_response_body_bytes"), 1024 * 1024);
		config.allowedPathPrefixes = parsePrefixes(System.getProperty("hooker.remote.allowed_path_prefixes"));
		if (config.allowedPathPrefixes.isEmpty()) {
			config.allowedPathPrefixes.add("/hooker/");
		}
		config.deviceId = DeviceIdentity.resolve(context, trim(System.getProperty("hooker.remote.device_id")));
		return config;
	}

	private static List<String> parsePrefixes(String raw) {
		List<String> prefixes = new ArrayList<String>();
		if (raw == null || raw.trim().length() == 0) {
			return prefixes;
		}
		String[] arr = raw.split(",");
		for (int i = 0; i < arr.length; i++) {
			String item = trim(arr[i]);
			if (item.length() > 0) {
				prefixes.add(item);
			}
		}
		return prefixes;
	}

	private static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(value) || "1".equals(value);
	}

	private static int parseInt(String value, int defaultValue) {
		if (value == null || value.trim().length() == 0) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private static String defaultIfEmpty(String value, String fallback) {
		return value == null || value.length() == 0 ? fallback : value;
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
