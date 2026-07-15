package dev.sasser.refrigerantcontrol.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import dev.sasser.refrigerantcontrol.application.CylinderUseCases;
import dev.sasser.refrigerantcontrol.application.UsageActivityResult;
import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
import dev.sasser.refrigerantcontrol.application.port.CylinderStore;
import dev.sasser.refrigerantcontrol.application.port.UsageActivityStore;
import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryCylinderStore;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryUsageActivityStore;
import dev.sasser.refrigerantcontrol.web.HomeController;
import dev.sasser.refrigerantcontrol.web.StatusController;

@SpringBootTest
class ApplicationConfigurationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldExposeExactlyOneConfiguredBeanForEachApplicationType() {
		assertEquals(1, applicationContext.getBeansOfType(CylinderStore.class).size());
		assertEquals(1, applicationContext.getBeansOfType(UsageActivityStore.class).size());
		assertEquals(1, applicationContext.getBeansOfType(Clock.class).size());
		assertEquals(1, applicationContext.getBeansOfType(CylinderUseCases.class).size());
		assertEquals(1, applicationContext.getBeansOfType(UsageActivityUseCases.class).size());
		assertNotNull(applicationContext.getBean(HomeController.class));
		assertNotNull(applicationContext.getBean(StatusController.class));
	}

	@Test
	void shouldUseConfiguredSingletonImplementationsAndUtcClock() {
		CylinderStore cylinderStore = applicationContext.getBean(CylinderStore.class);
		UsageActivityStore usageActivityStore = applicationContext.getBean(UsageActivityStore.class);
		Clock clock = applicationContext.getBean(Clock.class);
		CylinderUseCases cylinderUseCases = applicationContext.getBean(CylinderUseCases.class);
		UsageActivityUseCases usageActivityUseCases = applicationContext.getBean(UsageActivityUseCases.class);

		assertInstanceOf(InMemoryCylinderStore.class, cylinderStore);
		assertInstanceOf(InMemoryUsageActivityStore.class, usageActivityStore);
		assertSame(cylinderStore, applicationContext.getBean(CylinderStore.class));
		assertSame(usageActivityStore, applicationContext.getBean(UsageActivityStore.class));
		assertSame(clock, applicationContext.getBean(Clock.class));
		assertSame(cylinderUseCases, applicationContext.getBean(CylinderUseCases.class));
		assertSame(usageActivityUseCases, applicationContext.getBean(UsageActivityUseCases.class));
		assertSame(cylinderStore, applicationContext.getBean("cylinderStore", CylinderStore.class));
		assertSame(usageActivityStore, applicationContext.getBean("usageActivityStore", UsageActivityStore.class));
		assertSame(clock, applicationContext.getBean("clock", Clock.class));
		assertSame(cylinderUseCases, applicationContext.getBean("cylinderUseCases", CylinderUseCases.class));
		assertSame(
				usageActivityUseCases,
				applicationContext.getBean("usageActivityUseCases", UsageActivityUseCases.class));
		assertEquals(ZoneOffset.UTC, clock.getZone());
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void shouldShareConfiguredStoresAcrossUseCases() {
		CylinderUseCases cylinderUseCases = applicationContext.getBean(CylinderUseCases.class);
		UsageActivityUseCases usageActivityUseCases = applicationContext.getBean(UsageActivityUseCases.class);

		cylinderUseCases.registerCylinder("SPRING-COMPOSITION-001", "R410A");
		cylinderUseCases.registerInitialGrossWeight(
				"SPRING-COMPOSITION-001",
				new BigDecimal("15.140"));
		UsageActivityResult activity = usageActivityUseCases.startUsageActivity(
				"SPRING-COMPOSITION-001",
				new BigDecimal("15.140"));

		assertEquals("SPRING-COMPOSITION-001", activity.sealNumber());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
	}
}
