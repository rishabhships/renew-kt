package com.rishabhships.renew

import com.rishabhships.renew.SubscriptionEvent.Cancel
import com.rishabhships.renew.SubscriptionEvent.CrossGrade
import com.rishabhships.renew.SubscriptionEvent.Downgrade
import com.rishabhships.renew.SubscriptionEvent.EnterGracePeriod
import com.rishabhships.renew.SubscriptionEvent.EnterHold
import com.rishabhships.renew.SubscriptionEvent.Expire
import com.rishabhships.renew.SubscriptionEvent.Pause
import com.rishabhships.renew.SubscriptionEvent.Purchase
import com.rishabhships.renew.SubscriptionEvent.Renew
import com.rishabhships.renew.SubscriptionEvent.Resume
import com.rishabhships.renew.SubscriptionEvent.Upgrade
import com.rishabhships.renew.SubscriptionState.Active
import com.rishabhships.renew.SubscriptionState.Cancelled
import com.rishabhships.renew.SubscriptionState.Expired
import com.rishabhships.renew.SubscriptionState.InGracePeriod
import com.rishabhships.renew.SubscriptionState.NotPurchased
import com.rishabhships.renew.SubscriptionState.OnHold
import com.rishabhships.renew.SubscriptionState.Paused
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubscriptionStateMachineTest {

    private val machine = SubscriptionStateMachine()

    // ----- happy paths -----

    @Test
    fun `purchase from NotPurchased transitions to Active`() {
        val result = machine.reduce(
            state = NotPurchased,
            event = Purchase(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L),
        )

        val success = assertIs<TransitionResult.Success>(result)
        val active = assertIs<Active>(success.newState)
        assertEquals("pro_monthly", active.productId)
        assertEquals(1_700_000_000_000L, active.expiryEpochMs)
        assertTrue(active.autoRenew)
    }

    @Test
    fun `renew while Active extends expiry`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, Renew(newExpiryEpochMs = 1_702_592_000_000L))

        val active = assertIs<Active>(result.require())
        assertEquals("pro_monthly", active.productId)
        assertEquals(1_702_592_000_000L, active.expiryEpochMs)
    }

    @Test
    fun `cancel from Active transitions to Cancelled with same expiry`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, Cancel(effectiveEpochMs = 1_699_000_000_000L))

        val cancelled = assertIs<Cancelled>(result.require())
        assertEquals("pro_monthly", cancelled.productId)
        assertEquals(1_700_000_000_000L, cancelled.expiryEpochMs)
    }

    @Test
    fun `upgrade from Active switches product immediately`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(
            state,
            Upgrade(newProductId = "pro_yearly", newExpiryEpochMs = 1_730_000_000_000L),
        )

        val upgraded = assertIs<Active>(result.require())
        assertEquals("pro_yearly", upgraded.productId)
        assertEquals(1_730_000_000_000L, upgraded.expiryEpochMs)
    }

    @Test
    fun `cross-grade from Active switches product immediately and preserves autoRenew flag`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = false)
        val result = machine.reduce(
            state,
            CrossGrade(
                newProductId = "business_monthly",
                prorationMode = ProrationMode.ChargeProratedPrice,
                newExpiryEpochMs = 1_702_592_000_000L,
            ),
        )

        val crossed = assertIs<Active>(result.require())
        assertEquals("business_monthly", crossed.productId)
        assertEquals(1_702_592_000_000L, crossed.expiryEpochMs)
        assertEquals(false, crossed.autoRenew)
    }

    @Test
    fun `downgrade from Active leaves state unchanged until renewal boundary`() {
        val state = Active(productId = "pro_yearly", expiryEpochMs = 1_730_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, Downgrade(newProductId = "basic_yearly"))

        assertEquals(state, result.require())
    }

    // ----- grace + hold paths -----

    @Test
    fun `Active to InGracePeriod on billing failure`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, EnterGracePeriod(gracePeriodEndEpochMs = 1_700_500_000_000L))

        val grace = assertIs<InGracePeriod>(result.require())
        assertEquals("pro_monthly", grace.productId)
        assertEquals(1_700_500_000_000L, grace.gracePeriodEndEpochMs)
    }

    @Test
    fun `InGracePeriod to Active on recovery via Renew`() {
        val grace = InGracePeriod(productId = "pro_monthly", gracePeriodEndEpochMs = 1_700_500_000_000L)
        val result = machine.reduce(grace, Renew(newExpiryEpochMs = 1_702_592_000_000L))

        val active = assertIs<Active>(result.require())
        assertEquals("pro_monthly", active.productId)
        assertTrue(active.autoRenew)
    }

    @Test
    fun `InGracePeriod to OnHold when grace ends without recovery`() {
        val grace = InGracePeriod(productId = "pro_monthly", gracePeriodEndEpochMs = 1_700_500_000_000L)
        val result = machine.reduce(grace, EnterHold(holdEndEpochMs = 1_703_100_000_000L))

        val hold = assertIs<OnHold>(result.require())
        assertEquals("pro_monthly", hold.productId)
        assertEquals(1_703_100_000_000L, hold.holdEndEpochMs)
    }

    @Test
    fun `OnHold to Active on recovery`() {
        val hold = OnHold(productId = "pro_monthly", holdEndEpochMs = 1_703_100_000_000L)
        val result = machine.reduce(hold, Resume(newExpiryEpochMs = 1_705_700_000_000L))

        val active = assertIs<Active>(result.require())
        assertEquals("pro_monthly", active.productId)
        assertEquals(1_705_700_000_000L, active.expiryEpochMs)
    }

    @Test
    fun `OnHold to Expired when hold period ends`() {
        val hold = OnHold(productId = "pro_monthly", holdEndEpochMs = 1_703_100_000_000L)
        val result = machine.reduce(hold, Expire)

        assertEquals(Expired, result.require())
    }

    // ----- pause + resume -----

    @Test
    fun `Active to Paused`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, Pause(resumeEpochMs = 1_710_000_000_000L))

        val paused = assertIs<Paused>(result.require())
        assertEquals("pro_monthly", paused.productId)
        assertEquals(1_710_000_000_000L, paused.resumeEpochMs)
    }

    @Test
    fun `Paused to Active on resume`() {
        val paused = Paused(productId = "pro_monthly", resumeEpochMs = 1_710_000_000_000L)
        val result = machine.reduce(paused, Resume(newExpiryEpochMs = 1_712_000_000_000L))

        val active = assertIs<Active>(result.require())
        assertEquals("pro_monthly", active.productId)
        assertEquals(1_712_000_000_000L, active.expiryEpochMs)
    }

    // ----- winback -----

    @Test
    fun `Expired to Active via winback Purchase`() {
        val result = machine.reduce(
            state = Expired,
            event = Purchase(productId = "pro_monthly", expiryEpochMs = 1_715_000_000_000L),
        )

        val active = assertIs<Active>(result.require())
        assertEquals("pro_monthly", active.productId)
        assertEquals(1_715_000_000_000L, active.expiryEpochMs)
    }

    @Test
    fun `Cancelled to Active via Purchase resubscribe`() {
        val cancelled = Cancelled(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L)
        val result = machine.reduce(cancelled, Purchase(productId = "pro_yearly", expiryEpochMs = 1_730_000_000_000L))

        val active = assertIs<Active>(result.require())
        assertEquals("pro_yearly", active.productId)
    }

    // ----- invalid transitions -----

    @Test
    fun `Renew from NotPurchased is rejected`() {
        val result = machine.reduce(NotPurchased, Renew(newExpiryEpochMs = 1_700_000_000_000L))

        val invalid = assertIs<TransitionResult.Invalid>(result)
        assertEquals(NotPurchased, invalid.from)
        assertTrue(invalid.reason.contains("Only Purchase"))
    }

    @Test
    fun `Purchase while Active is rejected`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, Purchase(productId = "pro_monthly", expiryEpochMs = 1_702_000_000_000L))

        val invalid = assertIs<TransitionResult.Invalid>(result)
        assertTrue(invalid.reason.contains("Already active"))
    }

    @Test
    fun `EnterHold directly from Active is rejected`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, EnterHold(holdEndEpochMs = 1_703_000_000_000L))

        val invalid = assertIs<TransitionResult.Invalid>(result)
        assertTrue(invalid.reason.contains("must pass through InGracePeriod"))
    }

    @Test
    fun `Upgrade while in grace period is rejected`() {
        val grace = InGracePeriod(productId = "pro_monthly", gracePeriodEndEpochMs = 1_700_500_000_000L)
        val result = machine.reduce(grace, Upgrade(newProductId = "pro_yearly", newExpiryEpochMs = 1_730_000_000_000L))

        val invalid = assertIs<TransitionResult.Invalid>(result)
        assertTrue(invalid.reason.contains("during grace period"))
    }

    @Test
    fun `Resume from Active is rejected`() {
        val state = Active(productId = "pro_monthly", expiryEpochMs = 1_700_000_000_000L, autoRenew = true)
        val result = machine.reduce(state, Resume(newExpiryEpochMs = 1_705_000_000_000L))

        val invalid = assertIs<TransitionResult.Invalid>(result)
        assertTrue(invalid.reason.contains("already active"))
    }

    @Test
    fun `Renew from Expired is rejected`() {
        val result = machine.reduce(Expired, Renew(newExpiryEpochMs = 1_705_000_000_000L))

        val invalid = assertIs<TransitionResult.Invalid>(result)
        assertTrue(invalid.reason.contains("winback"))
    }
}
