package gz.httpserver.mustang;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.util.Log;
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
		List<MustangServlet> allServlets = mustangControllerRouter.getAllServlets();
		Collections.sort(allServlets, new Comparator<MustangServlet>() {
			@Override
			public int compare(MustangServlet left, MustangServlet right) {
				return buildRoutePath(left).compareTo(buildRoutePath(right));
			}
		});
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
			Parameter[] parameters = mustangServlet.getTargetMethod().getParameters();
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

	private String describeParameter(Parameter parameter, int index) {
		HookerRequestParam hookerRequestParam = parameter.getAnnotation(HookerRequestParam.class);
		if (hookerRequestParam != null) {
			StringBuilder desc = new StringBuilder();
			desc.append("Param ")
				.append(index + 1)
				.append(": ")
				.append(hookerRequestParam.name())
				.append(" (")
				.append(parameter.getType().getSimpleName())
				.append(")");
			desc.append(" required=")
				.append(hookerRequestParam.required());
			if (hookerRequestParam.defaultValue() != null && !hookerRequestParam.defaultValue().isEmpty()) {
				desc.append(" default=")
					.append(hookerRequestParam.defaultValue());
			}
			return desc.toString();
		}
		if (parameter.isAnnotationPresent(HookerRequestPostJson.class)) {
			return "Body: JSON -> " + parameter.getType().getSimpleName();
		}
		return "Injected: " + parameter.getType().getSimpleName();
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
