package gz.httpserver.mustang;

import gz.httpserver.HookerHTTPRequest;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;

public abstract class MustangTimerController extends MustangController {
	
	private final long timeInterval;
    private volatile long lastResponseTime;

    public MustangTimerController(HookerController controllerDefinition, HookerRequestMapping requestMappingDefinition, long timeInterval) {
		super(controllerDefinition, requestMappingDefinition);
		this.timeInterval = timeInterval;
	}

	@Override
	public Object onResponse(HookerHTTPRequest request) throws Exception {
		synchronized (this){
            if (System.currentTimeMillis() - lastResponseTime < timeInterval) {
                return "调用太频繁";
            }
            lastResponseTime = System.currentTimeMillis();
        }
        return timerOnResponse(request);
	}
	
	public abstract Object timerOnResponse(HookerHTTPRequest request) throws Exception;
    
}
