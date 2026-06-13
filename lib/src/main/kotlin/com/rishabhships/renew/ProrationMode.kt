package com.rishabhships.renew

/**
 * Proration modes mirroring Play Billing's
 * [ReplacementMode](https://developer.android.com/reference/com/android/billingclient/api/SubscriptionUpdateParams.ReplacementMode)
 * options for cross-grade / replacement scenarios.
 */
public enum class ProrationMode {

    /** Immediate replacement; remaining time on the old product is credited towards the new product. */
    ChargeProratedPrice,

    /** Immediate replacement at full new-product price; remaining time on the old product is lost. */
    ChargeFullPrice,

    /** Replacement deferred to the next renewal boundary. */
    Deferred,

    /** Immediate replacement; the user is not charged until the current paid period ends. */
    WithoutProration,
}
