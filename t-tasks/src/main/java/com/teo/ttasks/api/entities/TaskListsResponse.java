package com.teo.ttasks.api.entities;

import com.google.gson.annotations.Expose;
import com.teo.ttasks.data.model.TaskList;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TaskListsResponse extends RealmObject {

    /**
     * The ID of this TaskListResponse, which is the email of the user
     */
    @Expose
    @PrimaryKey
    public String id;

    @Expose
    public String etag;

    @Expose
    public RealmList<TaskList> items;
}
