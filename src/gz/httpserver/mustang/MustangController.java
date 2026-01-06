package gz.httpserver.mustang;

import gz.httpserver.HookerHTTPRequest;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;

public abstract class MustangController {
	
	public static enum MatchStatus {
	    SUCCESS, FAILURE, CONTROLLER_NOT_SUPPORT_GET, CONTROLLER_NOT_SUPPORT_POST
	}
	
	private final HookerController controllerDefinition;
	
    private final HookerRequestMapping  requestMappingDefinition;
    
    public MustangController(HookerController controllerDefinition, HookerRequestMapping  requestMappingDefinition) {
        this.controllerDefinition = controllerDefinition;
    	this.requestMappingDefinition = requestMappingDefinition;
    }
    
    public MatchStatus matchRequest(HookerHTTPRequest request) {
    	String controllerPrefix = controllerDefinition.value();
    	if (!controllerPrefix.startsWith("/")) {
    		controllerPrefix = "/" + controllerPrefix;
    	}
    	if (controllerPrefix.length() > 1 && controllerPrefix.endsWith("/")) {
    		controllerPrefix = controllerPrefix.substring(0, controllerPrefix.length() - 1);
    	}
    	if (!request.getUrlPath().startsWith(controllerPrefix)) {
    		return MatchStatus.FAILURE;
    	}
    	if (request.getUrlPath().length() < controllerPrefix.length()) {
    		return MatchStatus.FAILURE;
    	}
    	String urlPathTail = request.getUrlPath().substring(controllerPrefix.length());
    	if (!urlPathTail.startsWith("/")) {
    		urlPathTail = "/" + urlPathTail;
    	}
    	
    	if (!requestMappingDefinition.value().equals(urlPathTail)) {
    		return MatchStatus.FAILURE;
    	}
    	HookerRequestMapping.Method requestMethod = request.getMethod();
    	if (requestMappingDefinition.method() != requestMethod) {
    		if (request.isGet()) {
    			return MatchStatus.CONTROLLER_NOT_SUPPORT_GET;
    		}else {
    			return MatchStatus.CONTROLLER_NOT_SUPPORT_POST;
    		}
    	}
    	return MatchStatus.SUCCESS;
    }


    public abstract Object onResponse(HookerHTTPRequest request) throws Exception;

}
