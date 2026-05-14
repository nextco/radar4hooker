package gz.agent.protocol;

import gz.agent.model.AgentMessage;
import gz.com.alibaba.fastjson.JSON;

public class AgentCodec {

	public static String encode(AgentMessage message) {
		return JSON.toJSONString(message);
	}

	public static AgentMessage decode(String text) {
		return JSON.parseObject(text, AgentMessage.class);
	}

	public static <T> T convertPayload(Object payload, Class<T> clazz) {
		if (payload == null) {
			return null;
		}
		return JSON.parseObject(JSON.toJSONString(payload), clazz);
	}
}
