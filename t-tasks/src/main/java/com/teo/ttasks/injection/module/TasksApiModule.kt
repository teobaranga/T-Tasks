package com.teo.ttasks.injection.module

import com.google.gson.*
import com.teo.ttasks.BuildConfig
import com.teo.ttasks.TTasksApp
import com.teo.ttasks.api.PeopleApi
import com.teo.ttasks.api.TasksApi
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.data.remote.TasksHelper
import com.teo.ttasks.data.remote.TokenHelper
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
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
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
    internal fun provideTokenHelper(prefHelper: PrefHelper, tTasksApp: TTasksApp): TokenHelper =
            TokenHelper(prefHelper, tTasksApp)

    @Provides
    @Singleton
    internal fun provideTasksHelper(tasksApi: TasksApi, prefHelper: PrefHelper): TasksHelper =
            TasksHelper(tasksApi, prefHelper)

    @Provides
    @Singleton
    internal fun provideTasksApi(retrofitBuilder: Retrofit.Builder): TasksApi =
            retrofitBuilder.baseUrl(TASKS_BASE_URL).build().create(TasksApi::class.java)

    @Provides
    @Singleton
    internal fun providePeopleApi(retrofitBuilder: Retrofit.Builder): PeopleApi =
            retrofitBuilder.baseUrl(PEOPLE_BASE_URL).build().create(PeopleApi::class.java)

    @Provides
    @Singleton
    internal fun provideOkHttpClient(tokenHelper: TokenHelper, prefHelper: PrefHelper): OkHttpClient {
        val protocols = ArrayList<Protocol>()
        protocols.add(Protocol.HTTP_1_1)
        return OkHttpClient.Builder()
                .protocols(protocols)
                .connectionPool(ConnectionPool(0, 5L, TimeUnit.MINUTES))
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .addInterceptor(HttpLoggingInterceptor().setLevel(if (BuildConfig.DEBUG) BASIC else NONE))
                .addInterceptor { chain ->
                    // Use the access token to access the Tasks API
                    val request = chain.request()
                    val accessToken = prefHelper.accessToken
                    if (accessToken != null) {
                        Timber.d("authorizing with %s", accessToken)
                        // Add the authorization header
                        return@addInterceptor chain.proceed(request.newBuilder()
                                .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                .build())
                    }
                    Timber.d("access token not available, will try to request a new token")
                    chain.proceed(request)
                }
                .authenticator { _, response ->
                    // Refresh the access token when it expires
                    Timber.d("requesting new access token")
                    try {
                        val accessToken = tokenHelper.refreshAccessToken().blockingGet()
                        prefHelper.accessToken = accessToken
                        Timber.d("saved new access token %s", accessToken)
                        return@authenticator response.request().newBuilder()
                                .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                .build()
                    } catch (e: Exception) {
                        Timber.e("could not get new access token")
                    }
                    null
                }
                .build()
    }

    @Provides
    @Singleton
    internal fun provideRetrofitBuilder(okHttpClient: OkHttpClient): Retrofit.Builder {
        val gson = GsonBuilder()
                .registerTypeAdapter(Date::class.java, GsonUTCDateAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create()
        return Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
    }

    private inner class GsonUTCDateAdapter internal constructor() : JsonSerializer<Date>, JsonDeserializer<Date> {

        private val dateFormat: DateFormat

        init {
            dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }

        @Synchronized override fun serialize(date: Date, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
            return JsonPrimitive(dateFormat.format(date))
        }

        @Synchronized override fun deserialize(jsonElement: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): Date {
            try {
                return dateFormat.parse(jsonElement.asString)
            } catch (e: ParseException) {
                throw JsonParseException(e)
            }

        }
    }

    companion object {
        private const val TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/"
        private const val PEOPLE_BASE_URL = "https://people.googleapis.com/v1/"

        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val VALUE_BEARER = "Bearer %s"
    }
}
