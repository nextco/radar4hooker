package gz.httpserver.mustang;

import gz.httpserver.HookerWebRequest;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;

public abstract class MustangTimerServlet extends MustangServlet {
	
	private final long timeInterval;
    private volatile long lastResponseTime;

    public MustangTimerServlet(HookerController controllerDefinition, HookerRequestMapping requestMappingDefinition, long timeInterval) {
		super(controllerDefinition, requestMappingDefinition);
		this.timeInterval = timeInterval;
	}

	@Override
	public Object onResponse(HookerWebRequest request) throws Exception {
		synchronized (this){
            if (System.currentTimeMillis() - lastResponseTime < timeInterval) {
                return "调用太频繁";
            }
            lastResponseTime = System.currentTimeMillis();
        }
        return timerOnResponse(request);
	}
	
	public abstract Object timerOnResponse(HookerWebRequest request) throws Exception;
    
}
