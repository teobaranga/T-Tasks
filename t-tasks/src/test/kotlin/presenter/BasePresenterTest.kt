package presenter

import android.support.annotation.CallSuper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.staticMockk
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import org.junit.After
import org.junit.Before

abstract class BasePresenterTest {

    @MockK
    protected lateinit var realm: Realm

    @Before
    @CallSuper
    open fun setup() {
        MockKAnnotations.init(this)
        // Make sure tests use a mocked version of realm since we're not testing it in this context
        staticMockk<Realm>().mock()
        every { Realm.getDefaultInstance() } returns realm
        // Use the trampoline schedule to replace the Android main thread
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    @CallSuper
    open fun finish() {
        staticMockk<Realm>().unmock()
    }
}
