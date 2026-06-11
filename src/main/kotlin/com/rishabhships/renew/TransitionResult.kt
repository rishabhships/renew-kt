package com.rishabhships.renew

/**
 * Outcome of attempting a state transition via [SubscriptionStateMachine.reduce].
 */
public sealed class TransitionResult {

    /** The transition was valid; the subscription is now in [newState]. */
    public data class Success(
        val newState: SubscriptionState,
    ) : TransitionResult()

    /**
     * The transition was invalid (e.g. attempting to renew an expired subscription).
     *
     * @property from The source state.
     * @property event The event that was rejected.
     * @property reason A human-readable explanation, useful for debugging and logs.
     */
    public data class Invalid(
        val from: SubscriptionState,
        val event: SubscriptionEvent,
        val reason: String,
    ) : TransitionResult()

    /**
     * Returns the new state if this is a [Success], otherwise throws.
     *
     * Useful in test code or when you've already independently validated the transition.
     */
    public fun require(): SubscriptionState = when (this) {
        is Success -> newState
        is Invalid -> error("Invalid transition: $reason (from=$from, event=$event)")
    }
}
