package feign.packet;

import java.io.Serializable;

public class PpcPacketBody implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9210058445686656268L;

	//请求方法中的参数值
	private Object[] methodArgs;

	public PpcPacketBody(Object[] methodArgs) {
		this.methodArgs = methodArgs;
	}
	
	public Object[] getMethodArgs() {
		return this.methodArgs;
	}

	public void setMethodArgs(Object[] methodArgs) {
		this.methodArgs = methodArgs;
	}

	

}
