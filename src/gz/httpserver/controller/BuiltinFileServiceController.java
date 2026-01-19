package gz.httpserver.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.content.Context;
import gz.httpserver.NanoHTTPD.Response;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.mustang.MustangServlet;
import gz.radar.Android;
import gz.util.Logger;

@HookerController()
public class BuiltinFileServiceController {
	
	private static Logger logger = new Logger(BuiltinFileServiceController.class);

	public static File tempFileDir = new File("/sdcard/webserver_file_cache/");
	
	static {
		try {
			if (Android.hasSdcardWritePermit()) {
				tempFileDir.mkdir();
			}else {
				try {
					Context ctx = Android.getApplication();
					File dataDir = ctx.getDataDir(); ///data/user/0/com.xxx.xxx
					tempFileDir = new File(dataDir.getAbsolutePath()+ "/webserver_file_cache/");
				} catch (Exception e1) {
				}
			}
		} catch (Exception e) {
			logger.warn(e);
		}finally {
			tempFileDir.mkdir();
		}
	}
	
	@HookerRequestMapping(path = "/file")
	public Response getFile(@HookerRequestParam(name = "filename") String filename, MustangServlet servlet) throws FileNotFoundException {
		File file = new File(filename);
		if (file.isAbsolute()) {
			return servlet.newFileResponse(file);
		}else {
			return servlet.newFileResponse(new File(tempFileDir.getAbsolutePath() + "/" + filename));
		}
	}
	
}
