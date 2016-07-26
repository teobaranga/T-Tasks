package com.teo.ttasks.api.entities;

import com.google.gson.annotations.Expose;
import com.teo.ttasks.data.model.Task;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TasksResponse extends RealmObject {

    @Expose
    @PrimaryKey
    public String id;

    @Expose
    public String etag;

    @Expose
    public String nextPageToken;

    @Expose
    public RealmList<Task> items;
}
