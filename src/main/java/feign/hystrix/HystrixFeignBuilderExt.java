package feign.hystrix;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import com.netflix.hystrix.HystrixCommand;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Logger;
import feign.RequestInterceptor;
import feign.ResponseMapper;
import feign.Retryer;
import feign.Target;
import feign.Contract.Default;
import feign.FeignBuilderExt;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Logger.Level;
import feign.Request;
import feign.Request.Options;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hystrix.FallbackFactory;
import feign.hystrix.HystrixDelegatingContract;
import feign.hystrix.SetterFactory;

public class HystrixFeignBuilderExt extends FeignBuilderExt {

	public static Feign.Builder builder() {
		return new HystrixFeignBuilderExt();
	}
	 

	private Contract contract = new Contract.Default();
	private SetterFactory setterFactory = new SetterFactory.Default();
	
	/**
	 * Allows you to override hystrix properties such as thread pools and command keys.
	 */
	public HystrixFeignBuilderExt setterFactory(SetterFactory setterFactory) {
	  this.setterFactory = setterFactory;
	  return this;
	}
	
	/**
	 * @see #target(Class, String, Object)
	 */
	public <T> T target(Target<T> target, T fallback) {
	  return build(fallback != null ? new FallbackFactory.Default<T>(fallback) : null)
	      .newInstance(target);
	}
	
	/**
	 * @see #target(Class, String, FallbackFactory)
	 */
	public <T> T target(Target<T> target, FallbackFactory<? extends T> fallbackFactory) {
	  return build(fallbackFactory).newInstance(target);
	}
	
	/**
	 * Like {@link Feign#newInstance(Target)}, except with {@link HystrixCommand#getFallback()
	 * fallback} support.
	 *
	 * <p>Fallbacks are known values, which you return when there's an error invoking an http
	 * method. For example, you can return a cached result as opposed to raising an error to the
	 * caller. To use this feature, pass a safe implementation of your target interface as the last
	 * parameter.
	 *
	 * Here's an example:
	 * <pre>
	 * {@code
	 *
	 * // When dealing with fallbacks, it is less tedious to keep interfaces small.
	 * interface GitHub {
	 *   @RequestLine("GET /repos/{owner}/{repo}/contributors")
	 *   List<String> contributors(@Param("owner") String owner, @Param("repo") String repo);
	 * }
	 *
	 * // This instance will be invoked if there are errors of any kind.
	 * GitHub fallback = (owner, repo) -> {
	 *   if (owner.equals("Netflix") && repo.equals("feign")) {
	 *     return Arrays.asList("stuarthendren"); // inspired this approach!
	 *   } else {
	 *     return Collections.emptyList();
	 *   }
	 * };
	 *
	 * GitHub github = HystrixFeign.builder()
	 *                             ...
	 *                             .target(GitHub.class, "https://api.github.com", fallback);
	 * }</pre>
	 *
	 * @see #target(Target, Object)
	 */
	public <T> T target(Class<T> apiType, String url, T fallback) {
	  return target(new Target.HardCodedTarget<T>(apiType, url), fallback);
	}
	
	/**
	 * Same as {@link #target(Class, String, T)}, except you can inspect a source exception before
	 * creating a fallback object.
	 */
	public <T> T target(Class<T> apiType, String url, FallbackFactory<? extends T> fallbackFactory) {
	  return target(new Target.HardCodedTarget<T>(apiType, url), fallbackFactory);
	}
	
	@Override
	public Feign.Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
	  throw new UnsupportedOperationException();
	}
	
	@Override
	public HystrixFeignBuilderExt contract(Contract contract) {
	  this.contract = contract;
	  return this;
	}
	
	@Override
	public Feign build() {
	  return build(null);
	}
	
	/** Configures components needed for hystrix integration. */
	Feign build(final FallbackFactory<?> nullableFallbackFactory) {
	  super.invocationHandlerFactory(new InvocationHandlerFactory() {
	    @Override public InvocationHandler create(Target target,
	        Map<Method, MethodHandler> dispatch) {
	      return new HystrixInvocationHandler(target, dispatch, setterFactory, nullableFallbackFactory);
	    }
	  });
	  super.contract(new HystrixDelegatingContract(contract));
	  return super.build();
	}
	
	// Covariant overrides to support chaining to new fallback method.
	@Override
	public HystrixFeignBuilderExt logLevel(Logger.Level logLevel) {
	  return (HystrixFeignBuilderExt) super.logLevel(logLevel);
	}
	
	@Override
	public HystrixFeignBuilderExt client(Client client) {
	  return (HystrixFeignBuilderExt) super.client(client);
	}
	
	@Override
	public HystrixFeignBuilderExt retryer(Retryer retryer) {
	  return (HystrixFeignBuilderExt) super.retryer(retryer);
	}
	
	@Override
	public HystrixFeignBuilderExt logger(Logger logger) {
	  return (HystrixFeignBuilderExt) super.logger(logger);
	}
	
	@Override
	public HystrixFeignBuilderExt encoder(Encoder encoder) {
	  return (HystrixFeignBuilderExt) super.encoder(encoder);
	}
	
	@Override
	public HystrixFeignBuilderExt decoder(Decoder decoder) {
	  return (HystrixFeignBuilderExt) super.decoder(decoder);
	}
	
	@Override
	public HystrixFeignBuilderExt mapAndDecode(ResponseMapper mapper, Decoder decoder) {
	  return (HystrixFeignBuilderExt) super.mapAndDecode(mapper, decoder);
	}
	
	@Override
	public HystrixFeignBuilderExt decode404() {
	  return (HystrixFeignBuilderExt) super.decode404();
	}
	
	@Override
	public HystrixFeignBuilderExt errorDecoder(ErrorDecoder errorDecoder) {
	  return (HystrixFeignBuilderExt) super.errorDecoder(errorDecoder);
	}
	
	@Override
	public HystrixFeignBuilderExt options(Request.Options options) {
	  return (HystrixFeignBuilderExt) super.options(options);
	}
	
	@Override
	public HystrixFeignBuilderExt requestInterceptor(RequestInterceptor requestInterceptor) {
	  return (HystrixFeignBuilderExt) super.requestInterceptor(requestInterceptor);
	}
	
	@Override
	public HystrixFeignBuilderExt requestInterceptors(Iterable<RequestInterceptor> requestInterceptors) {
	  return (HystrixFeignBuilderExt) super.requestInterceptors(requestInterceptors);
	}
}
