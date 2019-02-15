package feign.packet;

import java.io.Serializable;
import java.util.Map;

public class RpcPacket implements IPacket,Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6023499201608468129L;
	//需要请求的方法名称，以全限定名的方式
	private String invokeMethod = "";
	//请求方法中包体内容
	private PpcPacketBody packetBody = null;
	//请求方法的返回值
	private Object result = null;

	public RpcPacket(String invokeMethod) {
		this.invokeMethod = invokeMethod;
	}
	
	public RpcPacket(String invokeMethod,PpcPacketBody packetBody) {
		this.invokeMethod = invokeMethod;
		this.setPacketBody(packetBody);
	}
	
	public RpcPacket(String invokeMethod,PpcPacketBody packetBody,Object result) {
		this.invokeMethod = invokeMethod;
		this.setPacketBody(packetBody);
		this.result = result;
	}

	public String getInvokeMethod() {
		return invokeMethod;
	}


	public void setInvokeMethod(String invokeMethod) {
		this.invokeMethod = invokeMethod;
	}


	public Object getResult() {
		return result;
	}


	public void setResult(Object result) {
		this.result = result;
	}

	public PpcPacketBody getPacketBody() {
		return packetBody;
	}

	public void setPacketBody(PpcPacketBody packetBody) {
		this.packetBody = packetBody;
	}
}