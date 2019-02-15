package feign.client.socket;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import feign.Client;
import feign.client.LoadBalancerFeignClientExt;
import feign.loadbalancer.CachingSpringLoadBalancerFactoryExt;
import feign.properties.FeignSocketClientProperties;

public class SocketClientFactory {
	
	//创建实际请求的Client对象
	static SocketClient createDelegate(FeignSocketClientProperties properties) {
		return SocketClient.Builder.create()
					.ConnectTimeout(properties.getConnectionTimeout())
					.ReadTimeout(properties.getReadTimeout())
					.build();
	}

	public static Client createClient(CachingSpringLoadBalancerFactoryExt cachingFactory,
			  SpringClientFactory clientFactory, FeignSocketClientProperties properties) {
		SocketClient delegate = createDelegate(properties);
		return new LoadBalancerFeignClientExt(delegate, cachingFactory, clientFactory);
	}



}
