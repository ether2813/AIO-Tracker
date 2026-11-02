package com.aio.tracker

import com.aio.tracker.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesCommonFormats() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1250L, Money.parse("12.5"))
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(123450L, Money.parse("1,234.50"))
        assertEquals(123450L, Money.parse("1.234,50"))
        assertEquals(100000000L, Money.parse("1,000,000"))
        assertEquals(1L, Money.parse("0.005"))
    }

    @Test
    fun rejectsInvalid() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("0"))
        assertNull(Money.parse("-5"))
    }

    @Test
    fun inputRoundTrip() {
        assertEquals("12.5", Money.toInput(1250))
        assertEquals("12", Money.toInput(1200))
    }
}
