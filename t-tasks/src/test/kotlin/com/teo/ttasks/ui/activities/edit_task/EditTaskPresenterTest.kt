package com.teo.ttasks.ui.activities.edit_task

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class EditTaskPresenterTest {

    private var editTaskPresenter: EditTaskPresenter? = null

    @Before
    fun setup() {
        editTaskPresenter = EditTaskPresenter(null, null, null, null)
    }

    /**
     * No matter what locale the user belongs to, when the user sets a due date, it's as if he's in UTC.
     * Therefore, the due date in UTC should be on the same day as the day from the user's locale, even though
     * they are not actually at the same time.
     */
    @Test
    fun dueDateTest() {
        dueDates.forEach {
            // Create a date in the tester's locale (EDT)
            val date = Date(it)
            editTaskPresenter?.dueDate = date
            // Check if the due date is 02 Oct 2016 in UTC
            assert(editTaskPresenter?.dueDate?.time == 1475366400000)
        }
    }

    companion object {

        /**
         * 24 dates in milliseconds for every hour of a specific date: 02 Oct 2016
         */
        private val dueDates = longArrayOf(1475380800000, 1475384400000, 1475388000000, 1475391600000, 1475395200000,
                1475398800000, 1475402400000, 1475406000000, 1475409600000, 1475413200000, 1475416800000, 1475420400000,
                1475424000000, 1475427600000, 1475431200000, 1475434800000, 1475438400000, 1475442000000, 1475445600000,
                1475449200000, 1475452800000, 1475456400000, 1475460000000, 1475463600000)
    }
}
