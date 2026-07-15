package dev.sasser.refrigerantcontrol.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.sasser.refrigerantcontrol.application.CylinderUseCases;
import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
import dev.sasser.refrigerantcontrol.application.port.CylinderStore;
import dev.sasser.refrigerantcontrol.application.port.UsageActivityStore;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryCylinderStore;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryUsageActivityStore;

@Configuration(proxyBeanMethods = false)
public final class ApplicationConfiguration {

	@Bean
	public CylinderStore cylinderStore() {
		return new InMemoryCylinderStore();
	}

	@Bean
	public UsageActivityStore usageActivityStore() {
		return new InMemoryUsageActivityStore();
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public CylinderUseCases cylinderUseCases(CylinderStore cylinderStore) {
		return new CylinderUseCases(cylinderStore);
	}

	@Bean
	public UsageActivityUseCases usageActivityUseCases(
			CylinderStore cylinderStore,
			UsageActivityStore usageActivityStore,
			Clock clock) {
		return new UsageActivityUseCases(cylinderStore, usageActivityStore, clock);
	}
}
