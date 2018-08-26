package presenter

import androidx.annotation.CallSuper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import org.junit.Before

abstract class BasePresenterTest {

    @MockK
    protected lateinit var realm: Realm

    @Before
    @CallSuper
    open fun setup() {
        MockKAnnotations.init(this)
        // Make sure tests use a mocked version of Realm since we're not testing it in this context
        mockkStatic(Realm::class)
        every { Realm.getDefaultInstance() } returns realm
        // Use the trampoline scheduler to replace Schedulers.io()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        // Use the trampoline scheduler to replace the Android main thread
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }
}
