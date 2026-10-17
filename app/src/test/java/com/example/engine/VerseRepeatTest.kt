package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The repeat choice's own arithmetic, asked purely.
 *
 * The pill cycles once, three, five, until-stopped, and back - and the machine
 * reads not the choice but what the choice *owes*: the plays still owed after
 * the one sounding ends. One and done owes nothing; three owes two more; five
 * owes four; the loop's answer is one that no completion can spend.
 */
class VerseRepeatTest {

    @Test
    fun `the pill cycles once, three, five, loop, and back`() {
        assertEquals(VerseRepeat.THRICE, VerseRepeat.ONCE.next())
        assertEquals(VerseRepeat.FIVE_TIMES, VerseRepeat.THRICE.next())
        assertEquals(VerseRepeat.LOOP, VerseRepeat.FIVE_TIMES.next())
        assertEquals(VerseRepeat.ONCE, VerseRepeat.LOOP.next())
    }

    @Test
    fun `the plays a choice owes are the count minus the one sounding`() {
        assertEquals(0, VerseRepeat.ONCE.playsRemaining)
        assertEquals(2, VerseRepeat.THRICE.playsRemaining)
        assertEquals(4, VerseRepeat.FIVE_TIMES.playsRemaining)
    }

    @Test
    fun `the loop owes a play no completion can spend`() {
        assertEquals(-1, VerseRepeat.LOOP.playsRemaining)
    }
}
