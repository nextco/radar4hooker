package gz.httpserver.controller;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import gz.httpserver.annotation.HookerController;
import gz.httpserver.annotation.HookerRequestMapping;
import gz.httpserver.annotation.HookerRequestMapping.Produces;
import gz.httpserver.annotation.HookerRequestParam;
import gz.radar.Android;
import gz.util.Logger;

@HookerController("/hooker/appinfo/")
public class BasicAppInfoController {
	
	private Logger logger = new Logger(BasicAppInfoController.class);
	
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
	
}
