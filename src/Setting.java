


import java.lang.annotation.*;

/**
 * @author Ed Baskerville
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting
{
	String shortName() default "";
	String humanReadableName() default "";
	String description() default "";
//	double[] range() default {0,1,0.1};
//	String rangeIndexSettings() default "";	
	boolean allowFieldName() default true;
}
