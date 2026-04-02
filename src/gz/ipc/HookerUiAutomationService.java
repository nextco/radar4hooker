package gz.ipc;

import java.lang.reflect.Method;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import gz.util.Logger;

public final class HookerUiAutomationService extends Binder {

	private static final Logger logger = new Logger(HookerUiAutomationService.class);
	private static volatile HookerUiAutomationService instance;

	private final HookerUiAutomationDispatcher dispatcher = new HookerUiAutomationDispatcher();

	private HookerUiAutomationService() {
		attachInterface(null, HookerUiAutomationContract.DESCRIPTOR);
	}

	public static synchronized void ensureStarted() {
		try {
			if (findRegisteredService() != null) {
				return;
			}
			if (instance == null) {
				instance = new HookerUiAutomationService();
			}
			registerService(instance);
			logger.info("binder service registered: " + HookerUiAutomationContract.SERVICE_NAME);
		} catch (Throwable throwable) {
			logger.warn(throwable);
		}
	}

	@Override
	protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
		if (code == INTERFACE_TRANSACTION) {
			reply.writeString(HookerUiAutomationContract.DESCRIPTOR);
			return true;
		}
		if (code != HookerUiAutomationContract.TRANSACTION_CALL) {
			return superOnTransact(code, data, reply, flags);
		}
		data.enforceInterface(HookerUiAutomationContract.DESCRIPTOR);
		String action = data.readString();
		Bundle args = readBundle(data);
		Bundle result = dispatcher.dispatch(action, args);
		reply.writeNoException();
		writeBundle(reply, result);
		return true;
	}

	private boolean superOnTransact(int code, Parcel data, Parcel reply, int flags) {
		try {
			return super.onTransact(code, data, reply, flags);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	private static Bundle readBundle(Parcel parcel) {
		if (parcel.readInt() == 0) {
			return null;
		}
		Bundle bundle = Bundle.CREATOR.createFromParcel(parcel);
		bundle.setClassLoader(HookerUiAutomationService.class.getClassLoader());
		return bundle;
	}

	private static void writeBundle(Parcel parcel, Bundle bundle) {
		if (bundle == null) {
			parcel.writeInt(0);
			return;
		}
		parcel.writeInt(1);
		bundle.writeToParcel(parcel, 0);
	}

	private static IBinder findRegisteredService() throws Exception {
		Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
		Method getService = serviceManagerClass.getDeclaredMethod("getService", String.class);
		return (IBinder) getService.invoke(null, HookerUiAutomationContract.SERVICE_NAME);
	}

	private static void registerService(IBinder binder) throws Exception {
		Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
		Method addService = serviceManagerClass.getDeclaredMethod("addService", String.class, IBinder.class);
		addService.setAccessible(true);
		addService.invoke(null, HookerUiAutomationContract.SERVICE_NAME, binder);
	}
}
