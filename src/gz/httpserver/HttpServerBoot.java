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

import gz.httpserver.annotation.HttpServer;
import gz.httpserver.annotation.RequestMapping;
import gz.httpserver.annotation.RequestParam;
import gz.httpserver.defaultcontroller.HelloWordContraler;
import gz.httpserver.mustang.MustangController;
import gz.httpserver.mustang.MustangHTTPParams;
import gz.httpserver.mustang.MustangHttpServer;

public class HttpServerBoot {

	public static final String HTTP_FLAG = "hooker_http_server";

	public static final class AutoMustangController extends MustangController {

		private Object target;

		private Method method;

		public AutoMustangController(Object target, Method method, String path) {
			super(path);
			this.target = target;
			this.method = method;

		}

		@Override
		public String onResponse(MustangHTTPParams params) throws Exception {
			Object[] args = buildMethodArgs(method, params);
			Object result = method.invoke(target, args);
			return result != null ? result.toString() : "";
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
	
//	public static void main(String[] args) {
//		List<String> allClz = new ArrayList<String>();
//		allClz.add("douyin.patch.XServer");
//		new Thread() {
//			public void run() {
//				System.out.println(scanAndStartHttpServer(allClz));
//			};
//		}.start();
//	}

	public static String scanAndStartHttpServer(List<String> allClz) {
		try {
			Class<?> httpServerClz = null;
			for (String clzName : allClz) {
				Class<?> clz = Class.forName(clzName);
				// 判断class是否有@HttpServer注解
				if (!clz.isAnnotationPresent(HttpServer.class)) {
					continue;
				}
				httpServerClz = clz;
				return start(httpServerClz);
			}
			return "Not found http server definition class" ;
		} catch (Exception e) {
			// 获取堆栈信息
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		}
	}

	private static String start(Class<?> httpServerClass) throws Exception {
		String info = "";
		stop();
		gz.httpserver.annotation.HttpServer serverAnno = httpServerClass
				.getAnnotation(gz.httpserver.annotation.HttpServer.class);
		int port = serverAnno.port();
		info += "Http server port:" + port + "\n";
		String lanIP = getLanIp();
		if (lanIP != null) {
			info += "Http server: http://" +lanIP + ":" + port + "\n";
		}
		Object target = httpServerClass.getDeclaredConstructor().newInstance();
		MustangHttpServer mustangHttpServer = new MustangHttpServer(port);
		for (Method method : httpServerClass.getDeclaredMethods()) {
			RequestMapping mapping = method.getAnnotation(RequestMapping.class);
			if (mapping == null) {
				continue;
			}
			String path = mapping.value();
			// 5️⃣ 注册路由
			mustangHttpServer.addController(new AutoMustangController(target, method, path));
			info += "Mapping: " + path + " GET/POST\n";
			Parameter[] parameters = method.getParameters();
			Object[] args = new Object[parameters.length];
			
			for (int i = 0; i < parameters.length; i++) {
				Parameter p = parameters[i];
				RequestParam rp = p.getAnnotation(RequestParam.class);
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
		mustangHttpServer.addController(new HelloWordContraler("/helloworld"));
		info += "\nMapping: /helloworld GET/POST\n";
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
		mustangHttpServer.addController(new HelloWordContraler("/helloworld"));
		info += "Mapping: /helloworld GET/POST\n";
		mustangHttpServer.start();
		System.getProperties().put(HTTP_FLAG, mustangHttpServer);
		return info;
	}

	private static Object[] buildMethodArgs(Method method, MustangHTTPParams params) throws Exception {
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter p = parameters[i];
			RequestParam rp = p.getAnnotation(RequestParam.class);
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
			String value = params.getParamter(name);
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
