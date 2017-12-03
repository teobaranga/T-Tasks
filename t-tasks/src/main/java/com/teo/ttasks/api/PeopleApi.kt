package com.teo.ttasks.api

import com.teo.ttasks.api.entities.PersonResponse
import io.reactivex.Flowable
import retrofit2.http.GET

interface PeopleApi {

    @GET("people/me")
    fun getCurrentUserProfile(): Flowable<PersonResponse>
}
