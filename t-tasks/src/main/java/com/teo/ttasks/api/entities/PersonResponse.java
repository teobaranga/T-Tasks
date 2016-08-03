package com.teo.ttasks.api.entities;

import com.google.gson.annotations.Expose;

public class PersonResponse {

    @Expose
    public Image image;

    @Expose
    public Cover cover;

    public static class Email {

        @Expose
        public String value;
    }

    public static class Image {

        @Expose
        public String url;
    }

    public static class Cover {

        @Expose
        public CoverPhoto coverPhoto;

        public static class CoverPhoto {

            @Expose
            public String url;
        }
    }
}
