package com.teo.ttasks.api;

import com.teo.ttasks.api.entities.PersonResponse;

import retrofit2.http.GET;
import rx.Observable;

public interface PeopleApi {

    @GET("people/me")
    Observable<PersonResponse> getCurrentUserProfile();
}
