package gz.httpserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)               // 只能贴在类 / 接口 上
@Retention(RetentionPolicy.RUNTIME)     // 运行期可反射读取
public @interface HttpServer {

	int port() default 8080;            // HTTP 服务端口
}
