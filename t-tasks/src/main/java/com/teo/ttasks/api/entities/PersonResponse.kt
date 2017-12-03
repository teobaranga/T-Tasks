package com.teo.ttasks.api.entities

import com.google.gson.annotations.Expose

class PersonResponse {

    @Expose
    lateinit var image: Image

    @Expose
    var cover: Cover? = null

    class Email {

        @Expose
        var value: String? = null
    }

    class Image {

        @Expose
        lateinit var url: String
    }

    class Cover {

        @Expose
        var coverPhoto: CoverPhoto? = null

        class CoverPhoto {

            @Expose
            var url: String? = null
        }
    }
}
