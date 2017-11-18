package com.teo.ttasks.injection.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.teo.ttasks.BuildConfig;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.api.PeopleApi;
import com.teo.ttasks.api.TasksApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.data.remote.TokenHelper;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BASIC;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

/**
 * This is the module that holds everything related to the Tasks API
 */
@Module
public class TasksApiModule {

    private static final String TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/";
    private static final String PEOPLE_BASE_URL = "https://www.googleapis.com/plus/v1/";

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String VALUE_BEARER = "Bearer %s";

    @Provides @Singleton
    TokenHelper provideTokenHelper(PrefHelper prefHelper, TTasksApp tTasksApp) {
        return new TokenHelper(prefHelper, tTasksApp);
    }

    @Provides @Singleton
    TasksHelper provideTasksHelper(TasksApi tasksApi, PrefHelper prefHelper) {
        return new TasksHelper(tasksApi, prefHelper);
    }

    @Provides @Singleton
    TasksApi provideTasksApi(Retrofit.Builder retrofitBuilder) {
        return retrofitBuilder.baseUrl(TASKS_BASE_URL).build().create(TasksApi.class);
    }

    @Provides @Singleton
    PeopleApi providePeopleApi(Retrofit.Builder retrofitBuilder) {
        return retrofitBuilder.baseUrl(PEOPLE_BASE_URL).build().create(PeopleApi.class);
    }

    @Provides @Singleton
    OkHttpClient provideOkHttpClient(TokenHelper tokenHelper, PrefHelper prefHelper) {
        final List<Protocol> protocols = new ArrayList<>();
        protocols.add(Protocol.HTTP_1_1);
        return new OkHttpClient.Builder()
                .protocols(protocols)
                .connectionPool(new ConnectionPool(0, 5L, TimeUnit.MINUTES))
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.DEBUG ? BASIC : NONE))
                .addInterceptor(chain -> {
                    // Use the access token to access the Tasks API
                    Request request = chain.request();
                    String accessToken = prefHelper.getAccessToken();
                    if (accessToken != null) {
                        Timber.d("authorizing with %s", accessToken);
                        // Add the authorization header
                        return chain.proceed(request.newBuilder()
                                .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                .build());
                    }
                    Timber.d("access token not available, will try to request a new token");
                    return chain.proceed(request);
                })
                .authenticator((route, response) -> {
                    // Refresh the access token when it expires
                    Timber.d("requesting new access token");
                    String accessToken = tokenHelper.refreshAccessToken().blockingFirst(null);
                    if (accessToken != null) {
                        prefHelper.setAccessToken(accessToken);
                        Timber.d("saved new access token %s", accessToken);
                        return response.request().newBuilder()
                                .header(HEADER_AUTHORIZATION, String.format(VALUE_BEARER, accessToken))
                                .build();
                    }
                    Timber.e("could not get new access token");
                    return null;
                })
                .build();
    }

    @Provides @Singleton
    Retrofit.Builder provideRetrofitBuilder(OkHttpClient okHttpClient) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        return new Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()));
    }

    private class GsonUTCDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private final DateFormat dateFormat;

        GsonUTCDateAdapter() {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(dateFormat.format(date));
        }

        @Override
        public synchronized Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            try {
                return dateFormat.parse(jsonElement.getAsString());
            } catch (ParseException e) {
                throw new JsonParseException(e);
            }
        }
    }
}
