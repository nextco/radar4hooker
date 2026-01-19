package gz.httpserver.mustang;

import java.util.concurrent.locks.ReentrantLock;

import gz.httpserver.HookerWebRequest;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.util.XLog;

public abstract class MustangAtomServlet extends MustangServlet {
	
	private final ReentrantLock reentrantLock = new ReentrantLock(true);

    public MustangAtomServlet(HookerController controllerDefinition, HookerRequestMapping requestMappingDefinition) {
		super(controllerDefinition, requestMappingDefinition);
	}
    
    @Override
	public Object onResponse(HookerWebRequest request) throws Exception {
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
    
    public abstract Object atomOnResponse(HookerWebRequest request) throws Exception;
    
}
