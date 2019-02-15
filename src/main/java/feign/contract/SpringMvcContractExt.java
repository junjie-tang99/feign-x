package feign.contract;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestParamParameterProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import feign.Contract;
import feign.Feign;
import feign.MethodMetadata;
import feign.Param;
import feign.annotation.Protocol;
import feign.enumerate.ProtocolType;

public class SpringMvcContractExt extends Contract.BaseContract implements ResourceLoaderAware {

	private static final String ACCEPT = "Accept";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
	//该Processors的类型是一个map，
	//map的key是注解的class类型，例如：PathVariable.class、RequestHeader.class、RequestParam.class
	//map的value是注解的Processor，例如：RequestParamParameterProcessor、RequestHeaderParameterProcessor、PathVariableParameterProcessor、
	private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;
	private final Map<String, Method> processedMethods = new HashMap<>();

	private final ConversionService conversionService;
	private final Param.Expander expander;
	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	public SpringMvcContractExt() {
		this(Collections.<AnnotatedParameterProcessor> emptyList());
	}

	public SpringMvcContractExt(
			List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
		this(annotatedParameterProcessors, new DefaultConversionService());
	}

	public SpringMvcContractExt(
			List<AnnotatedParameterProcessor> annotatedParameterProcessors,
			ConversionService conversionService) {
		Assert.notNull(annotatedParameterProcessors,
				"Parameter processors can not be null.");
		Assert.notNull(conversionService, "ConversionService can not be null.");

		List<AnnotatedParameterProcessor> processors;
		if (!annotatedParameterProcessors.isEmpty()) {
			//如果创建SpringMvcContractExt时，传入的annotatedParameterProcessors的list长度不为0
			//那么重新构建一个List，并保存到SpringMvcContractExt中
			processors = new ArrayList<>(annotatedParameterProcessors);
		}
		else {
			//如果创建SpringMvcContractExt时，传入的annotatedParameterProcessors的为空
			//那么创建默认的AnnotatedArgumentsProcessors，其中包括了：RequestParamParameterProcessor、RequestHeaderParameterProcessor、PathVariableParameterProcessor
			processors = getDefaultAnnotatedArgumentsProcessors();
		}
		//将包含RequestParamParameterProcessor、RequestHeaderParameterProcessor、PathVariableParameterProcessor的list转换为map
		///map的key是注解的class类型，例如：PathVariable.class、RequestHeader.class、RequestParam.class
		this.annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
		this.conversionService = conversionService;
		this.expander = new ConvertingExpander(conversionService);
	}
	
	
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		// TODO Auto-generated method stub
		this.resourceLoader = resourceLoader;
	}

	//当在Contract.BaseContract.parseAndValidatateMetadata()方法中调用parseAndValidateMetadata时，
	//会调用当前SpringMvcContractExt子类中的parseAndValidateMetadata方法
	@Override
	public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
		this.processedMethods.put(Feign.configKey(targetType, method), method);
		//调用Contract.BaseContract.parseAndValidatateMetadata()，
		//在该方法中，又会分别调用子类中的以下方法，用于解析class、method、param信息
		//1、processAnnotationOnClass，获取ApiService.class上@RequestMapping中的value，并添加到MethodMetadata中的RequestTemplate中
		//2、processAnnotationOnMethod，获取ApiService.method上@RequestMapping中的value，并添加到MethodMetadata中的RequestTemplate中
		//3、processAnnotationsOnParameter
		MethodMetadata md = super.parseAndValidateMetadata(targetType, method);

		//当@RequestMapping标记在class上，且method的@RequestMapping上未设置ACCEPT、CONTENT_TYPE信息时，才会解析producer以及consumer
		RequestMapping classAnnotation = findMergedAnnotation(targetType,
				RequestMapping.class);
		if (classAnnotation != null) {
			// produces - use from class annotation only if method has not specified this
			if (!md.template().headers().containsKey(ACCEPT)) {
				parseProduces(md, method, classAnnotation);
			}

			// consumes -- use from class annotation only if method has not specified this
			if (!md.template().headers().containsKey(CONTENT_TYPE)) {
				parseConsumes(md, method, classAnnotation);
			}

			// headers -- class annotation is inherited to methods, always write these if
			// present
			parseHeaders(md, method, classAnnotation);
		}
		return md;
	}
	
	@Override
	protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
		//获取指定Class上所有实现的接口
		if (clz.getInterfaces().length == 0) {
			//解析ApiService.class上的@RequestMapping注解
			RequestMapping classAnnotation = findMergedAnnotation(clz,RequestMapping.class);
			if (classAnnotation != null) {
				//判断@RequestMapping上，是否指定了Value参数
				if (classAnnotation.value().length > 0) {
					//获取@RequestMapping Value参数上的第一个值
					String pathValue = emptyToNull(classAnnotation.value()[0]);
					pathValue = resolve(pathValue);
					if (!pathValue.startsWith("/")) {
						pathValue = "/" + pathValue;
					}
					//将@RequestMapping中的Path参数设置到MethodMetadata.RequestTemplate.url属性中去
					data.template().insert(0, pathValue);
				}
			}
		}
	}

	@Override
	protected void processAnnotationOnMethod(MethodMetadata data, Annotation methodAnnotation, Method method) {
		//处理@RequestMapping的注解
		if (RequestMapping.class.isInstance(methodAnnotation)) {
			RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
			//获取注解中的HTTP Method属性
			RequestMethod[] methods = methodMapping.method();
			//如果未在注解中设置Method属性，那么直接设置为Get方式
			if (methods.length == 0) {
				methods = new RequestMethod[] { RequestMethod.GET };
			}
			checkOne(method, methods, "method");
			data.template().method(methods[0].name());

			//处理@RequestMapping中设置的Path参数
			checkAtMostOne(method, methodMapping.value(), "value");
			if (methodMapping.value().length > 0) {
				String pathValue = emptyToNull(methodMapping.value()[0]);
				if (pathValue != null) {
					pathValue = resolve(pathValue);
					// Append path from @RequestMapping if value is present on method
					if (!pathValue.startsWith("/")
							&& !data.template().toString().endsWith("/")) {
						pathValue = "/" + pathValue;
					}
					//在MethodMetadata.template().append方法中，会执行以下2个操作
					//1、把方法上设置的@RequestMapping注解中的Path参数append到MethodMetadata.template().url之后
					//2、解析@RequestMapping注解中的Path数据，例如：feign-server?interface=com.migu.Controller2.rpcHelloWorldWithParams
					//并将其中的url参数（interface=com.migu.Controller2.rpcHelloWorldWithParams），设置到MethodMetadata.template().queries中
					data.template().append(pathValue);
				}
			}

			// 解析@RequestMapping中的produces属性，并将属性值设置到requestTemplate.header.Accept中，例如application/json, text/html
			parseProduces(data, method, methodMapping);

			// 解析@RequestMapping中，consumes属性，并将属性值设置到requestTemplate.header.Content-Type中，例如application/json, text/html
			parseConsumes(data, method, methodMapping);

			// 解析@RequestMapping中，headers属性，并将属性值设置到requestTemplate.header中
			parseHeaders(data, method, methodMapping);

			data.indexToExpander(new LinkedHashMap<Integer, Param.Expander>());
		}else {
			return;
		}
	}

	@Override
	protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
		// TODO Auto-generated method stub
		boolean isHttpAnnotation = false;
		//创建注解参数的Context，用来保存参数信息，该参数主要用来保存MethodMetadata对象
		AnnotatedParameterProcessor.AnnotatedParameterContext context = new SimpleAnnotatedParameterContext(data, paramIndex);
		Method method = this.processedMethods.get(data.configKey());
		//获取某个参数上的所有注解
		for (Annotation parameterAnnotation : annotations) {
			//根据参数上的注解类型，获取对应的AnnotatedParameterProcessor
			//支持处理的注解类型包括：PathVariable.class、RequestHeader.class、RequestParam.class
			AnnotatedParameterProcessor processor = this.annotatedArgumentProcessors
					.get(parameterAnnotation.annotationType());
			if (processor != null) {
				Annotation processParameterAnnotation;
				// synthesize, handling @AliasFor, while falling back to parameter name on
				// missing String #value():
				processParameterAnnotation = synthesizeWithMethodParameterNameAsFallbackValue(
						parameterAnnotation, method, paramIndex);
				//核心功能就是：
				//1、将@RequestParam注解中设置的value作为参数名添加到MethodMetadata.RequestTemplate.queries中
				//特别说明：
				//（1）MethodMetadata.RequestTemplate.queries的结构是Map，所以在将@RequestParam中的value添加到map中的时候
				//map的key为@RequestParam中的value值，设置的格式为{value}
				//（2）如果在方法的@RequestMapping中设置的url变量（例如：@RequestMapping("/test?name=tangjunjie")），与@RequestParam中value设置的属性名一致，
				//那么在设置queries的值时，会将@RequestMapping中设置的url变量值，与@RequestParam中value合并成一个String数组，例如：name=["tangjunjie","{name}"]
				//2、将@PathVariable注解中设置的value作为参数名，添加到MethodMetadata.formParams中
				//特别说明：
				//（1）@PathVariable注解中设置的value，必须不在@RequestMapping中设置的url中，且不在@RequestParam的value中，且不在@RequestMapping的headers属性中
				isHttpAnnotation |= processor.processArgument(context,
						processParameterAnnotation, method);
			}
		}
		//如果参数注解的类型是Http的类型，并且未对该参数设置过expander，并且该参数能够转换为String
		if (isHttpAnnotation && data.indexToExpander().get(paramIndex) == null
				&& this.conversionService.canConvert(
						method.getParameterTypes()[paramIndex], String.class)) {
			//在当前参数对应的index上，设置对应的expander，其实这个expander就是对conversionService的封装
			data.indexToExpander().put(paramIndex, this.expander);
		}
		return isHttpAnnotation;
	}
	
	private void parseProduces(MethodMetadata md, Method method,
			RequestMapping annotation) {
		checkAtMostOne(method, annotation.produces(), "produces");
		String[] serverProduces = annotation.produces();
		String clientAccepts = serverProduces.length == 0 ? null
				: emptyToNull(serverProduces[0]);
		if (clientAccepts != null) {
			md.template().header(ACCEPT, clientAccepts);
		}
	}
	
	private void parseConsumes(MethodMetadata md, Method method,
			RequestMapping annotation) {
		checkAtMostOne(method, annotation.consumes(), "consumes");
		String[] serverConsumes = annotation.consumes();
		String clientProduces = serverConsumes.length == 0 ? null
				: emptyToNull(serverConsumes[0]);
		if (clientProduces != null) {
			md.template().header(CONTENT_TYPE, clientProduces);
		}
	}	
	
	private void parseHeaders(MethodMetadata md, Method method,
			RequestMapping annotation) {
		// TODO: only supports one header value per key
		if (annotation.headers() != null && annotation.headers().length > 0) {
			for (String header : annotation.headers()) {
				int index = header.indexOf('=');
				if (!header.contains("!=") && index >= 0) {
					md.template().header(resolve(header.substring(0, index)),
						resolve(header.substring(index + 1).trim()));
				}
			}
		}
	}	
	
	private String resolve(String value) {
		if (StringUtils.hasText(value)
				&& this.resourceLoader instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext) this.resourceLoader).getEnvironment()
					.resolvePlaceholders(value);
		}
		return value;
	}

	private void checkAtMostOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && (values.length == 0 || values.length == 1),
				"Method %s can only contain at most 1 %s field. Found: %s",
				method.getName(), fieldName,
				values == null ? null : Arrays.asList(values));
	}

	private void checkOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && values.length == 1,
				"Method %s can only contain 1 %s field. Found: %s", method.getName(),
				fieldName, values == null ? null : Arrays.asList(values));
	}
	
	private List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {

		List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

		annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
		annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
		annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

		return annotatedArgumentResolvers;
	}
	
	private Map<Class<? extends Annotation>, AnnotatedParameterProcessor> toAnnotatedArgumentProcessorMap(
			List<AnnotatedParameterProcessor> processors) {
		Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
		for (AnnotatedParameterProcessor processor : processors) {
			result.put(processor.getAnnotationType(), processor);
		}
		return result;
	}
	
	private Annotation synthesizeWithMethodParameterNameAsFallbackValue(
			Annotation parameterAnnotation, Method method, int parameterIndex) {
		Map<String, Object> annotationAttributes = AnnotationUtils
				.getAnnotationAttributes(parameterAnnotation);
		Object defaultValue = AnnotationUtils.getDefaultValue(parameterAnnotation);
		if (defaultValue instanceof String
				&& defaultValue.equals(annotationAttributes.get(AnnotationUtils.VALUE))) {
			Type[] parameterTypes = method.getGenericParameterTypes();
			String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
			if (shouldAddParameterName(parameterIndex, parameterTypes, parameterNames)) {
				annotationAttributes.put(AnnotationUtils.VALUE,
						parameterNames[parameterIndex]);
			}
		}
		return AnnotationUtils.synthesizeAnnotation(annotationAttributes,
				parameterAnnotation.annotationType(), null);
	}	
	
	private boolean shouldAddParameterName(int parameterIndex, Type[] parameterTypes, String[] parameterNames) {
		// has a parameter name
		return parameterNames != null && parameterNames.length > parameterIndex
				// has a type
				&& parameterTypes != null && parameterTypes.length > parameterIndex;
	}	
	
	private class SimpleAnnotatedParameterContext implements AnnotatedParameterProcessor.AnnotatedParameterContext {

		private final MethodMetadata methodMetadata;
		
		private final int parameterIndex;
		
		public SimpleAnnotatedParameterContext(MethodMetadata methodMetadata,
				int parameterIndex) {
			this.methodMetadata = methodMetadata;
			this.parameterIndex = parameterIndex;
		}
		
		@Override
		public MethodMetadata getMethodMetadata() {
			return this.methodMetadata;
		}
		
		@Override
		public int getParameterIndex() {
			return this.parameterIndex;
		}
		
		@Override
		public void setParameterName(String name) {
			nameParam(this.methodMetadata, name, this.parameterIndex);
		}
		
		@Override
		public Collection<String> setTemplateParameter(String name,
				Collection<String> rest) {
			return addTemplatedParam(rest, name);
		}
	}
		
	public static class ConvertingExpander implements Param.Expander {
		
		private final ConversionService conversionService;
		
		public ConvertingExpander(ConversionService conversionService) {
			this.conversionService = conversionService;
		}
		
		@Override
		public String expand(Object value) {
			return this.conversionService.convert(value, String.class);
		}
	
	}

}
