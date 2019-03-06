package feign.client.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


import feign.Client;
import feign.Request;
import feign.Response;
import feign.packet.PpcPacketBody;
import feign.packet.RpcPacket;
import feign.util.BytesConversionUtils;
import feign.util.URLUtils;
import feign.Request.Options;

public class SocketClient implements Client {
	private String INVOKE_METHOD_KEY = "interface";
	
	private String ip;
	private int port;
	//连接的超时时间
	private int connectTimeout;
	//读取的超时时间
	private int readTimeout;
	
	private Socket socket = null;
	
	private SocketClient(String ip,int port,int connectTimeout, int readTimeout) {
		this.ip = ip;
		this.port = port;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}
	
	@Override
	public Response execute(Request request, Options options) throws IOException {
		try {
			System.out.println("Socket:" + this.socket);
			
			URI uri = new URI(request.url());
			String host = uri.getHost();
			int port = uri.getPort();
			String invokeMethodName = this.getInvokeMethod(request.url());
			//Map<String,String> params = this.getRequestParams(request.url());			
			PpcPacketBody packetBody = PpcPacketBody.class.cast(BytesConversionUtils.toObject(request.body()));
			//创建Socket的连接endpoint
			InetSocketAddress endpoint = new InetSocketAddress(host,port);
			if (socket == null || socket.isClosed() || !socket.isConnected()) { 
				this.socket = new Socket();
                this.socket.connect(endpoint);
    			//this.socket.connect(endpoint, this.connectTimeout);
				//this.socket.setSoTimeout(readTimeout);
            }

			//创建Socket的Ouput对象，用于发送请求
			ObjectOutputStream output = new ObjectOutputStream(this.socket.getOutputStream());
			//构建请求的包
			RpcPacket requestPacket = new RpcPacket(invokeMethodName,request.headers(),packetBody);
			output.writeObject(requestPacket);
			//创建Socket的Input对象，用于接受远程调用的结果
			ObjectInputStream input = new ObjectInputStream(this.socket.getInputStream()); 
			//构建返回的包
			RpcPacket responsePacket = (RpcPacket)input.readObject();
			input.close();
			output.close();
			//获取返回包体中的结果数据
			byte[] result = BytesConversionUtils.toBytes(responsePacket.getPacketBody().getResult());
			return Response.builder()
					.status(200)
					.headers(responsePacket.getHeaders())
					.body(result)
					.build();
			
		}catch(Exception e) {
			return Response.builder().status(502).headers(request.headers()).reason(e.getMessage()).build();
		}finally {
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
				return Response.builder().status(502).headers(request.headers()).reason(e.getMessage()).build();
			}
		}
	}
	
	//获取rpc协议中调用的方法
	private String getInvokeMethod(String url) {
		return URLUtils.resolveParam(url, INVOKE_METHOD_KEY);
	}

	//从请求uri中获取所有的参数
	public Map<String,String> getRequestParams(String url) {
		return URLUtils.resolveAllParams(url);
	}
    
	
	public static class Builder{
		//连接的IP地址
		private String ip = "localhost";
		//连接的端口
		private int port = 12345;
		//连接的超时时间，默认超时时间为5秒
		private int connectTimeout = 5000;
		//读取数据的超时时间，默认超时时间为5秒
		private int readTimeout = 5000;
		
		//创建SocketClient Builder的实例
		public static SocketClient.Builder create(){
			return new SocketClient.Builder();
		}
		
		//创建SocketClient的实例
		public SocketClient build() {
			return new SocketClient(this.ip,this.port,this.connectTimeout,this.readTimeout);
		}
		
		public int getConnectTimeout() {
			return connectTimeout;
		}
		public SocketClient.Builder ConnectTimeout(int connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}
		public int getReadTimeout() {
			return readTimeout;
		}
		
		public SocketClient.Builder ReadTimeout(int readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}
		public String getIP() {
			return ip;
		}
		public SocketClient.Builder IP(String ip) {
			this.ip = ip;
			return this;
		}
		public int getPort() {
			return port;
		}
		public SocketClient.Builder Port(int port) {
			this.port = port;
			return this;
		}
		
		
	}

}
