package gz.httpserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import gz.httpserver.annotation.HookerLogger;

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
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			return newFixedLengthResponse(stringWriter.toString());
		}
	}

	@Override
	public void start() throws IOException {
		super.start();
	}

	@Override
	public void start(int arg0, boolean arg1) throws IOException {
		super.start(arg0, arg1);
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
