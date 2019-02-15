package feign.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import feign.enumerate.ProtocolType;

@Documented
@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Protocol {
	//默认的协议是Http协议
	ProtocolType value() default ProtocolType.HTTP;
}
