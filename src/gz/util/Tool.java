package gz.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class Tool {
	
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: gz.util.Tool <package.name>");
            return;
        }
        String pkg = args[0];

        try {
            Class<?> atClz = Class.forName("android.app.ActivityThread");

            Object at = atClz.getMethod("currentActivityThread").invoke(null);
            if (at == null) {
                // 在部分 ROM 的 app_process 环境里必须这样初始化，否则拿不到 system context
                at = atClz.getMethod("systemMain").invoke(null);
            }
            Context ctx = (Context) atClz.getMethod("getSystemContext").invoke(at);
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            String label = pm.getApplicationLabel(ai).toString();
            System.out.println(pkg + " -> " + label);
        } catch (Throwable t) {
            System.out.println("ERR: " + t);
            t.printStackTrace(System.out);
        }
    }
}

