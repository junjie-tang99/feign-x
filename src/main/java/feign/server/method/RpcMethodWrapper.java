package feign.server.method;

import java.lang.reflect.Method;

import feign.enumerate.ProtocolType;


public class RpcMethodWrapper {

	private Object target;
	
	private String className;
	
	private String methodName;
	
	private Method method;
	
	private Class<?>[] parameterTypes;
	
	private Object[] args;
	
	private Class<?> returnType;
	
	private ProtocolType[] protocol;

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Class<?> getReturnType() {
		return returnType;
	}

	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public ProtocolType[] getProtocol() {
		return protocol;
	}

	public void setProtocol(ProtocolType[] protocols) {
		this.protocol = protocols;
	}

	
}
