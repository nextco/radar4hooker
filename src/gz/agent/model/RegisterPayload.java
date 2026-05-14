package gz.agent.model;

import java.util.List;
import java.util.Map;

public class RegisterPayload {
	public String token;
	public String app;
	public String packageName;
	public Integer localPort;
	public String lanIp;
	public List<String> capabilities;
	public List<Map<String, Object>> endpoints;
	public String version;
}
