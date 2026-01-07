package gz.httpserver.mustang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gz.com.alibaba.fastjson.JSON;
import gz.httpserver.HookerHTTPRequest;
import gz.httpserver.HookerHTTPServer;
import gz.httpserver.HookerHttpServerBoot;
import gz.httpserver.NanoHTTPD;
import gz.httpserver.controller.WelcomeController;
import gz.httpserver.mustang.MustangController.MatchStatus;
import gz.util.Logger;
import gz.util.XLog;

public class MustangHttpServer extends HookerHTTPServer {
	
	private static Logger logger = new Logger(MustangHttpServer.class);

    protected final List<MustangController> mustangControllers = new ArrayList<>();
    
    public MustangHttpServer(int port) {
        super(port);
    }

    public void addController(MustangController mustangController) {
        mustangControllers.add(mustangController);
    }
    
    private boolean isHtml(String text) {
        return text != null && text.trim().startsWith("<");
    }

    
    @SuppressWarnings({ "static-access" })
	@Override
    public Response onResponse(HookerHTTPRequest request) throws Exception {
    	logger.info("onResponse path: " + request.getUrlPath());
    	if (request.getUrlPath().equals("/")) {
			return newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html; charset=utf-8",
                    new WelcomeController().welcome()
            );
    	}
        for (MustangController mustangController : mustangControllers) {
        	MatchStatus matchStatus = mustangController.matchRequest(request);
        	if (matchStatus.FAILURE == matchStatus) {
        		continue;
        	}
        	if (matchStatus.CONTROLLER_NOT_SUPPORT_GET == matchStatus) {
        		return newFixedLengthResponse(
                        Response.Status.METHOD_NOT_ALLOWED,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "GET not supported"
                );
        	}
        	if (matchStatus.CONTROLLER_NOT_SUPPORT_POST == matchStatus) {
        		return newFixedLengthResponse(
                        Response.Status.METHOD_NOT_ALLOWED,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "POST not supported"
                );
        	}
            if (matchStatus.SUCCESS == matchStatus) {
            	try {
            		Object result = mustangController.onResponse(request);
            		if (result == null) {
            			return newFixedLengthResponse("");
            		}
            		if (result instanceof String) {
            			String contentType = isHtml((String) result)
            		            ? "text/html; charset=utf-8"
            		            : "text/plain; charset=utf-8";
            			return newFixedLengthResponse(
                                Response.Status.OK,
                                contentType,
                                (String) result
                        );
            		}else if (result instanceof Map || result instanceof List) {
            			return newFixedLengthResponse(
                                Response.Status.OK,
                                "application/json; charset=utf-8",
                                JSON.toJSONString(result)
                        );
            		}else if (result instanceof Response) {
            			return (Response) result;
            		}else {
            			return newFixedLengthResponse(
                                Response.Status.OK,
                                NanoHTTPD.MIME_PLAINTEXT,
                                result.toString()
                        );
            		}
				} catch (Exception e) {
					logger.error(e);
					String exceptionHtml = XLog.getPrettyHtml(XLog.getException(e));
					return newFixedLengthResponse(
	                        Response.Status.INTERNAL_ERROR,
	                        "text/html; charset=utf-8",
	                        exceptionHtml
	                );
				}
            }
        }
        return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                NanoHTTPD.MIME_PLAINTEXT,
                "404 Not Found: " + request.getUrlPath()
        );
    }
    
}
