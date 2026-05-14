package gz.httpserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)               // 只能贴在类 / 接口 上
@Retention(RetentionPolicy.RUNTIME)     // 运行期可反射读取
public @interface HookerWebServerConfiguration {

	int port() default 8080;            // HTTP 服务端口

	String remoteWsUrl() default "";    // agent hub 的 ws 地址，例如 ws://192.168.1.10:18080/ws/device

	String remoteToken() default "";    // hub 侧校验 token，可留空

	String remoteDeviceId() default ""; // 自定义 deviceId；不填则走默认设备标识

	String remoteApp() default "";      // 上报给 hub 的 app 名称，可留空

	String remotePackageName() default ""; // 上报给 hub 的包名，可留空

	int remoteHeartbeatSec() default 0; // 心跳秒数；0 表示沿用默认值或服务端下发值

	boolean autoStartRemoteAgent() default false; // 配了 hub 后是否在 webserver 启动完成时自动建连
}
