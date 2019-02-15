package feign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.client.LoadBalancerFeignClientExt;
import feign.loadbalancer.CachingSpringLoadBalancerFactoryExt;


@Configuration
class DefaultFeignLoadBalancedConfigurationExt {
	@Bean
	@ConditionalOnMissingBean
	public Client feignClient(CachingSpringLoadBalancerFactoryExt cachingFactory,
							  SpringClientFactory clientFactory) {
		
		return new LoadBalancerFeignClientExt(new Client.Default(null, null),
				cachingFactory, clientFactory);
	}
}