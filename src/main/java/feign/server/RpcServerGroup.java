package feign.server;

import java.util.ArrayList;
import java.util.List;

public class RpcServerGroup {
	private List<IServer> rpcServerList = new ArrayList<IServer>();
	
	//将服务器添加到需要启动的RPC服务器列表中
	public void addServer(IServer server) {
		this.rpcServerList.add(server);
	}
	
	//获取所有需要启动的RPC服务器
	public List<IServer> getServerList(){
		return this.rpcServerList;
	}
	
	//获取所有服务器的数量
	public int size() {
		return this.rpcServerList.size();
	}
}
