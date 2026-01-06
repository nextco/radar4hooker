package gz.httpserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HookerRequestMapping {
	
	public static enum Method {
	    GET, POST
	}
	
	String value() default "/";
	
	Method method() default Method.GET;
	
}
