package gz.ipc;

import java.lang.reflect.Method;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;

public final class HookerUiAutomationClient {

	private HookerUiAutomationClient() {
	}

	public static Bundle call(String action, Bundle args) throws Exception {
		throw new IllegalStateException("Use call(hostPackageName, action, args) to specify the host app package");
	}

	public static Bundle call(String hostPackageName, String action, Bundle args) throws Exception {
		IBinder binder = findService(hostPackageName);
		if (binder == null) {
			throw new IllegalStateException("Service not found: " + HookerUiAutomationContract.buildServiceName(hostPackageName));
		}
		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		try {
			data.writeInterfaceToken(HookerUiAutomationContract.DESCRIPTOR);
			data.writeString(action);
			writeBundle(data, args);
			binder.transact(HookerUiAutomationContract.TRANSACTION_CALL, data, reply, 0);
			reply.readException();
			Bundle bundle = readBundle(reply);
			if (bundle == null) {
				return new Bundle();
			}
			bundle.setClassLoader(HookerUiAutomationClient.class.getClassLoader());
			return bundle;
		} finally {
			reply.recycle();
			data.recycle();
		}
	}

	private static IBinder findService(String hostPackageName) throws Exception {
		Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
		Method getService = serviceManagerClass.getDeclaredMethod("getService", String.class);
		return (IBinder) getService.invoke(null, HookerUiAutomationContract.buildServiceName(hostPackageName));
	}

	private static Bundle readBundle(Parcel parcel) {
		if (parcel.readInt() == 0) {
			return null;
		}
		return Bundle.CREATOR.createFromParcel(parcel);
	}

	private static void writeBundle(Parcel parcel, Bundle bundle) {
		if (bundle == null) {
			parcel.writeInt(0);
			return;
		}
		parcel.writeInt(1);
		bundle.writeToParcel(parcel, 0);
	}
}
