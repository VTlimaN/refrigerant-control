package dev.sasser.refrigerantcontrol.application.port;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;

public interface UsageActivityStore {

	/**
	 * Returns every pending activity as an unmodifiable collection of activities
	 * detached from stored state. Returns an empty collection when none are pending;
	 * iteration order is unspecified.
	 */
	Collection<UsageActivity> findPendingUsageActivities();

	/**
	 * Supplies all stored activities for the seal as an unmodifiable detached
	 * collection, performs synchronous domain work once, and appends only after a
	 * successful callback. The callback must not perform I/O, asynchronous work, or
	 * nested store calls.
	 */
	UsageActivity startAtomically(
			SealNumber sealNumber,
			Function<Collection<UsageActivity>, UsageActivity> startOperation);

	/**
	 * Applies synchronous completion work once to the detached pending activity and
	 * saves it only when the callback completes normally. The callback must not
	 * perform I/O, asynchronous work, or nested store calls.
	 */
	Optional<UsageActivity> completePendingAtomically(
			SealNumber sealNumber,
			Consumer<UsageActivity> completion);
}
