package gz.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

import android.content.Context;
import gz.util.Logger;

public class DeviceIdentity {

	private static final Logger logger = new Logger(DeviceIdentity.class);
	private static final String FILE_NAME = "hrdid.txt";
	private static final String LEGACY_FILE_NAME = "hooker_remote_device_id.txt";

	public static String resolve(Context context, String explicitDeviceId) {
		if (explicitDeviceId != null && explicitDeviceId.trim().length() > 0) {
			return explicitDeviceId.trim();
		}
		String persisted = readPersisted(context);
		if (persisted != null && persisted.length() > 0) {
			return persisted;
		}
		String generated = "device-" + UUID.randomUUID().toString();
		persist(context, generated);
		return generated;
	}

	private static String readPersisted(Context context) {
		if (context == null) {
			return "";
		}
		File file = new File(context.getFilesDir(), FILE_NAME);
		if (!file.exists()) {
			File legacyFile = new File(context.getFilesDir(), LEGACY_FILE_NAME);
			if (legacyFile.exists()) {
				String legacyValue = readFile(legacyFile);
				if (legacyValue != null && legacyValue.length() > 0) {
					persist(context, legacyValue);
					return legacyValue;
				}
			}
		}
		return readFile(file);
	}

	private static String readFile(File file) {
		if (file == null || !file.exists()) {
			return "";
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			int read = fis.read(data);
			if (read <= 0) {
				return "";
			}
			return new String(data, 0, read, "UTF-8").trim();
		} catch (Exception e) {
			logger.warn(e);
			return "";
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					logger.warn(e);
				}
			}
		}
	}

	private static void persist(Context context, String deviceId) {
		if (context == null || deviceId == null || deviceId.length() == 0) {
			return;
		}
		FileOutputStream fos = null;
		try {
			File file = new File(context.getFilesDir(), FILE_NAME);
			fos = new FileOutputStream(file);
			fos.write(deviceId.getBytes("UTF-8"));
			fos.flush();
		} catch (Exception e) {
			logger.warn(e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					logger.warn(e);
				}
			}
		}
	}
}
