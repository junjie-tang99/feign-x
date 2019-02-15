package feign;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import feign.annotation.RpcController;
import feign.enumerate.ProtocolType;
import feign.server.RpcServerGroup;
import feign.server.SocketServer;
import feign.server.context.RpcServerContext;
import feign.server.method.RpcMethodWrapper;

@Configuration
@ConditionalOnProperty(value = "spring.application.name", matchIfMissing = false)
public class RpcServerAutoConfiguration implements ApplicationContextAware,EnvironmentAware {
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerAutoConfiguration.class);
	
	private ApplicationContext applicationContext;
	private Environment environment;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	
	@Bean
	@ConditionalOnMissingBean
	public RpcServerGroup getRpcServerGroup() throws IOException {
		RpcServerGroup serverGroup = new RpcServerGroup();
		int socketPort = environment.getProperty("eureka.instance.metadata-map.socket-port", Integer.class, ProtocolType.SOCKET.getDefaultPort());
		int dubboPort = environment.getProperty("eureka.instance.metadata-map.socket-port", Integer.class, ProtocolType.DUBBO.getDefaultPort());
		int thriftPort = environment.getProperty("eureka.instance.metadata-map.socket-port", Integer.class, ProtocolType.THRIFT.getDefaultPort());
		Map<ProtocolType,RpcServerContext> contextMap = getRpcServerContext();
		
		if (contextMap.containsKey(ProtocolType.SOCKET))
			serverGroup.addServer(new SocketServer(socketPort,contextMap.get(ProtocolType.SOCKET)));
		return serverGroup;
	}
	
	//从Spring容器中获取所有使用@RpcController注解标记的类，
	//并解析其中的方法，并将方法转换成RpcMethodWrapper
	public Map<ProtocolType,RpcServerContext> getRpcServerContext() {
		Map<ProtocolType,RpcServerContext> contextMap = new HashMap<ProtocolType,RpcServerContext>();
		String[] beanNames = applicationContext.getBeanNamesForAnnotation(RpcController.class);
		if (beanNames.length == 0) {
		    LOGGER.error("Can't search any rpc controller annotated with @RpcController");
		    return null;
		}
		
		Arrays.stream(beanNames)
			.flatMap((Function<String, Stream<RpcMethodWrapper>>) beanName-> {
				//方法的映射，key是方法的全限定名，value是methodWrapper
				List<RpcMethodWrapper> rpcMethodrapperList = new ArrayList<RpcMethodWrapper>();
				Object bean = applicationContext.getBean(beanName);
				RpcController controller = bean.getClass().getAnnotation(RpcController.class);
				//获取设置在@RpcController注解上，用来表示支持哪些协议
				ProtocolType[] supportProtocols = controller.protocol();
				//获取Controller下的所有方法信息，并创建methoMrapper对象
				Method[] methods = bean.getClass().getDeclaredMethods();
				for(Method method : methods) {
					RpcMethodWrapper wrapper =  new RpcMethodWrapper();
					wrapper.setClassName(bean.getClass().getName());
					wrapper.setMethodName(method.getName());
					wrapper.setMethod(method);
					wrapper.setParameterTypes(method.getParameterTypes());
					wrapper.setReturnType(method.getReturnType());
					wrapper.setTarget(bean);
					wrapper.setProtocol(supportProtocols);
					rpcMethodrapperList.add(wrapper);
					LOGGER.info("Mapped RPC Service [" + wrapper.getClassName() + "." + wrapper.getMethodName() + "] ");
				}
				return rpcMethodrapperList.stream();
			})
			.forEach(wrapper->{
				//获取Controller支持的所有协议类型
				ProtocolType[] protocols = wrapper.getProtocol();
				if (protocols != null && protocols.length>=1) {
					Arrays.stream(protocols).forEach(protocol->{
						//从ServerContext的map中，获取对应协议的ServerContext
						RpcServerContext serverContext = contextMap.get(protocol);
						if (serverContext == null)
							serverContext = new RpcServerContext();
						serverContext.appendMethodMapping(wrapper);
						contextMap.put(protocol, serverContext);
					});
				}
			});	
		return contextMap;
	}

	@Bean
	@ConditionalOnMissingBean
	public RpcServerBootstrap getRpcServerBootstrap() {
		return new RpcServerBootstrap();
	} 

	
}
