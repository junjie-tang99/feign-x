package feign;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.netflix.feign.FeignClientProperties;
import org.springframework.cloud.netflix.feign.FeignLoggerFactory;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Feign.Builder;
import feign.Logger.Level;
import feign.Request;
import feign.Request.Options;
import feign.Target.HardCodedTarget;
import feign.client.socket.SocketClientFactory;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.enumerate.ProtocolType;
import feign.loadbalancer.CachingSpringLoadBalancerFactoryExt;
import feign.properties.FeignSocketClientProperties;
import feign.target.Targeter;
import feign.util.ProtocolUtils;

/**
 * @author Spencer Gibb
 * @author Venil Noronha
 * @author Eko Kurniawan Khannedy
 * @author Gregor Zurowski
 */
public class FeignClientFactoryBeanExt implements FactoryBean<Object>, InitializingBean,
		ApplicationContextAware {
	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
	 ***********************************/

	private Class<?> type;

	private String name;

	private String url;

	private String path;
	
	//增加Protocol的属性
	private ProtocolType protocol;

	private boolean decode404;

	private ApplicationContext applicationContext;

	private Class<?> fallback = void.class;

	private Class<?> fallbackFactory = void.class;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.name, "Name must be set");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.applicationContext = context;
	}

	protected Feign.Builder feign(FeignContextExt context) {
		FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);
		Logger logger = loggerFactory.create(this.type);

		//获取Feign的扩展Builder，在返回的类型为：HystrixFeignBuilderExt 或者 FeignBuilderExt
//		// @formatter:off
		Feign.Builder builder = get(context, Feign.Builder.class)
				// required values
				.logger(logger)
				.encoder(get(context, Encoder.class))
				.decoder(get(context, Decoder.class))
				.contract(get(context, Contract.class));
		// @formatter:on
		
		//作用是啥？
		configureFeign(context, builder);

		return builder;
	}

	protected void configureFeign(FeignContextExt context, Feign.Builder builder) {
		FeignClientProperties properties = applicationContext.getBean(FeignClientProperties.class);
		if (properties != null) {
			if (properties.isDefaultToProperties()) {
				configureUsingConfiguration(context, builder);
				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
				configureUsingProperties(properties.getConfig().get(this.name), builder);
			} else {
				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
				configureUsingProperties(properties.getConfig().get(this.name), builder);
				configureUsingConfiguration(context, builder);
			}
		} else {
			configureUsingConfiguration(context, builder);
		}
	}

	protected void configureUsingConfiguration(FeignContextExt context, Feign.Builder builder) {
		Logger.Level level = getOptional(context, Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}
		Retryer retryer = getOptional(context, Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(context, ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		Request.Options options = getOptional(context, Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = context.getInstances(
				this.name, RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}

		if (decode404) {
			builder.decode404();
		}
	}

	protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration config, Feign.Builder builder) {
		if (config == null) {
			return;
		}

		if (config.getLoggerLevel() != null) {
			builder.logLevel(config.getLoggerLevel());
		}

		if (config.getConnectTimeout() != null && config.getReadTimeout() != null) {
			builder.options(new Request.Options(config.getConnectTimeout(), config.getReadTimeout()));
		}

		if (config.getRetryer() != null) {
			Retryer retryer = getOrInstantiate(config.getRetryer());
			builder.retryer(retryer);
		}

		if (config.getErrorDecoder() != null) {
			ErrorDecoder errorDecoder = getOrInstantiate(config.getErrorDecoder());
			builder.errorDecoder(errorDecoder);
		}

		if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
			// this will add request interceptor to builder, not replace existing
			for (Class<RequestInterceptor> bean : config.getRequestInterceptors()) {
				RequestInterceptor interceptor = getOrInstantiate(bean);
				builder.requestInterceptor(interceptor);
			}
		}

		if (config.getDecode404() != null) {
			if (config.getDecode404()) {
				builder.decode404();
			}
		}
	}

	private <T> T getOrInstantiate(Class<T> tClass) {
		try {
			return applicationContext.getBean(tClass);
		} catch (NoSuchBeanDefinitionException e) {
			return BeanUtils.instantiateClass(tClass);
		}
	}

	protected <T> T get(FeignContextExt context, Class<T> type) {
		T instance = context.getInstance(this.name, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for "
					+ this.name);
		}
		return instance;
	}

	protected <T> T getOptional(FeignContextExt context, Class<T> type) {
		return context.getInstance(this.name, type);
	}

	protected <T> T loadBalance(Feign.Builder builder, FeignContextExt context,
			HardCodedTarget<T> target) {
		
		//获取调用ApiService的协议类型
		ProtocolType protocol =  ProtocolUtils.getProtocol(target.url());
		Client client = null;
		//根据不同的协议类型，创建不同的loadBalancerFeignCLient
		if (protocol == ProtocolType.SOCKET) {
			//如果调用的协议类型是Socket，那么创建包含了SocketClient的LoadBalancerFeignClientExt
			SpringClientFactory clientFactory = get(context, SpringClientFactory.class);
			CachingSpringLoadBalancerFactoryExt cachingFactory = get(context, CachingSpringLoadBalancerFactoryExt.class);
			FeignSocketClientProperties properties = get(context, FeignSocketClientProperties.class);
			client = SocketClientFactory.createClient(cachingFactory, clientFactory, properties);
		}
		else if (protocol == ProtocolType.HTTP) {
			//从FeignContext中获取Client对象
			client = getOptional(context, Client.class);
		}
		
		if (client != null) {
			//设置Builder的Client对象
			builder.client(client);
			//从FeignContext中获取中间的target对象
			Targeter targeter = get(context, Targeter.class);
			//返回Proxy.newInstance出来的代理对象
			return targeter.target(this, builder, context, target);
		}

		throw new IllegalStateException(
				"No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon?");
	}

	@Override
	public Object getObject() throws Exception {
		//从Spring容器中获取FeignContext对象，该对象
		FeignContextExt context = applicationContext.getBean(FeignContextExt.class);
		
		//从Context中获取FeignBuilder对象，并设置Feign的相关组件
		//1、设置Feign.Builder的Encoder
		//2、设置Feign.Builder的Decoder
		//3、设置Feign.Builder的Contract
		//4、设置Feign.Builder的Logger
		//说明：Builder中的Client在后续的代码中创建，创建的类型是LoadBalancerFeignClientExt.class
		Feign.Builder builder = feign(context);

		//【条件1 】如果@FeignClient注解中不包含url属性
		if (!StringUtils.hasText(this.url)) {
			String url;
			//注释源代码
//			if (!this.name.startsWith("http")) {
//				url = "http://" + this.name;
//			}
//			else {
//				url = this.name;
//			}
			//检查@FeignClient注解的name属性中，是否包含了协议，例如：dubbo://service
			//如果不包含该协议，那么将协议+FeignClientName设置到url变量上
			if (!ProtocolUtils.containsSupportedProtocol(this.name)) {
				url = ProtocolUtils.appendToUrl(this.protocol, this.name);
			}else {
			//否则，直接将FeignClientName设置到url变量上
				url = this.name;
			}
			//拼接@FeignClient注解的path属性
			url += cleanPath();
			//返回Proxy.newInstance出来的代理对象
			return loadBalance(builder, context, new HardCodedTarget<>(this.type,
					this.name, url));
		}
		
		//注释源代码
//		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
//			this.url = "http://" + this.url;
//		}
		//【条件2 】如果@FeignClient注解中包含了url，但是url属性中不包含支持的协议
		if (StringUtils.hasText(this.url) && !ProtocolUtils.containsSupportedProtocol(this.url)) {
			this.url = ProtocolUtils.appendToUrl(this.protocol, this.url);
		}
		String url = this.url + cleanPath();
		//获取Client对象：Spring封装了基于Ribbon的客户端（LoadBalancerFeignClient）
		//1、Feign自己封装的Request（基于java.net原生），2、OkHttpClient（新一代/HTTP2），3、ApacheHttpClient（常规）
		Client client = getOptional(context, Client.class);
		if (client != null) {
			if (client instanceof LoadBalancerFeignClient) {
				// not lod balancing because we have a url,
				// but ribbon is on the classpath, so unwrap
				client = ((LoadBalancerFeignClient)client).getDelegate();
			}
			//设置调用客户端
			builder.client(client);
		}
		//获取target的中间对象
		//支持的中间对象有DefaultTargeter或者HystrixTargeter，其中HystrixTargeter带熔断和降级功能
		Targeter targeter = get(context, Targeter.class);
		//返回Proxy.newInstance出来的代理对象
		return targeter.target(this, builder, context, new HardCodedTarget<>(
				this.type, this.name, url));
	}

	private String cleanPath() {
		String path = this.path.trim();
		if (StringUtils.hasLength(path)) {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
		}
		return path;
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDecode404() {
		return decode404;
	}

	public void setDecode404(boolean decode404) {
		this.decode404 = decode404;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public Class<?> getFallback() {
		return fallback;
	}

	public void setFallback(Class<?> fallback) {
		this.fallback = fallback;
	}

	public Class<?> getFallbackFactory() {
		return fallbackFactory;
	}

	public void setFallbackFactory(Class<?> fallbackFactory) {
		this.fallbackFactory = fallbackFactory;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FeignClientFactoryBeanExt that = (FeignClientFactoryBeanExt) o;
		return Objects.equals(applicationContext, that.applicationContext) &&
				decode404 == that.decode404 &&
				Objects.equals(fallback, that.fallback) &&
				Objects.equals(fallbackFactory, that.fallbackFactory) &&
				Objects.equals(name, that.name) &&
				Objects.equals(path, that.path) &&
				Objects.equals(type, that.type) &&
				Objects.equals(url, that.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicationContext, decode404, fallback, fallbackFactory,
				name, path, type, url);
	}

	@Override
	public String toString() {
		return new StringBuilder("FeignClientFactoryBean{")
				.append("type=").append(type).append(", ")
				.append("name='").append(name).append("', ")
				.append("url='").append(url).append("', ")
				.append("path='").append(path).append("', ")
				.append("decode404=").append(decode404).append(", ")
				.append("applicationContext=").append(applicationContext).append(", ")
				.append("fallback=").append(fallback).append(", ")
				.append("fallbackFactory=").append(fallbackFactory)
				.append("}").toString();
	}

	//增加protocolType属性的Setter方法
	public ProtocolType getProtocol() {
		return protocol;
	}

	//增加protocolType属性的getter方法
	public void setProtocol(ProtocolType protocol) {
		this.protocol = protocol;
	}

}
