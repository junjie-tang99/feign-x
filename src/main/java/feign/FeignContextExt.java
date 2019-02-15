package feign;

import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;


public class FeignContextExt extends NamedContextFactory<FeignClientSpecification> {

	public FeignContextExt() {
		super(FeignClientsConfigurationExt.class, "feign", "feign.client.name");
	}

}

