package com.teo.ttasks.api.entities

import com.google.gson.annotations.Expose

class CoverPhotosResponse : BaseResponse() {

    @Expose
    lateinit var coverPhotos: Array<CoverPhoto>

    inner class CoverPhoto {
        @Expose
        lateinit var url: String
    }
}
