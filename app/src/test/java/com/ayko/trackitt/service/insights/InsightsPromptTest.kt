package com.ayko.trackitt.service.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InsightsPromptTest {

    @Test
    fun parse_headline_and_where_response() {
        val parsed = InsightsPrompt.parseHeadlineAndWhere(
            """
            HEADLINE: April spending was concentrated in a few heavy categories.
            WHERE: You spent GH₵1,200.00 mostly across House and Food, and one merchant stood out clearly.
            """.trimIndent()
        )

        assertNotNull(parsed)
        assertEquals(
            "April spending was concentrated in a few heavy categories.",
            parsed?.headline,
        )
        assertEquals(
            "You spent GH₵1,200.00 mostly across House and Food, and one merchant stood out clearly.",
            parsed?.whereMoneyWent,
        )
    }

    @Test
    fun parse_cut_response() {
        val parsed = InsightsPrompt.parseCut(
            "CUT: House is the first place to press because it already absorbed GH₵700.00, and Other is still too vague to trim safely without cleanup."
        )

        assertNotNull(parsed)
        assertEquals(
            "House is the first place to press because it already absorbed GH₵700.00, and Other is still too vague to trim safely without cleanup.",
            parsed?.cutBack,
        )
    }

    @Test
    fun parse_save_response() {
        val parsed = InsightsPrompt.parseSave(
            "SAVE: Next month, set budgets for House and Food before the month starts, and move large transfers to cheaper rails so fees stop leaking cash."
        )

        assertNotNull(parsed)
        assertEquals(
            "Next month, set budgets for House and Food before the month starts, and move large transfers to cheaper rails so fees stop leaking cash.",
            parsed?.saveMore,
        )
    }
}
