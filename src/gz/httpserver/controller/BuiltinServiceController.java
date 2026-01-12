package gz.httpserver.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gz.httpserver.HookerWebServer;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.mustang.MustangWebServer;
import gz.util.Logger;

@HookerController("/")
public class BuiltinServiceController {
	
	private Logger logger = new Logger(BuiltinServiceController.class);

	@HookerRequestMapping(path="/", produces = Produces.HTML)
	public String welcome(HookerWebServer hookerWebServer) {
		MustangWebServer mustangWebServer = (MustangWebServer) hookerWebServer;
		return "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n" + "  <meta charset=\"UTF-8\">\n"
				+ "  <title>Hooker HTTP Server</title>\n" + "  <style>\n"
				+ "    body { font-family: -apple-system, Arial; background:#101010; color:#eee; }\n"
				+ "    .box { max-width:900px; margin:40px auto; background:#1b1b1b; padding:30px; border-radius:12px; }\n"
				+ "    h1 { color:#4CAF50; margin-top:0; }\n" + "    .tip { color:#aaa; font-size:14px; }\n"
				+ "    .card { background:#111; padding:12px 14px; border-radius:8px; margin-top:14px; }\n"
				+ "    a { color:#4CAF50; text-decoration:none; }\n" + "  </style>\n" + "</head>\n" + "<body>\n"
				+ "<div class='box'>\n" + "  <h1>🔥 Hooker Web Server Running</h1>\n"
				+ "  <div class='tip'>Android Embedded HTTP Debug Panel</div>\n" + "\n" + "  <div class='card'>\n"
				+ "    <b>Server Status:</b> OK<br>\n" + "    <b>Time:</b> " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+ "<br>\n"
				+ "  </div>\n" + "\n" + "  <div class='card'>\n" + "    <b>Useful Endpoints</b><br>\n"
				+ "    <a href=\"/welcome.html\">/welcome.html</a><br>\n"
				+ "    <!-- 未来可以在这里自动列出所有 RequestMapping -->\n" + mustangWebServer.getAPIInfo() + "  </div>\n" + "</div>\n" + "</body>\n" + "</html>";
	}
	
	
	@HookerRequestMapping(path = "/stop")
	public String stop(HookerWebServer  hookerWebServer) {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(2 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				hookerWebServer.stop();
				logger.info("stop HookerWebServer");
			};
		}.start();
		return "webserver will be stoped soon. Good-bye.";
	}
	
}
