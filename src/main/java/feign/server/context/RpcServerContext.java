package feign.server.context;

import java.util.HashMap;
import java.util.Map;

import feign.server.method.RpcMethodWrapper;


public class RpcServerContext {

	private Map<String,RpcMethodWrapper> methodMapping;

	public RpcServerContext() {
	}
	
	public Map<String,RpcMethodWrapper> getMethodMapping() {
		return this.methodMapping;
	}

	//往MethodMapping中新增wrapper对象
	public void appendMethodMapping(RpcMethodWrapper wrapper) {
		if (methodMapping == null)
			methodMapping = new HashMap<String,RpcMethodWrapper>();
		methodMapping.put(wrapper.getClassName()+"."+wrapper.getMethodName(), wrapper);
		
	}
	
}
