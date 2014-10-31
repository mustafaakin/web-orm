package weborm.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {
	RequestMethod method() default RequestMethod.GET;

	ContentType contentType() default ContentType.JSON;

	String path();
}
