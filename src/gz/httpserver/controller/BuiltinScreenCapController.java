package gz.httpserver.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.mustang.MustangServlet;

@HookerController("/hooker/screencap/")
public class BuiltinScreenCapController {

	@HookerRequestMapping(path = "screenshot", produces = Produces.AUTO, method = Method.GET)
	public Response screenshot(MustangServlet servlet) throws Exception {
		Process process = null;
		try {
			process = new ProcessBuilder("/system/bin/sh", "-c", "screencap -p").start();
			byte[] pngBytes = readAll(process.getInputStream());
			byte[] errorBytes = readAll(process.getErrorStream());
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				return servlet.newServerError("screencap failed, exitCode=" + exitCode + ", stderr="
						+ new String(errorBytes, "UTF-8"));
			}
			if (pngBytes == null || pngBytes.length == 0) {
				return servlet.newServerError("screencap returned empty output, stderr="
						+ new String(errorBytes, "UTF-8"));
			}
			return servlet.newFixedLengthResponse(pngBytes, "image/png");
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	private byte[] readAll(InputStream inputStream) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = inputStream.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			return baos.toByteArray();
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
			}
			baos.close();
		}
	}
}
