package com.teo.ttasks.util;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class TaskUtils {

    private static final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    /**
     * Gets every {@link TaskList} belonging to the user
     */
    public static class GetTaskLists extends AsyncTask<Void, Void, List<TaskList>> {

        private com.google.api.services.tasks.Tasks mService = null;

        public GetTaskLists(GoogleAccountCredential credential) {
            // Tasks client
            mService = new Tasks.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("T-Tasks")
                    .build();
        }

        @Override
        protected List<TaskList> doInBackground(Void... voids) {
            // Get up to date task lists
            // TODO: only if connected to the Internet
            Timber.d("Fetching all task lists...");
            try {
                return mService.tasklists().list().execute().getItems();
            } catch (IOException e) {
                Timber.e(e.toString());
                cancel(true);
                return null;
            }
        }
    }

    /**
     * Get the tasks belonging to a specific task list
     */
    public static class GetTasks extends AsyncTask<Void, Void, com.google.api.services.tasks.model.Tasks> {

        private String mTaskListId;
        private com.google.api.services.tasks.Tasks mService = null;

        public GetTasks(String tasklistId, GoogleAccountCredential credential) {
            mTaskListId = tasklistId;
            mService = new Tasks.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("T-Tasks")
                    .build();
        }

        @Override
        public com.google.api.services.tasks.model.Tasks doInBackground(Void... ignored) {
            Timber.d("Fetching the tasks for the task list with ID %s", mTaskListId);
            try {
                return mService.tasks().list(mTaskListId).execute();
            } catch (IOException e) {
                Timber.e(e.toString());
                cancel(true);
                return null;
            }
        }
    }

    public static Date parseRFC3339Date(String datestring) {
        Date d;
        try {
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()); //spec for RFC3339
            d = s.parse(datestring);
        } catch (ParseException pe) {
            //try again with optional decimals
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());//spec for RFC3339 (with fractional seconds)
            s.setLenient(true);
            try {
                d = s.parse(datestring);
            } catch (ParseException e) {
                return null;
            }
        }
        return d;
    }
}
