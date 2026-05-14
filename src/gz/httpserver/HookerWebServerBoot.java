package gz.httpserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import gz.agent.DeviceAgent;
import gz.agent.DeviceAgentConfig;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerWebServerConfiguration;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.controller.BuiltinAgentController;
import gz.httpserver.controller.BuiltinAppInfoController;
import gz.httpserver.controller.BuiltinServiceController;
import gz.httpserver.controller.BuiltinUIServiceController;
import gz.httpserver.controller.BuiltinClassHelperController;
import gz.httpserver.controller.BuiltinFileServiceController;
import gz.httpserver.controller.BuiltinMediaProjectionController;
import gz.httpserver.controller.BuiltinUIAutomatorDumpController;
import gz.httpserver.mustang.MustangAutoWireServlet;
import gz.httpserver.mustang.MustangWebServer;
import gz.ipc.HookerUiAutomationService;
import gz.radar.Android;
import gz.util.Logger;

public class HookerWebServerBoot {
	
	private static Logger logger = new Logger(HookerWebServerBoot.class);
	// 远端 agent 支持“先起本地 webserver，后显式开启 agent”，所以这里记住最近一次本地端口，
	// 让开发者后续调用 startRemoteAgent() 时不需要再重复传 port。
	private static volatile int lastStartedPort = 8080;

	public static String getLanIp() {
	    try {
	        Enumeration<NetworkInterface> interfaces =
	                NetworkInterface.getNetworkInterfaces();

	        while (interfaces.hasMoreElements()) {
	            NetworkInterface nif = interfaces.nextElement();
	            Enumeration<InetAddress> addresses = nif.getInetAddresses();

	            while (addresses.hasMoreElements()) {
	                InetAddress addr = addresses.nextElement();

	                if (!addr.isLoopbackAddress()
	                        && addr instanceof Inet4Address) {
	                    return addr.getHostAddress();
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public static String scanAndStartHttpServer(List<String> allClz) {
		try {
			List<Class<?>> httpServerClzList = new ArrayList<Class<?>>(); 
			List<Class<?>> requestMappingClzList = new ArrayList<Class<?>>(); 
			for (String clzName : allClz) {
				Class<?> clz = Class.forName(clzName);
				// 判断class是否有@HttpServer注解
				if (clz.isAnnotationPresent(HookerWebServerConfiguration.class)) {
					httpServerClzList.add(clz);
				} 
				if (clz.isAnnotationPresent(HookerController.class)) {
					requestMappingClzList.add(clz);
				}
			}
			if (httpServerClzList.size() > 1) {
				return "You can configure at most one @HookerHttpServer";
			}
			return start(httpServerClzList, requestMappingClzList);
		} catch (Exception e) {
			// 获取堆栈信息
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.flush();
			logger.error(e);
			return sw.toString();
		}
	}

	private static String start(List<Class<?>> httpServerClassList, List<Class<?>> controllerClzList) throws Exception {
		int port = 8080;
		if (!httpServerClassList.isEmpty()) {
			HookerWebServerConfiguration serverAnno = httpServerClassList.get(0)
					.getAnnotation(HookerWebServerConfiguration.class);
			port = serverAnno.port();
		}
		return startWithPort(port, httpServerClassList, controllerClzList);
	}
	
	private static void stopLastServer() {
		String lastPort = (String) System.getProperties().get("hooker_server_flag");
		try {
			// webserver 重启时要先停掉旧的 remote agent，避免它继续拿旧端口或旧连接工作。
			DeviceAgent.getInstance().stop();
			MustangWebServer lastServer = MustangWebServer.getInstance();
			if (lastServer != null) {
				lastServer.stop();
				logger.info("stop last webserver, old port=" + lastPort);
				return;
			}
		} catch (Exception e) {
			logger.warn(e);
		}
		System.getProperties().remove("hooker_server_flag");
		logger.warn("last webserver flag exists but instance missing, old port=" + lastPort);
	}
	
	private static String startWithPort(int port, List<Class<?>> httpServerClassList, List<Class<?>> controllerClzList) throws Exception {
		String hooker_server_flag = (String) System.getProperties().get("hooker_server_flag");
		if (hooker_server_flag != null) {
			stopLastServer();
		}
		System.getProperties().setProperty("hooker_server_flag", String.valueOf(port));
		// 这里只记录端口，不默认启动 remote agent。
		// 是否启用 agent 由开发者显式调用 startRemoteAgent()/stopRemoteAgent() 决定。
		// 如果开发者在 @HookerWebServerConfiguration 里显式配置了 hub，则下面会单独判断并自动启动 agent。
		lastStartedPort = port;
		String info = "";
		info += "Http server port: " + port + "\n";
		String lanIP = getLanIp();
		if (lanIP != null) {
			info += "Http server: http://" +lanIP + ":" + port + "\n";
		}
		MustangWebServer mustangHttpServer = new MustangWebServer(port);
		
		controllerClzList.add(BuiltinServiceController.class);
		controllerClzList.add(BuiltinUIServiceController.class);
		controllerClzList.add(BuiltinMediaProjectionController.class);
		controllerClzList.add(BuiltinUIAutomatorDumpController.class);
		// 内置 agent controller 只提供“远端 agent 的控制入口”，不会因为注册了 controller 就自动建连。
		controllerClzList.add(BuiltinAgentController.class);
		controllerClzList.add(BuiltinAppInfoController.class);
		controllerClzList.add(BuiltinClassHelperController.class);
		controllerClzList.add(BuiltinFileServiceController.class);
		
		for (Class<?> controllerClz : controllerClzList) {
			Object target = controllerClz.getDeclaredConstructor().newInstance();
			HookerController controller = controllerClz.getAnnotation(HookerController.class);
			for (Method method : controllerClz.getDeclaredMethods()) {
				HookerRequestMapping mapping = method.getAnnotation(HookerRequestMapping.class);
				if (mapping == null) {
					//logger.info("找不到HookerRequestMapping:"+method);
					continue;
				}
				// 5️⃣ 注册路由
				mustangHttpServer.addController(new MustangAutoWireServlet(target, method, controller, mapping));
			}
		}
		mustangHttpServer.start();
		HookerUiAutomationService.ensureStarted();
		logger.info("webserver start at " + port);
		HookerWebServerConfiguration serverAnno = httpServerClassList.isEmpty() ? null
				: httpServerClassList.get(0).getAnnotation(HookerWebServerConfiguration.class);
		String remoteAgentInfo = maybeAutoStartRemoteAgent(serverAnno, port);
		if (remoteAgentInfo != null && remoteAgentInfo.length() > 0) {
			info += remoteAgentInfo;
		}
		boolean hasPermission;
		Context ctx = Android.getApplication();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		    hasPermission =
		            ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
		                    == PackageManager.PERMISSION_GRANTED;
		} else {
		    hasPermission = true; // 6.0 以下安装即授予
		}
		return info;
	}

	private static String maybeAutoStartRemoteAgent(HookerWebServerConfiguration serverAnno, int port) {
		if (serverAnno == null || !serverAnno.autoStartRemoteAgent()) {
			return "";
		}
		if (serverAnno.remoteWsUrl() == null || serverAnno.remoteWsUrl().trim().length() == 0) {
			return "Remote agent auto-start skipped: remoteWsUrl is empty.\n";
		}
		applyRemoteAgentConfiguration(serverAnno);
		try {
			String startMessage = startRemoteAgent(port);
			
			if (startMessage == null || startMessage.length() == 0) {
				return "";
			}
			if (startMessage.endsWith("\n")) {
				return startMessage;
			}
			logger.info(startMessage);
			return startMessage + "\n";
		} catch (Exception e) {
			logger.warn(e);
			return "Remote agent auto-start failed: " + e.toString() + "\n";
		}
	}

	private static void applyRemoteAgentConfiguration(HookerWebServerConfiguration serverAnno) {
		// 这里把注解配置落到现有 system properties 上，
		// 这样 DeviceAgentConfig 仍然只维护一套取值逻辑，HTTP 控制接口和自动启动也共用同一套配置源。
		System.getProperties().setProperty("hooker.remote.enabled", "true");
		applySystemPropertyIfNotEmpty("hooker.remote.ws_url", serverAnno.remoteWsUrl());
		applySystemPropertyIfNotEmpty("hooker.remote.token", serverAnno.remoteToken());
		applySystemPropertyIfNotEmpty("hooker.remote.device_id", serverAnno.remoteDeviceId());
		applySystemPropertyIfNotEmpty("hooker.remote.app", serverAnno.remoteApp());
		applySystemPropertyIfNotEmpty("hooker.remote.package_name", serverAnno.remotePackageName());
		if (serverAnno.remoteHeartbeatSec() > 0) {
			System.getProperties().setProperty("hooker.remote.heartbeat_sec", String.valueOf(serverAnno.remoteHeartbeatSec()));
		}
	}

	private static void applySystemPropertyIfNotEmpty(String key, String value) {
		if (value == null) {
			return;
		}
		String trimmed = value.trim();
		if (trimmed.length() == 0) {
			return;
		}
		System.getProperties().setProperty(key, trimmed);
	}
	
	public static String startDefaultHttpServer() throws Exception {
		return start(new ArrayList<Class<?>>(), new ArrayList<Class<?>>());
	}

	public static synchronized String startRemoteAgent() throws Exception {
		// 复用最近一次 webserver 端口，适合先 webserver start，再通过 HTTP 或 patch 代码显式开启 agent。
		return startRemoteAgent(lastStartedPort);
	}

	public static synchronized String startRemoteAgent(int localPort) throws Exception {
		// 这里单独暴露显式入口，而不是在 startWithPort() 里自动启动，
		// 是为了把“是否接公网服务端”这个决定权留给 patch 开发者。
		DeviceAgentConfig agentConfig = DeviceAgentConfig.fromSystemProperties(localPort);
		if (!agentConfig.isEnabled()) {
			return "Remote agent disabled. Set hooker.remote.enabled=true and hooker.remote.ws_url first.";
		}
		long waitMs = Math.max(agentConfig.connectTimeoutMs + 5000, 8000);
		return DeviceAgent.getInstance().startAndWait(agentConfig, waitMs);
	}

	public static synchronized void stopRemoteAgent() {
		// 提供和 startRemoteAgent 对称的显式停止入口，方便通过内置 HTTP 接口或 patch 代码远程开关。
		DeviceAgent.getInstance().stop();
	}
	
//	public static void main(String[] args) throws Exception {
//		startDefaultHttpServer();
//		find("/das//dasd//d212121//11w");
//		find("");
//		find("/stopxxx");
//	}
//
//	private static void find(String path) {
//		MustangController finded = MustangWebServer.getInstance().mustangControllerRouter.findMustangController(path);
//		System.out.println(finded.getTargetMethod());
//	}

}
