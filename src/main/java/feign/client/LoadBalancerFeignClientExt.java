package feign.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import com.netflix.client.ClientException;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.loadbalancer.CachingSpringLoadBalancerFactoryExt;
import feign.loadbalancer.FeignLoadBalancerExt;
import feign.loadbalancer.FeignLoadBalancerExt.RibbonRequest;

public class LoadBalancerFeignClientExt implements Client {

	static final Request.Options DEFAULT_OPTIONS = new Request.Options();

	private final Client delegate;
	private CachingSpringLoadBalancerFactoryExt lbClientFactory;
	private SpringClientFactory clientFactory;

	public LoadBalancerFeignClientExt(Client delegate,
			CachingSpringLoadBalancerFactoryExt lbClientFactory,
								   SpringClientFactory clientFactory) {
		this.delegate = delegate;
		this.lbClientFactory = lbClientFactory;
		this.clientFactory = clientFactory;
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		try {
			URI asUri = URI.create(request.url());
			String clientName = asUri.getHost();
			URI uriWithoutHost = cleanUrl(request.url(), clientName);
			FeignLoadBalancerExt.RibbonRequest ribbonRequest = new FeignLoadBalancerExt.RibbonRequest(
					this.delegate, request, uriWithoutHost);

			IClientConfig requestConfig = getClientConfig(options, clientName);
			return lbClient(clientName).executeWithLoadBalancer(ribbonRequest,
					requestConfig).toResponse();
		}
		catch (ClientException e) {
			IOException io = findIOException(e);
			if (io != null) {
				throw io;
			}
			throw new RuntimeException(e);
		}
	}

	IClientConfig getClientConfig(Request.Options options, String clientName) {
		IClientConfig requestConfig;
		if (options == DEFAULT_OPTIONS) {
			requestConfig = this.clientFactory.getClientConfig(clientName);
		} else {
			requestConfig = new FeignOptionsClientConfig(options);
		}
		return requestConfig;
	}

	protected IOException findIOException(Throwable t) {
		if (t == null) {
			return null;
		}
		if (t instanceof IOException) {
			return (IOException) t;
		}
		return findIOException(t.getCause());
	}

	public Client getDelegate() {
		return this.delegate;
	}

	static URI cleanUrl(String originalUrl, String host) {
		return URI.create(originalUrl.replaceFirst(host, ""));
	}

	private FeignLoadBalancerExt lbClient(String clientName) {
		return this.lbClientFactory.create(clientName);
	}

	static class FeignOptionsClientConfig extends DefaultClientConfigImpl {

		public FeignOptionsClientConfig(Request.Options options) {
			setProperty(CommonClientConfigKey.ConnectTimeout,
					options.connectTimeoutMillis());
			setProperty(CommonClientConfigKey.ReadTimeout, options.readTimeoutMillis());
		}

		@Override
		public void loadProperties(String clientName) {

		}

		@Override
		public void loadDefaultValues() {

		}

	}
}