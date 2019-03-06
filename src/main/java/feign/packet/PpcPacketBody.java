package feign.packet;

import java.io.Serializable;

public class PpcPacketBody implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9210058445686656268L;

	//请求方法中的参数值
	private Object[] methodArgs;
	
	//方法返回值
	private Object result;

	public PpcPacketBody(Object[] methodArgs) {
		this.methodArgs = methodArgs;
	}
	
	public PpcPacketBody(Object resutl) {
		this.result = resutl;
	}
	
	public Object[] getMethodArgs() {
		return this.methodArgs;
	}

	public void setMethodArgs(Object[] methodArgs) {
		this.methodArgs = methodArgs;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	

}
