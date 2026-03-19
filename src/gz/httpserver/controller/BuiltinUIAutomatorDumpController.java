package gz.httpserver.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import gz.com.alibaba.fastjson.JSON;
import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.mustang.MustangServlet;
import gz.radar.WindowHierarchyDumper;

@HookerController("/hooker/uiauto/")
public class BuiltinUIAutomatorDumpController {

	@HookerRequestMapping(path = "dump", produces = Produces.AUTO, method = Method.GET)
	public Response dump(@HookerRequestParam(name = "format", defaultValue = "xml") String format,
			MustangServlet servlet) throws Exception {
		WindowHierarchyDumper.DumpResult dumpResult = WindowHierarchyDumper.dumpCurrentWindow();
		if ("json".equalsIgnoreCase(format)) {
			return servlet.newJsonResponse(JSON.toJSONString(dumpResult.toJsonObject(), true));
		}
		return servlet.newFixedLengthResponse("application/xml; charset=utf-8", dumpResult.toXml());
	}

	@HookerRequestMapping(path = "window_dump.xml", produces = Produces.AUTO, method = Method.GET)
	public Response windowDumpXml(MustangServlet servlet) throws Exception {
		WindowHierarchyDumper.DumpResult dumpResult = WindowHierarchyDumper.dumpCurrentWindow();
		File file = writeTempFile("window_dump.xml", dumpResult.toXml().getBytes("UTF-8"));
		return servlet.newFileResponse(file);
	}

	@HookerRequestMapping(path = "window_dump.json", produces = Produces.AUTO, method = Method.GET)
	public Response windowDumpJson(MustangServlet servlet) throws Exception {
		WindowHierarchyDumper.DumpResult dumpResult = WindowHierarchyDumper.dumpCurrentWindow();
		String json = JSON.toJSONString(dumpResult.toJsonObject(), true);
		File file = writeTempFile("window_dump.json", json.getBytes("UTF-8"));
		return servlet.newFileResponse(file);
	}

	@HookerRequestMapping(path = "summary", produces = Produces.AUTO, method = Method.GET)
	public Map<String, Object> summary() throws Exception {
		return WindowHierarchyDumper.dumpCurrentWindow().toJsonObject();
	}

	private File writeTempFile(String fileName, byte[] content) throws Exception {
		File file = new File(BuiltinFileServiceController.tempFileDir, fileName);
		FileOutputStream fos = new FileOutputStream(file);
		try {
			fos.write(content);
			fos.flush();
		} finally {
			fos.close();
		}
		return file;
	}
}
