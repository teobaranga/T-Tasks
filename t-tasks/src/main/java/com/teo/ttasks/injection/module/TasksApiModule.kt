package com.teo.ttasks.injection.module

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.gson.*
import com.teo.ttasks.BuildConfig
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
import com.teo.ttasks.util.DateUtils.Companion.utcDateFormat
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.lang.reflect.Type
import java.text.ParseException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * This is the module that holds everything related to the Tasks API
 */
@Module
class TasksApiModule {

    @Provides
    @Singleton
    internal fun provideTokenHelper(prefHelper: PrefHelper, tTasksApp: TTasksApp) = TokenHelper(prefHelper, tTasksApp)

    @Provides
    @Singleton
    internal fun provideTasksHelper(tasksApi: TasksApi, prefHelper: PrefHelper) = TasksHelper(tasksApi, prefHelper)

    @Provides
    @Singleton
    internal fun provideTasksApi(retrofitBuilder: Retrofit.Builder) =
        retrofitBuilder.baseUrl(TASKS_BASE_URL).build().create(TasksApi::class.java)

    @Provides
    @Singleton
    internal fun providePeopleApi(retrofitBuilder: Retrofit.Builder) =
        retrofitBuilder.baseUrl(PEOPLE_BASE_URL).build().create(PeopleApi::class.java)

    @Provides
    @Singleton
    internal fun provideOkHttpClient(tTasksApp: TTasksApp, tokenHelper: TokenHelper, prefHelper: PrefHelper) =
        OkHttpClient.Builder()
            .protocols(arrayListOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(0, 5L, TimeUnit.MINUTES))
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(HttpLoggingInterceptor().setLevel(if (BuildConfig.DEBUG) BASIC else NONE))
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
            .authenticator { _, response ->
                // Refresh the access token when it expires
                Timber.d("Unauthorized, requesting new access token")
                return@authenticator when (val account = GoogleSignIn.getLastSignedInAccount(tTasksApp)?.account) {
                    null -> {
                        Timber.e("User is null, should be signed out")
                        null
                    }
                    else -> {
                        try {
                            val accessToken = tokenHelper.refreshAccessToken(account).blockingGet()
                            response.request().newBuilder()
                                .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                .build()
                        } catch (e: Exception) {
                            Timber.e(e, "Could not get new access token")
                            null
                        }
                    }
                }
            }
            .build()

    @Provides
    @Singleton
    internal fun provideRetrofitBuilder(okHttpClient: OkHttpClient) =
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .registerTypeAdapter(Date::class.java, GsonUTCDateAdapter())
                        .excludeFieldsWithoutExposeAnnotation()
                        .serializeNulls()
                        .create()
                )
            )
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))

    private inner class GsonUTCDateAdapter internal constructor() : JsonSerializer<Date>, JsonDeserializer<Date> {

        @Synchronized
        override fun serialize(date: Date, type: Type, context: JsonSerializationContext) =
            JsonPrimitive(utcDateFormat.format(date))

        @Synchronized
        override fun deserialize(jsonElement: JsonElement, type: Type, context: JsonDeserializationContext) =
            try {
                utcDateFormat.parse(jsonElement.asString)
            } catch (e: ParseException) {
                throw JsonParseException(e)
            }!!
    }

    companion object {
        private const val TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/"
        private const val PEOPLE_BASE_URL = "https://people.googleapis.com/v1/"

        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val VALUE_BEARER = "Bearer %s"
    }
}
