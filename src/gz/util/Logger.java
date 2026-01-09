package gz.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	private final String className;
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public Logger(Class<?> cls) {
        this.className = cls.getSimpleName();
        this.logFile = new File("/sdcard/webserver.log"); // Android 上需权限
    }

    private synchronized void log(String level, String msg) {
        String time = dateFormat.format(new Date());
        // 获取调用堆栈
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // stack[0] = Thread.getStackTrace
        // stack[1] = this.log
        // stack[2] = info/warn/error
        // stack[3] = 调用者 → 我们要取这个
        StackTraceElement caller = stack[0];
        for (StackTraceElement e : stack) {
            if (e.getClassName().equals(className)) {
            	caller = e;
            	break;
            }
        }
        String line = String.format(
                "[%s] [%s] [%s.%s:%d] %s%n",
                level,
                time,
                caller.getClassName(),   // 调用类全名
                caller.getMethodName(),  // 调用方法
                caller.getLineNumber(),  // 行号
                msg
        );
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(line);
        } catch (IOException e) {
            XLog.appendText(e);
        }
    }

    public void info(String msg) {
        log("INFO", msg);
    }

    public void warn(String msg) {
        log("WARN", msg);
    }

    public void warn(Exception e) {
        logException("WARN", e);
    }

    public void error(Exception e) {
        logException("ERROR", e);
    }

    public void error(String msg) {
        log("ERROR", msg);
    }
    
    private void logException(String level, Exception ex) {
        log(level, XLog.getException(ex));
    }
}
