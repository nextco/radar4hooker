package gz.httpserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gz.httpserver.NanoHTTPD.IHTTPSession;
import gz.httpserver.NanoHTTPD.Method;
import gz.httpserver.NanoHTTPD.ResponseException;
import gz.httpserver.annotation.HookerRequestMapping;

public class HookerHTTPRequest {

	private final IHTTPSession session;
	
	private final Map<String, String> files = new HashMap<>();
	
	private final Map<String, List<String>> parameter;
	
	private String urlPath;

	public HookerHTTPRequest(IHTTPSession session) throws IOException, ResponseException {
		this.session = session;
		this.parameter = session.getParameters();
		if (isPost()) {
			try {
				session.parseBody(files);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String path = session.getUri();
		if (path == null) {
			this.urlPath = "/";
		}else {
			path = path.replaceAll("[/]{2,}", "/");
	        if (path.contains("?")) {
	        	path = urlPath.split("\\?")[0];
	        }
	        if (path.length() > 1 && path.endsWith("/")) {
	        	path = path.substring(0, path.length() - 1);
	    	}
	        this.urlPath = path;
		}
	}
	
	public String getUrlPath() {
		return urlPath;
	}
	
	public String getHeader(String headerName) {
		return session.getHeaders().get(headerName);
	}
	
	public boolean isGet() {
		return session.getMethod() == Method.GET;
	}
	
	public boolean isPost() {
		return session.getMethod() == Method.POST;
	}
	
	public HookerRequestMapping.Method getMethod() {
		return isGet()? HookerRequestMapping.Method.GET : HookerRequestMapping.Method.POST;
	}
	
	public String getContentType() {
		String contentType = getHeader("Content-Type");
		if (contentType != null) {
			return contentType;
		}
		return getHeader("content-type");
	}
	
	public String getPostRaw() {
		String body = files.get("postData");
		return body;
	}
	
	public String getParam(String pName) {
		List<String> list = parameter.get(pName);
		return list != null && list.size() > 0 ? list.get(0) : null;
	}
	
	
	
	
}
