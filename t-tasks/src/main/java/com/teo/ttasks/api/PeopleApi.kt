package com.teo.ttasks.api

import com.teo.ttasks.api.entities.CoverPhotosResponse
import io.reactivex.Single
import retrofit2.http.GET

interface PeopleApi {

    @GET("people/me?personFields=coverPhotos")
    fun getCurrentPersonCoverPhotos(): Single<CoverPhotosResponse>
}
