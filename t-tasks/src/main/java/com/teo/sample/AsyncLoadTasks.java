/*
 * Copyright (c) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.teo.sample;

import com.google.api.services.tasks.model.Task;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Asynchronously load the tasks.
 *
 * @author Yaniv Inbar
 */
class AsyncLoadTasks extends CommonAsyncTask {

    AsyncLoadTasks(MainActivity mainActivity) {
        super(mainActivity);
    }

    @Override
    protected void doInBackground() throws IOException {

        // Get up to date tasks if connected to the Internet
        activity.ni = activity.cm.getActiveNetworkInfo();
        if (activity.ni != null){

            final TasksDBAdapter dbHelper = new TasksDBAdapter(activity.getApplicationContext());
            dbHelper.open();

            //Clean all data - replace all tasks
            dbHelper.deleteAllTasks();

            // This only gets the tasks from the default tasklist
            List<Task> tasks = client.tasks().list("@default").setFields("items(completed,deleted,due,notes,status,title,updated)").execute().getItems();
            if (tasks == null) {
                dbHelper.insertTask("user", "@default", "No Tasks", "N/A", "N/A");
            }
            else{
                // Sorting by due date
                // TODO: implement custom sorting
                Collections.sort(tasks, new Comparator<Task>() {
                    @Override
                    public int compare(Task task1, Task task2) {
                        String date1 = task1.getDue() == null ? "" : task1.getDue().toString();
                        String date2 = task2.getDue() == null ? "" : task2.getDue().toString();
                        return date1.compareTo(date2);
                    }
                });

                for (Task task : tasks){
                    // TODO: change user and @default
                    dbHelper.insertTask("user", "@default", task.getTitle(), task.getStatus(), task.getDue() == null ? "" : task.getDue().toString());
                }
            }
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    activity.adapter.swapCursor(dbHelper.fetchAllTasks());
                    dbHelper.close();
                }
            });
        }
    }

    static void run(MainActivity mainActivity) {
        new AsyncLoadTasks(mainActivity).execute();
    }
}

