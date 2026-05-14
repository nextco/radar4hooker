package gz.httpserver.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import gz.agent.DeviceAgent;
import gz.httpserver.HookerWebServerBoot;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;

@HookerController("/hooker/agent/")
public class BuiltinAgentController {

	@HookerRequestMapping(path = "start", produces = Produces.AUTO)
	public Map<String, Object> start(
			@HookerRequestParam(name = "local_port", required = false, defaultValue = "0") int localPort) throws Exception {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		String message = localPort > 0
				? HookerWebServerBoot.startRemoteAgent(localPort)
				: HookerWebServerBoot.startRemoteAgent();
		result.put("ok", Boolean.TRUE);
		result.put("message", message);
		result.put("status", DeviceAgent.getInstance().getStatus());
		return result;
	}

	@HookerRequestMapping(path = "stop", produces = Produces.AUTO)
	public Map<String, Object> stop() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		HookerWebServerBoot.stopRemoteAgent();
		result.put("ok", Boolean.TRUE);
		result.put("message", "Remote agent stopped");
		result.put("status", DeviceAgent.getInstance().getStatus());
		return result;
	}

	@HookerRequestMapping(path = "status", produces = Produces.AUTO)
	public Map<String, Object> status() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("ok", Boolean.TRUE);
		result.put("status", DeviceAgent.getInstance().getStatus());
		return result;
	}
}
