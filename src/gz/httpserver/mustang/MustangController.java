package gz.httpserver.mustang;

import java.lang.reflect.Method;

import gz.httpserver.HookerWebRequest;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.util.Logger;

public abstract class MustangController {
	
	private static Logger logger = new Logger(MustangController.class);
	
	private final HookerController controllerDefinition;
	
    private final HookerRequestMapping  requestMappingDefinition;
    
    public MustangController(HookerController controllerDefinition, HookerRequestMapping  requestMappingDefinition) {
        this.controllerDefinition = controllerDefinition;
    	this.requestMappingDefinition = requestMappingDefinition;
    }
    
    public Produces getProduces() {
    	return requestMappingDefinition.produces();
    }
    
    
    public HookerController getControllerDefinition() {
		return controllerDefinition;
	}

	public HookerRequestMapping getRequestMappingDefinition() {
		return requestMappingDefinition;
	}
	
	public abstract Object getTarget();
	
	public abstract Method getTargetMethod();

	public abstract Object onResponse(HookerWebRequest request) throws Exception;

}
