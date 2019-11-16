package com.teo.ttasks.util

import com.teo.ttasks.data.model.Task
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilTest {

    companion object {
        private const val dateStringGoogle = "2019-01-23T02:19:56.000Z"
        private const val dateStringNormalized = "2019-01-23T02:19:56Z"
    }

    @Test
    fun parseFormat_normalizedDate_equals() {
        val parsedDate = ZonedDateTime.parse(dateStringNormalized)
        val reconstructedDateString = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(parsedDate)
        assertEquals(dateStringNormalized, reconstructedDateString)
    }

    @Test
    fun parse_googleAndNormalized_equals() {
        val google = ZonedDateTime.parse(dateStringGoogle)
        val normalized = ZonedDateTime.parse(dateStringNormalized)
        assertEquals(normalized, google)
    }

    @Test
    fun setDueDate_normalized_setsDue() {
        val task = Task()

        val zonedDateTime = ZonedDateTime.parse(dateStringNormalized)

        task.dueDate = zonedDateTime

        assertEquals(dateStringNormalized, task.due)
    }

    @Test
    fun setCompletedDate_normalized_setsCompleted() {
        val task = Task()

        val zonedDateTime = ZonedDateTime.parse(dateStringNormalized)

        task.completedDate = zonedDateTime

        assertEquals(dateStringNormalized, task.completed)
    }
}
