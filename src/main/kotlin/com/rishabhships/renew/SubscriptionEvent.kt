package com.rishabhships.renew

/**
 * Discrete events that can cause a [SubscriptionState] transition.
 *
 * Events typically correspond to Play Billing real-time developer notifications,
 * client-side purchase callbacks, or scheduled timer triggers (e.g. grace period end).
 */
public sealed class SubscriptionEvent {

    /** A user purchases a subscription for the first time (or resubscribes after expiry/cancellation). */
    public data class Purchase(
        val productId: String,
        val expiryEpochMs: Long,
    ) : SubscriptionEvent()

    /** A renewal succeeded — extend the current period. */
    public data class Renew(
        val newExpiryEpochMs: Long,
    ) : SubscriptionEvent()

    /** User initiates a cancellation. The subscription remains active until expiry. */
    public data class Cancel(
        val effectiveEpochMs: Long,
    ) : SubscriptionEvent()

    /** User upgrades to a higher-tier product, taking effect immediately. */
    public data class Upgrade(
        val newProductId: String,
        val newExpiryEpochMs: Long,
    ) : SubscriptionEvent()

    /** User downgrades. Takes effect at the next renewal boundary, so current state is unchanged. */
    public data class Downgrade(
        val newProductId: String,
    ) : SubscriptionEvent()

    /**
     * User cross-grades — switches to a different product at the same or similar tier.
     * Behaviour depends on [prorationMode].
     */
    public data class CrossGrade(
        val newProductId: String,
        val prorationMode: ProrationMode,
        val newExpiryEpochMs: Long,
    ) : SubscriptionEvent()

    /** Billing failed; enter grace period. */
    public data class EnterGracePeriod(
        val gracePeriodEndEpochMs: Long,
    ) : SubscriptionEvent()

    /** Grace period ended without resolution; enter account hold. */
    public data class EnterHold(
        val holdEndEpochMs: Long,
    ) : SubscriptionEvent()

    /** User pauses the subscription. */
    public data class Pause(
        val resumeEpochMs: Long?,
    ) : SubscriptionEvent()

    /** User resumes from a paused or held state (or billing recovered from hold). */
    public data class Resume(
        val newExpiryEpochMs: Long,
    ) : SubscriptionEvent()

    /** Subscription fully expired (e.g. after hold expires, or after cancelled period ends). */
    public data object Expire : SubscriptionEvent()
}
