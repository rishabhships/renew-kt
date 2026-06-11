package com.rishabhships.renew

/**
 * Maps Google Play Billing real-time developer notifications (RTDN) into
 * Renew [SubscriptionEvent]s.
 *
 * An RTDN payload includes a `subscriptionNotification.notificationType`
 * integer that describes what happened, but the payload itself doesn't carry
 * every timestamp Renew needs. To bridge the gap, callers fetch the
 * corresponding `Subscriptions.get` payload from the Google Play Developer
 * API and pass the relevant timestamps into [toEvent].
 *
 * Example:
 * ```
 * // After receiving RTDN and calling Subscriptions.get for full details:
 * val event = RTDNAdapter.toEvent(
 *     notificationType = RTDNNotificationType.SUBSCRIPTION_RENEWED,
 *     productId = "pro_monthly",
 *     expiryEpochMs = subscriptionDetails.expiryTimeMillis,
 * )
 *
 * event?.let { stateMachine.reduce(currentState, it) }
 * ```
 *
 * Returns `null` for notification types that are informational only and don't
 * imply a state transition (e.g. price-change confirmations, pause-schedule
 * changes, deferred replacements, and pending-purchase cancellations).
 */
public object RTDNAdapter {

    /**
     * Maps a Play Billing notification type integer into a [SubscriptionEvent].
     *
     * @param notificationType One of the [RTDNNotificationType] constants.
     * @param productId The product / SKU identifier associated with this notification.
     * @param expiryEpochMs The current paid-period expiry time, fetched from
     *   Subscriptions.get. Used for purchase, renew, and resume events.
     * @param gracePeriodEndEpochMs When the grace period ends. Required for
     *   [RTDNNotificationType.SUBSCRIPTION_IN_GRACE_PERIOD].
     * @param holdEndEpochMs When the account hold ends. Required for
     *   [RTDNNotificationType.SUBSCRIPTION_ON_HOLD].
     * @param resumeEpochMs When a paused subscription will auto-resume, or `null`
     *   if the user must manually resume. Used for
     *   [RTDNNotificationType.SUBSCRIPTION_PAUSED].
     * @param eventEpochMs The wall-clock time the notification was received,
     *   used as the effective epoch for cancellation events.
     * @return The corresponding [SubscriptionEvent], or `null` if this
     *   notification type is informational only.
     */
    @Suppress("LongParameterList")
    public fun toEvent(
        notificationType: Int,
        productId: String,
        expiryEpochMs: Long = 0L,
        gracePeriodEndEpochMs: Long = 0L,
        holdEndEpochMs: Long = 0L,
        resumeEpochMs: Long? = null,
        eventEpochMs: Long = 0L,
    ): SubscriptionEvent? = when (notificationType) {
        RTDNNotificationType.SUBSCRIPTION_RECOVERED ->
            SubscriptionEvent.Resume(expiryEpochMs)

        RTDNNotificationType.SUBSCRIPTION_RENEWED ->
            SubscriptionEvent.Renew(expiryEpochMs)

        RTDNNotificationType.SUBSCRIPTION_CANCELED ->
            SubscriptionEvent.Cancel(eventEpochMs)

        RTDNNotificationType.SUBSCRIPTION_PURCHASED,
        RTDNNotificationType.SUBSCRIPTION_RESTARTED ->
            SubscriptionEvent.Purchase(productId, expiryEpochMs)

        RTDNNotificationType.SUBSCRIPTION_ON_HOLD ->
            SubscriptionEvent.EnterHold(holdEndEpochMs)

        RTDNNotificationType.SUBSCRIPTION_IN_GRACE_PERIOD ->
            SubscriptionEvent.EnterGracePeriod(gracePeriodEndEpochMs)

        RTDNNotificationType.SUBSCRIPTION_PAUSED ->
            SubscriptionEvent.Pause(resumeEpochMs)

        RTDNNotificationType.SUBSCRIPTION_REVOKED,
        RTDNNotificationType.SUBSCRIPTION_EXPIRED ->
            SubscriptionEvent.Expire

        // Informational only — no state transition implied:
        // - SUBSCRIPTION_PRICE_CHANGE_CONFIRMED (8)
        // - SUBSCRIPTION_DEFERRED (9)
        // - SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED (11)
        // - SUBSCRIPTION_PENDING_PURCHASE_CANCELED (20)
        // - any unknown / future notification type
        else -> null
    }
}
