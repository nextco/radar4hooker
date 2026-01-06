package gz.httpserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerHttpServer;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.mustang.MustangController;
import gz.httpserver.mustang.MustangHttpServer;

public class HookerHttpServerBoot {

	public static final String HTTP_FLAG = "hooker_http_server";

	public static final class AutoMustangController extends MustangController {
		
		private Object target;

		private Method targetMethod;

		public AutoMustangController(Object target, Method targetMethod, HookerController controllerDefinition, HookerRequestMapping requestMappingDefinition) {
			super(controllerDefinition, requestMappingDefinition);
			this.target = target;
			this.targetMethod = targetMethod;
		}

		@Override
		public Object onResponse(HookerHTTPRequest request) throws Exception {
			Object[] args = buildMethodArgs(targetMethod, request);
			return targetMethod.invoke(target, args);
		}
	}
	
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
				if (clz.isAnnotationPresent(HookerHttpServer.class)) {
					httpServerClzList.add(clz);
				} 
				if (clz.isAnnotationPresent(HookerController.class)) {
					requestMappingClzList.add(clz);
				}
			}
			if (httpServerClzList.size() == 0) {
				return "You should at least configure a @HookerHttpServer";
			}
			if (httpServerClzList.size() > 1) {
				return "You can configure at most one @HookerHttpServer";
			}
			return start(httpServerClzList.get(0), requestMappingClzList);
		} catch (Exception e) {
			// 获取堆栈信息
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		}
	}

	private static String start(Class<?> httpServerClass, List<Class<?>> controllerClzList) throws Exception {
		String info = "";
		stop();
		HookerHttpServer serverAnno = httpServerClass
				.getAnnotation(HookerHttpServer.class);
		int port = serverAnno.port();
		info += "Http server port:" + port + "\n";
		String lanIP = getLanIp();
		if (lanIP != null) {
			info += "Http server: http://" +lanIP + ":" + port + "\n";
		}
		MustangHttpServer mustangHttpServer = new MustangHttpServer(port);
		for (Class<?> controllerClz : controllerClzList) {
			Object target = controllerClz.getDeclaredConstructor().newInstance();
			HookerController controller = controllerClz.getAnnotation(HookerController.class);
			for (Method method : controllerClz.getDeclaredMethods()) {
				HookerRequestMapping mapping = method.getAnnotation(HookerRequestMapping.class);
				if (mapping == null) {
					continue;
				}
				// 5️⃣ 注册路由
				mustangHttpServer.addController(new AutoMustangController(target, method, controller, mapping));
				String path = controller.value();
				path += mapping.value();
				info += "Mapping: " + path + " " + mapping.method().name()  + "\n";
				Parameter[] parameters = method.getParameters();
				Object[] args = new Object[parameters.length];
				
				for (int i = 0; i < parameters.length; i++) {
					Parameter p = parameters[i];
					HookerRequestParam rp = p.getAnnotation(HookerRequestParam.class);
					if (rp == null) {
						args[i] = null;
						continue;
					}
					String name = p.getName(); //兜底（需 -parameters）
					if (!"".equals(rp.value())) {
						name = rp.value();
					}else if (!"".equals(rp.name())) {
						name = rp.name();
					}
					int index = i + 1;
					info += "\tParam "+index+" name:" + name + " type:" +  p.getType().getSimpleName() + " required:" + rp.required() + " default value:" + rp.defaultValue();
					info += "\n";
				}
			}
		}
		mustangHttpServer.start();
		System.getProperties().put(HTTP_FLAG, mustangHttpServer);
		return info;
	}
	
	public static String startDefaultHttpServer() throws Exception {
		String info = "";
		stop();
		int port = 8080;
		info += "Http server port:" + port + "\n";
		String lanIP = getLanIp();
		if (lanIP != null) {
			info += "Http server: http://" +lanIP + ":" + port + "\n";
		}
		MustangHttpServer mustangHttpServer = new MustangHttpServer(port);
		mustangHttpServer.start();
		System.getProperties().put(HTTP_FLAG, mustangHttpServer);
		return info;
	}

	private static Object[] buildMethodArgs(Method targetMethod, HookerHTTPRequest request) throws Exception {
		Parameter[] parameters = targetMethod.getParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter p = parameters[i];
			HookerRequestParam rp = p.getAnnotation(HookerRequestParam.class);
			if (rp == null) {
				args[i] = null;
				continue;
			}
			String name = p.getName(); //兜底（需 -parameters）
			if (!"".equals(rp.value())) {
				name = rp.value();
			}else if (!"".equals(rp.name())) {
				name = rp.name();
			}
			String value = request.getParam(name);
			if ((value == null || value.isEmpty())) {
				value = rp.defaultValue();
			}
			if ((value == null || value.isEmpty()) && rp.required()) {
				throw new IllegalArgumentException("Missing request param: " + name);
			}
			args[i] = convert(value, p.getType());
		}
		return args;
	}

	private static Object convert(String value, Class<?> type) {
		if (value == null) {
			return null;
		}
		if (type == String.class) {
			return value;
		}
		if (type == int.class || type == Integer.class) {
			return Integer.parseInt(value);
		}
		if (type == long.class || type == Long.class) {
			return Long.parseLong(value);
		}
		if (type == boolean.class || type == Boolean.class) {
			return Boolean.parseBoolean(value);
		}
		throw new IllegalArgumentException("Unsupported param type: " + type);
	}

	public static void stop() {
		try {
			MustangHttpServer mustangHttpServer = (MustangHttpServer) System.getProperties().get(HTTP_FLAG);
			if (mustangHttpServer != null) {
				mustangHttpServer.stop();
				System.getProperties().put(HTTP_FLAG, null);
			}
		} catch (Exception e) {
		}
	}

}
