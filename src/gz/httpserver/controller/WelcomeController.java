package gz.httpserver.controller;

import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;

@HookerController()
public class WelcomeController {

	@HookerRequestMapping("/welcome.html")
	public String welcome() {
		return "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n" + "  <meta charset=\"UTF-8\">\n"
				+ "  <title>Hooker HTTP Server</title>\n" + "  <style>\n"
				+ "    body { font-family: -apple-system, Arial; background:#101010; color:#eee; }\n"
				+ "    .box { max-width:900px; margin:40px auto; background:#1b1b1b; padding:30px; border-radius:12px; }\n"
				+ "    h1 { color:#4CAF50; margin-top:0; }\n" + "    .tip { color:#aaa; font-size:14px; }\n"
				+ "    .card { background:#111; padding:12px 14px; border-radius:8px; margin-top:14px; }\n"
				+ "    a { color:#4CAF50; text-decoration:none; }\n" + "  </style>\n" + "</head>\n" + "<body>\n"
				+ "<div class='box'>\n" + "  <h1>🔥 Hooker Web Server Running</h1>\n"
				+ "  <div class='tip'>Android Embedded HTTP Debug Panel</div>\n" + "\n" + "  <div class='card'>\n"
				+ "    <b>Server Status:</b> OK<br>\n" + "    <b>Time:</b> " + System.currentTimeMillis() + "<br>\n"
				+ "  </div>\n" + "\n" + "  <div class='card'>\n" + "    <b>Useful Endpoints</b><br>\n"
				+ "    <a href=\"/welcome.html\">/welcome.html</a><br>\n"
				+ "    <!-- 未来可以在这里自动列出所有 RequestMapping -->\n" + "  </div>\n" + "</div>\n" + "</body>\n" + "</html>";
	}
}
