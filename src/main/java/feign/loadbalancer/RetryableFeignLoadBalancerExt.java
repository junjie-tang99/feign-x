package feign.loadbalancer;

import java.io.IOException;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;


import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

public class RetryableFeignLoadBalancerExt extends FeignLoadBalancerExt implements ServiceInstanceChooser {

	private final LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;
	private final LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory;
	private final LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory;

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableFeignLoadBalancerExt(ILoadBalancer lb, IClientConfig clientConfig,
							 ServerIntrospector serverIntrospector, LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(lb, clientConfig, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.setRetryHandler(new DefaultLoadBalancerRetryHandler(clientConfig));
		this.loadBalancedBackOffPolicyFactory = new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
		this.loadBalancedRetryListenerFactory = new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();
	}

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableFeignLoadBalancerExt(ILoadBalancer lb, IClientConfig clientConfig,
									  ServerIntrospector serverIntrospector, LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
									  LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory) {
		super(lb, clientConfig, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.setRetryHandler(new DefaultLoadBalancerRetryHandler(clientConfig));
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory == null ?
			new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory() : loadBalancedBackOffPolicyFactory;
		this.loadBalancedRetryListenerFactory = new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();
	}

	public RetryableFeignLoadBalancerExt(ILoadBalancer lb, IClientConfig clientConfig, ServerIntrospector serverIntrospector,
									  LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
									  LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
									  LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		super(lb, clientConfig, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.setRetryHandler(new DefaultLoadBalancerRetryHandler(clientConfig));
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory == null ?
			new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory() : loadBalancedBackOffPolicyFactory;
		this.loadBalancedRetryListenerFactory = loadBalancedRetryListenerFactory == null ?
			new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory() : loadBalancedRetryListenerFactory;
	}

	@Override
	public RibbonResponse execute(final RibbonRequest request, IClientConfig configOverride)
			throws IOException {
//		final Request.Options options;
//		if (configOverride != null) {
//			options = new Request.Options(
//					configOverride.get(CommonClientConfigKey.ConnectTimeout,
//							this.connectTimeout),
//					(configOverride.get(CommonClientConfigKey.ReadTimeout,
//							this.readTimeout)));
//		}
//		else {
//			options = new Request.Options(this.connectTimeout, this.readTimeout);
//		}
//		final LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryPolicyFactory.create(this.getClientName(), this);
//		RetryTemplate retryTemplate = new RetryTemplate();
//		BackOffPolicy backOffPolicy = loadBalancedBackOffPolicyFactory.createBackOffPolicy(this.getClientName());
//		retryTemplate.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
//		RetryListener[] retryListeners = this.loadBalancedRetryListenerFactory.createRetryListeners(this.getClientName());
//		if (retryListeners != null && retryListeners.length != 0) {
//			retryTemplate.setListeners(retryListeners);
//		}
//		retryTemplate.setRetryPolicy(retryPolicy == null ? new NeverRetryPolicy()
//				: new FeignRetryPolicy(request.toHttpRequest(), retryPolicy, this, this.getClientName()));
//		return retryTemplate.execute(new RetryCallback<RibbonResponse, IOException>() {
//			@Override
//			public RibbonResponse doWithRetry(RetryContext retryContext) throws IOException {
//				Request feignRequest = null;
//				//on retries the policy will choose the server and set it in the context
//				//extract the server and update the request being made
//				if (retryContext instanceof LoadBalancedRetryContext) {
//					ServiceInstance service = ((LoadBalancedRetryContext) retryContext).getServiceInstance();
//					if (service != null) {
//						feignRequest = ((RibbonRequest) request.replaceUri(reconstructURIWithServer(new Server(service.getHost(), service.getPort()), request.getUri()))).toRequest();
//					}
//				}
//				if (feignRequest == null) {
//					feignRequest = request.toRequest();
//				}
//				Response response = request.client().execute(feignRequest, options);
//				if (retryPolicy.retryableStatusCode(response.status())) {
//					byte[] byteArray = response.body() == null ? new byte[]{} : StreamUtils.copyToByteArray(response.body().asInputStream());
//					response.close();
//					throw new RibbonResponseStatusCodeException(RetryableFeignLoadBalancerExt.this.clientName, response,
//							byteArray, request.getUri());
//				}
//				return new RibbonResponse(request.getUri(), response);
//			}
//		}, new RibbonRecoveryCallback<RibbonResponse, Response>() {
//			@Override
//			protected RibbonResponse createResponse(Response response, URI uri) {
//				return new RibbonResponse(uri, response);
//			}
//		});
		return null;
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			FeignLoadBalancerExt.RibbonRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, this.getRetryHandler(), requestConfig);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return new RibbonLoadBalancerClient.RibbonServer(serviceId,
				this.getLoadBalancer().chooseServer(serviceId));
	}
}
