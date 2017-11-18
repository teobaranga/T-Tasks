package com.teo.ttasks.api;

import com.teo.ttasks.api.entities.PersonResponse;

import io.reactivex.Flowable;
import retrofit2.http.GET;

public interface PeopleApi {

    @GET("people/me")
    Flowable<PersonResponse> getCurrentUserProfile();
}
