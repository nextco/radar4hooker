package gz.agent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.net.URI;

import android.os.Build;
import gz.com.alibaba.fastjson.JSON;
import gz.agent.model.AgentMessage;
import gz.agent.model.ErrorPayload;
import gz.agent.model.HeartbeatPayload;
import gz.agent.model.RegisterPayload;
import gz.agent.model.RegisteredPayload;
import gz.agent.model.ResponsePayload;
import gz.agent.protocol.AgentCodec;
import gz.httpserver.HookerWebServerBoot;
import gz.httpserver.mustang.MustangWebServer;
import gz.okhttp3.OkHttpClient;
import gz.okhttp3.Request;
import gz.okhttp3.Response;
import gz.okhttp3.WebSocket;
import gz.okhttp3.WebSocketListener;
import gz.util.Logger;

public class DeviceAgent {

	private static final DeviceAgent INSTANCE = new DeviceAgent();
	private final Logger logger = new Logger(DeviceAgent.class);
	private final Object lock = new Object();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final ScheduledExecutorService invokeExecutor = Executors.newSingleThreadScheduledExecutor();

	private DeviceAgentConfig config;
	private OkHttpClient webSocketClient;
	private WebSocket webSocket;
	private ScheduledFuture<?> heartbeatFuture;
	private ScheduledFuture<?> reconnectFuture;
	private volatile State state = State.STOPPED;
	private volatile boolean manuallyStopped = true;
	private volatile int nextRetryMs = 1000;
	private volatile String sessionId;
	private volatile String lastErrorCode;
	private volatile String lastErrorMessage;

	private enum State {
		STOPPED,
		CONNECTING,
		REGISTERING,
		ONLINE,
		RECONNECT_WAIT
	}

	public static DeviceAgent getInstance() {
		return INSTANCE;
	}

	public void start(DeviceAgentConfig newConfig) {
		synchronized (lock) {
			if (newConfig == null || !newConfig.isEnabled()) {
				logger.info("remote agent disabled, skip start");
				stopLocked(true);
				return;
			}
			if (isSameConfig(this.config, newConfig)
					&& (state == State.CONNECTING || state == State.REGISTERING || state == State.ONLINE)) {
				logger.info("remote agent already running");
				return;
			}
			stopLocked(false);
			this.config = newConfig;
			this.manuallyStopped = false;
			this.nextRetryMs = Math.max(1000, newConfig.retryMinMs);
			this.lastErrorCode = null;
			this.lastErrorMessage = null;
			connectLocked();
		}
	}

	public String startAndWait(DeviceAgentConfig newConfig, long timeoutMs) throws InterruptedException {
		start(newConfig);
		long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 1000L);
		synchronized (lock) {
			while (true) {
				if (state == State.ONLINE && sessionId != null && sessionId.length() > 0) {
					return buildStartResult("Remote agent connected");
				}
				if (state == State.RECONNECT_WAIT || state == State.STOPPED) {
					return buildStartResult("Remote agent connect failed");
				}
				long waitMs = deadline - System.currentTimeMillis();
				if (waitMs <= 0) {
					return buildStartResult("Remote agent connect timeout");
				}
				lock.wait(waitMs);
			}
		}
	}

	private String buildStartResult(String title) {
		StringBuilder builder = new StringBuilder();
		builder.append(title).append("\n");
		builder.append("Remote ws: ").append(config != null ? config.wsUrl : "").append("\n");
		builder.append("Remote deviceId: ").append(config != null ? config.deviceId : "").append("\n");
		builder.append("Remote state: ").append(state.name()).append("\n");
		if (sessionId != null && sessionId.length() > 0) {
			builder.append("Remote sessionId: ").append(sessionId).append("\n");
		}
		if (lastErrorCode != null && lastErrorCode.length() > 0) {
			builder.append("Remote error: ").append(lastErrorCode);
			if (lastErrorMessage != null && lastErrorMessage.length() > 0) {
				builder.append(" - ").append(lastErrorMessage);
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	public void stop() {
		synchronized (lock) {
			stopLocked(true);
		}
	}

	public boolean isRunning() {
		return state != State.STOPPED;
	}

	public Map<String, Object> getStatus() {
		Map<String, Object> status = new LinkedHashMap<String, Object>();
		status.put("state", state.name());
		status.put("running", Boolean.valueOf(isRunning()));
		status.put("manuallyStopped", Boolean.valueOf(manuallyStopped));
		status.put("sessionId", sessionId);
		status.put("lastErrorCode", lastErrorCode);
		status.put("lastErrorMessage", lastErrorMessage);
		if (config != null) {
			status.put("enabled", Boolean.valueOf(config.enabled));
			status.put("wsUrl", config.wsUrl);
			status.put("deviceId", config.deviceId);
			status.put("app", config.app);
			status.put("packageName", config.packageName);
			status.put("localPort", Integer.valueOf(config.localPort));
			status.put("heartbeatSec", Integer.valueOf(config.heartbeatSec));
		} else {
			status.put("enabled", Boolean.FALSE);
		}
		return status;
	}

	public String getHubHttpBaseUrl() {
		DeviceAgentConfig current = config;
		if (current == null || current.wsUrl == null || current.wsUrl.length() == 0) {
			return "";
		}
		try {
			URI uri = URI.create(current.wsUrl);
			String scheme = "ws".equalsIgnoreCase(uri.getScheme()) ? "http" : "https";
			String host = uri.getHost();
			int port = uri.getPort();
			if (host == null || host.length() == 0) {
				return "";
			}
			if (port > 0) {
				return scheme + "://" + host + ":" + port;
			}
			return scheme + "://" + host;
		} catch (Exception e) {
			logger.warn(e);
			return "";
		}
	}

	private void stopLocked(boolean manualStop) {
		manuallyStopped = manualStop;
		cancelFuture(heartbeatFuture);
		cancelFuture(reconnectFuture);
		heartbeatFuture = null;
		reconnectFuture = null;
		sessionId = null;
		if (webSocket != null) {
			try {
				webSocket.close(1000, "client stop");
			} catch (Exception e) {
				logger.warn(e);
			}
		}
		webSocket = null;
		if (webSocketClient != null) {
			try {
				webSocketClient.dispatcher().executorService().shutdown();
				webSocketClient.connectionPool().evictAll();
			} catch (Exception e) {
				logger.warn(e);
			}
		}
		webSocketClient = null;
		state = State.STOPPED;
		lock.notifyAll();
	}

	private void connectLocked() {
		if (config == null || manuallyStopped) {
			return;
		}
		state = State.CONNECTING;
		webSocketClient = new OkHttpClient.Builder()
				.connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
				.readTimeout(0, TimeUnit.MILLISECONDS)
				.writeTimeout(0, TimeUnit.MILLISECONDS)
				.build();
		Request request = new Request.Builder().url(config.wsUrl).build();
		webSocket = webSocketClient.newWebSocket(request, new AgentWebSocketListener());
		logger.info("remote agent connecting: " + config.wsUrl);
		lock.notifyAll();
	}

	private void scheduleReconnect(final String reason) {
		synchronized (lock) {
			if (manuallyStopped || config == null) {
				return;
			}
			cancelFuture(heartbeatFuture);
			heartbeatFuture = null;
			if (reconnectFuture != null && !reconnectFuture.isDone()) {
				return;
			}
			state = State.RECONNECT_WAIT;
			final int delayMs = Math.max(config.retryMinMs, Math.min(nextRetryMs, config.retryMaxMs));
			nextRetryMs = Math.min(delayMs * 2, config.retryMaxMs);
			reconnectFuture = scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					synchronized (lock) {
						if (manuallyStopped || config == null) {
							return;
						}
						logger.warn("remote agent reconnect, reason=" + reason + ", delayMs=" + delayMs);
						connectLocked();
					}
				}
			}, delayMs, TimeUnit.MILLISECONDS);
			lock.notifyAll();
		}
	}

	private void startHeartbeat(final int intervalSec) {
		synchronized (lock) {
			cancelFuture(heartbeatFuture);
			heartbeatFuture = scheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						sendHeartbeat();
					} catch (Exception e) {
						logger.warn(e);
					}
				}
			}, intervalSec, intervalSec, TimeUnit.SECONDS);
		}
	}

	private void sendRegister() {
		RegisterPayload payload = new RegisterPayload();
		payload.token = config.token;
		payload.app = config.app;
		payload.packageName = config.packageName;
		payload.localPort = Integer.valueOf(config.localPort);
		payload.lanIp = HookerWebServerBoot.getLanIp();
		payload.manufacturer = Build.MANUFACTURER;
		payload.brand = Build.BRAND;
		payload.model = Build.MODEL;
		payload.sdkInt = Integer.valueOf(Build.VERSION.SDK_INT);
		payload.androidRelease = Build.VERSION.RELEASE;
		payload.capabilities = Arrays.asList("hooker_http");
		MustangWebServer mustangWebServer = MustangWebServer.getInstance();
		payload.endpoints = mustangWebServer != null ? mustangWebServer.getAPIDefinitions() : null;
		payload.version = "1.0.0";
		send("register", null, payload);
	}

	private void sendHeartbeat() {
		if (state != State.ONLINE) {
			return;
		}
		HeartbeatPayload payload = new HeartbeatPayload();
		payload.status = "online";
		payload.localPort = Integer.valueOf(config.localPort);
		payload.app = config.app;
		payload.packageName = config.packageName;
		send("heartbeat", null, payload);
	}

	private void sendResponse(String requestId, ResponsePayload payload) {
		send("response", requestId, payload);
	}

	private void sendError(String requestId, String code, String message) {
		ErrorPayload payload = new ErrorPayload();
		payload.code = code;
		payload.message = message;
		send("error", requestId, payload);
	}

	private void send(String type, String requestId, Object payload) {
		WebSocket currentWebSocket = this.webSocket;
		if (currentWebSocket == null) {
			return;
		}
		AgentMessage message = new AgentMessage();
		message.type = type;
		message.requestId = requestId;
		message.deviceId = config != null ? config.deviceId : null;
		message.timestamp = System.currentTimeMillis();
		message.payload = payload;
		String text = AgentCodec.encode(message);
		boolean sent = currentWebSocket.send(text);
		if (!sent) {
			logger.warn("remote agent send failed, type=" + type);
		}
	}

	private void handleMessage(String text) {
		try {
			AgentMessage message = AgentCodec.decode(text);
			if (message == null || message.type == null) {
				return;
			}
			if ("registered".equals(message.type)) {
				handleRegistered(message);
				return;
			}
			if ("invoke".equals(message.type)) {
				// 旧的本地 HTTP 转发链路已经废弃，这里保留协议兜底，只返回明确错误，避免 silently ignore。
				sendError(message.requestId, "UNSUPPORTED_MESSAGE", "invoke is deprecated, use invoke_java");
				return;
			}
			if ("invoke_java".equals(message.type)) {
				handleInvokeJava(message);
				return;
			}
			if ("error".equals(message.type)) {
				handleRemoteError(message);
			}
		} catch (Exception e) {
			logger.warn(e);
		}
	}

	private void handleRegistered(AgentMessage message) {
		RegisteredPayload payload = AgentCodec.convertPayload(message.payload, RegisteredPayload.class);
		synchronized (lock) {
			sessionId = payload != null ? payload.sessionId : null;
			state = State.ONLINE;
			nextRetryMs = Math.max(1000, config.retryMinMs);
			lastErrorCode = null;
			lastErrorMessage = null;
			int heartbeatSec = config.heartbeatSec;
			if (payload != null && payload.heartbeatIntervalSec != null && payload.heartbeatIntervalSec.intValue() > 0) {
				heartbeatSec = payload.heartbeatIntervalSec.intValue();
			}
			startHeartbeat(heartbeatSec);
			lock.notifyAll();
		}
		logger.info("remote agent registered, sessionId=" + sessionId);
	}

	private void handleInvokeJava(final AgentMessage message) {
		invokeExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Map<String, Object> invokePayload = AgentCodec.convertPayload(message.payload, Map.class);
					if (invokePayload == null) {
						throw new IllegalArgumentException("missing invoke_java payload");
					}
					MustangWebServer mustangWebServer = MustangWebServer.getInstance();
					if (mustangWebServer == null) {
						throw new IllegalStateException("MustangWebServer not started");
					}
					Object result = mustangWebServer.invodeJava(invokePayload);
					sendResponse(message.requestId, buildInvokeJavaResponse(result));
				} catch (AgentException e) {
					sendError(message.requestId, e.getCode(), e.getMessage());
				} catch (Exception e) {
					sendError(message.requestId, "INTERNAL_ERROR", e.toString());
				}
			}
		});
	}

	private ResponsePayload buildInvokeJavaResponse(Object result) {
		ResponsePayload payload = new ResponsePayload();
		payload.status = 200;
		payload.headers = new LinkedHashMap<String, String>();
		if (result == null) {
			payload.headers.put("Content-Type", "text/plain; charset=utf-8");
			payload.body = "";
			return payload;
		}
		if (result instanceof Map || result instanceof List) {
			payload.headers.put("Content-Type", "application/json; charset=utf-8");
			payload.body = JSON.toJSONString(result);
			return payload;
		}
		String text = String.valueOf(result);
		payload.headers.put("Content-Type", detectContentType(text));
		payload.body = text;
		return payload;
	}

	private String detectContentType(String text) {
		if (text == null) {
			return "text/plain; charset=utf-8";
		}
		String trimmed = text.trim();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			return "application/json; charset=utf-8";
		}
		if (trimmed.startsWith("<")) {
			return "text/html; charset=utf-8";
		}
		return "text/plain; charset=utf-8";
	}

	private void handleRemoteError(AgentMessage message) {
		ErrorPayload payload = AgentCodec.convertPayload(message.payload, ErrorPayload.class);
		String code = payload != null ? payload.code : "";
		String msg = payload != null ? payload.message : "";
		logger.warn("remote agent error: " + code + " " + msg);
		synchronized (lock) {
			lastErrorCode = code;
			lastErrorMessage = msg;
			lock.notifyAll();
		}
		if (state == State.REGISTERING) {
			if ("UNAUTHORIZED".equals(code) || "REGISTER_REJECTED".equals(code)) {
				synchronized (lock) {
					manuallyStopped = true;
				}
			}
			synchronized (lock) {
				if (webSocket != null) {
					try {
						webSocket.close(4001, "register rejected");
					} catch (Exception e) {
						logger.warn(e);
					}
				}
			}
		}
	}

	private boolean isSameConfig(DeviceAgentConfig left, DeviceAgentConfig right) {
		if (left == null || right == null) {
			return false;
		}
		return equals(left.wsUrl, right.wsUrl)
				&& equals(left.deviceId, right.deviceId)
				&& equals(left.token, right.token)
				&& left.localPort == right.localPort;
	}

	private boolean equals(String left, String right) {
		if (left == null) {
			return right == null;
		}
		return left.equals(right);
	}

	private void cancelFuture(ScheduledFuture<?> future) {
		if (future != null) {
			future.cancel(true);
		}
	}

	private final class AgentWebSocketListener extends WebSocketListener {

		@Override
		public void onOpen(WebSocket webSocket, Response response) {
			synchronized (lock) {
				if (manuallyStopped || DeviceAgent.this.webSocket != webSocket) {
					return;
				}
				state = State.REGISTERING;
				sendRegister();
			}
		}

		@Override
		public void onMessage(WebSocket webSocket, String text) {
			handleMessage(text);
		}

		@Override
		public void onClosing(WebSocket webSocket, int code, String reason) {
			webSocket.close(code, reason);
		}

		@Override
		public void onClosed(WebSocket webSocket, int code, String reason) {
			synchronized (lock) {
				if (DeviceAgent.this.webSocket == webSocket) {
					DeviceAgent.this.webSocket = null;
				}
				state = State.STOPPED;
				lastErrorCode = "DEVICE_OFFLINE";
				lastErrorMessage = "closed:" + code + ":" + reason;
				lock.notifyAll();
			}
			if (!manuallyStopped) {
				scheduleReconnect("closed:" + code + ":" + reason);
			}
		}

		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response) {
			logger.warn(t);
			synchronized (lock) {
				if (DeviceAgent.this.webSocket == webSocket) {
					DeviceAgent.this.webSocket = null;
				}
				state = State.STOPPED;
				lastErrorCode = "CONNECT_FAILED";
				lastErrorMessage = t == null ? "unknown" : t.toString();
				lock.notifyAll();
			}
			if (!manuallyStopped) {
				scheduleReconnect(t == null ? "unknown" : t.toString());
			}
		}
	}
}
