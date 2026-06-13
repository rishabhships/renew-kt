package com.rishabhships.renew.sample.ui

import androidx.lifecycle.ViewModel
import com.rishabhships.renew.ProrationMode
import com.rishabhships.renew.SubscriptionEvent
import com.rishabhships.renew.SubscriptionState
import com.rishabhships.renew.SubscriptionStateMachine
import com.rishabhships.renew.TransitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State + events for the Renew demo screen.
 *
 * We pre-evaluate every event against the current state so the UI can show, at
 * a glance, which transitions are valid right now. The machine itself does the
 * real work; we just probe it.
 */
public data class DemoUiState(
    val currentState: SubscriptionState,
    val availability: Map<EventKind, Availability>,
    val history: List<HistoryEntry>,
)

public enum class EventKind(public val label: String) {
    Purchase("Purchase"),
    Renew("Renew"),
    EnterGracePeriod("EnterGracePeriod"),
    EnterHold("EnterHold"),
    Pause("Pause"),
    Resume("Resume"),
    Cancel("Cancel"),
    Upgrade("Upgrade"),
    CrossGrade("CrossGrade"),
    Downgrade("Downgrade"),
    Expire("Expire"),
}

public sealed class Availability {
    public object Allowed : Availability()
    public data class Rejected(val reason: String) : Availability()
}

public data class HistoryEntry(
    val event: EventKind,
    val from: SubscriptionState,
    val outcome: Outcome,
)

public sealed class Outcome {
    public data class Accepted(val to: SubscriptionState) : Outcome()
    public data class Rejected(val reason: String) : Outcome()
}

internal class DemoViewModel : ViewModel() {

    private val machine = SubscriptionStateMachine()

    private val _uiState = MutableStateFlow(initial())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    fun onIntent(kind: EventKind) {
        val current = _uiState.value.currentState
        val event = buildEvent(kind, current)
        when (val result = machine.reduce(current, event)) {
            is TransitionResult.Success -> {
                val next = result.newState
                _uiState.value = DemoUiState(
                    currentState = next,
                    availability = evaluateAvailability(next),
                    history = (_uiState.value.history + HistoryEntry(kind, current, Outcome.Accepted(next))).takeLast(HISTORY_LIMIT),
                )
            }
            is TransitionResult.Invalid -> {
                _uiState.value = _uiState.value.copy(
                    history = (_uiState.value.history + HistoryEntry(kind, current, Outcome.Rejected(result.reason))).takeLast(HISTORY_LIMIT),
                )
            }
        }
    }

    fun reset() {
        _uiState.value = initial()
    }

    private fun initial(): DemoUiState {
        val s: SubscriptionState = SubscriptionState.NotPurchased
        return DemoUiState(
            currentState = s,
            availability = evaluateAvailability(s),
            history = emptyList(),
        )
    }

    private fun evaluateAvailability(state: SubscriptionState): Map<EventKind, Availability> =
        EventKind.values().associateWith { kind ->
            when (val r = machine.reduce(state, buildEvent(kind, state))) {
                is TransitionResult.Success -> Availability.Allowed
                is TransitionResult.Invalid -> Availability.Rejected(r.reason)
            }
        }

    /**
     * Builds a representative event for each kind given the current state. We
     * pick reasonable example payloads (product ids, expiry offsets) so the
     * demo runs without input forms.
     */
    private fun buildEvent(kind: EventKind, current: SubscriptionState): SubscriptionEvent {
        val now = System.currentTimeMillis()
        val thirtyDays = 30L * 24 * 60 * 60 * 1000
        val yearMs = 365L * 24 * 60 * 60 * 1000
        val sevenDays = 7L * 24 * 60 * 60 * 1000

        return when (kind) {
            EventKind.Purchase -> SubscriptionEvent.Purchase(
                productId = "pro_monthly",
                expiryEpochMs = now + thirtyDays,
            )
            EventKind.Renew -> SubscriptionEvent.Renew(newExpiryEpochMs = now + thirtyDays)
            EventKind.EnterGracePeriod -> SubscriptionEvent.EnterGracePeriod(gracePeriodEndEpochMs = now + sevenDays)
            EventKind.EnterHold -> SubscriptionEvent.EnterHold(holdEndEpochMs = now + sevenDays * 2)
            EventKind.Pause -> SubscriptionEvent.Pause(resumeEpochMs = now + thirtyDays)
            EventKind.Resume -> SubscriptionEvent.Resume(newExpiryEpochMs = now + thirtyDays)
            EventKind.Cancel -> SubscriptionEvent.Cancel(effectiveEpochMs = now + sevenDays)
            EventKind.Upgrade -> SubscriptionEvent.Upgrade(
                newProductId = "pro_yearly",
                newExpiryEpochMs = now + yearMs,
            )
            EventKind.CrossGrade -> SubscriptionEvent.CrossGrade(
                newProductId = "pro_family_monthly",
                prorationMode = ProrationMode.ChargeProratedPrice,
                newExpiryEpochMs = now + thirtyDays,
            )
            EventKind.Downgrade -> SubscriptionEvent.Downgrade(newProductId = "basic_monthly")
            EventKind.Expire -> SubscriptionEvent.Expire
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 12
    }
}
