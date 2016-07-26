package com.teo.ttasks.data.model;

import com.google.gson.annotations.Expose;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Teo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskList extends RealmObject {

    @Expose
    @PrimaryKey
    private String id;

    @Expose
    private String kind;

    @Expose
    private String selfLink;

    @Expose
    private String title;

    @Expose
    private String updated;

    public String getTitle() {
        return title;
    }
}
