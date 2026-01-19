package gz.httpserver.mustang;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.util.Log;
import gz.httpserver.HookerWebRequest;
import gz.httpserver.NanoHTTPD;
import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.util.Logger;

public abstract class MustangServlet {
	
	private static Logger logger = new Logger(MustangServlet.class);
	
	private final HookerController controllerDefinition;
	
    private final HookerRequestMapping  requestMappingDefinition;
    
    public MustangServlet(HookerController controllerDefinition, HookerRequestMapping  requestMappingDefinition) {
        this.controllerDefinition = controllerDefinition;
    	this.requestMappingDefinition = requestMappingDefinition;
    }
    
    public Produces getProduces() {
    	return requestMappingDefinition.produces();
    }
    
    
    public HookerController getControllerDefinition() {
		return controllerDefinition;
	}

	public HookerRequestMapping getRequestMappingDefinition() {
		return requestMappingDefinition;
	}
	
	/* ================= 固定长度响应 ================= */

    // text/plain 200
    public Response newFixedLengthResponse(String text) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "text/plain; charset=utf-8",
                text
        );
    }

    // 指定 mime
    public Response newFixedLengthResponse(String mime, String text) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                mime,
                text
        );
    }

    // 指定状态码 + mime
    public Response newFixedLengthResponse(NanoHTTPD.Response.Status status,
                                           String mime,
                                           String text) {
        return NanoHTTPD.newFixedLengthResponse(status, mime, text);
    }

    // 二进制数据
    public Response newFixedLengthResponse(byte[] data, String mime) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                mime,
                new ByteArrayInputStream(data),
                data.length
        );
    }

    /* ================= Chunked 流式响应 ================= */

    // 最常用
    public Response newChunkedResponse(InputStream is, String mime) {
        return NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.OK,
                mime,
                is
        );
    }

    // 指定状态码
    public Response newChunkedResponse(NanoHTTPD.Response.Status status,
                                       String mime,
                                       InputStream is) {
        return NanoHTTPD.newChunkedResponse(status, mime, is);
    }

    /* ================= 文件响应 ================= */

    public Response newFileResponse(File file) {
        try {
            String mime = NanoHTTPD.getMimeTypeForFile(file.getName());
            FileInputStream fis = new FileInputStream(file);
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mime, fis, fis.available());
//            return NanoHTTPD.newChunkedResponse(
//                    NanoHTTPD.Response.Status.OK,
//                    mime,
//                    fis
//            );
        } catch (Exception e) {
            return newErrorResponse(e);
        }
    }
    
    public static void main(String[] args) {
		System.out.println(NanoHTTPD.getMimeTypeForFile("d8f2a7.jpg"));
	}

    /* ================= JSON 响应 ================= */

    public Response newJsonResponse(String json) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json; charset=utf-8",
                json
        );
    }

    /* ================= 常用状态封装 ================= */

    public Response newOk(String text) {
        return newFixedLengthResponse(text);
    }

    public Response newNotFound(String msg) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "text/plain; charset=utf-8",
                msg
        );
    }

    public Response newForbidden(String msg) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.FORBIDDEN,
                "text/plain; charset=utf-8",
                msg
        );
    }

    public Response newBadRequest(String msg) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "text/plain; charset=utf-8",
                msg
        );
    }

    public Response newServerError(String msg) {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                "text/plain; charset=utf-8",
                msg
        );
    }

    public Response newErrorResponse(Throwable t) {
        return newServerError(Log.getStackTraceString(t));
    }

    /* ================= 目录列表 HTML ================= */

    public Response newDirListResponse(File dir, String uri) {
        if (!dir.isDirectory()) {
            return newBadRequest("Not a directory");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>Index of ").append(uri).append("</h3>");

        for (File f : dir.listFiles()) {
            sb.append("<a href=\"")
              .append(uri.endsWith("/") ? uri : uri + "/")
              .append(f.getName())
              .append("\">")
              .append(f.getName())
              .append("</a><br>");
        }

        sb.append("</body></html>");

        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "text/html; charset=utf-8",
                sb.toString()
        );
    }
	
	public abstract Object getTarget();
	
	public abstract Method getTargetMethod();

	public abstract Object onResponse(HookerWebRequest request) throws Exception;

}
