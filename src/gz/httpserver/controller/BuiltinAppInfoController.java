package gz.httpserver.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import gz.agent.DeviceAgent;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.radar.Android;
import gz.util.Logger;

@HookerController("/hooker/appinfo")
public class BuiltinAppInfoController {

	private static final long APP_DIR_MD5_MAX_SIZE = 20L * 1024L * 1024L;
	
	private Logger logger = new Logger(BuiltinAppInfoController.class);
	
	/**
	 * 获取当前 App 的基础信息、组件、权限、签名和常见目录。
	 * 返回包名、版本、Activity/Service/Receiver/Provider 列表，以及 shared_prefs 和 databases 目录位置。
	 */
	@HookerRequestMapping(path="", produces = Produces.AUTO)
	public Map<String, Object> app_info() throws Exception {
	    Map<String, Object> info = new HashMap<>();
	    Application context = Android.getApplication();
	    PackageManager pm = context.getPackageManager();
	    String packageName = context.getPackageName();

	    PackageInfo pi = pm.getPackageInfo(
	            packageName,
	            PackageManager.GET_PERMISSIONS
	                    | PackageManager.GET_SERVICES
	                    | PackageManager.GET_RECEIVERS
	                    | PackageManager.GET_PROVIDERS
	                    | PackageManager.GET_ACTIVITIES
	                    | PackageManager.GET_SIGNATURES
	    );

	    info.put("packageName", packageName);
	    info.put("appName", pm.getApplicationLabel(pi.applicationInfo).toString());
	    info.put("versionName", pi.versionName);
	    info.put("versionCode", pi.versionCode);
	    info.put("firstInstallTime", pi.firstInstallTime);
	    info.put("lastUpdateTime", pi.lastUpdateTime);
	    info.put("sourceDir", pi.applicationInfo.sourceDir);
	    info.put("dataDir", pi.applicationInfo.dataDir);
	    info.put("minSdk", pi.applicationInfo.minSdkVersion);
	    info.put("targetSdk", pi.applicationInfo.targetSdkVersion);
	    info.put("uid", pi.applicationInfo.uid);

	    /* ===== Manifest 组件信息 ===== */

	    // Activities
	    List<String> activities = new ArrayList<>();
	    if (pi.activities != null) {
	        for (ActivityInfo a : pi.activities) {
	            activities.add(a.name);
	        }
	    }
	    info.put("activities", activities);

	    // Services
	    List<String> services = new ArrayList<>();
	    if (pi.services != null) {
	        for (ServiceInfo s : pi.services) {
	            services.add(s.name);
	        }
	    }
	    info.put("services", services);

	    // Receivers
	    List<String> receivers = new ArrayList<>();
	    if (pi.receivers != null) {
	        for (ActivityInfo r : pi.receivers) {
	            receivers.add(r.name);
	        }
	    }
	    info.put("receivers", receivers);

	    // Content Providers
	    List<String> providers = new ArrayList<>();
	    if (pi.providers != null) {
	        for (ProviderInfo p : pi.providers) {
	            providers.add(p.authority + " -> " + p.name);
	        }
	    }
	    info.put("providers", providers);

	    /* ===== Permissions ===== */
	    if (pi.requestedPermissions != null) {
	        List<String> permList = new ArrayList<>();
	        for (String p : pi.requestedPermissions) {
	            int granted = pm.checkPermission(p, packageName);
	            permList.add(p + ":" + (granted == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
	        }
	        info.put("permissions", permList);
	    }

	    /* ===== 签名 ===== */
	    try {
	    	// 9.0 (Pie) 28
	    	info.put("signature", Android.getSignatureInfo());
	    } catch (Exception e) {
	        logger.warn(e);
	        // 失败时可降级处理，例如 pi.signatures（Android 9 以下）
	    }

	    /* ===== 常见目录 ===== */
	    info.put("databasesDir", new File(context.getApplicationInfo().dataDir, "databases").getAbsolutePath());
	    info.put("sharedPrefsDir", new File(context.getApplicationInfo().dataDir, "shared_prefs").getAbsolutePath());
	    info.put("filesDir", context.getFilesDir().getAbsolutePath());
	    info.put("cacheDir", context.getCacheDir().getAbsolutePath());

	    return info;
	}

	/**
	 * 读取当前 App 的 shared_prefs 内容。
	 * 返回每个 shared preferences 文件名及其完整键值对。
	 */
	@HookerRequestMapping(path="shared_prefs", produces = Produces.AUTO)
	public List<Map<String, ?>> shared_prefs() throws Exception {
		List<Map<String, ?>> all = new ArrayList<Map<String,?>>();
		Application context = Android.getApplication();
		File dir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
		File[] files = dir.listFiles();

		if (files != null) {
		    for (File f : files) {
		        String name = f.getName().replace(".xml", "");
		        SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		        Map<String, ?> info = sp.getAll();
		        Map<String, Object> profileInfo = new HashMap<String, Object>();
		        profileInfo.put("shared_preferences_name", name);
		        profileInfo.put("shared_preferences", info);
		        all.add(profileInfo);
		    }
		}
		return all;
	}
	
	/**
	 * 列出当前 App 的数据库、表和字段结构。
	 * 返回每个 .db 文件下的表名及 PRAGMA table_info 查询得到的列定义。
	 */
	@HookerRequestMapping(path="databases", produces = Produces.AUTO)
	public List<Map<String, Object>> databases() throws Exception {
	    List<Map<String, Object>> all = new ArrayList<>();
	    Application context = Android.getApplication();
	    File dbDir = new File(context.getApplicationInfo().dataDir, "databases");
	    File[] dbFiles = dbDir.listFiles();

	    if (dbFiles != null) {
	        for (File db : dbFiles) {
	            if (db.getName().endsWith(".db")) {
	                Map<String, Object> dbMap = new HashMap<>();
	                dbMap.put("database", db.getName());

	                List<Map<String, Object>> tableList = new ArrayList<>();
	                SQLiteDatabase sqlDb = SQLiteDatabase.openDatabase(
	                        db.getAbsolutePath(),
	                        null,
	                        SQLiteDatabase.OPEN_READONLY
	                );
	                // 获取表名
	                Cursor c = sqlDb.rawQuery(
	                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%';",
	                        null
	                );
	                List<String> tables = new ArrayList<>();
	                while (c.moveToNext()) {
	                    tables.add(c.getString(0));
	                }
	                c.close();
	                // 遍历每个表获取字段信息
	                for (String table : tables) {
	                    Map<String, Object> tableMap = new HashMap<>();
	                    tableMap.put("table", table);

	                    List<Map<String, Object>> columnsList = new ArrayList<>();
	                    Cursor info = sqlDb.rawQuery("PRAGMA table_info(" + table + ")", null);
	                    while (info.moveToNext()) {
	                        Map<String, Object> columnMap = new HashMap<>();
	                        columnMap.put("name", info.getString(info.getColumnIndexOrThrow("name")));
	                        columnMap.put("type", info.getString(info.getColumnIndexOrThrow("type")));
	                        columnMap.put("notNull", info.getInt(info.getColumnIndexOrThrow("notnull")));
	                        columnMap.put("defaultValue", info.getString(info.getColumnIndexOrThrow("dflt_value")));
	                        columnMap.put("primaryKey", info.getInt(info.getColumnIndexOrThrow("pk")));
	                        columnsList.add(columnMap);
	                    }
	                    info.close();
	                    tableMap.put("columns", columnsList);
	                    tableList.add(tableMap);
	                }
	                sqlDb.close();
	                dbMap.put("tables", tableList);
	                all.add(dbMap);
	            }
	        }
	    }
	    return all;
	}
	
	/**
	 * 读取指定数据库表的数据。
	 * 适合排查本地缓存和业务状态；limit 默认 100，最大限制为 1000。
	 */
	@HookerRequestMapping(path="read_table", produces = Produces.AUTO)
	public List<Map<String, Object>> read_table(
	        @HookerRequestParam(name = "database") String database,
	        @HookerRequestParam(name = "table") String table,
	        @HookerRequestParam(name = "limit", defaultValue = "100") int limit
	) throws Exception {
		if (limit > 1000) {
			limit = 1000;
		}else if (limit <= 0) {
			limit = 100;
		}
	    List<Map<String, Object>> all = new ArrayList<>();
	    Application context = Android.getApplication();
	    File dbDir = new File(context.getApplicationInfo().dataDir, "databases");
	    File dbFile = new File(dbDir, database);
	    if (!dbFile.exists()) {
	        throw new IllegalArgumentException("Database not found: " + database);
	    }
	    // 打开数据库（只读）
	    SQLiteDatabase sqlDb = SQLiteDatabase.openDatabase(
	            dbFile.getAbsolutePath(),
	            null,
	            SQLiteDatabase.OPEN_READONLY
	    );
	    // 查询指定表，限制行数
	    Cursor cursor = sqlDb.rawQuery("SELECT * FROM " + table + " LIMIT " + limit, null);
	    if (cursor != null) {
	        int columnCount = cursor.getColumnCount();
	        while (cursor.moveToNext()) {
	            Map<String, Object> row = new HashMap<>();
	            for (int i = 0; i < columnCount; i++) {
	                String colName = cursor.getColumnName(i);
	                int type = cursor.getType(i);
	                Object value;

	                switch (type) {
	                    case Cursor.FIELD_TYPE_INTEGER:
	                        value = cursor.getLong(i);
	                        break;
	                    case Cursor.FIELD_TYPE_FLOAT:
	                        value = cursor.getDouble(i);
	                        break;
	                    case Cursor.FIELD_TYPE_STRING:
	                        value = cursor.getString(i);
	                        break;
	                    case Cursor.FIELD_TYPE_BLOB:
	                        value = cursor.getBlob(i);
	                        break;
	                    case Cursor.FIELD_TYPE_NULL:
	                    default:
	                        value = null;
	                        break;
	                }
	                row.put(colName, value);
	            }
	            all.add(row);
	        }
	        cursor.close();
	    }
	    sqlDb.close();
	    return all;
	}

	@HookerRequestMapping(path="dex_md5s", produces = Produces.AUTO)
	public Map<String, Object> dex_md5s() throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		Application context = Android.getApplication();
		File rootDir = new File(context.getApplicationInfo().dataDir);
		List<Map<String, Object>> files = new ArrayList<Map<String,Object>>();
		collectFiles(rootDir, rootDir, files, true);
		Collections.sort(files, new java.util.Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> left, Map<String, Object> right) {
				String l = String.valueOf(left.get("relativePath"));
				String r = String.valueOf(right.get("relativePath"));
				return l.compareTo(r);
			}
		});
		result.put("rootDir", rootDir.getAbsolutePath());
		result.put("count", Integer.valueOf(files.size()));
		result.put("files", files);
		return result;
	}

	@HookerRequestMapping(path="app_dir_files", produces = Produces.AUTO)
	public Map<String, Object> app_dir_files() throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		Application context = Android.getApplication();
		File rootDir = new File(context.getApplicationInfo().dataDir);
		List<Map<String, Object>> files = new ArrayList<Map<String,Object>>();
		collectTopLevelFiles(rootDir, files);
		Collections.sort(files, new java.util.Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> left, Map<String, Object> right) {
				String l = String.valueOf(left.get("relativePath"));
				String r = String.valueOf(right.get("relativePath"));
				return l.compareTo(r);
			}
		});
		result.put("rootDir", rootDir.getAbsolutePath());
		result.put("count", Integer.valueOf(files.size()));
		result.put("files", files);
		return result;
	}

	@HookerRequestMapping(path="download_cloud_file", produces = Produces.AUTO, method = gz.httpserver.annotation.HookerRequestMapping.Method.POST)
	public Map<String, Object> download_cloud_file(
			@HookerRequestParam(name = "downloadUrl", defaultValue = "") String downloadUrl,
			@HookerRequestParam(name = "downloadPath", defaultValue = "") String downloadPath,
			@HookerRequestParam(name = "fileName", defaultValue = "") String fileName
	) throws Exception {
		Application context = Android.getApplication();
		File rootDir = new File(context.getApplicationInfo().dataDir);
		String resolvedDownloadUrl = resolveDownloadUrl(downloadUrl, downloadPath);
		if (resolvedDownloadUrl.length() == 0) {
			throw new IllegalArgumentException("downloadUrl or downloadPath is required");
		}
		String resolvedName = sanitizeFileName(fileName);
		if (resolvedName.length() == 0) {
			resolvedName = sanitizeFileName(extractFileName(resolvedDownloadUrl));
		}
		if (resolvedName.length() == 0) {
			throw new IllegalArgumentException("fileName is required");
		}
		File target = new File(rootDir, resolvedName);
		boolean overwritten = target.exists();
		downloadToFile(resolvedDownloadUrl, target);
		Map<String, Object> item = buildFileInfo(rootDir, target);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("ok", Boolean.TRUE);
		result.put("rootDir", rootDir.getAbsolutePath());
		result.put("downloadUrl", resolvedDownloadUrl);
		result.put("overwritten", Boolean.valueOf(overwritten));
		result.put("file", item);
		return result;
	}

	@HookerRequestMapping(path="delete_app_dir_file", produces = Produces.AUTO, method = gz.httpserver.annotation.HookerRequestMapping.Method.POST)
	public Map<String, Object> delete_app_dir_file(
			@HookerRequestParam(name = "fileName", defaultValue = "") String fileName
	) throws Exception {
		Application context = Android.getApplication();
		File rootDir = new File(context.getApplicationInfo().dataDir);
		String resolvedName = sanitizeFileName(fileName);
		if (resolvedName.length() == 0) {
			throw new IllegalArgumentException("fileName is required");
		}
		File target = new File(rootDir, resolvedName);
		String rootPath = rootDir.getCanonicalPath() + File.separator;
		String targetPath = target.getCanonicalPath();
		if (!targetPath.startsWith(rootPath)) {
			throw new IllegalArgumentException("fileName out of app dir");
		}
		if (!target.exists() || !target.isFile()) {
			throw new IllegalArgumentException("file not found: " + resolvedName);
		}
		if (!target.delete()) {
			throw new IllegalStateException("delete failed: " + resolvedName);
		}
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("ok", Boolean.TRUE);
		result.put("rootDir", rootDir.getAbsolutePath());
		result.put("fileName", resolvedName);
		result.put("deleted", Boolean.TRUE);
		return result;
	}

	private void collectFiles(File rootDir, File current, List<Map<String, Object>> files, boolean dexOnly) {
		if (current == null || !current.exists()) {
			return;
		}
		if (current.isFile()) {
			if (!dexOnly || current.getName().toLowerCase().endsWith(".dex")) {
				files.add(buildFileInfo(rootDir, current));
			}
			return;
		}
		File[] children = current.listFiles();
		if (children == null) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			collectFiles(rootDir, children[i], files, dexOnly);
		}
	}

	private void collectTopLevelFiles(File rootDir, List<Map<String, Object>> files) {
		if (rootDir == null || !rootDir.exists()) {
			return;
		}
		File[] children = rootDir.listFiles();
		if (children == null) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			if (child != null && child.isFile()) {
				files.add(buildFileInfo(rootDir, child, true));
			}
		}
	}

	private Map<String, Object> buildFileInfo(File rootDir, File current) {
		return buildFileInfo(rootDir, current, false);
	}

	private Map<String, Object> buildFileInfo(File rootDir, File current, boolean md5ForSmallFilesOnly) {
		Map<String, Object> item = new HashMap<String, Object>();
		item.put("name", current.getName());
		item.put("absolutePath", current.getAbsolutePath());
		item.put("relativePath", buildRelativePath(rootDir, current));
		item.put("size", Long.valueOf(current.length()));
		item.put("lastModified", Long.valueOf(current.lastModified()));
		if (!md5ForSmallFilesOnly || current.length() <= APP_DIR_MD5_MAX_SIZE) {
			item.put("md5", computeMd5(current));
		} else {
			item.put("md5", "");
		}
		return item;
	}

	private String buildRelativePath(File rootDir, File file) {
		String root = rootDir.getAbsolutePath();
		String absolute = file.getAbsolutePath();
		if (absolute.startsWith(root)) {
			return absolute.substring(root.length());
		}
		return absolute;
	}

	private String computeMd5(File file) {
		FileInputStream fis = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			fis = new FileInputStream(file);
			byte[] buf = new byte[8192];
			int read;
			while ((read = fis.read(buf)) > 0) {
				digest.update(buf, 0, read);
			}
			byte[] md5 = digest.digest();
			StringBuilder sb = new StringBuilder(md5.length * 2);
			for (int i = 0; i < md5.length; i++) {
				sb.append(String.format("%02x", Integer.valueOf(md5[i] & 0xff)));
			}
			return sb.toString();
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

	private String sanitizeFileName(String fileName) {
		if (fileName == null) {
			return "";
		}
		String value = fileName.trim();
		if (value.length() == 0) {
			return "";
		}
		value = new File(value).getName();
		if (".".equals(value) || "/".equals(value) || "..".equals(value)) {
			return "";
		}
		return value.replace("\u0000", "");
	}

	private String extractFileName(String downloadUrl) {
		if (downloadUrl == null) {
			return "";
		}
		try {
			URL url = new URL(downloadUrl);
			String path = url.getPath();
			if (path == null || path.length() == 0) {
				return "";
			}
			return new File(path).getName();
		} catch (Exception e) {
			logger.warn(e);
			return "";
		}
	}

	private String resolveDownloadUrl(String downloadUrl, String downloadPath) {
		String url = downloadUrl == null ? "" : downloadUrl.trim();
		String path = downloadPath == null ? "" : downloadPath.trim();
		if (path.length() > 0) {
			String baseUrl = DeviceAgent.getInstance().getHubHttpBaseUrl();
			if (baseUrl != null && baseUrl.length() > 0) {
				if (!path.startsWith("/")) {
					path = "/" + path;
				}
				return baseUrl + path;
			}
		}
		return url;
	}

	private void downloadToFile(String downloadUrl, File target) throws Exception {
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		File tmpFile = new File(target.getAbsolutePath() + ".download");
		try {
			connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(60000);
			connection.setUseCaches(false);
			connection.connect();
			int code = connection.getResponseCode();
			if (code < 200 || code >= 300) {
				throw new IllegalStateException("download failed, http status: " + code);
			}
			inputStream = connection.getInputStream();
			outputStream = new FileOutputStream(tmpFile, false);
			byte[] buffer = new byte[8192];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, read);
			}
			outputStream.flush();
			if (target.exists() && !target.delete()) {
				throw new IllegalStateException("failed to overwrite target: " + target.getAbsolutePath());
			}
			if (!tmpFile.renameTo(target)) {
				throw new IllegalStateException("failed to move file to target: " + target.getAbsolutePath());
			}
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (Exception e) {
					logger.warn(e);
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
					logger.warn(e);
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
			if (tmpFile.exists() && !target.exists()) {
				tmpFile.delete();
			}
		}
	}
	
}
