package feign;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.feign.FeignClientProperties;
import org.springframework.cloud.netflix.feign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import feign.properties.FeignSocketClientProperties;
import feign.target.DefaultTargeter;
import feign.target.HystrixTargeter;
import feign.target.Targeter;


@Configuration
@ConditionalOnClass(Feign.class)
@EnableConfigurationProperties({FeignClientProperties.class, FeignHttpClientProperties.class,FeignSocketClientProperties.class})
public class FeignAutoConfigurationExt {
	private static final Logger LOGGER = LoggerFactory.getLogger(FeignAutoConfigurationExt.class); 

	public FeignAutoConfigurationExt() {
		LOGGER.info("Initializing feign.FeignAutoConfigurationExt configuration...");
	}
	
	@Autowired(required = false)
	private List<FeignClientSpecification> configurations = new ArrayList<>();

	@Bean
	public HasFeatures feignFeature() {
		return HasFeatures.namedFeature("Feign", Feign.class);
	}

	@Bean
	public FeignContextExt feignContext() {
		LOGGER.info("Bean 'feign.FeignContextExt' has been created.");
		FeignContextExt context = new FeignContextExt();
		context.setConfigurations(this.configurations);
		return context;
	}
	
	@Configuration
	@ConditionalOnClass(name = "feign.hystrix.HystrixFeign")
	protected static class HystrixFeignTargeterConfiguration {
		@Bean
		@Primary
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			LOGGER.info("Bean 'feign.target.HystrixTargeter' has been created.");
			return new HystrixTargeter();
		}
	}

	@Configuration
	@ConditionalOnMissingClass("feign.hystrix.HystrixFeign")
	protected static class DefaultFeignTargeterConfiguration {
		@Bean
		@Primary
		@ConditionalOnMissingBean
		public Targeter feignTargeter() {
			LOGGER.info("Bean 'feign.target.DefaultTargeter' has been created.");
			return new DefaultTargeter();
		}
	}
	


//	@Configuration
//	@ConditionalOnClass(SocketClient.class)
//	@ConditionalOnMissingClass("com.netflix.loadbalancer.ILoadBalancer")
//	@ConditionalOnProperty(value = "feign.socket.enabled", matchIfMissing = true)
//	protected static class SocketFeignConfiguration {
//
//		@Bean
//		@ConditionalOnMissingBean(Client.class)
//		//@ConditionalOnMissingBean(SocketClient.class)
//		public Client socketClient(IClientConfig config) {
//			return SocketClient.Builder.create()
//					.ConnectTimeout(config.getPropertyAsInteger(CommonClientConfigKey.ConnectTimeout, 5000))
//					.ReadTimeout(config.getPropertyAsInteger(CommonClientConfigKey.ReadTimeout, 5000))
//					.build();
//		}
//	}
	
//	@Bean
//	@ConditionalOnMissingBean(SocketLoadBalancingClient.class)
//	public SocketLoadBalancingClient socketLoadBalancingClient(
//		IClientConfig config, ServerIntrospector serverIntrospector,
//		ILoadBalancer loadBalancer, SocketClient socketClient) {
//		SocketLoadBalancingClient client = new SocketLoadBalancingClient(socketClient, config, serverIntrospector);
//		client.setLoadBalancer(loadBalancer);
//		return client;
//	}
	
}
