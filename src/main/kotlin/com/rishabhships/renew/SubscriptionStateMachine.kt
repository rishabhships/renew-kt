package com.rishabhships.renew

/**
 * Reduces [SubscriptionEvent]s against a [SubscriptionState] to produce a new state.
 *
 * Encodes the Google Play Billing subscription lifecycle as a deterministic state
 * machine. Invalid transitions are rejected with descriptive reasons instead of
 * silently failing.
 *
 * Example:
 * ```
 * val machine = SubscriptionStateMachine()
 * val result = machine.reduce(
 *     state = SubscriptionState.NotPurchased,
 *     event = SubscriptionEvent.Purchase("pro_monthly", expiryEpochMs = 1_700_000_000_000L),
 * )
 * // result == TransitionResult.Success(SubscriptionState.Active("pro_monthly", ...))
 * ```
 *
 * The reducer is a pure function: same inputs always produce the same output, no
 * external state, no side effects. This makes it trivial to test and to plug into
 * any architecture (MVI, MVVM, Redux-style, server-side, CLI, etc.).
 */
public class SubscriptionStateMachine {

    /**
     * Apply [event] to [state] and return the resulting [TransitionResult].
     */
    public fun reduce(state: SubscriptionState, event: SubscriptionEvent): TransitionResult =
        when (state) {
            SubscriptionState.NotPurchased -> fromNotPurchased(event)
            is SubscriptionState.Active -> fromActive(state, event)
            is SubscriptionState.InGracePeriod -> fromGrace(state, event)
            is SubscriptionState.OnHold -> fromHold(state, event)
            is SubscriptionState.Paused -> fromPaused(state, event)
            is SubscriptionState.Cancelled -> fromCancelled(state, event)
            SubscriptionState.Expired -> fromExpired(event)
        }

    private fun fromNotPurchased(event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Purchase ->
            success(SubscriptionState.Active(event.productId, event.expiryEpochMs, autoRenew = true))

        else -> reject(SubscriptionState.NotPurchased, event, "Only Purchase is valid from NotPurchased.")
    }

    private fun fromActive(state: SubscriptionState.Active, event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Renew ->
            success(state.copy(expiryEpochMs = event.newExpiryEpochMs))

        is SubscriptionEvent.Cancel ->
            success(SubscriptionState.Cancelled(state.productId, state.expiryEpochMs))

        is SubscriptionEvent.Upgrade ->
            success(SubscriptionState.Active(event.newProductId, event.newExpiryEpochMs, autoRenew = true))

        is SubscriptionEvent.CrossGrade ->
            success(
                SubscriptionState.Active(
                    productId = event.newProductId,
                    expiryEpochMs = event.newExpiryEpochMs,
                    autoRenew = state.autoRenew,
                ),
            )

        is SubscriptionEvent.Downgrade -> {
            // Downgrade takes effect at the next renewal boundary; the current product
            // stays active until expiry. Modelled here by keeping Active unchanged.
            // Adopters who need to track the queued change can wrap this state.
            success(state)
        }

        is SubscriptionEvent.EnterGracePeriod ->
            success(SubscriptionState.InGracePeriod(state.productId, event.gracePeriodEndEpochMs))

        is SubscriptionEvent.Pause ->
            success(SubscriptionState.Paused(state.productId, event.resumeEpochMs))

        SubscriptionEvent.Expire ->
            success(SubscriptionState.Expired)

        is SubscriptionEvent.Purchase ->
            reject(state, event, "Already active; use Upgrade or CrossGrade to switch product.")

        is SubscriptionEvent.EnterHold ->
            reject(state, event, "Cannot enter hold directly from Active; must pass through InGracePeriod first.")

        is SubscriptionEvent.Resume ->
            reject(state, event, "Subscription is already active; nothing to resume.")
    }

    private fun fromGrace(state: SubscriptionState.InGracePeriod, event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Renew ->
            success(SubscriptionState.Active(state.productId, event.newExpiryEpochMs, autoRenew = true))

        is SubscriptionEvent.Cancel ->
            success(SubscriptionState.Cancelled(state.productId, state.gracePeriodEndEpochMs))

        is SubscriptionEvent.EnterHold ->
            success(SubscriptionState.OnHold(state.productId, event.holdEndEpochMs))

        is SubscriptionEvent.Pause ->
            success(SubscriptionState.Paused(state.productId, event.resumeEpochMs))

        SubscriptionEvent.Expire ->
            success(SubscriptionState.Expired)

        else ->
            reject(state, event, "Only Renew, Cancel, EnterHold, Pause, or Expire are valid during grace period.")
    }

    private fun fromHold(state: SubscriptionState.OnHold, event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Renew ->
            success(SubscriptionState.Active(state.productId, event.newExpiryEpochMs, autoRenew = true))

        is SubscriptionEvent.Resume ->
            success(SubscriptionState.Active(state.productId, event.newExpiryEpochMs, autoRenew = true))

        SubscriptionEvent.Expire ->
            success(SubscriptionState.Expired)

        else ->
            reject(state, event, "Only Renew, Resume, or Expire are valid from hold.")
    }

    private fun fromPaused(state: SubscriptionState.Paused, event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Resume ->
            success(SubscriptionState.Active(state.productId, event.newExpiryEpochMs, autoRenew = true))

        is SubscriptionEvent.Cancel ->
            success(SubscriptionState.Cancelled(state.productId, state.resumeEpochMs ?: event.effectiveEpochMs))

        SubscriptionEvent.Expire ->
            success(SubscriptionState.Expired)

        else ->
            reject(state, event, "Only Resume, Cancel, or Expire are valid while paused.")
    }

    private fun fromCancelled(state: SubscriptionState.Cancelled, event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Purchase ->
            success(SubscriptionState.Active(event.productId, event.expiryEpochMs, autoRenew = true))

        SubscriptionEvent.Expire ->
            success(SubscriptionState.Expired)

        else ->
            reject(state, event, "Only Purchase (resubscribe) or Expire are valid from cancelled.")
    }

    private fun fromExpired(event: SubscriptionEvent): TransitionResult = when (event) {
        is SubscriptionEvent.Purchase ->
            success(SubscriptionState.Active(event.productId, event.expiryEpochMs, autoRenew = true))

        else ->
            reject(SubscriptionState.Expired, event, "Only Purchase (winback) is valid from expired.")
    }

    private fun success(newState: SubscriptionState): TransitionResult.Success =
        TransitionResult.Success(newState)

    private fun reject(
        from: SubscriptionState,
        event: SubscriptionEvent,
        reason: String,
    ): TransitionResult.Invalid =
        TransitionResult.Invalid(from, event, reason)
}
