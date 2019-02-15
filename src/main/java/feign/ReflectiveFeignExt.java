package feign;

import static feign.Util.checkArgument;
import static feign.Util.checkNotNull;
import static feign.Util.checkState;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestBody;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Param.Expander;
import feign.Request.Options;
import feign.codec.Decoder;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.enumerate.ProtocolType;
import feign.packet.PpcPacketBody;
import feign.util.BytesConversionUtils;
import feign.util.ProtocolUtils;

public class ReflectiveFeignExt extends Feign  {

	  private final ParseHandlersByName targetToHandlersByName;
	  private final InvocationHandlerFactory factory;

	  ReflectiveFeignExt(ParseHandlersByName targetToHandlersByName, InvocationHandlerFactory factory) {
	    this.targetToHandlersByName = targetToHandlersByName;
	    this.factory = factory;
	  }

	  /**
	   * creates an api binding to the {@code target}. As this invokes reflection, care should be taken
	   * to cache the result.
	   */
	  @SuppressWarnings("unchecked")
	  @Override
	  public <T> T newInstance(Target<T> target) {
		//调用ParseHandlersByName的apply方法，解析apiService类上的方法，并生成一个LinkedHashMap
		//其中，map的key是：类名#方法名(参数类型)，例如：ApiServiceByRPC#rpcHelloWorldWithoutParams()，value是MethodHandler
	    Map<String, MethodHandler> nameToHandler = targetToHandlersByName.apply(target);
	    Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<Method, MethodHandler>();
	    List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<DefaultMethodHandler>();
	  	
	    for (Method method : target.type().getMethods()) {
	      if (method.getDeclaringClass() == Object.class) {
	        continue;
	      } else if(Util.isDefault(method)) {
	        DefaultMethodHandler handler = new DefaultMethodHandler(method);
	        defaultMethodHandlers.add(handler);
	        methodToHandler.put(method, handler);
	      } else {
	        methodToHandler.put(method, nameToHandler.get(Feign.configKey(target.type(), method)));
	      }
	    }
	    //通过InvocationHandlerFactory，new ReflectiveFeign.FeignInvocationHandler(target, dispatch);
	    InvocationHandler handler = factory.create(target, methodToHandler);
	    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(), new Class<?>[]{target.type()}, handler);

	    for(DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
	      defaultMethodHandler.bindTo(proxy);
	    }
	    return proxy;
	  }

	  static class FeignInvocationHandler implements InvocationHandler {

	    private final Target target;
	    private final Map<Method, MethodHandler> dispatch;

	    FeignInvocationHandler(Target target, Map<Method, MethodHandler> dispatch) {
	      this.target = checkNotNull(target, "target");
	      this.dispatch = checkNotNull(dispatch, "dispatch for %s", target);
	    }

	    @Override
	    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	      if ("equals".equals(method.getName())) {
	        try {
	          Object
	              otherHandler =
	              args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
	          return equals(otherHandler);
	        } catch (IllegalArgumentException e) {
	          return false;
	        }
	      } else if ("hashCode".equals(method.getName())) {
	        return hashCode();
	      } else if ("toString".equals(method.getName())) {
	        return toString();
	      }
	      return dispatch.get(method).invoke(args);
	    }

	    @Override
	    public boolean equals(Object obj) {
	      if (obj instanceof FeignInvocationHandler) {
	        FeignInvocationHandler other = (FeignInvocationHandler) obj;
	        return target.equals(other.target);
	      }
	      return false;
	    }

	    @Override
	    public int hashCode() {
	      return target.hashCode();
	    }

	    @Override
	    public String toString() {
	      return target.toString();
	    }
	  }

	  static final class ParseHandlersByName {

	    private final Contract contract;
	    private final Options options;
	    private final Encoder encoder;
	    private final Decoder decoder;
	    private final ErrorDecoder errorDecoder;
	    private final SynchronousMethodHandler.Factory factory;

	    ParseHandlersByName(Contract contract, Options options, Encoder encoder, Decoder decoder,
	                        ErrorDecoder errorDecoder, SynchronousMethodHandler.Factory factory) {
	      this.contract = contract;
	      this.options = options;
	      this.factory = factory;
	      this.errorDecoder = errorDecoder;
	      this.encoder = checkNotNull(encoder, "encoder");
	      this.decoder = checkNotNull(decoder, "decoder");
	    }

	    public Map<String, MethodHandler> apply(Target key) {
	    	//调用SpringMvcContractExt的parseAndValidatateMetadata方法，将方法解析成MethodMetadata
	    	//说明：由于SpringMvcContractExt是继承Contract.BaseContract，所以实际调用的是Contract.BaseContract.parseAndValidatateMetadata()方法
			List<MethodMetadata> metadata = contract.parseAndValidatateMetadata(key.type());
		  	
		    //获取当前代理对象（ApiService）上设置的Protocol
			ProtocolType protocol =  ProtocolUtils.getProtocol(key.url());
			
			Map<String, MethodHandler> result = new LinkedHashMap<String, MethodHandler>();
			for (MethodMetadata md : metadata) {
				//创建用来生成Request对象的RequestTemplate
				//通过调用RequestTemplate.create(args[])方法，可以创建实际发起请求的Request对象
				BuildTemplateByResolvingArgs buildTemplate = null;
				//如果ApiService的调用方式是RPC方式
				if (protocol == ProtocolType.SOCKET) {
					buildTemplate = new BuildRpcTemplateFromArgs(md,encoder);
				}else {
				//如果ApiService的调用方式是HTTP方式
					//1、当在@RequestMapping设置了PathVariable时（例如：@RequestMapping("/user/{id}")），
					//才会在processAnnotationsOnParameter中设置MethodMetadata.formParams
					//2、当使用原生的feign，且在Methodd的参数中设置了@Param注解时（例如：(@Param("user_name") String user, @Param("password") String password)）
					//才会在processAnnotationsOnParameter中设置MethodMetadata.formParams
					//3、当使用原生的feign，且在Method上设置了@Body注解（例如：@Body("<login \"user_name\"=\"{user_name}\" \"password\"=\"{password}\"/>")）时
					//才会在processAnnotationOnMethod中设置MethodMetadata.bodyTemplate
					if (!md.formParams().isEmpty() && md.template().bodyTemplate() == null) {
						buildTemplate = new BuildFormEncodedTemplateFromArgs(md, encoder);
					//当在Method的参数中设置了@RequestBody的情况下，例如：public String rpcHelloWorldWithParams(@RequestBody User user1);
					//才会在processAnnotationOnMethod中设置MethodMetadata.bodyIndex以及MethodMetadata.bodyType
					} else if (md.bodyIndex() != null) {
						buildTemplate = new BuildEncodedTemplateFromArgs(md, encoder);
					} else {
					//在Method的参数中未设置@RequestBody以及@PathVariable情况下
						buildTemplate = new BuildTemplateByResolvingArgs(md);
					}
				}
				result.put(md.configKey(),
					factory.create(key, md, buildTemplate, options, decoder, errorDecoder));//创建SynchronousMethodHandler对象
			}
			return result;
	    }
	  }

	  //适用于参数不在body以及form中的场景
	  private static class BuildTemplateByResolvingArgs implements RequestTemplate.Factory {

	    protected final MethodMetadata metadata;
	    private final Map<Integer, Expander> indexToExpander = new LinkedHashMap<Integer, Expander>();

	    private BuildTemplateByResolvingArgs(MethodMetadata metadata) {
	      this.metadata = metadata;
	      if (metadata.indexToExpander() != null) {
	        indexToExpander.putAll(metadata.indexToExpander());
	        return;
	      }
	      //当在使用原生Feign时，可以在@Param中指定参数的expander，例如：Result list(@Param(value = "date", expander = DateToMillis.class) Date date);
	      if (metadata.indexToExpanderClass().isEmpty()) {
	        return;
	      }
	      for (Entry<Integer, Class<? extends Expander>> indexToExpanderClass : metadata
	          .indexToExpanderClass().entrySet()) {
	        try {
	          indexToExpander
	              .put(indexToExpanderClass.getKey(), indexToExpanderClass.getValue().newInstance());
	        } catch (InstantiationException e) {
	          throw new IllegalStateException(e);
	        } catch (IllegalAccessException e) {
	          throw new IllegalStateException(e);
	        }
	      }
	    }

	    @Override
	    public RequestTemplate create(Object[] argv) {
	    	//将md中原本的RequestTemplate取出，创建新的RequestTemplate，以供修改，咋改呢？
	    	RequestTemplate mutable = new RequestTemplate(metadata.template());
	    	//在Contract.BaseContract.parseAndValidateMetadata方法中，会解析方法上的paramType
	    	//如果有参数的paramType是URI.class，那么就会在metadata.urlIndex记录参数的位置
			if (metadata.urlIndex() != null) {
				int urlIndex = metadata.urlIndex();
				checkArgument(argv[urlIndex] != null, "URI parameter %s was null", urlIndex);
				//如果存在参数的paramType是URI.class的情况，那么将方法参数中url添加到RequestTemplate.url的最前面
				//说明：不明白为啥要这样处理
				mutable.insert(0, String.valueOf(argv[urlIndex]));
			}
			
			Map<String, Object> varBuilder = new LinkedHashMap<String, Object>();
			//获取调用方法上的所有的方法名（注意：只有用@RequestParam、@RequestHeader、@PathVariable注解标识的方法名才有效）
			for (Entry<Integer, Collection<String>> entry : metadata.indexToName().entrySet()) {
				//获取当前方法名，在方法中的参数位置
				int i = entry.getKey();
				//根据参数位置，获取对应的参数值
				Object value = argv[entry.getKey()];
				if (value != null) { // Null values are skipped.
					//如果当前参数支持转换成String的话，那么将参数值转换为字符串
					if (indexToExpander.containsKey(i)) {
						value = expandElements(indexToExpander.get(i), value);
					}
					for (String name : entry.getValue()) {
						varBuilder.put(name, value);
					}
				}
			}

	      RequestTemplate template = resolve(argv, mutable, varBuilder);
	      if (metadata.queryMapIndex() != null) {
	        // add query map parameters after initial resolve so that they take
	        // precedence over any predefined values
	        template = addQueryMapQueryParameters(argv, template);
	      }

	      if (metadata.headerMapIndex() != null) {
	        template = addHeaderMapHeaders(argv, template);
	      }

	      return template;
	    }

	    private Object expandElements(Expander expander, Object value) {
	      if (value instanceof Iterable) {
	        return expandIterable(expander, (Iterable) value);
	      }
	      return expander.expand(value);
	    }

	    private List<String> expandIterable(Expander expander, Iterable value) {
	      List<String> values = new ArrayList<String>();
	      for (Object element : (Iterable) value) {
	        if (element!=null) {
	          values.add(expander.expand(element));
	        }
	      }
	      return values;
	    }

	    @SuppressWarnings("unchecked")
	    private RequestTemplate addHeaderMapHeaders(Object[] argv, RequestTemplate mutable) {
	      Map<Object, Object> headerMap = (Map<Object, Object>) argv[metadata.headerMapIndex()];
	      for (Entry<Object, Object> currEntry : headerMap.entrySet()) {
	        checkState(currEntry.getKey().getClass() == String.class, "HeaderMap key must be a String: %s", currEntry.getKey());

	        Collection<String> values = new ArrayList<String>();

	        Object currValue = currEntry.getValue();
	        if (currValue instanceof Iterable<?>) {
	          Iterator<?> iter = ((Iterable<?>) currValue).iterator();
	          while (iter.hasNext()) {
	            Object nextObject = iter.next();
	            values.add(nextObject == null ? null : nextObject.toString());
	          }
	        } else {
	          values.add(currValue == null ? null : currValue.toString());
	        }

	        mutable.header((String) currEntry.getKey(), values);
	      }
	      return mutable;
	    }

	    @SuppressWarnings("unchecked")
	    private RequestTemplate addQueryMapQueryParameters(Object[] argv, RequestTemplate mutable) {
	      Map<Object, Object> queryMap = (Map<Object, Object>) argv[metadata.queryMapIndex()];
	      for (Entry<Object, Object> currEntry : queryMap.entrySet()) {
	        checkState(currEntry.getKey().getClass() == String.class, "QueryMap key must be a String: %s", currEntry.getKey());

	        Collection<String> values = new ArrayList<String>();

	        boolean encoded = metadata.queryMapEncoded();
	        Object currValue = currEntry.getValue();
	        if (currValue instanceof Iterable<?>) {
	          Iterator<?> iter = ((Iterable<?>) currValue).iterator();
	          while (iter.hasNext()) {
	            Object nextObject = iter.next();
	            values.add(nextObject == null ? null : encoded ? nextObject.toString() : RequestTemplate.urlEncode(nextObject.toString()));
	          }
	        } else {
	          values.add(currValue == null ? null : encoded ? currValue.toString() : RequestTemplate.urlEncode(currValue.toString()));
	        }

	        mutable.query(true, encoded ? (String) currEntry.getKey() : RequestTemplate.urlEncode(currEntry.getKey()), values);
	      }
	      return mutable;
	    }

	    protected RequestTemplate resolve(Object[] argv, RequestTemplate mutable,
	                                      Map<String, Object> variables) {
	      // Resolving which variable names are already encoded using their indices
	      Map<String, Boolean> variableToEncoded = new LinkedHashMap<String, Boolean>();
	      for (Entry<Integer, Boolean> entry : metadata.indexToEncoded().entrySet()) {
	        Collection<String> names = metadata.indexToName().get(entry.getKey());
	        for (String name : names) {
	          variableToEncoded.put(name, entry.getValue());
	        }
	      }
	      return mutable.resolve(variables, variableToEncoded);
	    }
	  }

	  private static class BuildFormEncodedTemplateFromArgs extends BuildTemplateByResolvingArgs {

	    private final Encoder encoder;

	    private BuildFormEncodedTemplateFromArgs(MethodMetadata metadata, Encoder encoder) {
	      super(metadata);
	      this.encoder = encoder;
	    }

	    @Override
	    protected RequestTemplate resolve(Object[] argv, RequestTemplate mutable,
	                                      Map<String, Object> variables) {
	      Map<String, Object> formVariables = new LinkedHashMap<String, Object>();
	      for (Entry<String, Object> entry : variables.entrySet()) {
	        if (metadata.formParams().contains(entry.getKey())) {
	          formVariables.put(entry.getKey(), entry.getValue());
	        }
	      }
	      try {
	        encoder.encode(formVariables, Encoder.MAP_STRING_WILDCARD, mutable);
	      } catch (EncodeException e) {
	        throw e;
	      } catch (RuntimeException e) {
	        throw new EncodeException(e.getMessage(), e);
	      }
	      return super.resolve(argv, mutable, variables);
	    }
	  }

	  private static class BuildEncodedTemplateFromArgs extends BuildTemplateByResolvingArgs {

	    private final Encoder encoder;

	    private BuildEncodedTemplateFromArgs(MethodMetadata metadata, Encoder encoder) {
	      super(metadata);
	      this.encoder = encoder;
	    }

	    @Override
	    protected RequestTemplate resolve(Object[] argv, RequestTemplate mutable,
	                                      Map<String, Object> variables) {
	      Object body = argv[metadata.bodyIndex()];
	      checkArgument(body != null, "Body parameter %s was null", metadata.bodyIndex());
	      try {
	        encoder.encode(body, metadata.bodyType(), mutable);
	      } catch (EncodeException e) {
	        throw e;
	      } catch (RuntimeException e) {
	        throw new EncodeException(e.getMessage(), e);
	      }
	      return super.resolve(argv, mutable, variables);
	    }
	  }
	  
	  private static class BuildRpcTemplateFromArgs extends BuildTemplateByResolvingArgs {

		    private final Encoder encoder;

		    private BuildRpcTemplateFromArgs(MethodMetadata metadata, Encoder encoder) {
		      super(metadata);
		      this.encoder = encoder;
		    }
		    
		    @Override
		    public RequestTemplate create(Object[] argv) {
		    	RequestTemplate template = super.create(argv);
		    	//RequestTemplate template = new RequestTemplate(metadata.template());
		    	PpcPacketBody packetBody = new PpcPacketBody(argv);
		    	template.body(BytesConversionUtils.toBytes(packetBody),Charset.forName("UTF-8"));
		    	return template;
		    } 
	  }   

}
