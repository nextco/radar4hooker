package gz.httpserver.mustang;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import gz.com.alibaba.fastjson.JSON;
import gz.httpserver.HookerWebRequest;
import gz.httpserver.HookerWebServer;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.annotation.HookerRequestPostJson;
import gz.util.Logger;

/**
 * 一个MustangAutoJnjectionServlet对应一个Controller的一个方法
 */
public class MustangAutoWireServlet extends MustangServlet {
	
	private static Logger logger = new Logger(MustangAutoWireServlet.class);

	private Object target;

	private Method targetMethod;

	public MustangAutoWireServlet(Object target, Method targetMethod, HookerController controllerDefinition, HookerRequestMapping requestMappingDefinition) {
		super(controllerDefinition, requestMappingDefinition);
		this.target = target;
		this.targetMethod = targetMethod;
	}

	@Override
	public Object onResponse(HookerWebRequest request) throws Exception {
		// 一条路由最终就是“一个 controller 实例 + 一个方法调用”，
		// 所以这里把 HTTP 请求先解析成方法参数，再统一走反射调用。
		Object[] args = buildInvokeArgs(targetMethod, new HttpInvokeValueProvider(request));
		try {
			return targetMethod.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw unwrapInvocationTargetException(e);
		}
	}
	
	@Override
	public Object directInvoke(Map<String, Object> invokePayload) throws Exception {
		Object[] args = buildInvokeArgs(targetMethod, new DirectInvokeValueProvider(invokePayload));
		try {
			return targetMethod.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw unwrapInvocationTargetException(e);
		}
	}
	
	private Object[] buildInvokeArgs(Method targetMethod, InvokeValueProvider valueProvider) throws Exception {
		Class<?>[] parameterTypes = targetMethod.getParameterTypes();
		Annotation[][] parameterAnnotations = targetMethod.getParameterAnnotations();
		Object[] args = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> parameterClass = parameterTypes[i];
			HookerRequestParam hookerRquestParam = findAnnotation(parameterAnnotations, i, HookerRequestParam.class);
			if (hookerRquestParam != null) {
				String name = "".equals(hookerRquestParam.name()) ? "arg" + i : hookerRquestParam.name();
				Object rawValue = valueProvider.getRequestParam(name);
				if (rawValue == null) {
					String defaultValue = hookerRquestParam.defaultValue();
					if (!HookerRequestParam.NO_DEFAULT_VALUE.equals(defaultValue)) {
						rawValue = defaultValue;
					}
				}
				if (rawValue == null && hookerRquestParam.required()) {
					throw new IllegalArgumentException("Missing request param: " + name);
				}
				// HTTP 调用和 directInvoke 现在共用一套参数绑定逻辑，
				// 两种模式的差异只体现在“原始值从哪里取”，而不再各自维护一份参数规则。
				args[i] = convertValue(rawValue, parameterClass);
			}else if(findAnnotation(parameterAnnotations, i, HookerRequestPostJson.class) != null) {
				args[i] = convertValue(valueProvider.getPostJsonRaw(), parameterClass);
			}else if (parameterClass == HookerWebServer.class) {
				// 对这几种基础运行时对象做自动注入，是为了让 controller 能访问当前 server/request/servlet，
				// 同时不需要在每个 handler 里自己找全局单例。
				args[i] = MustangWebServer.getInstance();
			}else if (parameterClass == HookerWebRequest.class) {
				HookerWebRequest hookerWebRequest = valueProvider.getHookerWebRequest();
				if (hookerWebRequest == null) {
					throw new IllegalArgumentException("directInvoke does not support HookerWebRequest injection: " + targetMethod);
				}
				args[i] = hookerWebRequest;
			}else if (parameterClass == MustangServlet.class) {
				args[i] = this;
			}else {
				args[i] = null;
				// 未识别参数不直接抛错，保留向后兼容；但打日志提醒开发者签名没有被当前注入规则覆盖。
				logger.warn("parameter "+i+" have no annotations: " + targetMethod);
				continue;
			}
		}
		return args;
	}

	private static <T extends Annotation> T findAnnotation(Annotation[][] parameterAnnotations, int index, Class<T> annotationClass) {
		if (parameterAnnotations == null || index < 0 || index >= parameterAnnotations.length) {
			return null;
		}
		Annotation[] annotations = parameterAnnotations[index];
		if (annotations == null) {
			return null;
		}
		for (int i = 0; i < annotations.length; i++) {
			Annotation annotation = annotations[i];
			if (annotationClass.isInstance(annotation)) {
				return annotationClass.cast(annotation);
			}
		}
		return null;
	}

	private static Object convertValue(Object value, Class<?> type) {
		if (value == null) {
			return null;
		}
		if (type == String.class || CharSequence.class.isAssignableFrom(type)) {
			return String.valueOf(value);
		}
		if (type == byte.class || type == Byte.class) {
			return value instanceof Number ? ((Number) value).byteValue() : Byte.parseByte(String.valueOf(value));
		}
		if (type == short.class || type == Short.class) {
			return value instanceof Number ? ((Number) value).shortValue() : Short.parseShort(String.valueOf(value));
		}
		if (type == int.class || type == Integer.class) {
			return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
		}
		if (type == long.class || type == Long.class) {
			return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
		}
		if (type == float.class || type == Float.class) {
			return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(String.valueOf(value));
		}
		if (type == double.class || type == Double.class) {
			return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(String.valueOf(value));
		}
		if (type == boolean.class || type == Boolean.class) {
			if (value instanceof Boolean) {
				return value;
			}
			return Boolean.parseBoolean(String.valueOf(value));
		}
		if (Map.class.isAssignableFrom(type) || List.class.isAssignableFrom(type)) {
			// 这里允许 controller 直接声明 Map/List，是为了让简单 JSON body/query 参数不用单独写 DTO。
			if (value instanceof String) {
				return JSON.parseObject((String) value, type);
			}
			return JSON.parseObject(JSON.toJSONString(value), type);
		}
		throw new IllegalArgumentException("Unsupported param type: " + type);
	}

	private static Exception unwrapInvocationTargetException(InvocationTargetException e) {
		Throwable cause = e.getCause();
		if (cause instanceof Exception) {
			return (Exception) cause;
		}
		if (cause instanceof Error) {
			throw (Error) cause;
		}
		return e;
	}

	private interface InvokeValueProvider {
		Object getRequestParam(String name);
		Object getPostJsonRaw();
		HookerWebRequest getHookerWebRequest();
	}

	private static class HttpInvokeValueProvider implements InvokeValueProvider {
		private final HookerWebRequest request;

		private HttpInvokeValueProvider(HookerWebRequest request) {
			this.request = request;
		}

		@Override
		public Object getRequestParam(String name) {
			return request.getParam(name);
		}

		@Override
		public Object getPostJsonRaw() {
			return request.getPostRaw();
		}

		@Override
		public HookerWebRequest getHookerWebRequest() {
			return request;
		}
	}

	private static class DirectInvokeValueProvider implements InvokeValueProvider {
		private final Map argsMap;

		private DirectInvokeValueProvider(Map<String, Object> invokePayload) {
			Map localArgsMap = null;
			if (invokePayload != null) {
				Object rawArgs = invokePayload.get("args");
				if (rawArgs instanceof Map) {
					localArgsMap = (Map) rawArgs;
				}
			}
			this.argsMap = localArgsMap;
		}

		@Override
		public Object getRequestParam(String name) {
			return argsMap != null ? argsMap.get(name) : null;
		}

		@Override
		public Object getPostJsonRaw() {
			return argsMap;
		}

		@Override
		public HookerWebRequest getHookerWebRequest() {
			return null;
		}
	}

	@Override
	public Object getTarget() {
		return target;
	}

	@Override
	public Method getTargetMethod() {
		return targetMethod;
	}
}
