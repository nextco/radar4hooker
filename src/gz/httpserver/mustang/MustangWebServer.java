package gz.httpserver.mustang;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gz.com.alibaba.fastjson.JSON;
import gz.httpserver.HookerWebRequest;
import gz.httpserver.HookerWebServer;
import gz.httpserver.NanoHTTPD;
import gz.httpserver.annotation.HookerRequestParam;
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
    
    public void addController(MustangController mustangController) {
    	mustangControllerRouter.addMustangController(mustangController);
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
    			MustangController mustangController = findResult.getController();
        		Object result = mustangController.onResponse(request);
        		Produces produces = mustangController.getProduces();
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
                        Produces.TEXT.value(),
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
		String info = "";
//		for (MustangController mustangController : mustangControllers) {
//			String path = mustangController.getControllerDefinition().value();
//			path += mustangController.getRequestMappingDefinition().path();
//			info += "Mapping: " + path + " " + mustangController.getRequestMappingDefinition().method().name()  + "\n";
//			Parameter[] parameters = mustangController.getTargetMethod().getParameters();
//			Object[] args = new Object[parameters.length];
//			
//			for (int i = 0; i < parameters.length; i++) {
//				Parameter p = parameters[i];
//				HookerRequestParam hookerRequestParam = p.getAnnotation(HookerRequestParam.class);
//				if (hookerRequestParam == null) {
//					args[i] = null;
//					continue;
//				}
//				String name = p.getName(); //兜底（需 -parameters）
//				if (!"".equals(hookerRequestParam.name())) {
//					name = hookerRequestParam.name();
//				}
//				int index = i + 1;
//				info += "\tParam "+index+" name:" + name + " type:" +  p.getType().getSimpleName() + " required:" + hookerRequestParam.required() + " default value:" + hookerRequestParam.defaultValue();
//				info += "\n";
//			}
//		}
		return info;
	}
    
}
