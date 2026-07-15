package dev.sasser.refrigerantcontrol.infrastructure.memory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.Weight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCylinderStoreTest {

	private static final SealNumber SEAL_NUMBER = SealNumber.of("LACRE-001");

	@Test
	void shouldStoreAndReturnFreshDetachedCylinders() {
		InMemoryCylinderStore store = new InMemoryCylinderStore();
		Cylinder original = cylinder("LACRE-001", "R410A");

		assertTrue(store.addIfAbsent(original));
		Cylinder firstRead = store.findBySealNumber(SEAL_NUMBER).orElseThrow();
		Cylinder secondRead = store.findBySealNumber(SEAL_NUMBER).orElseThrow();

		assertNotSame(original, firstRead);
		assertNotSame(firstRead, secondRead);
		firstRead.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));
		assertTrue(store.findBySealNumber(SEAL_NUMBER).orElseThrow().initialGrossWeight().isEmpty());
	}

	@Test
	void shouldAtomicallyRejectDuplicateAndPreserveOriginalCylinder() {
		InMemoryCylinderStore store = new InMemoryCylinderStore();

		assertTrue(store.addIfAbsent(cylinder("LACRE-001", "R410A")));
		assertFalse(store.addIfAbsent(cylinder("LACRE-001", "R32")));
		assertEquals(
				"R410A",
				store.findBySealNumber(SEAL_NUMBER).orElseThrow().refrigerantGas().operationalName());
	}

	@Test
	void shouldInvokeUpdateOnceAndReturnFreshDetachedCylinder() {
		InMemoryCylinderStore store = new InMemoryCylinderStore();
		store.addIfAbsent(cylinder("LACRE-001", "R410A"));
		AtomicInteger calls = new AtomicInteger();
		AtomicReference<Cylinder> callbackCylinder = new AtomicReference<>();

		Cylinder returned = store.updateAtomically(SEAL_NUMBER, cylinder -> {
			calls.incrementAndGet();
			callbackCylinder.set(cylinder);
			cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("16.140")));
		}).orElseThrow();

		Cylinder stored = store.findBySealNumber(SEAL_NUMBER).orElseThrow();
		assertEquals(1, calls.get());
		assertNotSame(callbackCylinder.get(), returned);
		assertNotSame(returned, stored);
		assertNotSame(callbackCylinder.get(), stored);
		assertEquals(new BigDecimal("16.140"), returned.initialGrossWeight().orElseThrow().inKilograms());
		assertEquals(new BigDecimal("16.140"), stored.initialGrossWeight().orElseThrow().inKilograms());
	}

	@Test
	void shouldNotPersistLaterMutationOfReturnedUpdateValue() {
		InMemoryCylinderStore store = new InMemoryCylinderStore();
		store.addIfAbsent(cylinder("LACRE-001", "R410A"));

		Cylinder returned = store.updateAtomically(SEAL_NUMBER, cylinder -> {
		}).orElseThrow();
		returned.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));

		assertTrue(store.findBySealNumber(SEAL_NUMBER).orElseThrow().initialGrossWeight().isEmpty());
	}

	@Test
	void shouldPropagateUpdateFailureUnchangedWithoutSaving() {
		InMemoryCylinderStore store = new InMemoryCylinderStore();
		store.addIfAbsent(cylinder("LACRE-001", "R410A"));
		AtomicInteger calls = new AtomicInteger();
		RuntimeException failure = new RuntimeException("callback failure");

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> store.updateAtomically(
				SEAL_NUMBER,
				cylinder -> {
					calls.incrementAndGet();
					cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));
					throw failure;
				}));

		assertSame(failure, thrown);
		assertEquals(1, calls.get());
		assertTrue(store.findBySealNumber(SEAL_NUMBER).orElseThrow().initialGrossWeight().isEmpty());
	}

	@Test
	void shouldRejectNullArgumentsAndSkipCallbackForMissingCylinder() {
		InMemoryCylinderStore store = new InMemoryCylinderStore();
		AtomicInteger calls = new AtomicInteger();

		assertThrows(NullPointerException.class, () -> store.findBySealNumber(null));
		assertThrows(NullPointerException.class, () -> store.addIfAbsent(null));
		assertThrows(NullPointerException.class, () -> store.updateAtomically(SEAL_NUMBER, null));
		Optional<Cylinder> result = store.updateAtomically(SEAL_NUMBER, cylinder -> calls.incrementAndGet());
		assertTrue(result.isEmpty());
		assertEquals(0, calls.get());
	}

	@Test
	void shouldAllowOnlyOneConcurrentDuplicateRegistration() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			InMemoryCylinderStore store = new InMemoryCylinderStore();
			int callers = 8;
			ExecutorService executor = Executors.newFixedThreadPool(callers);
			CountDownLatch ready = new CountDownLatch(callers);
			CountDownLatch start = new CountDownLatch(1);
			try {
				List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, callers)
						.mapToObj(index -> executor.submit(() -> {
							ready.countDown();
							if (!start.await(5, TimeUnit.SECONDS)) {
								throw new IllegalStateException("concurrent registration was not released");
							}
							return store.addIfAbsent(cylinder("LACRE-001", "R410A"));
						}))
						.toList();
				assertTrue(ready.await(5, TimeUnit.SECONDS));
				start.countDown();
				long successes = 0;
				for (Future<Boolean> future : futures) {
					if (future.get(5, TimeUnit.SECONDS)) {
						successes++;
					}
				}
				assertEquals(1, successes);
			} finally {
				start.countDown();
				executor.shutdownNow();
				assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			}
		});
	}

	@Test
	void shouldSerializeConcurrentInitialWeightRegistrations() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			InMemoryCylinderStore store = new InMemoryCylinderStore();
			store.addIfAbsent(cylinder("LACRE-001", "R410A"));
			ExecutorService executor = Executors.newFixedThreadPool(2);
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch start = new CountDownLatch(1);
			try {
				List<BigDecimal> weights = List.of(new BigDecimal("16.00"), new BigDecimal("17.00"));
				List<Future<Boolean>> futures = weights.stream()
						.map(weight -> executor.submit(() -> {
							ready.countDown();
							if (!start.await(5, TimeUnit.SECONDS)) {
								throw new IllegalStateException("concurrent update was not released");
							}
							try {
								store.updateAtomically(
										SEAL_NUMBER,
										cylinder -> cylinder.registerInitialGrossWeight(Weight.of(weight)));
								return true;
							} catch (IllegalStateException exception) {
								return false;
							}
						}))
						.toList();
				assertTrue(ready.await(5, TimeUnit.SECONDS));
				start.countDown();
				int successes = 0;
				for (Future<Boolean> future : futures) {
					if (future.get(5, TimeUnit.SECONDS)) {
						successes++;
					}
				}
				assertEquals(1, successes);
				BigDecimal storedWeight = store.findBySealNumber(SEAL_NUMBER)
						.orElseThrow()
						.initialGrossWeight()
						.orElseThrow()
						.inKilograms();
				assertTrue(weights.contains(storedWeight));
			} finally {
				start.countDown();
				executor.shutdownNow();
				assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			}
		});
	}

	@Test
	void shouldIsolateAdapterInstances() {
		InMemoryCylinderStore firstStore = new InMemoryCylinderStore();
		InMemoryCylinderStore secondStore = new InMemoryCylinderStore();

		firstStore.addIfAbsent(cylinder("LACRE-001", "R410A"));

		assertTrue(firstStore.findBySealNumber(SEAL_NUMBER).isPresent());
		assertTrue(secondStore.findBySealNumber(SEAL_NUMBER).isEmpty());
	}

	private static Cylinder cylinder(String sealNumber, String refrigerantName) {
		return Cylinder.register(SealNumber.of(sealNumber), RefrigerantGas.of(refrigerantName));
	}
}
