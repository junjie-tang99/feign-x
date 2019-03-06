package feign;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.DefaultFeignLoggerFactory;
import org.springframework.cloud.netflix.feign.FeignFormatterRegistrar;
import org.springframework.cloud.netflix.feign.FeignLoggerFactory;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringDecoder;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

import com.netflix.hystrix.HystrixCommand;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.contract.SpringMvcContractExt;
import feign.decoder.SpringDecoderExt;
import feign.hystrix.HystrixFeign;
import feign.hystrix.HystrixFeignBuilderExt;

@Configuration
public class FeignClientsConfigurationExt {
	
	@Autowired
	private ObjectFactory<HttpMessageConverters> messageConverters;

	@Autowired(required = false)
	private List<AnnotatedParameterProcessor> parameterProcessors = new ArrayList<>();

	@Autowired(required = false)
	private List<FeignFormatterRegistrar> feignFormatterRegistrars = new ArrayList<>();

	@Autowired(required = false)
	private feign.Logger feignLogger;

	private static final Logger LOGGER = LoggerFactory.getLogger(FeignAutoConfigurationExt.class); 
	
	public FeignClientsConfigurationExt() {
		LOGGER.info("Initializing feign.FeignClientsConfigurationExt configuration...");
	}
	
	@Bean
	@ConditionalOnMissingBean
	public Decoder feignDecoder() {
		LOGGER.info("Bean 'org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder' has been created.");
		//return new ResponseEntityDecoder(new SpringDecoder(this.messageConverters));
		return new ResponseEntityDecoder(new SpringDecoderExt(this.messageConverters));
	}

	@Bean
	@ConditionalOnMissingBean
	public Encoder feignEncoder() {
		LOGGER.info("Bean 'org.springframework.cloud.netflix.feign.support.SpringEncoder.SpringEncoder' has been created.");
		return new SpringEncoder(this.messageConverters);
	}

	@Bean
	@ConditionalOnMissingBean
	public Contract feignContract(ConversionService feignConversionService) {
		LOGGER.info("Bean 'feign.contract.SpringMvcContractExt' has been created.");
		return new SpringMvcContractExt(this.parameterProcessors, feignConversionService);
	}

	@Bean
	public FormattingConversionService feignConversionService() {
		LOGGER.info("Bean 'org.springframework.format.support.DefaultFormattingConversionService' has been created.");
		//创建默认的FormattingConversionService
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		for (FeignFormatterRegistrar feignFormatterRegistrar : feignFormatterRegistrars) {
			feignFormatterRegistrar.registerFormatters(conversionService);
		}
		return conversionService;
	}

	@Configuration
	@ConditionalOnClass({ HystrixCommand.class, HystrixFeign.class })
	protected static class HystrixFeignConfiguration {
		@Bean
		@Scope("prototype")
		@ConditionalOnMissingBean
		@ConditionalOnProperty(name = "feign.hystrix.enabled", matchIfMissing = false)
		public Feign.Builder feignHystrixBuilderExt() {
			//注释源代码
			//return HystrixFeign.builder();
			//扩展了rpc功能的HystrixBuilder
			LOGGER.info("Bean 'feign.hystrix.HystrixFeignBuilderExt' has been created.");
			return HystrixFeignBuilderExt.builder();
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public Retryer feignRetryer() {
		return Retryer.NEVER_RETRY;
	}
	
	
	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public Feign.Builder feignBuilderExt(Retryer retryer) {
		//注释源代码
		//return Feign.builder().retryer(retryer);
		//扩展了rpc功能的Builder
		LOGGER.info("Bean 'feign.FeignBuilderExt' has been created.");
		return FeignBuilderExt.builder().retryer(retryer);
	}

	@Bean
	@ConditionalOnMissingBean(FeignLoggerFactory.class)
	public FeignLoggerFactory feignLoggerFactory() {
		LOGGER.info("Bean 'org.springframework.cloud.netflix.feign.DefaultFeignLoggerFactory' has been created.");
		return new DefaultFeignLoggerFactory(feignLogger);
	}
	
	
}
