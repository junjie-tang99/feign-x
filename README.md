# feign-x
--------
feign-x主要是基于spring-cloud-feign，对feignClient进行了扩展，使其能够支持原生Scoket、Thrift、Dubbo方式调用其他的SpringCloud微服务。

## 使用说明
### 客户端配置
####1、添加Maven依赖
``` xml
...
	<properties>
		<feign-x.version>0.0.1-SNAPSHOT</feign-x.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>com.migu</groupId>
			<artifactId>feign-x</artifactId>
			<version>${feign-x.version}</version>
		</dependency>
	<dependencies>
...
```

####2、在Feign客户端的启动类上增加@EnableFeignClientsExt注解
``` xml
@EnableFeignClientsExt
@EnableDiscoveryClient
@SpringBootApplication
public class FeignXClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeignXClientApplication.class, args);
	}

}
```

####3、在Feign客户端的调用的Api接口上，增加@FeignClientExt注解
``` java
@FeignClientExt(name = "feign-server", protocol = ProtocolType.SOCKET,configuration=FeignClientsConfigurationExt.class)
public interface ApiServiceByRPC {
	
	@RequestMapping("feign-server?interface=com.migu.Controller2.rpcHelloWorldWithoutParams")
	public String rpcHelloWorldWithoutParams();	

	@RequestMapping("feign-server?interface=com.migu.Controller2.rpcHelloWorldWithParams")
	public String rpcHelloWorldWithParams(@RequestParam(value="name") String name);
	
}

```
在`@FeignClientExt`的`protocol`属性中，可设置Feign客户端调用后端服务时，所使用的RPC协议的类型，未设置`protocol`时，默认使用HTTP的调用方式。feign-x目前支持HTTP、原生Scoket、Thrift、Dubbo等4种调用协议。

### 服务端配置
####1、添加Maven依赖
``` xml
...
	<properties>
		<feign-x.version>0.0.1-SNAPSHOT</feign-x.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>com.migu</groupId>
			<artifactId>feign-x</artifactId>
			<version>${feign-x.version}</version>
		</dependency>
	<dependencies>
...
```

####2、在Feign服务端的启动类上增加@EnableRpcServer注解
``` xml
@EnableRpcServer
@SpringBootApplication
public class FeignXServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeignXServerApplication.class, args);
	}

}
```

####3、在需要支持RPC调用的服务端Controller接口上，增加@RpcController注解
``` java
@RpcController(protocol = {ProtocolType.SOCKET})
@RestController
public class Controller2 {

	@RequestMapping("/rpcHelloWorldWithoutParams")
	public String rpcHelloWorldWithoutParams() {
		System.out.println("Invoking rpcHelloWorldWithoutParams");
		return "Haha! I'm control2-helloworld by rpc";
	}
	
	@RequestMapping("/rpcHelloWorldWithParams")
	public String rpcHelloWorldWithParams(@RequestParam(value="name") String name) {
		System.out.println("Invoking rpcHelloWorldWithoutParams");
		return "Haha! I'm control2-"+name+" by rpc";
	}
	
}
```
在`@RpcController`的`protocol`属性中，设置当前Controllers支持哪些调用协议，`protocol`同时可设置多个调用协议。