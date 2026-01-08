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
		private final MustangController controller;
		
		public FindResult(int resultCode, MustangController controller) {
			this.resultCode = resultCode;
			this.controller = controller;
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
		
		public MustangController getController() {
			return controller;
		}
		
	}
	
	private String nodeName;
	
	private MustangController nodeController;
	
	private Map<String, MustangControllerRouter> childRouters = new HashMap<String, MustangControllerRouter>();
	
    public MustangControllerRouter() {
    	this("", null);
	}
    
    
    public MustangControllerRouter(String nodeName, MustangController nodeController) {
		this.nodeName = nodeName;
		this.nodeController = nodeController;
	}
    
    /**
     * 注册一个 Controller 到 Router 树中
     */
    public void addMustangController(MustangController mustangController) {
        // 拼接完整路径
        String fullPath = mustangController.getControllerDefinition().value()
                + "/" +
                mustangController.getRequestMappingDefinition().path();
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
        current.nodeController = mustangController;
        logger.info("Register route: " + optimizedFullPath + " -> Controller: " + mustangController.getTargetMethod());
    }

    /**
     * 根据请求路径查找处理 Controller
     */
    private MustangController findMustangControllerWithPath(String path) {
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
    	MustangController mustangController = findMustangControllerWithPath(request.getUrlPath());
    	if (mustangController == null) {
    		return new FindResult(FindResult.RESULT_FAILURE, null);
    	}
    	HookerRequestMapping requestMappingDefinition = mustangController.getRequestMappingDefinition();
    	if (requestMappingDefinition.method() != request.getMethod()) {
    		if (request.isGet()) {
    			return new FindResult(FindResult.RESULT_CONTROLLER_NOT_SUPPORT_GET, null);
    		}else {
    			return new FindResult(FindResult.RESULT_CONTROLLER_NOT_SUPPORT_POST, null);
    		}
    	}
    	return new FindResult(FindResult.RESULT_SUCCESS, mustangController);
    }
    
}
