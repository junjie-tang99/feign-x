package feign.target;


import feign.Feign;
import feign.FeignClientFactoryBeanExt;
import feign.FeignContextExt;
import feign.Target;

public class DefaultTargeter implements Targeter {

	@Override
	public <T> T target(FeignClientFactoryBeanExt factory, Feign.Builder feign, FeignContextExt context,
						Target.HardCodedTarget<T> target) {
		return feign.target(target);
	}
}
