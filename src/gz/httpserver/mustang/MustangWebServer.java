package gz.httpserver.mustang;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gz.com.alibaba.fastjson.JSON;
import gz.httpserver.HookerWebRequest;
import gz.httpserver.HookerWebServer;
import gz.httpserver.NanoHTTPD;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.annotation.HookerRequestPostJson;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.mustang.MustangControllerRouter.FindResult;
import gz.util.Logger;
import gz.util.XLog;

public class MustangWebServer extends HookerWebServer {
	
	private static MustangWebServer mustangWebServer = null;
	
	private static Logger logger = new Logger(MustangWebServer.class);

    public final MustangControllerRouter mustangControllerRouter = new MustangControllerRouter();
    
    public MustangWebServer(int port) {
        super(port);
        mustangWebServer = this;
    }
    
    public static final MustangWebServer getInstance() {
    	return mustangWebServer;
    }
    
    public void addController(MustangServlet mustangServlet) {
    	mustangControllerRouter.addMustangController(mustangServlet);
    }
    
    private boolean isHtml(String text) {
        return text != null && text.trim().startsWith("<");
    }
    
    private boolean isJson(String text) {
        return text != null && text.trim().startsWith("{");
    }
    
    public Object invodeJava(Map<String, Object> invokePayload) throws Exception {
    	FindResult findResult = mustangControllerRouter.findMustangController(invokePayload);
    	if (findResult.isSuccess()) {
    		MustangServlet mustangServlet = findResult.getServlet();
    		return mustangServlet.directInvoke(invokePayload);
    	}else if (findResult.isNotSupportGet()) {
    		throw new IllegalArgumentException("GET not supported");
    	}else if (findResult.isNotSupportPost()) {
    		throw new IllegalArgumentException("POST not supported");
    	}
    	Object routeObj = invokePayload == null ? null : invokePayload.get("route");
    	Object pathObj = routeObj instanceof Map ? ((Map) routeObj).get("path") : null;
    	throw new IllegalArgumentException("404 Not Found: " + String.valueOf(pathObj));
    }
    
    
    @SuppressWarnings({ "static-access" })
	@Override
    public Response onResponse(HookerWebRequest request) throws Exception {
    	logger.info("request path: " + request.getUrlPath());
    	FindResult findResult = mustangControllerRouter.findMustangController(request);
    	if (findResult.isSuccess()) {
    		try {
    			MustangServlet mustangServlet = findResult.getServlet();
        		Object result = mustangServlet.onResponse(request);
        		Produces produces = mustangServlet.getProduces();
        		if (result == null) {
        			return newFixedLengthResponse("");
        		}
        		if (result instanceof String) {
        			String contentType = null;
        			contentType = extractContentType(result, produces);
        			return newFixedLengthResponse(
                            Response.Status.OK,
                            contentType,
                            (String) result
                    );
        		}else if (result instanceof Map || result instanceof List) {
        			return newFixedLengthResponse(
                            Response.Status.OK,
                            Produces.JSON.value(),
                            JSON.toJSONString(result)
                    );
        		}else if (result instanceof Response) {
        			return (Response) result;
        		}else {
        			return newFixedLengthResponse(
                            Response.Status.OK,
                            Produces.TEXT.value(),
                            result.toString()
                    );
        		}
			} catch (Exception e) {
				logger.error(e);
				String exceptionHtml = XLog.getPrettyHtml(XLog.getException(e));
				return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        Produces.HTML.value(),
                        exceptionHtml
                );
			}
    	}else if (findResult.isFailure()) {
    		return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "404 Not Found: " + request.getUrlPath()
            );
    	}else if (findResult.isNotSupportGet()) {
    		return newFixedLengthResponse(
                    Response.Status.METHOD_NOT_ALLOWED,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "GET not supported"
            );
    	}else if (findResult.isNotSupportPost()) {
    		return newFixedLengthResponse(
                    Response.Status.METHOD_NOT_ALLOWED,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "POST not supported"
            );
    	}
        return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                NanoHTTPD.MIME_PLAINTEXT,
                "404 Not Found: " + request.getUrlPath()
        );
    }

	private String extractContentType(Object result, Produces produces) {
		String contentType;
		if (produces == Produces.AUTO) {
			if (isHtml((String) result)) {
				contentType = Produces.HTML.value();
			}else if (isJson((String) result)) {
				contentType = Produces.JSON.value();
			}else {
				contentType = Produces.TEXT.value();
			}
		}else {
			contentType = produces.value();
		}
		return contentType;
	}
	
	
	public String getAPIInfo() {
		// HTML 首页和 remote agent 上报都依赖同一份排序后的路由数据，
		// 这样浏览器里看到的接口顺序和设备注册给服务端的接口目录一致。
		List<MustangServlet> allServlets = getSortedServlets();
		StringBuilder info = new StringBuilder();
		for (MustangServlet mustangServlet : allServlets) {
			info.append("<div style='margin-top:12px;padding:12px 14px;background:#171717;border-radius:8px;'>");
			info.append("<div><b>")
				.append(escapeHtml(mustangServlet.getRequestMappingDefinition().method().name()))
				.append("</b> ");
			info.append("<a href=\"")
				.append(escapeHtmlAttr(buildRoutePath(mustangServlet)))
				.append("\">")
				.append(escapeHtml(buildRoutePath(mustangServlet)))
				.append("</a></div>");
			info.append("<div style='color:#aaa;font-size:13px;margin-top:4px;'>");
			info.append("Produces: ")
				.append(escapeHtml(mustangServlet.getProduces().name()));
			info.append(" | Handler: ")
				.append(escapeHtml(mustangServlet.getTargetMethod().getDeclaringClass().getSimpleName()))
				.append(".")
				.append(escapeHtml(mustangServlet.getTargetMethod().getName()))
				.append("</div>");
			MethodParameterInfo[] parameters = describeParameters(mustangServlet.getTargetMethod());
			if (parameters.length > 0) {
				info.append("<div style='margin-top:8px;font-size:13px;'>");
				for (int i = 0; i < parameters.length; i++) {
					info.append("<div style='margin-top:4px;color:#ddd;'>")
						.append(escapeHtml(describeParameter(parameters[i], i)))
						.append("</div>");
				}
				info.append("</div>");
			}
			info.append("</div>\n");
		}
		return info.toString();
	}

	public List<Map<String, Object>> getAPIDefinitions() {
		// 这是给 remote agent / 管理后台用的结构化接口目录：
		// 不返回纯文本，而是把 method/path/handler/parameters 拆开，方便服务端 UI 展示和后续做能力发现。
		List<MustangServlet> allServlets = getSortedServlets();
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (MustangServlet mustangServlet : allServlets) {
			Map<String, Object> item = new LinkedHashMap<String, Object>();
			item.put("method", mustangServlet.getRequestMappingDefinition().method().name());
			item.put("path", buildRoutePath(mustangServlet));
			item.put("produces", mustangServlet.getProduces().name());
			item.put("handler", mustangServlet.getTargetMethod().getDeclaringClass().getSimpleName()
					+ "." + mustangServlet.getTargetMethod().getName());
			List<Map<String, Object>> parameters = new ArrayList<Map<String, Object>>();
			MethodParameterInfo[] parameterInfos = describeParameters(mustangServlet.getTargetMethod());
			for (int i = 0; i < parameterInfos.length; i++) {
				parameters.add(describeParameterMap(parameterInfos[i], i));
			}
			item.put("parameters", parameters);
			result.add(item);
		}
		return result;
	}

	private List<MustangServlet> getSortedServlets() {
		// 单独抽出来是为了让 HTML API 列表和结构化 API 定义共用同一套排序逻辑，
		// 避免不同调用方看到的路由顺序不一致。
		List<MustangServlet> allServlets = mustangControllerRouter.getAllServlets();
		Collections.sort(allServlets, new Comparator<MustangServlet>() {
			@Override
			public int compare(MustangServlet left, MustangServlet right) {
				return buildRoutePath(left).compareTo(buildRoutePath(right));
			}
		});
		return allServlets;
	}

	private MethodParameterInfo[] describeParameters(java.lang.reflect.Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		MethodParameterInfo[] parameters = new MethodParameterInfo[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			parameters[i] = new MethodParameterInfo(
					parameterTypes[i],
					findAnnotation(parameterAnnotations, i, HookerRequestParam.class),
					findAnnotation(parameterAnnotations, i, HookerRequestPostJson.class));
		}
		return parameters;
	}

	private String describeParameter(MethodParameterInfo parameter, int index) {
		HookerRequestParam hookerRequestParam = parameter.requestParam;
		if (hookerRequestParam != null) {
			StringBuilder desc = new StringBuilder();
			desc.append("Param ")
				.append(index + 1)
				.append(": ")
				.append(hookerRequestParam.name())
				.append(" (")
				.append(parameter.type.getSimpleName())
				.append(")");
			desc.append(" required=")
				.append(hookerRequestParam.required());
			if (hookerRequestParam.defaultValue() != null
					&& !hookerRequestParam.defaultValue().isEmpty()
					&& !HookerRequestParam.NO_DEFAULT_VALUE.equals(hookerRequestParam.defaultValue())) {
				desc.append(" default=")
					.append(hookerRequestParam.defaultValue());
			}
			return desc.toString();
		}
		if (parameter.postJson != null) {
			return "Body: JSON -> " + parameter.type.getSimpleName();
		}
		return "Injected: " + parameter.type.getSimpleName();
	}

	private Map<String, Object> describeParameterMap(MethodParameterInfo parameter, int index) {
		// 远端管理页需要逐项展示参数，不适合只传 describeParameter() 那种拼好的字符串，
		// 所以这里保留结构化字段，后面服务端可以自由渲染。
		Map<String, Object> item = new LinkedHashMap<String, Object>();
		item.put("index", Integer.valueOf(index));
		item.put("type", parameter.type.getSimpleName());
		HookerRequestParam hookerRequestParam = parameter.requestParam;
		if (hookerRequestParam != null) {
			item.put("kind", "request_param");
			item.put("name", hookerRequestParam.name());
			item.put("required", Boolean.valueOf(hookerRequestParam.required()));
			String defaultValue = hookerRequestParam.defaultValue();
			if (defaultValue != null && !defaultValue.isEmpty()
					&& !HookerRequestParam.NO_DEFAULT_VALUE.equals(defaultValue)) {
				item.put("defaultValue", defaultValue);
			}
			return item;
		}
		if (parameter.postJson != null) {
			item.put("kind", "post_json");
			item.put("name", "body");
			item.put("required", Boolean.TRUE);
			return item;
		}
		item.put("kind", "injected");
		item.put("name", "arg" + index);
		item.put("required", Boolean.FALSE);
		return item;
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

	private static class MethodParameterInfo {
		private final Class<?> type;
		private final HookerRequestParam requestParam;
		private final HookerRequestPostJson postJson;

		private MethodParameterInfo(Class<?> type, HookerRequestParam requestParam, HookerRequestPostJson postJson) {
			this.type = type;
			this.requestParam = requestParam;
			this.postJson = postJson;
		}
	}

	private String buildRoutePath(MustangServlet mustangServlet) {
		String fullPath = mustangServlet.getControllerDefinition().value()
				+ "/"
				+ mustangServlet.getRequestMappingDefinition().path();
		String optimizedPath = fullPath.replaceAll("[/]+", "/");
		if (optimizedPath.isEmpty()) {
			return "/";
		}
		if (!optimizedPath.startsWith("/")) {
			return "/" + optimizedPath;
		}
		return optimizedPath;
	}

	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}

	private String escapeHtmlAttr(String text) {
		return escapeHtml(text).replace("'", "&#39;");
	}
    
}
