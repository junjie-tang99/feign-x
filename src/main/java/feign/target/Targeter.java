package feign.target;


import feign.Feign;
import feign.FeignClientFactoryBeanExt;
import feign.FeignContextExt;
import feign.Target;

public interface Targeter {
	<T> T target(FeignClientFactoryBeanExt factory, Feign.Builder feign, FeignContextExt context,
				 Target.HardCodedTarget<T> target);
}