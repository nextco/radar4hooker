package gz.httpserver.controller;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestPostJson;
import gz.util.Logger;
import gz.util.XLog;

@HookerController("/hooker/classhelper/")
public class BuiltinClassHelperController {
	
	private Logger logger = new Logger(BuiltinClassHelperController.class);

	@HookerRequestMapping(path="invoke_static_method", produces = Produces.AUTO, method = gz.httpserver.annotation.HookerRequestMapping.Method.POST)
	public Map<String, Object> invoke_static_method(@HookerRequestPostJson Map<String, Object> class_info) throws Exception {
	    Map<String, Object> result = new HashMap<>();
	    try {
	        String className = (String) class_info.get("class_name");
	        String methodName = (String) class_info.get("method_name");
	        List<Object> methodArgs = (List<Object>) class_info.get("method_args");
	        List<String> argTypeNames = (List<String>) class_info.get("method_arg_types");
	        if (className == null || methodName == null) {
	            result.put("success", false);
	            result.put("error", "class_name or method_name missing");
	            return result;
	        }
	        Class<?> targetClass = Class.forName(className);
	        int argCount = (argTypeNames == null) ? 0 : argTypeNames.size();
	        Class<?>[] argTypes = new Class[argCount];
	        for (int i = 0; i < argCount; i++) {
	            argTypes[i] = resolveClass(argTypeNames.get(i));
	        }
	        Object[] args = new Object[argCount];
	        for (int i = 0; i < argCount; i++) {
	            Object raw = methodArgs.get(i);
	            Class<?> target = argTypes[i];
	            args[i] = convertNumber(raw, target);
	        }
	        Method method = targetClass.getDeclaredMethod(methodName, argTypes);
	        method.setAccessible(true);
	        Object returnValue = method.invoke(null, args);
	        result.put("success", true);
	        result.put("return_value", returnValue);
	        result.put("return_type", returnValue == null ? "void/null" : returnValue.getClass().getName());
	    } catch (Exception e) {
	        result.put("success", false);
	        result.put("exception", e.toString());
	        result.put("stacktrace", XLog.getException(e));
	    }
	    return result;
	}

	/**
	 * 将字符串类型名转换为 Class
	 * 支持:
	 * int / boolean / long / float / double / short / byte / char
	 * java.lang.String
	 * fully qualified class
	 * 数组: java.lang.String[]  int[]
	 */
	private Class<?> resolveClass(String name) throws ClassNotFoundException {
	    if (name == null) return Object.class;

	    switch (name) {
	        case "int": return int.class;
	        case "long": return long.class;
	        case "boolean": return boolean.class;
	        case "float": return float.class;
	        case "double": return double.class;
	        case "short": return short.class;
	        case "byte": return byte.class;
	        case "char": return char.class;
	        case "void": return void.class;
	    }
	    // array like int[] / java.lang.String[]
	    if (name.endsWith("[]")) {
	        String base = name.substring(0, name.length() - 2);
	        return java.lang.reflect.Array.newInstance(resolveClass(base), 0).getClass();
	    }
	    return Class.forName(name);
	}
	
	private Object convertNumber(Object value, Class<?> targetType) {
	    if (value == null) return null;

	    if (!(value instanceof Number)) {
	        return value;
	    }

	    Number n = (Number) value;

	    if (targetType == int.class || targetType == Integer.class) {
	        return n.intValue();
	    }
	    if (targetType == long.class || targetType == Long.class) {
	        return n.longValue();
	    }
	    if (targetType == float.class || targetType == Float.class) {
	        return n.floatValue();
	    }
	    if (targetType == double.class || targetType == Double.class) {
	        return n.doubleValue();
	    }
	    if (targetType == short.class || targetType == Short.class) {
	        return n.shortValue();
	    }
	    if (targetType == byte.class || targetType == Byte.class) {
	        return n.byteValue();
	    }
	    // 不是数字参数或不需要处理
	    return value;
	}


	
}
