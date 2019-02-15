package feign.loadbalancer;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToHttpsIfNeeded;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import com.google.common.base.Strings;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.enumerate.ProtocolType;
import feign.util.ProtocolUtils;

public class FeignLoadBalancerExt extends AbstractLoadBalancerAwareClient<FeignLoadBalancerExt.RibbonRequest, FeignLoadBalancerExt.RibbonResponse> {

	protected int connectTimeout;
	protected int readTimeout;
	protected IClientConfig clientConfig;
	protected ServerIntrospector serverIntrospector;

	public FeignLoadBalancerExt(ILoadBalancer lb, IClientConfig clientConfig,
							 ServerIntrospector serverIntrospector) {
		super(lb, clientConfig);
		this.setRetryHandler(RetryHandler.DEFAULT);
		this.clientConfig = clientConfig;
		this.connectTimeout = clientConfig.get(CommonClientConfigKey.ConnectTimeout);
		this.readTimeout = clientConfig.get(CommonClientConfigKey.ReadTimeout);
		this.serverIntrospector = serverIntrospector;
	}

	@Override
	public RibbonResponse execute(RibbonRequest request, IClientConfig configOverride)
			throws IOException {
		Request.Options options;
		if (configOverride != null) {
			options = new Request.Options(
					configOverride.get(CommonClientConfigKey.ConnectTimeout,
							this.connectTimeout),
					(configOverride.get(CommonClientConfigKey.ReadTimeout,
							this.readTimeout)));
		}
		else {
			options = new Request.Options(this.connectTimeout, this.readTimeout);
		}
		Response response = request.client().execute(request.toRequest(), options);
		return new RibbonResponse(request.getUri(), response);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			RibbonRequest request, IClientConfig requestConfig) {
		if (this.clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations,
				false)) {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(),
					requestConfig);
		}
		if (!request.toRequest().method().equals("GET")) {
			return new RequestSpecificRetryHandler(true, false, this.getRetryHandler(),
					requestConfig);
		}
		else {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(),
					requestConfig);
		}
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		URI uri = updateToHttpsIfNeeded(original, this.clientConfig, this.serverIntrospector, server);
		
		//return super.reconstructURIWithServer(server, uri);
		//重写生成loadbalance URI的
		//获取目标Server的host，例如：192.168.1.1
        String host = server.getHost();
        //获取目标Server的port，该port是通过在application.yml中设置的server.port
        int port = server.getPort();
        //获取目标Server的scheme，例如http
        String scheme = server.getScheme();
        //从url中获取对应的协议，例如：http、dubbo
        //ProtocolType protocol = ProtocolUtils.getProtocol(original.toString());
       
//        if (host.equals(original.getHost()) 
//                && port == original.getPort()
//                && scheme == original.getScheme()) {
//            return original;
//        }
        //如果无法从server中获取到scheme信息，那么从URL中获取
        if (scheme == null) {
            scheme = original.getScheme();
        }
        if (scheme == null) {
            scheme = deriveSchemeAndPortFromPartialUri(original).first();
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://");
            if (!Strings.isNullOrEmpty(original.getRawUserInfo())) {
                sb.append(original.getRawUserInfo()).append("@");
            }
            sb.append(host);
            //如果URL中的协议是http协议
            if (scheme.equals(ProtocolType.HTTP.getName())) {
	            if (port >= 0)
	                sb.append(":").append(port);
            }else {
            	//如果URL的协议，是FeignX支持的协议
            	if (ProtocolUtils.containsSupportedProtocol(scheme)) {
            		Map<String, String> serverMetaData = ((DiscoveryEnabledServer)server).getInstanceInfo().getMetadata();
            		ProtocolType protocol = ProtocolUtils.getProtocol(original.toString());
            		//如果无法从目标server的MetaData中获取到port信息，那么使用支持scheme的默认port
            		port = Integer.parseInt(serverMetaData.getOrDefault(scheme+"-port", String.valueOf(protocol.getDefaultPort())));
            		sb.append(":").append(port);
            	}else {
            		throw new RuntimeException("Not supported scheme in URL:"+original);
            	}
            }
            sb.append(original.getRawPath());
            if (!Strings.isNullOrEmpty(original.getRawQuery())) {
                sb.append("?").append(original.getRawQuery());
            }
            if (!Strings.isNullOrEmpty(original.getRawFragment())) {
                sb.append("#").append(original.getRawFragment());
            }
            URI newURI = new URI(sb.toString());
            return newURI;            
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
		
	}

	public static class RibbonRequest extends ClientRequest implements Cloneable {

		private final Request request;
		private final Client client;

		public RibbonRequest(Client client, Request request, URI uri) {
			this.client = client;
			setUri(uri);
			this.request = toRequest(request);
		}

		private Request toRequest(Request request) {
			Map<String, Collection<String>> headers = new LinkedHashMap<>(
					request.headers());
			return Request.create(request.method(),getUri().toASCIIString(),headers,request.body(),request.charset());
		}

		Request toRequest() {
			return toRequest(this.request);
		}

		Client client() {
			return this.client;
		}

		HttpRequest toHttpRequest() {
			return new HttpRequest() {
				@Override
				public HttpMethod getMethod() {
					return HttpMethod.resolve(RibbonRequest.this.toRequest().method());
				}

				@Override
				public URI getURI() {
					return RibbonRequest.this.getUri();
				}

				@Override
				public HttpHeaders getHeaders() {
					Map<String, List<String>> headers = new HashMap<String, List<String>>();
					Map<String, Collection<String>> feignHeaders = RibbonRequest.this.toRequest().headers();
					for(String key : feignHeaders.keySet()) {
						headers.put(key, new ArrayList<String>(feignHeaders.get(key)));
					}
					HttpHeaders httpHeaders = new HttpHeaders();
					httpHeaders.putAll(headers);
					return httpHeaders;

				}
			};
		}


		@Override
		public Object clone() {
			return new RibbonRequest(this.client, this.request, getUri());
		}
	}

	public static class RibbonResponse implements IResponse {

		private final URI uri;
		private final Response response;

		public RibbonResponse(URI uri, Response response) {
			this.uri = uri;
			this.response = response;
		}

		@Override
		public Object getPayload() throws ClientException {
			return this.response.body();
		}

		@Override
		public boolean hasPayload() {
			return this.response.body() != null;
		}

		@Override
		public boolean isSuccess() {
			return this.response.status() == 200;
		}

		@Override
		public URI getRequestedURI() {
			return this.uri;
		}

		@Override
		public Map<String, Collection<String>> getHeaders() {
			return this.response.headers();
		}

		public Response toResponse() {
			return this.response;
		}

		@Override
		public void close() throws IOException {
			if (this.response != null && this.response.body() != null) {
				this.response.body().close();
			}
		}

	}

	
}
