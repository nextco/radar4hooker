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
	
	public static enum Produces {
	    TEXT("text/plain; charset=utf-8"),
	    HTML("text/html; charset=utf-8"),
	    JSON("application/json; charset=utf-8"),
	    AUTO("auto");

	    private final String value;

	    Produces(String value) {
	        this.value = value;
	    }

	    public String value() {
	        return value;
	    }

	    @Override
	    public String toString() {
	        return value; // 想打印自定义名称就这样
	    }
	}

	
	String path();
	
	Produces produces() default Produces.AUTO;
	
	Method method() default Method.GET;
	
	
	
}
