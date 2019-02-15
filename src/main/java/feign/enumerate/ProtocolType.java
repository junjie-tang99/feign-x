package feign.enumerate;

public enum ProtocolType {
	HTTP("http",8080),DUBBO("dubbo",20880),THRIFT("thrift",8080),SOCKET("socket",12345);
	
	private String name;
	private int defaultPort;
	
	private ProtocolType(String name,int defaultPort) {
		this.name = name;
		this.setDefaultPort(defaultPort);
	}
	// get set 方法
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
	public int getDefaultPort() {
		return defaultPort;
	}
	public void setDefaultPort(int defaultPort) {
		this.defaultPort = defaultPort;
	}
}
