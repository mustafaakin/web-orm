package weborm.annotations.inputs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {
	public double min() default Double.MIN_VALUE;

	public double max() default Double.MAX_VALUE;

	public String message() default "The value is not in range. ";
}
