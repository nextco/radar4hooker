package gz.httpserver.mustang;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
		Object[] args = buildMethodArgs(targetMethod, request);
		return targetMethod.invoke(target, args);
	}
	
	private Object[] buildMethodArgs(Method targetMethod, HookerWebRequest request) throws Exception {
		Parameter[] parameters = targetMethod.getParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter reflectParam = parameters[i];
			Class<?> parameterClass = reflectParam.getType();
			if (reflectParam.isAnnotationPresent(HookerRequestParam.class)) {
				HookerRequestParam hookerRquestParam = reflectParam.getAnnotation(HookerRequestParam.class);
				String name = reflectParam.getName(); //兜底（需 -parameters）
				if (!"".equals(hookerRquestParam.name())) {
					name = hookerRquestParam.name();
				}
				String value = request.getParam(name);
				if ((value == null || value.isEmpty())) {
					value = hookerRquestParam.defaultValue();
				}
				if ((value == null || value.isEmpty()) && hookerRquestParam.required()) {
					throw new IllegalArgumentException("Missing request param: " + name);
				}
				args[i] = convert(value, reflectParam.getType());
			}else if(reflectParam.isAnnotationPresent(HookerRequestPostJson.class)) {
				String postRaw = request.getPostRaw();
				args[i] = convert(postRaw, reflectParam.getType());
			}else if (parameterClass == HookerWebServer.class) {
				args[i] = MustangWebServer.getInstance();
			}else if (parameterClass == HookerWebRequest.class) {
				args[i] = request;
			}else if (parameterClass == MustangServlet.class) {
				args[i] = this;
			}else {
				args[i] = null;
				logger.warn("parameter "+i+" have no annotations: " + targetMethod);
				continue;
			}
		}
		return args;
	}

	private static Object convert(String value, Class<?> type) {
		if (value == null) {
			return null;
		}
		if (type == String.class || CharSequence.class.isAssignableFrom(type)) {
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
		if (Map.class.isAssignableFrom(type) || List.class.isAssignableFrom(type)) {
			return JSON.parseObject(value, type);
		}
		throw new IllegalArgumentException("Unsupported param type: " + type);
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
