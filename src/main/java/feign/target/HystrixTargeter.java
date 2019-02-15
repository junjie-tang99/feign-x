package feign.target;

import org.springframework.util.Assert;


import feign.Feign;
import feign.FeignClientFactoryBeanExt;
import feign.FeignContextExt;
import feign.Target;
import feign.hystrix.FallbackFactory;
import feign.hystrix.HystrixFeign;
import feign.hystrix.SetterFactory;

public class HystrixTargeter implements Targeter {

	@Override
	public <T> T target(FeignClientFactoryBeanExt factory, Feign.Builder feign, FeignContextExt context,
						Target.HardCodedTarget<T> target) {
		//【条件1】如果不是HystrixFeign的Builder对象
		if (!(feign instanceof feign.hystrix.HystrixFeign.Builder)) {
			return feign.target(target);
		}
		//【条件2】如果是HystrixFeign的Builder对象
		feign.hystrix.HystrixFeign.Builder builder = (feign.hystrix.HystrixFeign.Builder) feign;
		//获取SetterFactory的对象
		SetterFactory setterFactory = getOptional(factory.getName(), context,SetterFactory.class);
		if (setterFactory != null) {
			builder.setterFactory(setterFactory);
		}
		//获取@FeignClientExt注解上的fallBack Class对象
		Class<?> fallback = factory.getFallback();
		if (fallback != void.class) {
			return targetWithFallback(factory.getName(), context, target, builder, fallback);
		}
		//获取@FeignClientExt注解上的fallBackFactory Class对象
		Class<?> fallbackFactory = factory.getFallbackFactory();
		if (fallbackFactory != void.class) {
			return targetWithFallbackFactory(factory.getName(), context, target, builder, fallbackFactory);
		}

		return feign.target(target);
	}

	private <T> T targetWithFallbackFactory(String feignClientName, FeignContextExt context,
											Target.HardCodedTarget<T> target,
											HystrixFeign.Builder builder,
											Class<?> fallbackFactoryClass) {
		FallbackFactory<? extends T> fallbackFactory = (FallbackFactory<? extends T>)
			getFromContext("fallbackFactory", feignClientName, context, fallbackFactoryClass, FallbackFactory.class);
		/* We take a sample fallback from the fallback factory to check if it returns a fallback
		that is compatible with the annotated feign interface. */
		Object exampleFallback = fallbackFactory.create(new RuntimeException());
		Assert.notNull(exampleFallback,
			String.format(
			"Incompatible fallbackFactory instance for feign client %s. Factory may not produce null!",
				feignClientName));
		if (!target.type().isAssignableFrom(exampleFallback.getClass())) {
			throw new IllegalStateException(
				String.format(
					"Incompatible fallbackFactory instance for feign client %s. Factory produces instances of '%s', but should produce instances of '%s'",
					feignClientName, exampleFallback.getClass(), target.type()));
		}
		return builder.target(target, fallbackFactory);
	}


	private <T> T targetWithFallback(String feignClientName, FeignContextExt context,
									 Target.HardCodedTarget<T> target,
									 HystrixFeign.Builder builder, Class<?> fallback) {
		T fallbackInstance = getFromContext("fallback", feignClientName, context, fallback, target.type());
		return builder.target(target, fallbackInstance);
	}

	private <T> T getFromContext(String fallbackMechanism, String feignClientName, FeignContextExt context,
								 Class<?> beanType, Class<T> targetType) {
		Object fallbackInstance = context.getInstance(feignClientName, beanType);
		if (fallbackInstance == null) {
			throw new IllegalStateException(String.format(
				"No " + fallbackMechanism + " instance of type %s found for feign client %s",
				beanType, feignClientName));
		}

		if (!targetType.isAssignableFrom(beanType)) {
			throw new IllegalStateException(
					String.format(
						"Incompatible " + fallbackMechanism + " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
						beanType, targetType, feignClientName));
		}
		return (T) fallbackInstance;
	}

	private <T> T getOptional(String feignClientName, FeignContextExt context,
		Class<T> beanType) {
		return context.getInstance(feignClientName, beanType);
	}
}