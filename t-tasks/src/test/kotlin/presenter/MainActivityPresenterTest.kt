package presenter

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.teo.ttasks.UserManager
import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.api.entities.CoverPhotosResponse
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.ui.activities.main.MainActivityPresenter
import com.teo.ttasks.ui.activities.main.MainView
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
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

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var firebaseAuth: FirebaseAuth

    @RelaxedMockK
    private lateinit var mainView: MainView

    private lateinit var mainActivityPresenter: MainActivityPresenter

    @Before
    override fun setup() {
        super.setup()
        every { firebaseAuth.addAuthStateListener(any()) } answers { }
        mainActivityPresenter = MainActivityPresenter(tasksHelper, prefHelper, peopleApi, userManager, firebaseAuth)
    }

    @Test
    fun `Load Cover Photo - 0 results - Success`() {
        // arrange
        val currentUser: FirebaseUser = mockk()
        val photoUrl: Uri = mockk()
        val photoUrlString = "test"

        every { photoUrl.toString() } returns photoUrlString
        every { currentUser.photoUrl } returns photoUrl
        every { firebaseAuth.currentUser } returns currentUser
        every { peopleApi.getCurrentPersonCoverPhotos() } returns Single.just(coverPhotosResponseEmpty)

        // act
        mainActivityPresenter.bindView(mainView)
        mainActivityPresenter.loadCurrentUser()

        // assert
        verify(exactly = 1) { mainView.onUserPicture(photoUrlString) }
        verify(exactly = 1) { peopleApi.getCurrentPersonCoverPhotos() }
        verify(exactly = 0) { mainView.onUserCover(any()) }
    }
}
