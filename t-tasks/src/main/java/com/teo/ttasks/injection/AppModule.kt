package com.teo.ttasks.injection

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.GsonBuilder
import com.teo.ttasks.BuildConfig
import com.teo.ttasks.UserManager
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.local.WidgetHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.jobs.TaskCreateJob
import com.teo.ttasks.jobs.TaskDeleteJob
import com.teo.ttasks.jobs.TaskUpdateJob
import com.teo.ttasks.receivers.NetworkInfoReceiver
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter
import com.teo.ttasks.ui.activities.main.MainActivity
import com.teo.ttasks.ui.activities.main.MainActivityPresenter
import com.teo.ttasks.ui.activities.sign_in.SignInActivity
import com.teo.ttasks.ui.activities.sign_in.SignInPresenter
import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment
import com.teo.ttasks.ui.fragments.task_lists.TaskListsPresenter
import com.teo.ttasks.ui.fragments.tasks.TasksFragment
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter
import com.teo.ttasks.ui.task_detail.TaskDetailActivity
import com.teo.ttasks.ui.task_detail.TaskDetailPresenter
import com.teo.ttasks.util.NotificationHelper
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity
import com.teo.ttasks.widget.configure.TasksWidgetConfigurePresenter
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/"

private const val HEADER_AUTHORIZATION = "Authorization"
private const val VALUE_BEARER = "Bearer %s"

const val SCOPE_TASKS = "https://www.googleapis.com/auth/tasks"

val appModule = module {

    single { FirebaseAuth.getInstance() }

    single { NetworkInfoReceiver() }

    single { UserManager(context = get()) }

    single { PrefHelper(context = get(), firebaseAuth = get()) }

    single { WidgetHelper(context = get(), prefHelper = get()) }

    single { NotificationHelper(context = get(), prefHelper = get()) }

    factory { TaskCreateJob(tasksHelper = get(), widgetHelper = get(), notificationHelper = get(), tasksApi = get()) }

    factory { TaskUpdateJob(tasksHelper = get(), widgetHelper = get(), notificationHelper = get(), tasksApi = get()) }

    factory { TaskDeleteJob(tasksHelper = get(), tasksApi = get()) }

    scope(named<SignInActivity>()) {
        scoped {
            SignInPresenter(tokenHelper = get(), tasksHelper = get(), prefHelper = get())
        }
    }

    scope(named<MainActivity>()) {
        scoped {
            MainActivityPresenter(
                context = get(),
                tasksHelper = get(),
                prefHelper = get(),
                userManager = get(),
                firebaseAuth = get())
        }
    }

    scope(named<TasksFragment>()) {
        scoped {
            TasksPresenter(tasksHelper = get(), prefHelper = get())
        }
    }

    scope(named<TaskListsFragment>()) {
        scoped {
            TaskListsPresenter(tasksHelper = get())
        }
    }

    scope(named<EditTaskActivity>()) {
        scoped {
            EditTaskPresenter(tasksHelper = get(), widgetHelper = get(), notificationHelper = get())
        }
    }

    scope(named<TaskDetailActivity>()) {
        scoped {
            TaskDetailPresenter(tasksHelper = get(), widgetHelper = get(), notificationHelper = get())
        }
    }

    scope(named<TasksWidgetConfigureActivity>()) {
        scoped {
            TasksWidgetConfigurePresenter(tasksHelper = get(), prefHelper = get())
        }
    }
}

val networkModule = module {

    single { TasksHelper(tasksApi = get(), prefHelper = get()) }

    single { TokenHelper(prefHelper = get(), context = get()) }

    single { get<Retrofit.Builder>().baseUrl(TASKS_BASE_URL).build().create(TasksApi::class.java) }

    single {
        Retrofit.Builder()
            .client(get<OkHttpClient>())
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .serializeNulls()
                        .create()
                )
            )
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
    }

    single {
        val prefHelper: PrefHelper = get()
        val tokenHelper: TokenHelper = get()
        val context: Context = get()

        OkHttpClient.Builder()
//            .protocols(arrayListOf(Protocol.HTTP_1_1))
//            .connectionPool(ConnectionPool(0, 5L, TimeUnit.MINUTES))
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor { chain ->
                // Use the access token to access the Tasks API
                val request = chain.request()
                return@addInterceptor when (val accessToken = prefHelper.accessToken) {
                    null -> {
                        Timber.d("Access token not available, will try to request a new token")
                        chain.proceed(request)
                    }
                    else -> {
                        Timber.v("Authorizing with %s", accessToken)
                        // Add the authorization header
                        chain.proceed(
                            request.newBuilder()
                                .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                .build()
                        )
                    }
                }
            }
            .authenticator(object: Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    // Refresh the access token when it expires
                    Timber.d("Unauthorized, requesting new access token")
                    return when (val account = GoogleSignIn.getLastSignedInAccount(context)?.account) {
                        null -> {
                            Timber.e("User is null, should be signed out")
                            null
                        }
                        else -> {
                            try {
                                val accessToken = tokenHelper.refreshAccessToken(account).blockingGet()
                                response.request.newBuilder()
                                    .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                    .build()
                            } catch (e: Exception) {
                                Timber.e(e, "Could not get new access token")
                                null
                            }
                        }
                    }
                }
            })
            .build()
    }
}
