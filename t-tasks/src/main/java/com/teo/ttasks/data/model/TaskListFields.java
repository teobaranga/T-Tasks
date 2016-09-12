package com.teo.ttasks.data.model;

import android.support.annotation.Nullable;

import java.util.HashMap;

public class TaskListFields extends HashMap<String, Object> {

    private static final String KEY_TITLE = "title";

    public void putTitle(@Nullable String title) {
        if (title == null || title.isEmpty()) {
            remove(KEY_TITLE);
            return;
        }
        put(KEY_TITLE, title);
    }

    public String getTitle() {
        return (String) get(KEY_TITLE);
    }
}
