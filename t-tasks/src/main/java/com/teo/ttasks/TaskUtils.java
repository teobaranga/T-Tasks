package com.teo.ttasks;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

/**
 * @author Teo
 */
public class TaskUtils {

    private static final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    private static final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    public static class GetTokenTask extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private String mEmail;

        public GetTokenTask(Context context, String email) {
            mContext = context;
            mEmail = email;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                String token = fetchToken();
                if (token == null)
                    return null;
                // Use the token to access the user's Google data.
                Timber.d("Token: %s", token);
                GoogleCredential credential = new GoogleCredential().setAccessToken(token);
                // Tasks client
                TTasks.service = (new Tasks.Builder(httpTransport, jsonFactory, credential)
                        .setApplicationName("T-Tasks/1.0")
                        .build());
            } catch (IOException e) {
                // The fetchToken() method handles Google-specific exceptions,
                // so this indicates something went wrong at a higher level.
                // TIP: Check for network connectivity before starting the AsyncTask.
                // Network or server error, try later
                Timber.e(e.getMessage());
            }
            return null;
        }

        /**
         * Gets an authentication token from Google and handles any
         * GoogleAuthException that may occur.
         */
        private String fetchToken() throws IOException {
            try {
                if (mEmail == null)
                    return null;
                Timber.d("Fetching token for %s", mEmail);
                Account account = new Account(mEmail, "com.google");
                return GoogleAuthUtil.getToken(mContext, account, TTasks.Scopes);
            } catch (UserRecoverableAuthException userRecoverableException) {
                // GooglePlayServices.apk is either old, disabled, or not present
                // so we need to show the user some UI in the activity to recover.
                // Recover (with e.getIntent())
                userRecoverableException.printStackTrace();
            } catch (GoogleAuthException fatalException) {
                // Some other type of unrecoverable exception has occurred.
                // Report and log the error as appropriate for your app.

                // The call is not ever expected to succeed
                // assuming you have already verified that
                // Google Play services is installed.
                fatalException.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Gets every {@link TaskList} belonging to the user
     */
    public static class GetTaskLists extends AsyncTask<Void, Void, List<TaskList>> {

        @Override
        protected List<TaskList> doInBackground(Void... voids) {
            // Get up to date task lists if connected to the Internet
            try {
                Timber.d("Fetching task lists");
                // Get all task lists
                List<TaskList> taskLists  = TTasks.service.tasklists().list().execute().getItems();
                if (taskLists == null)
                    return null;
                for (TaskList taskList : taskLists) {
                    Timber.d(taskList.toPrettyString());
                }
                return taskLists;
            } catch (IOException e) {
                Timber.e(e.toString());
            }
            return null;
        }
    }

    /**
     * Asynchronously load the tasks.

     * @author Yaniv Inbar
     */
    public static class GetTasks extends AsyncTask<Void, Void, com.google.api.services.tasks.model.Tasks> {

        @Override
        public com.google.api.services.tasks.model.Tasks doInBackground(Void... ignored) {
            try {

                // This only gets the tasks from the default tasklist
                com.google.api.services.tasks.model.Tasks tasks = TTasks.service.tasks().list("@default").setFields("items(completed,deleted,due,notes,status,title,updated)").execute();
                if (tasks != null) {
                    // Sorting by due date
                    // TODO: implement custom sorting, not here
                    //                        Collections.sort(tasks.getItems(), new Comparator<Task>() {
                    //                            @Override
                    //                            public int compare(Task task1, Task task2) {
                    //                                String date1 = task1.getDue() == null ? "" : task1.getDue().toString();
                    //                                String date2 = task2.getDue() == null ? "" : task2.getDue().toString();
                    //                                return date1.compareTo(date2);
                    //                            }
                    //                        });

                    return tasks;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }
    }
}
