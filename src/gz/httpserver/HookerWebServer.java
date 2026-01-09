package gz.httpserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerLogger;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.radar.AndroidUI;
import gz.util.XLog;

public abstract class HookerWebServer extends NanoHTTPD {
	
	protected HookerWebServer(int port) {
		super("0.0.0.0", port);
	}
	
	public abstract Response onResponse(HookerWebRequest request) throws Exception;

	@Override
	public Response serve(IHTTPSession session) {
		try {
			HookerWebRequest request = new HookerWebRequest(session);
			Response reponse = onResponse(request);
			return reponse;
		} catch (Exception e) {
			String exceptionHtml = XLog.getPrettyHtml(XLog.getException(e));
			return newFixedLengthResponse(
                    Response.Status.OK,
                    Produces.HTML.value(),
                    exceptionHtml
            );
		}
	}

	@Override
	public void start() throws IOException {
		super.start();
		try {
			AndroidUI.showToast("webserver启动了，牛逼不？");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(int arg0, boolean arg1) throws IOException {
		super.start(arg0, arg1);
		try {
			AndroidUI.showToast("webserver启动了，牛逼不？");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(int timeout) throws IOException {
		super.start(timeout);
	}

	@Override
	public void stop() {
		super.stop();
	}

}
