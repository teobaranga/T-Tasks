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
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;
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
    Retrofit.Builder provideRetrofitBuilder(TokenHelper tokenHelper, PrefHelper prefHelper) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonUTCDateAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
        return new Retrofit.Builder()
                .client(new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.MINUTES)
                        .readTimeout(5, TimeUnit.MINUTES)
                        .addInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.DEBUG ? BASIC : NONE))
                        .addInterceptor(chain -> {
                            // Use the access token to access the Tasks API
                            Request request = chain.request();
                            String accessToken = prefHelper.getAccessToken();
                            if (accessToken != null) {
                                Timber.d("authorizing with %s", accessToken);
                                // Add the authorization header
                                Request authorizedRequest = request;
                                authorizedRequest = request.newBuilder().header("Authorization", "Bearer " + accessToken).build();
                                return chain.proceed(authorizedRequest);
                            }
                            Timber.e("Access token is not available");
                            return chain.proceed(request);
                        })
                        .authenticator((route, response) -> {
                            // Refresh the access token when it expires
                            Timber.d("requesting new access token");
                            String access_token = tokenHelper.refreshAccessToken().toBlocking().firstOrDefault(null);
                            if (access_token != null) {
                                prefHelper.setAccessToken(access_token);
                                Timber.d("saved new access token %s", access_token);
                                return response.request().newBuilder().header("Authorization", "Bearer " + access_token).build();
                            }
                            Timber.e("could not get new access token");
                            return null;
                        })
                        .build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()));
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
