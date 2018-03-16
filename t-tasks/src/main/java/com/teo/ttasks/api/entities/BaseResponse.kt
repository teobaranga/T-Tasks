package com.teo.ttasks.api.entities

import com.google.gson.annotations.Expose

open class BaseResponse {

    @Expose
    lateinit var resourceName: String

    @Expose
    lateinit var etag: String
}
