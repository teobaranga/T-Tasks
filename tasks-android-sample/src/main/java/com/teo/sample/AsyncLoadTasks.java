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

import android.support.v7.widget.RecyclerView;

import com.google.api.services.tasks.model.Task;

import java.io.IOException;
import java.util.ArrayList;
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

        final ArrayList<Triplet<String,String,String>> tasksList = new ArrayList<>();
        List<Task> tasks = client.tasks().list("@default").setFields("items(title,status,due)").execute().getItems();
        if (tasks != null) {
            for (Task task : tasks) {
                tasksList.add(new Triplet<>(task.getTitle(), task.getStatus(), (task.getDue() == null) ? "null" : task.getDue().toString()));
            }
        } else {
            tasksList.add(new Triplet<>("No tasks.", "", ""));
        }

        //Sorting
        Collections.sort(tasksList, new Comparator<Triplet<String,String,String>>() {
            @Override
            public int compare(Triplet<String,String,String> task1, Triplet<String,String,String> task2) {
                return task1.getCompleted().compareTo(task2.getCompleted());
            }
        });

        activity.tasksList = tasksList;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recyclerView);
                ((RecyclerAdapter)recyclerView.getAdapter()).setDataset(tasksList);
                recyclerView.getAdapter().notifyDataSetChanged();
                activity.mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    static void run(MainActivity mainActivity) {
        new AsyncLoadTasks(mainActivity).execute();
    }
}

