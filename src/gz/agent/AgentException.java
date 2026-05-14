package gz.agent;

public class AgentException extends Exception {

	private final String code;

	public AgentException(String code, String message) {
		super(message);
		this.code = code;
	}

	public AgentException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
