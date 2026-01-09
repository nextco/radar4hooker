package gz.httpserver.controller;

import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.radar.AndroidUI;
import gz.util.Logger;

@HookerController("/hooker/ui/")
public class BasicUIServiceController {
	
	private Logger logger = new Logger(BasicUIServiceController.class);

	@HookerRequestMapping(path="finish_current_activity", produces = Produces.AUTO)
	public String finish_current_activity() throws Exception {
		AndroidUI.finishCurrentActivity();
		return "ok";
	}
	
	@HookerRequestMapping(path="back", produces = Produces.AUTO)
	public String back() throws Exception {
		AndroidUI.back();
		return "ok";
	}
	
	@HookerRequestMapping(path="home", produces = Produces.AUTO)
	public String home() throws Exception {
		AndroidUI.home();
		return "ok";
	}
	
	@HookerRequestMapping(path="click_by_text", produces = Produces.AUTO)
	public String click_by_text(@HookerRequestParam(name = "text") String text) throws Exception {
		AndroidUI.clickByText(text);
		return "ok";
	}
	
	@HookerRequestMapping(path="click_by_id", produces = Produces.AUTO)
	public String click_by_id(@HookerRequestParam(name = "id") int id) throws Exception {
		AndroidUI.clickById(id);
		return "ok";
	}
	
	@HookerRequestMapping(path="show_toast", produces = Produces.AUTO)
	public String show_toast(@HookerRequestParam(name = "text", defaultValue = "牛逼不？") String text) throws Exception {
		AndroidUI.showToast(text);
		return "ok";
	}
	
}
