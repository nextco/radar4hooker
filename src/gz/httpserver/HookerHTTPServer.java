package gz.httpserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import gz.httpserver.annotation.HookerLogger;

public abstract class HookerHTTPServer extends NanoHTTPD {

	public HookerHTTPServer(int port) {
		super("0.0.0.0", port);
	}
	
	public abstract Response onResponse(HookerHTTPRequest request) throws Exception;

	@Override
	public Response serve(IHTTPSession session) {
		try {
			HookerHTTPRequest request = new HookerHTTPRequest(session);
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
		// TODO Auto-generated method stub
		super.start();
	}

	@Override
	public void start(int arg0, boolean arg1) throws IOException {
		// TODO Auto-generated method stub
		super.start(arg0, arg1);
	}

	@Override
	public void start(int timeout) throws IOException {
		// TODO Auto-generated method stub
		super.start(timeout);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
	}

}
