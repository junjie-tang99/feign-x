package feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import feign.server.RpcServerGroup;

public class RpcServerBootstrap implements SmartLifecycle {
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerBootstrap.class);
	private boolean isRunning = false;
	
	@Autowired(required = false)
	RpcServerGroup serverGroup;
	
	public RpcServerBootstrap() {
		
	}
	
	@Override
	public void start() {
		LOGGER.info("Starting RpcServer...");
		if ((serverGroup!=null) && (serverGroup.size()!=0))
			serverGroup.getServerList().stream().forEach(server->server.start());
		isRunning = true;
	}

	@Override
	public void stop() {
		LOGGER.info("Stoping RpcServer...");
		if ((serverGroup!=null) && (serverGroup.size()!=0))
			serverGroup.getServerList().stream().forEach(server->server.stop());
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		//自动启动
		return isRunning;
	}

	@Override
	public int getPhase() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		//在Bean加载完成后，自启动该SmartLifecycle
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		// TODO Auto-generated method stub
		callback.run();
		isRunning = false;
	}

}
