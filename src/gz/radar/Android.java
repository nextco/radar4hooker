package gz.radar;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.system.Os;
import android.view.View;
import gz.radar.objects.ActivityInfo;
import gz.radar.objects.ExplainObjects;
import gz.radar.objects.ObjectInfo;
import gz.radar.objects.ObjectsStore;
import gz.radar.objects.ServiceInfo;
import gz.radar.objects.ViewInfo;
import gz.util.X;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import gz.com.alibaba.fastjson.JSON;
import gz.com.alibaba.fastjson.JSONArray;
import gz.com.alibaba.fastjson.JSONObject;

public class Android {
	
	/**
	 * sdcard writie permit
	 * @return
	 * @throws Exception
	 */
	public static boolean hasSdcardWritePermit() throws Exception {
    	boolean hasPermission;
		Context ctx = Android.getApplication();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		    hasPermission =
		            ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
		                    == PackageManager.PERMISSION_GRANTED;
		} else {
		    hasPermission = true; // 6.0 以下安装即授予
		}
		return hasPermission;
    }
	
	public static String getBundleProfile(Bundle bundle) throws Exception {
		if (bundle != null) {
			Object mMap = X.getField(bundle, "mMap");
			return JSON.toJSONString(mMap);
		}
		return "";
	}
	
	public static String getIntentProfile(Intent intent) throws Exception {
		JSONObject profile = new JSONObject();
		profile.put("action", intent.getAction());
		if (X.hasField(intent, "mData")) {
			Object mData = X.getField(intent, "mData");
			if (mData != null) {
				profile.put("data", mData.toString());
			}
		}
		profile.put("type", intent.getType());
		if (X.hasMehtod(intent, "getIdentifier")) {
			profile.put("identifier", X.invokeObject(intent, "getIdentifier"));
		}
		profile.put("package", intent.getPackage());
		profile.put("flags", intent.getFlags());
		profile.put("categories", intent.getCategories());
		profile.put("type", intent.getType());
		ComponentName componentName = intent.getComponent();
		if (componentName != null) {
			profile.put("component", componentName.getClassName());
		}
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			Object mMap = X.getField(bundle, "mMap");
			profile.put("bundle", mMap);
		}
		return profile.toJSONString();
	}

	public static void checkSelfPermission(String permission) throws Exception {
		Activity context = Android.getTopActivity();
		if (context.checkPermission(permission, android.os.Process.myPid(), Os.getuid()) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(context, new String[] {permission}, 19021);
		}
	}
	
	public static void requestPermissions(final Activity activity,
            final String[] permissions, final int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            activity.requestPermissions(permissions, requestCode);
        }
    }
	

    public static <T extends Activity> T getTopActivity() throws Exception {
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
        activitiesField.setAccessible(true);
        Map activities = (Map) activitiesField.get(activityThread);
        ActivityRecord topRecord = findTopActivityRecord(activities);
        if (topRecord != null) {
            return (T) topRecord.activity;
        }
        return null;
    }

    public static <T extends Application> T getApplication() throws Exception {
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
        Method currentActivityThread = activityThread.getDeclaredMethod("currentActivityThread");
        Object current = currentActivityThread.invoke((Object)null);
        Object app = currentApplication.invoke(current);
        return (T) app;
    }

    public static String getVersionName() throws Exception {
        Application context = getApplication();
        //获取包管理器
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //返回版本号
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    public static String getMainActivity() throws Exception {
        Application context = getApplication();
        //获取包管理器
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
            return intent.getComponent().getClassName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    public static ObjectInfo getObjectInfo(String objId) throws Exception {
        Object obj = ObjectsStore.getObject(objId);
        if (obj == null) {
            return null;
        }
        return new ObjectInfo(obj);
    }
    
    public static ExplainObjects object2Explain(String objId) throws Exception {
    	ExplainObjects explainObjs = new ExplainObjects();
        Object obj = ObjectsStore.getObject(objId);
        if (obj == null || obj instanceof ObjectInfo) {
            return explainObjs;
        }
        if (obj instanceof Collection) {
        	Collection<Object> collection = (Collection<Object>) obj;
        	int index = 0;
        	for (Object item : collection) {
        		explainObjs.put(String.valueOf(index), item);
        		index ++;
        	}
        }else if (obj instanceof Map) {
        	Map map = (Map) obj;
        	Set keys = map.keySet();
        	for (Object key : keys) {
        		Object item = map.get(key);
        		if (item != null) {
        			explainObjs.put(key.toString(), item);
        		}
        	}
        }else if (obj instanceof Object[]) {
        	Object[] arr = (Object[]) obj;
        	for (int i = 0; i < arr.length; i++) {
        		explainObjs.put(String.valueOf(i), arr[i]);
			}
        }
        return explainObjs;
    }

    public static ViewInfo getViewInfo(String id) throws Exception {
        if (Pattern.compile("^\\d{10}$").matcher(id).find()) {
            int viewId = Integer.parseInt(id.toString());
            View view = AndroidUI.findViewById(viewId);
            if (view != null) {
                return new ViewInfo(view);
            }
        }
        Object getObj = ObjectsStore.getObject(id);
        if (getObj != null && getObj instanceof View) {
        	View view = (View) getObj;
            return new ViewInfo(view);
        }
        if (Pattern.compile("[a-z]").matcher(id).find()) {
            String idName = id.toString();
            View view = AndroidUI.findViewByIdName(idName);
            if (view != null) {
                return new ViewInfo(view);
            }
        }
        return null;
    }
    

    public static ServiceInfo[] getServiceInfos() throws Exception {
        ServiceInfo[] serviceInfos = null;
        List<ServiceInfo> results = new ArrayList<>();
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field mServicesField = activityThreadClass.getDeclaredField("mServices");
        mServicesField.setAccessible(true);
        Map mServices = (Map) mServicesField.get(activityThread);
        for (Object service : mServices.values()) {
            ServiceInfo serviceInfo = new ServiceInfo((Service) service);
            results.add(serviceInfo);
        }
        serviceInfos = new ServiceInfo[results.size()];
        for (int i = 0; i < results.size(); i++) {
            serviceInfos[i] = results.get(i);
        }
        return serviceInfos;
    }

    public static ActivityInfo[] getActivityInfos() throws Exception {
        ActivityInfo[] activityInfos = null;
        List<ActivityInfo> results = new ArrayList<>();
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
        activitiesField.setAccessible(true);
        Map activities = (Map) activitiesField.get(activityThread);
        ActivityRecord topRecord = findTopActivityRecord(activities);
        for (Object activityClientRecord : activities.values()) {
            ActivityRecord record = toActivityRecord(activityClientRecord);
            Activity activity = record.activity;
            if (activity == null) {
                continue;
            }
            ActivityInfo activityInfo = new ActivityInfo(activity);
            activityInfo.setName(activity.getClass().getName());
            activityInfo.setPaused(record.paused);
            activityInfo.setOnTop(topRecord != null && topRecord.activity == activity);
            activityInfo.setTitle(activity.getTitle().toString());
            activityInfo.setStopped(record.stopped);
            results.add(activityInfo);
        }
        results.sort(new Comparator<ActivityInfo>() {
            @Override
            public int compare(ActivityInfo left, ActivityInfo right) {
                if (left.isOnTop() == right.isOnTop()) {
                    return 0;
                }
                return left.isOnTop() ? -1 : 1;
            }
        });
        int length = results.size();
        activityInfos = new ActivityInfo[length];
        if (length == 0) {
            return activityInfos;
        }
        for (int i = 0; i < results.size(); i++) {
            activityInfos[i] = results.get(i);
        }
        return activityInfos;
    }

    private static ActivityRecord findTopActivityRecord(Map activities) throws Exception {
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        ActivityRecord bestRecord = null;
        for (Object activityClientRecord : activities.values()) {
            ActivityRecord record = toActivityRecord(activityClientRecord);
            if (record.activity == null) {
                continue;
            }
            if (bestRecord == null || compareActivityRecord(record, bestRecord) < 0) {
                bestRecord = record;
            }
        }
        return bestRecord;
    }

    private static ActivityRecord toActivityRecord(Object activityClientRecord) throws Exception {
        Class activityClientRecordClass = activityClientRecord.getClass();

        Field activityField = activityClientRecordClass.getDeclaredField("activity");
        activityField.setAccessible(true);
        Activity activity = (Activity) activityField.get(activityClientRecord);

        Field pausedField = activityClientRecordClass.getDeclaredField("paused");
        pausedField.setAccessible(true);

        Field stoppedField = activityClientRecordClass.getDeclaredField("stopped");
        stoppedField.setAccessible(true);

        ActivityRecord record = new ActivityRecord();
        record.activity = activity;
        record.paused = pausedField.getBoolean(activityClientRecord);
        record.stopped = stoppedField.getBoolean(activityClientRecord);
        return record;
    }

    private static int compareActivityRecord(ActivityRecord candidate, ActivityRecord current) {
        if (candidate.paused != current.paused) {
            return candidate.paused ? 1 : -1;
        }
        if (candidate.stopped != current.stopped) {
            return candidate.stopped ? 1 : -1;
        }
        return 0;
    }

    private static class ActivityRecord {
        private Activity activity;
        private boolean paused;
        private boolean stopped;
    }
    
    public static String getSignatureInfo() throws Exception {
    	JSONArray result = new JSONArray();
    	Application app = getApplication();
    	PackageManager packageManager = app.getPackageManager();
    	PackageInfo packageInfo = packageManager.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNATURES);
        for (Signature sig : packageInfo.signatures) {
        	JSONObject item = new JSONObject();
        	item.put("charsString", sig.toCharsString());
        	result.add(item);
        }
        return result.toJSONString();
    }
}
