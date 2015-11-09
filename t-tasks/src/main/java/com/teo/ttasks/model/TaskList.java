package com.teo.ttasks.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * @author Teo
 */
public class TaskList extends RealmObject {

    @PrimaryKey
    private String id;
    private String kind;
    private String selfLink;
    private String title;
    private String updated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }


}
