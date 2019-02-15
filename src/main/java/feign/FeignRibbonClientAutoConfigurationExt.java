package feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.feign.support.FeignHttpClientProperties;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.netflix.loadbalancer.ILoadBalancer;

import feign.loadbalancer.CachingSpringLoadBalancerFactoryExt;

@ConditionalOnClass({ ILoadBalancer.class, Feign.class })
@Configuration
@AutoConfigureBefore(FeignAutoConfigurationExt.class)
//@TODO:暂时不处理
//@EnableConfigurationProperties({ FeignHttpClientProperties.class })

//Order is important here, last should be the default, first should be optional
// see https://github.com/spring-cloud/spring-cloud-netflix/issues/2086#issuecomment-316281653
//TODO:确认是否要启动import configuration
//@Import({ HttpClientFeignLoadBalancedConfiguration.class,
//	OkHttpFeignLoadBalancedConfiguration.class,
//	DefaultFeignLoadBalancedConfiguration.class })
//@TODO:暂时不处理
@Import({DefaultFeignLoadBalancedConfigurationExt.class })
public class FeignRibbonClientAutoConfigurationExt {
	private static final Logger LOGGER = LoggerFactory.getLogger(FeignRibbonClientAutoConfigurationExt.class); 

	public FeignRibbonClientAutoConfigurationExt() {
		LOGGER.info("Initializing feign.FeignRibbonClientAutoConfigurationExt configuration");
	}
	
	@Bean
	@Primary
	@ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
	public CachingSpringLoadBalancerFactoryExt cachingLBClientFactory(
			SpringClientFactory factory) {
		LOGGER.info("Bean 'feign.loadbalancer.CachingSpringLoadBalancerFactoryExt.CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory)' has been created.");
		return new CachingSpringLoadBalancerFactoryExt(factory);
	}

	@Bean
	@Primary
	@ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
	public CachingSpringLoadBalancerFactoryExt retryabeCachingLBClientFactory(
		SpringClientFactory factory,
		LoadBalancedRetryPolicyFactory retryPolicyFactory,
		LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
		LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		LOGGER.info("Bean 'feign.loadbalancer.CachingSpringLoadBalancerFactoryExt.CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory, LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory, LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory, LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory)' has been created.");
		return new CachingSpringLoadBalancerFactoryExt(factory, retryPolicyFactory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
	}
	
	
//	@Bean
//	@ConditionalOnMissingBean
//	public Request.Options feignRequestOptions() {
//		return LoadBalancerFeignClient.DEFAULT_OPTIONS;
//	}

}
