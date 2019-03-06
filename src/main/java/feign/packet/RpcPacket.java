package feign.packet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class RpcPacket implements IPacket,Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6023499201608468129L;
	//需要请求的方法名称，以全限定名的方式
	private String invokeMethod = "";
	//请求的头信息
	private Map<String, Collection<String>> headers;
	//需要请求方法的返回类型
	private Class<?> returnType = null;
	//请求方法中包体内容
	private PpcPacketBody packetBody = null;

	public RpcPacket(String invokeMethod,Map<String, Collection<String>> headers) {
		this.invokeMethod = invokeMethod;
		this.headers = headers;
	}
	
	public RpcPacket(String invokeMethod,Map<String, Collection<String>> headers,PpcPacketBody packetBody) {
		this.invokeMethod = invokeMethod;
		this.headers = headers;
		this.setPacketBody(packetBody);
	}
	
	public RpcPacket(String invokeMethod,Map<String, Collection<String>> headers,Class<?> returnType,PpcPacketBody packetBody) {
		this.invokeMethod = invokeMethod;
		this.headers = headers;
		this.returnType = returnType;
		this.setPacketBody(packetBody);
	}

	public String getInvokeMethod() {
		return invokeMethod;
	}


	public void setInvokeMethod(String invokeMethod) {
		this.invokeMethod = invokeMethod;
	}

	public PpcPacketBody getPacketBody() {
		return packetBody;
	}

	public void setPacketBody(PpcPacketBody packetBody) {
		this.packetBody = packetBody;
	}

	public Class getReturnType() {
		return returnType;
	}

	public void setReturnType(Class returnType) {
		this.returnType = returnType;
	}

	public Map<String, Collection<String>> getHeaders() {
		return headers;
	}

}