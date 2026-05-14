package gz.httpserver.mustang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gz.httpserver.HookerWebRequest;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.util.Logger;

public class MustangControllerRouter {

	private static Logger logger = new Logger(MustangControllerRouter.class);
	
	public final class FindResult {
		public static final int RESULT_SUCCESS = 0;
		public static final int RESULT_FAILURE = 1;
		public static final int RESULT_CONTROLLER_NOT_SUPPORT_GET = 2;
		public static final int RESULT_CONTROLLER_NOT_SUPPORT_POST = 3;
		
		private final int resultCode;
		private final MustangServlet servlet;
		
		public FindResult(int resultCode, MustangServlet controller) {
			this.resultCode = resultCode;
			this.servlet = controller;
		}
		
		public boolean isSuccess() {
			return this.resultCode == RESULT_SUCCESS;
		}
		
		public boolean isFailure() {
			return this.resultCode == RESULT_FAILURE;
		}
		
		public boolean isNotSupportGet() {
			return this.resultCode == RESULT_CONTROLLER_NOT_SUPPORT_GET;
		}
		
		public boolean isNotSupportPost() {
			return this.resultCode == RESULT_CONTROLLER_NOT_SUPPORT_POST;
		}
		
		public MustangServlet getServlet() {
			return servlet;
		}
		
	}
	
	private String nodeName;
	
	private MustangServlet nodeController;
	
	private Map<String, MustangControllerRouter> childRouters = new HashMap<String, MustangControllerRouter>();
	
    public MustangControllerRouter() {
    	this("", null);
	}
    
    
    public MustangControllerRouter(String nodeName, MustangServlet nodeController) {
		this.nodeName = nodeName;
		this.nodeController = nodeController;
	}
    
    /**
     * 注册一个 Controller 到 Router 树中
     */
    public void addMustangController(MustangServlet mustangServlet) {
        // 拼接完整路径
        String fullPath = mustangServlet.getControllerDefinition().value()
                + "/" +
                mustangServlet.getRequestMappingDefinition().path();
        String optimizedFullPath = fullPath.replaceAll("[/]+", "/");
        // 规范化 + 切分
        List<String> pathSegments = new ArrayList<String>();
        String[] arr = optimizedFullPath.split("/");
        for (String s : arr) {
            if (s != null && !s.isEmpty()) {
                pathSegments.add(s);
            }
        }
        MustangControllerRouter current = this;
        for (String segment : pathSegments) {
            MustangControllerRouter next =
                    current.childRouters.get(segment);
            if (next == null) {
                next = new MustangControllerRouter(segment, null);
                current.childRouters.put(segment, next);
            }
            current = next;
        }
        // 叶子节点绑定 Controller
        current.nodeController = mustangServlet;
        logger.info("Register route: " + optimizedFullPath + " -> Controller: " + mustangServlet.getTargetMethod());
    }

    /**
     * 根据请求路径查找处理 Controller
     */
    private MustangServlet findMustangControllerWithPath(String path) {
        if (path == null && nodeName.equals("")) {
        	return nodeController;
        }
        String optimizedPath = path.replaceAll("//", "/");
        String[] arr = optimizedPath.split("/");
        List<String> pathSegments = new ArrayList<String>();
        for (String s : arr) {
            if (s != null && !s.isEmpty()) {
                pathSegments.add(s);
            }
        }
        if (pathSegments.isEmpty() && nodeName.equals(path)) {
        	return nodeController;
        }
        MustangControllerRouter current = this;
        for (String segment : pathSegments) {
            MustangControllerRouter next =
                    current.childRouters.get(segment);
            //System.out.println("find segment: " + segment + " next:" + next);
            if (next == null) {
                return null;   // 404
            }
            current = next;
        }
        return current.nodeController;
    }
    
    public FindResult findMustangController(HookerWebRequest request) {
    	MustangServlet mustangServlet = findMustangControllerWithPath(request.getUrlPath());
    	if (mustangServlet == null) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	HookerRequestMapping requestMappingDefinition = mustangServlet.getRequestMappingDefinition();
    	if (requestMappingDefinition.method() != request.getMethod()) {
    		if (request.isGet()) {
    			return new FindResult(FindResult.RESULT_CONTROLLER_NOT_SUPPORT_GET, null);
    		}else {
    			return new FindResult(FindResult.RESULT_CONTROLLER_NOT_SUPPORT_POST, null);
    		}
    	}
    	return new FindResult(FindResult.RESULT_SUCCESS, mustangServlet);
    }

    public FindResult findMustangController(Map<String, Object> invokePayload) {
    	if (invokePayload == null) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	Object routeObj = invokePayload.get("route");
    	if (!(routeObj instanceof Map)) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	Map route = (Map) routeObj;
    	Object pathObj = route.get("path");
    	if (pathObj == null) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	String path = String.valueOf(pathObj);
    	MustangServlet mustangServlet = findMustangControllerWithPath(path);
    	if (mustangServlet == null) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	Object methodObj = route.get("method");
    	String method = methodObj == null ? HookerRequestMapping.Method.GET.name() : String.valueOf(methodObj).toUpperCase();
    	HookerRequestMapping.Method invokeMethod;
    	try {
    		invokeMethod = HookerRequestMapping.Method.valueOf(method);
    	} catch (Exception e) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	HookerRequestMapping requestMappingDefinition = mustangServlet.getRequestMappingDefinition();
    	if (requestMappingDefinition.method() != invokeMethod) {
    		if (invokeMethod == HookerRequestMapping.Method.GET) {
    			return new FindResult(FindResult.RESULT_CONTROLLER_NOT_SUPPORT_GET, null);
    		}else {
    			return new FindResult(FindResult.RESULT_CONTROLLER_NOT_SUPPORT_POST, null);
    		}
    	}
    	return new FindResult(FindResult.RESULT_SUCCESS, mustangServlet);
    }

    public List<MustangServlet> getAllServlets() {
    	List<MustangServlet> allServlets = new ArrayList<MustangServlet>();
    	collectServlets(allServlets);
    	return allServlets;
    }

    private void collectServlets(List<MustangServlet> allServlets) {
    	if (nodeController != null) {
    		allServlets.add(nodeController);
    	}
    	for (MustangControllerRouter childRouter : childRouters.values()) {
    		childRouter.collectServlets(allServlets);
    	}
    }
    
}
