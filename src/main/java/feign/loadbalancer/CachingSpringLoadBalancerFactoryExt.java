package feign.loadbalancer;

import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

/**
 * Factory for SpringLoadBalancer instances that caches the entries created.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Ryan Baxter
 * @author Gang Li
 */
public class CachingSpringLoadBalancerFactoryExt{

	private final SpringClientFactory factory;
	private final LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;
	private final LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory;
	private final LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory;
	private boolean enableRetry = false;

	private volatile Map<String, FeignLoadBalancerExt> cache = new ConcurrentReferenceHashMap<>();

	public CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = new RibbonLoadBalancedRetryPolicyFactory(factory);
		this.loadBalancedBackOffPolicyFactory = null;
		this.loadBalancedRetryListenerFactory = null;
	}

	@Deprecated
	//TODO remove in 2.0.x
	public CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory,
											LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = null;
		this.loadBalancedRetryListenerFactory = null;
	}

	@Deprecated
	//TODO remove in 2.0.0x
	public CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory,
											LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory, boolean enableRetry) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.enableRetry = enableRetry;
		this.loadBalancedBackOffPolicyFactory = null;
		this.loadBalancedRetryListenerFactory = null;
	}

	@Deprecated
	//TODO remove in 2.0.0x
	public CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory,
											LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
											LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory;
		this.loadBalancedRetryListenerFactory = null;
		this.enableRetry = true;
	}

	public CachingSpringLoadBalancerFactoryExt(SpringClientFactory factory, LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
											LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
											LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		this.factory = factory;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory;
		this.loadBalancedRetryListenerFactory = loadBalancedRetryListenerFactory;
		this.enableRetry = true;
	}

	public FeignLoadBalancerExt create(String clientName) {
		if (this.cache.containsKey(clientName)) {
			return this.cache.get(clientName);
		}
		IClientConfig config = this.factory.getClientConfig(clientName);
		ILoadBalancer lb = this.factory.getLoadBalancer(clientName);
		ServerIntrospector serverIntrospector = this.factory.getInstance(clientName, ServerIntrospector.class);
		//暂时不支持重试的功能
		//FeignLoadBalancerExt client = enableRetry ? new RetryableFeignLoadBalancerExt(lb, config, serverIntrospector,
		//	loadBalancedRetryPolicyFactory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory) : new FeignLoadBalancerExt(lb, config, serverIntrospector);
		FeignLoadBalancerExt client = new FeignLoadBalancerExt(lb, config, serverIntrospector);
		this.cache.put(clientName, client);
		return client;
	}

}
