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

import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerWebServerConfiguration;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.controller.BasicServiceController;
import gz.httpserver.mustang.MustangAutoJnjectionController;
import gz.httpserver.mustang.MustangWebServer;
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
		String info = "";
		int port = 8080;
		if (!httpServerClassList.isEmpty()) {
			HookerWebServerConfiguration serverAnno = httpServerClassList.get(0)
					.getAnnotation(HookerWebServerConfiguration.class);
			port = serverAnno.port();
		}
		info += "Http server port:" + port + "\n";
		String lanIP = getLanIp();
		if (lanIP != null) {
			info += "Http server: http://" +lanIP + ":" + port + "\n";
		}
		MustangWebServer mustangHttpServer = new MustangWebServer(port);
		
		controllerClzList.add(BasicServiceController.class);
		
		for (Class<?> controllerClz : controllerClzList) {
			Object target = controllerClz.getDeclaredConstructor().newInstance();
			HookerController controller = controllerClz.getAnnotation(HookerController.class);
			for (Method method : controllerClz.getDeclaredMethods()) {
				HookerRequestMapping mapping = method.getAnnotation(HookerRequestMapping.class);
				if (mapping == null) {
					logger.info("找不到HookerRequestMapping:"+method);
					continue;
				}
				// 5️⃣ 注册路由
				mustangHttpServer.addController(new MustangAutoJnjectionController(target, method, controller, mapping));
			}
		}
		mustangHttpServer.start();
		logger.info("webserver start at " + port);
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
