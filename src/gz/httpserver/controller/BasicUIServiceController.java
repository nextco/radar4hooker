package gz.httpserver.controller;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Method;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.httpserver.annotation.HookerRequestPostJson;
import gz.radar.Android;
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
	public String click_by_text(@HookerRequestParam(name = "text") String text, @HookerRequestParam(name = "text_equeal", defaultValue = "false") boolean mustBeTextEqueal, @HookerRequestParam(name = "visible", defaultValue = "false") boolean mustBeVisible) throws Exception {
		AndroidUI.clickByText(text, mustBeTextEqueal, mustBeVisible);
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
	
	@HookerRequestMapping(path="start_activity", produces = Produces.AUTO, method = Method.POST)
	public String start_activity(@HookerRequestPostJson Map<String, Object> postJson) throws Exception {
	    String className = (String) postJson.get("class_name"); // 可选，显式启动
	    Map<String, Object> extrasMap = (Map<String, Object>) postJson.get("extras"); // 可选参数
	    String type = (String) postJson.get("type"); // 可选 MIME type
	    String action = (String) postJson.get("action"); // 可选 Action，例如 "android.intent.action.VIEW"
	    String dataUri = (String) postJson.get("data_uri"); // 可选 Data URI
	    Boolean browsable = (Boolean) postJson.getOrDefault("browsable", false); // 是否加 CATEGORY_BROWSABLE
	    Context context = Android.getApplication();
	    Intent intent;
	    if (className != null && !className.isEmpty()) {
	        // 显式启动
	        intent = new Intent();
	        intent.setClassName(context, className);
	    } else {
	        // 隐式启动
	        intent = new Intent();
	    }
	    // 设置 Action
	    if (action != null) intent.setAction(action);
	    // 设置 Data
	    if (dataUri != null) intent.setData(Uri.parse(dataUri));
	    // 设置 MIME type
	    if (type != null) intent.setType(type);
	    // 设置 extras
	    if (extrasMap != null) {
	        Bundle bundle = new Bundle();
	        for (Map.Entry<String, Object> entry : extrasMap.entrySet()) {
	            Object v = entry.getValue();
	            String k = entry.getKey();
	            if (v instanceof String) bundle.putString(k, (String)v);
	            else if (v instanceof Integer) bundle.putInt(k, (Integer)v);
	            else if (v instanceof Boolean) bundle.putBoolean(k, (Boolean)v);
	            else if (v instanceof Long) bundle.putLong(k, (Long)v);
	            else if (v instanceof Float) bundle.putFloat(k, (Float)v);
	            else if (v instanceof Double) bundle.putDouble(k, (Double)v);
	            // 还可以继续支持 Parcelable / Serializable
	        }
	        intent.putExtras(bundle);
	    }
	    // 设置浏览器调用标记
	    if (browsable) intent.addCategory(Intent.CATEGORY_BROWSABLE);
	    // FLAG: 如果不是 Activity Context，需要加 NEW_TASK
	    if (!(context instanceof Activity)) {
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    }
	    context.startActivity(intent);
	    return "ok";
	}

	
}
