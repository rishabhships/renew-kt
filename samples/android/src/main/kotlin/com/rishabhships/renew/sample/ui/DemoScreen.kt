package com.rishabhships.renew.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabhships.renew.SubscriptionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DemoScreen(
    state: DemoUiState,
    onEvent: (EventKind) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detail by remember { mutableStateOf<DetailDialogState?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "renew-kt demo",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    TextButton(onClick = onReset) { Text("Reset") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            CurrentStateCard(state.currentState)
            Spacer(Modifier.height(20.dp))

            SectionLabel("Try an event")
            Spacer(Modifier.height(8.dp))

            EventGrid(
                availability = state.availability,
                onClick = { kind ->
                    val a = state.availability[kind]
                    onEvent(kind)
                    if (a is Availability.Rejected) detail = DetailDialogState(kind, a.reason)
                },
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            SectionLabel("History")
            Spacer(Modifier.height(8.dp))

            HistoryList(history = state.history)
            Spacer(Modifier.height(24.dp))
        }
    }

    detail?.let { d ->
        RejectionDialog(
            kind = d.kind,
            reason = d.reason,
            onDismiss = { detail = null },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CurrentStateCard(state: SubscriptionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "CURRENT STATE",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.displayName(),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = state.tagline(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun EventGrid(
    availability: Map<EventKind, Availability>,
    onClick: (EventKind) -> Unit,
) {
    val kinds = EventKind.values()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        kinds.toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kind ->
                    val a = availability[kind] ?: Availability.Rejected("Unknown")
                    EventButton(
                        kind = kind,
                        availability = a,
                        onClick = { onClick(kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EventButton(
    kind: EventKind,
    availability: Availability,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (availability) {
        Availability.Allowed -> Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(kind.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        is Availability.Rejected -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            ),
        ) {
            Text(kind.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HistoryList(history: List<HistoryEntry>) {
    if (history.isEmpty()) {
        Text(
            text = "No transitions yet. Tap Purchase to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        history.reversed().forEach { entry -> HistoryRow(entry) }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val isAccepted = entry.outcome is Outcome.Accepted
    val dot = if (isAccepted) "●" else "✕"
    val dotColor = if (isAccepted) Color(0xFF2E7D32) else Color(0xFFC62828)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(dot, color = dotColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.event.label,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
            when (val o = entry.outcome) {
                is Outcome.Accepted -> Text(
                    text = "${entry.from.displayName()}  →  ${o.to.displayName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is Outcome.Rejected -> Text(
                    text = "Rejected. ${o.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RejectionDialog(kind: EventKind, reason: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${kind.label} not allowed") },
        text = { Text(reason) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

private data class DetailDialogState(val kind: EventKind, val reason: String)

private fun SubscriptionState.displayName(): String = when (this) {
    SubscriptionState.NotPurchased -> "NotPurchased"
    is SubscriptionState.Active -> "Active"
    is SubscriptionState.InGracePeriod -> "InGracePeriod"
    is SubscriptionState.OnHold -> "OnHold"
    is SubscriptionState.Paused -> "Paused"
    is SubscriptionState.Cancelled -> "Cancelled"
    SubscriptionState.Expired -> "Expired"
}

private fun SubscriptionState.tagline(): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return when (this) {
        SubscriptionState.NotPurchased -> "No subscription has ever been purchased."
        is SubscriptionState.Active -> "$productId · expires ${fmt.format(Date(expiryEpochMs))}"
        is SubscriptionState.InGracePeriod -> "$productId · grace ends ${fmt.format(Date(gracePeriodEndEpochMs))}"
        is SubscriptionState.OnHold -> "$productId · hold ends ${fmt.format(Date(holdEndEpochMs))}"
        is SubscriptionState.Paused -> {
            val r = resumeEpochMs?.let { "resumes ${fmt.format(Date(it))}" } ?: "manual resume"
            "$productId · $r"
        }
        is SubscriptionState.Cancelled -> "$productId · access until ${fmt.format(Date(expiryEpochMs))}"
        SubscriptionState.Expired -> "Previously subscribed; access has ended."
    }
}
