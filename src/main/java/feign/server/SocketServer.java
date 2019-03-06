package feign.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import feign.enumerate.ProtocolType;
import feign.packet.PpcPacketBody;
import feign.packet.RpcPacket;
import feign.server.context.RpcServerContext;
import feign.server.method.RpcMethodWrapper;


public class SocketServer implements IServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);
	int port = 0;
	//SocketServer
	ServerSocket server = null;
	//SockerServer的acceptor的线程
	Thread acceptor = null;
	//执行Socket的Worker线程池
	ExecutorService executor = Executors.newSingleThreadExecutor();
	//Server的Context信息，包括了所有需要支持处理的方法
	RpcServerContext context = null;
	
	public SocketServer(int port,RpcServerContext context) throws IOException {
		this.port = port;
		this.context = context;
		this.server = new ServerSocket(port);
	}
	
	@Override
	public boolean start() {
		//创建SocketServer的Acceptor线程
		this.acceptor = new Thread(()->{
			//如果acceptor线程的状态为中断状态，那么不执行accept请求
			while(!Thread.currentThread().isInterrupted()) {
				try {
					executor.execute(new Worker(server.accept(),this.context));
				} catch (IOException e) {
					LOGGER.error("Executor execute worker failed! " + e.getMessage());
				}
			}
			LOGGER.info("Thread " + this.acceptor.getName() + "stoped!");
		});
		this.acceptor.setName("SocketServer-Acceptor");
		this.acceptor.setDaemon(true);
		this.acceptor.start();
		LOGGER.info("Socket server started on port(s):" + this.port + " (TCP)");
		return true;
	}

	@Override
	public boolean stop() {
		if (this.acceptor != null) {
			//将acceptor线程的状态设置为中断状态
			this.acceptor.interrupt();
		}
		return true;
	}
	
	class Worker implements Runnable {
		//客户端Client
		private Socket socket;
		//服务器上下文
		private RpcServerContext context;
		
		Worker(Socket socket, RpcServerContext context){
			this.socket = socket;
			this.context = context;
		}
		
		@Override
		public void run() {
			ObjectInputStream input = null;
			ObjectOutputStream output = null;
			try {
				//从上下文中获取所有，所有支持远程调用的方法
				input = new ObjectInputStream(socket.getInputStream());
				RpcPacket requestPacket = (RpcPacket) input.readObject();
				String invokeMethodName = requestPacket.getInvokeMethod();			
				Map<String,RpcMethodWrapper> methodMapping = context.getMethodMapping();

		        //响应包体
		        RpcPacket responsePacket  = null;
		        //如果远程调用的方法，没有在rpc的methodMapping中
				if(methodMapping==null || !methodMapping.containsKey(invokeMethodName)){
					//在MethodMapping中找不到RPC的方法
					//则返回无法找到对应Mapping的错误信息
					Object result = "Can not find " + requestPacket.getInvokeMethod() + " in the RPC method mapping!";
					//设置返回的头信息
					Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
					ArrayList<String> headerValueList = new ArrayList<String>(Arrays.asList(ProtocolType.SOCKET.getName()));
					headers.put("X-RPC-CALL", headerValueList);
					//设置返回包体大小
					//headers.put("Content-Length", new ArrayList<String>(Arrays.asList());
					//生成返回的包
					PpcPacketBody body = new PpcPacketBody(result);
					responsePacket = new  RpcPacket(requestPacket.getInvokeMethod(),headers,null,body);
					
				}else {
					//获取支持远程调用方法的Wrapper
					RpcMethodWrapper wrapper = methodMapping.get(invokeMethodName);
					Method method = wrapper.getMethod();
					Class<?> returnType = wrapper.getReturnType();
					Object[] args = requestPacket.getPacketBody().getMethodArgs();
					Object result = method.invoke(wrapper.getTarget(), args);
					//设置返回的头信息
					Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
					ArrayList<String> headerValueList = new ArrayList<String>(Arrays.asList(ProtocolType.SOCKET.getName()));
					headers.put("X-RPC-CALL", headerValueList);
					//设置返回包体大小
					//headers.put("Content-Length", new ArrayList<String>(Arrays.asList());
					//生成返回的包
					PpcPacketBody body = new PpcPacketBody(result);
			        responsePacket = new  RpcPacket(requestPacket.getInvokeMethod(),headers,returnType,body);
				}
				
		        output = new ObjectOutputStream(socket.getOutputStream());
		        output.writeObject(responsePacket);
		        output.flush();
		        output.close();
		        socket.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
					try {
						if (output != null)
							output.close();
						if (input != null)
							input.close();
						if (socket != null)
							socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			
		}
	}

}
