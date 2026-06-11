package com.rishabhships.renew

/**
 * Represents the state of a Google Play Billing subscription at a point in time.
 *
 * Renew models the canonical subscription lifecycle described in the
 * [Google Play Billing documentation](https://developer.android.com/google/play/billing/subscriptions).
 * Each subclass represents a stable state; transitions between states are managed
 * by [SubscriptionStateMachine].
 */
public sealed class SubscriptionState {

    /** No subscription has ever been purchased on this account. */
    public data object NotPurchased : SubscriptionState()

    /**
     * Subscription is active and billing normally.
     *
     * @property productId Identifier of the active SKU / product.
     * @property expiryEpochMs Wall-clock time at which the current paid period expires.
     * @property autoRenew Whether the user has auto-renewal enabled.
     */
    public data class Active(
        val productId: String,
        val expiryEpochMs: Long,
        val autoRenew: Boolean,
    ) : SubscriptionState()

    /**
     * Subscription has hit a billing failure and is in the grace period.
     *
     * During this period the user retains access while Play retries billing.
     *
     * @property productId The product that is in grace.
     * @property gracePeriodEndEpochMs When the grace period expires.
     */
    public data class InGracePeriod(
        val productId: String,
        val gracePeriodEndEpochMs: Long,
    ) : SubscriptionState()

    /**
     * Subscription is in account hold — billing failed and grace ended.
     *
     * The user loses access during hold but the subscription can still be recovered.
     *
     * @property productId Product on hold.
     * @property holdEndEpochMs When the hold period expires.
     */
    public data class OnHold(
        val productId: String,
        val holdEndEpochMs: Long,
    ) : SubscriptionState()

    /**
     * User has explicitly paused the subscription.
     *
     * @property productId Product that is paused.
     * @property resumeEpochMs When the subscription is scheduled to auto-resume, or
     *   `null` if the user must manually resume.
     */
    public data class Paused(
        val productId: String,
        val resumeEpochMs: Long?,
    ) : SubscriptionState()

    /**
     * User has cancelled — subscription is still active until [expiryEpochMs],
     * but auto-renewal is implicitly off.
     */
    public data class Cancelled(
        val productId: String,
        val expiryEpochMs: Long,
    ) : SubscriptionState()

    /** Subscription has fully expired. The user previously held a subscription but no longer does. */
    public data object Expired : SubscriptionState()
}
