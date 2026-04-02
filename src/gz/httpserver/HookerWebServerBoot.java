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
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerWebServerConfiguration;
import gz.httpserver.annotation.HookerRequestMapping;
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
import gz.radar.AndroidUI;
import gz.radar.objects.ObjectsStore;
import gz.util.Logger;

public class HookerWebServerBoot {
	
	private static Logger logger = new Logger(HookerWebServerBoot.class);

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
	
	public static String startDefaultHttpServer() throws Exception {
		return start(new ArrayList<Class<?>>(), new ArrayList<Class<?>>());
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
