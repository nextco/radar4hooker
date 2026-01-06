package gz.httpserver.mustang;

import java.util.concurrent.locks.ReentrantLock;

import gz.httpserver.HookerHTTPRequest;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.util.XLog;

public abstract class MustangAtomController extends MustangController {
	
	private final ReentrantLock reentrantLock = new ReentrantLock(true);

    public MustangAtomController(HookerController controllerDefinition, HookerRequestMapping requestMappingDefinition) {
		super(controllerDefinition, requestMappingDefinition);
	}
    
    @Override
	public Object onResponse(HookerHTTPRequest request) throws Exception {
    	if (reentrantLock.isLocked()) {
            return "当前有任务在执行";
        }
        Exception happendEx = null;
        Object response = null;
        try{
            reentrantLock.lock();
            response = atomOnResponse(request);
        }catch (Exception e) {
            XLog.appendText(e);
            happendEx = e;;
        }finally {
            reentrantLock.unlock();
        }
        if (happendEx != null) {
            throw happendEx;
        }
        return response;
	}
    
    public abstract Object atomOnResponse(HookerHTTPRequest request) throws Exception;
    
}
