package presenter

import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter
import com.teo.ttasks.ui.activities.edit_task.EditTaskView
import com.teo.ttasks.util.NotificationHelper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import java.util.*

class EditTaskPresenterTest : BasePresenterTest() {

    private val validTaskId = "123"

    private val validTask = Task().apply {
        due = Date()
    }

    @MockK
    private lateinit var tasksHelper: TasksHelper
    @MockK
    private lateinit var widgetHelper: WidgetHelper
    @MockK
    private lateinit var notificationHelper: NotificationHelper
    @RelaxedMockK
    private lateinit var editTaskView: EditTaskView

    private lateinit var editTaskPresenter: EditTaskPresenter

    @Before
    override fun setup() {
        super.setup()
        editTaskPresenter = EditTaskPresenter(tasksHelper, widgetHelper, notificationHelper)
    }

    @Test
    fun `Load Task Info - Valid Task - Success`() {
        // arrange
        every {
            tasksHelper.getTaskAsSingle(
                    taskId = validTaskId,
                    realm = any())
        } returns Single.just(validTask)

        // act
        editTaskPresenter.bindView(editTaskView)
        editTaskPresenter.loadTaskInfo(validTaskId)

        // assert
        verify(exactly = 1) { tasksHelper.getTaskAsSingle(validTaskId, any()) }
        verify(exactly = 1) { editTaskView.onTaskLoaded(task = validTask) }
        verify(exactly = 0) { editTaskView.onTaskLoadError() }
    }
}
