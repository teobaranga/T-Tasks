package com.teo.ttasks

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.teo.ttasks.injection.component.TestApplicationComponent
import com.teo.ttasks.ui.activities.sign_in.SignInActivity
import io.mockk.every
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class StartActivityForResultTest {

    @get:Rule
    var rule = IntentsTestRule(SignInActivity::class.java)

    @Inject
    lateinit var userManager: UserManager

    companion object {
        private const val ACTION_SIGN_IN = "com.google.android.gms.auth.GOOGLE_SIGN_IN"

        private lateinit var testAppComponent: TestApplicationComponent

        @BeforeClass
        @JvmStatic
        fun setup() {
            testAppComponent =
                    (ApplicationProvider.getApplicationContext<TTasksApp>().applicationComponent as TestApplicationComponent)
        }
    }

    @Before
    fun before() {
        testAppComponent.inject(this)
    }

    @Test
    fun signIn_cancel_signInButtonVisible() {
        every { userManager.signInIntent } answers { Intent(ACTION_SIGN_IN) }

        val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
        intending(hasAction(ACTION_SIGN_IN)).respondWith(result)

        // Simulate a button click that starts the Google SignIn activity:
        onView(withId(R.id.sign_in_button)).perform(click())

        // Make sure that the Sign In button is still visible after cancelling
        onView(withId(R.id.sign_in_button)).check(matches(withEffectiveVisibility(VISIBLE)))
    }

    @Test
    fun signIn_apiException_signInButtonVisible() {
        every { userManager.signInIntent } answers { Intent(ACTION_SIGN_IN) }
        every { userManager.getSignedInAccountFromIntent(any()) } throws ApiException(Status.RESULT_INTERNAL_ERROR)

        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasAction(ACTION_SIGN_IN)).respondWith(result)

        // Simulate a button click that starts the Google SignIn activity:
        onView(withId(R.id.sign_in_button)).perform(click())

        // Make sure that the Sign In button is still visible after a sign in error
        onView(withId(R.id.sign_in_button)).check(matches(withEffectiveVisibility(VISIBLE)))
    }

}
