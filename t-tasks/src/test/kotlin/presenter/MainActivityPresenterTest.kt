package presenter

import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.api.entities.CoverPhotosResponse
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.main.MainActivityPresenter
import com.teo.ttasks.ui.activities.main.MainView
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class MainActivityPresenterTest : BasePresenterTest() {

    private val coverPhotosResponseEmpty = CoverPhotosResponse().apply { coverPhotos = emptyArray() }

    @MockK
    private lateinit var tasksHelper: TasksHelper
    @MockK
    private lateinit var prefHelper: PrefHelper
    @MockK
    private lateinit var peopleApi: PeopleApi
    @RelaxedMockK
    private lateinit var mainView: MainView

    private lateinit var mainActivityPresenter: MainActivityPresenter

    @Before
    override fun setup() {
        super.setup()
        mainActivityPresenter = MainActivityPresenter(tasksHelper, prefHelper, peopleApi)
    }

    @Test
    fun `Load Cover Photo - 0 results - Success`() {
        // arrange
        every {
            peopleApi.getCurrentPersonCoverPhotos()
        } returns Single.just(coverPhotosResponseEmpty)

        // act
        mainActivityPresenter.bindView(mainView)
        mainActivityPresenter.loadCurrentUser()

        // assert
        verify(exactly = 1) { peopleApi.getCurrentPersonCoverPhotos() }
        verify(exactly = 0) { mainView.onUserCover(any()) }
    }
}
