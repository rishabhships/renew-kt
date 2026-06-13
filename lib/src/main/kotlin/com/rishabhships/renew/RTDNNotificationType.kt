package com.rishabhships.renew

/**
 * Google Play Billing real-time developer notification (RTDN) types for
 * subscription notifications.
 *
 * These constants mirror the integer values published in the
 * `subscriptionNotification.notificationType` field of an RTDN payload.
 *
 * See the canonical reference:
 * https://developer.android.com/google/play/billing/rtdn-reference
 */
public object RTDNNotificationType {

    /** A subscription has been recovered from account hold. */
    public const val SUBSCRIPTION_RECOVERED: Int = 1

    /** An active subscription was renewed. */
    public const val SUBSCRIPTION_RENEWED: Int = 2

    /** A subscription was either voluntarily or involuntarily cancelled. */
    public const val SUBSCRIPTION_CANCELED: Int = 3

    /** A new subscription was purchased. */
    public const val SUBSCRIPTION_PURCHASED: Int = 4

    /** A subscription has entered account hold (if enabled). */
    public const val SUBSCRIPTION_ON_HOLD: Int = 5

    /** A subscription has entered the grace period (if enabled). */
    public const val SUBSCRIPTION_IN_GRACE_PERIOD: Int = 6

    /** A user has restarted a cancelled subscription before it expired. */
    public const val SUBSCRIPTION_RESTARTED: Int = 7

    /** A subscription price change has been confirmed by the user. */
    public const val SUBSCRIPTION_PRICE_CHANGE_CONFIRMED: Int = 8

    /** A subscription's recurrence has been deferred. */
    public const val SUBSCRIPTION_DEFERRED: Int = 9

    /** A subscription has been paused. */
    public const val SUBSCRIPTION_PAUSED: Int = 10

    /** A subscription's pause schedule has been changed. */
    public const val SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED: Int = 11

    /** A subscription has been revoked by Google or the developer. */
    public const val SUBSCRIPTION_REVOKED: Int = 12

    /** A subscription has fully expired. */
    public const val SUBSCRIPTION_EXPIRED: Int = 13

    /** A pending purchase was cancelled by the user before completion. */
    public const val SUBSCRIPTION_PENDING_PURCHASE_CANCELED: Int = 20
}
