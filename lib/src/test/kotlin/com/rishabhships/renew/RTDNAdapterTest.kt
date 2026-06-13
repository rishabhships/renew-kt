package com.rishabhships.renew

import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_CANCELED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_DEFERRED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_EXPIRED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_IN_GRACE_PERIOD
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_ON_HOLD
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_PAUSED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_PENDING_PURCHASE_CANCELED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_PRICE_CHANGE_CONFIRMED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_PURCHASED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_RECOVERED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_RENEWED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_RESTARTED
import com.rishabhships.renew.RTDNNotificationType.SUBSCRIPTION_REVOKED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RTDNAdapterTest {

    // ----- direct event mappings -----

    @Test
    fun `SUBSCRIPTION_RECOVERED maps to Resume`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_RECOVERED,
            productId = "pro_monthly",
            expiryEpochMs = 1_705_700_000_000L,
        )

        val resume = assertIs<SubscriptionEvent.Resume>(event)
        assertEquals(1_705_700_000_000L, resume.newExpiryEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_RENEWED maps to Renew`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_RENEWED,
            productId = "pro_monthly",
            expiryEpochMs = 1_702_592_000_000L,
        )

        val renew = assertIs<SubscriptionEvent.Renew>(event)
        assertEquals(1_702_592_000_000L, renew.newExpiryEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_CANCELED maps to Cancel with event timestamp`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_CANCELED,
            productId = "pro_monthly",
            eventEpochMs = 1_699_500_000_000L,
        )

        val cancel = assertIs<SubscriptionEvent.Cancel>(event)
        assertEquals(1_699_500_000_000L, cancel.effectiveEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_PURCHASED maps to Purchase`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_PURCHASED,
            productId = "pro_monthly",
            expiryEpochMs = 1_700_000_000_000L,
        )

        val purchase = assertIs<SubscriptionEvent.Purchase>(event)
        assertEquals("pro_monthly", purchase.productId)
        assertEquals(1_700_000_000_000L, purchase.expiryEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_RESTARTED maps to Purchase (re-subscribe)`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_RESTARTED,
            productId = "pro_yearly",
            expiryEpochMs = 1_730_000_000_000L,
        )

        val purchase = assertIs<SubscriptionEvent.Purchase>(event)
        assertEquals("pro_yearly", purchase.productId)
        assertEquals(1_730_000_000_000L, purchase.expiryEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_ON_HOLD maps to EnterHold`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_ON_HOLD,
            productId = "pro_monthly",
            holdEndEpochMs = 1_703_100_000_000L,
        )

        val hold = assertIs<SubscriptionEvent.EnterHold>(event)
        assertEquals(1_703_100_000_000L, hold.holdEndEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_IN_GRACE_PERIOD maps to EnterGracePeriod`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_IN_GRACE_PERIOD,
            productId = "pro_monthly",
            gracePeriodEndEpochMs = 1_700_500_000_000L,
        )

        val grace = assertIs<SubscriptionEvent.EnterGracePeriod>(event)
        assertEquals(1_700_500_000_000L, grace.gracePeriodEndEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_PAUSED maps to Pause`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_PAUSED,
            productId = "pro_monthly",
            resumeEpochMs = 1_710_000_000_000L,
        )

        val pause = assertIs<SubscriptionEvent.Pause>(event)
        assertEquals(1_710_000_000_000L, pause.resumeEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_PAUSED with null resume time is supported`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_PAUSED,
            productId = "pro_monthly",
            resumeEpochMs = null,
        )

        val pause = assertIs<SubscriptionEvent.Pause>(event)
        assertNull(pause.resumeEpochMs)
    }

    @Test
    fun `SUBSCRIPTION_REVOKED maps to Expire`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_REVOKED,
            productId = "pro_monthly",
        )

        assertEquals(SubscriptionEvent.Expire, event)
    }

    @Test
    fun `SUBSCRIPTION_EXPIRED maps to Expire`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_EXPIRED,
            productId = "pro_monthly",
        )

        assertEquals(SubscriptionEvent.Expire, event)
    }

    // ----- informational types return null -----

    @Test
    fun `SUBSCRIPTION_PRICE_CHANGE_CONFIRMED returns null`() {
        assertNull(RTDNAdapter.toEvent(SUBSCRIPTION_PRICE_CHANGE_CONFIRMED, "pro_monthly"))
    }

    @Test
    fun `SUBSCRIPTION_DEFERRED returns null`() {
        assertNull(RTDNAdapter.toEvent(SUBSCRIPTION_DEFERRED, "pro_monthly"))
    }

    @Test
    fun `SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED returns null`() {
        assertNull(RTDNAdapter.toEvent(SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED, "pro_monthly"))
    }

    @Test
    fun `SUBSCRIPTION_PENDING_PURCHASE_CANCELED returns null`() {
        assertNull(RTDNAdapter.toEvent(SUBSCRIPTION_PENDING_PURCHASE_CANCELED, "pro_monthly"))
    }

    @Test
    fun `unknown notification type returns null`() {
        assertNull(RTDNAdapter.toEvent(notificationType = 999, productId = "pro_monthly"))
    }

    // ----- integration with the state machine -----

    @Test
    fun `RTDN PURCHASED pipes through to Active state`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_PURCHASED,
            productId = "pro_monthly",
            expiryEpochMs = 1_700_000_000_000L,
        )!!

        val machine = SubscriptionStateMachine()
        val newState = machine.reduce(SubscriptionState.NotPurchased, event).require()

        val active = assertIs<SubscriptionState.Active>(newState)
        assertEquals("pro_monthly", active.productId)
        assertEquals(1_700_000_000_000L, active.expiryEpochMs)
    }

    @Test
    fun `RTDN RENEWED pipes through extending Active expiry`() {
        val event = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_RENEWED,
            productId = "pro_monthly",
            expiryEpochMs = 1_702_592_000_000L,
        )!!

        val current = SubscriptionState.Active(
            productId = "pro_monthly",
            expiryEpochMs = 1_700_000_000_000L,
            autoRenew = true,
        )
        val newState = SubscriptionStateMachine().reduce(current, event).require()

        val active = assertIs<SubscriptionState.Active>(newState)
        assertEquals(1_702_592_000_000L, active.expiryEpochMs)
    }

    @Test
    fun `RTDN ON_HOLD then EXPIRED pipes through to Expired terminal state`() {
        val machine = SubscriptionStateMachine()

        val current = SubscriptionState.InGracePeriod(
            productId = "pro_monthly",
            gracePeriodEndEpochMs = 1_700_500_000_000L,
        )

        val onHoldEvent = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_ON_HOLD,
            productId = "pro_monthly",
            holdEndEpochMs = 1_703_100_000_000L,
        )!!
        val held = machine.reduce(current, onHoldEvent).require()
        assertIs<SubscriptionState.OnHold>(held)

        val expireEvent = RTDNAdapter.toEvent(
            notificationType = SUBSCRIPTION_EXPIRED,
            productId = "pro_monthly",
        )!!
        val finalState = machine.reduce(held, expireEvent).require()
        assertEquals(SubscriptionState.Expired, finalState)
    }
}
