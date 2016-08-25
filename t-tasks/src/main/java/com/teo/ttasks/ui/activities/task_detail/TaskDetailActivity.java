package com.teo.ttasks.ui.activities.task_detail;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.transition.Transition;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.teo.ttasks.BuildConfig;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ActivityTaskDetailBinding;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.util.AnimUtils;
import com.teo.ttasks.widget.TasksWidgetProvider;

import javax.inject.Inject;

public class TaskDetailActivity extends AppCompatActivity implements TaskDetailView {

    public static final String EXTRA_TASK_ID = "taskId";
    public static final String EXTRA_TASK_LIST_ID = "taskListId";

    private static final String ACTION_SKIP_ANIMATION = BuildConfig.APPLICATION_ID + "SKIP_ANIMATION";

    @Inject TaskDetailPresenter taskDetailPresenter;

    ActivityTaskDetailBinding taskDetailBinding;

    private String taskId;
    private String taskListId;

    public static void start(Context context, String taskId, String taskListId, @Nullable Bundle bundle) {
        Intent starter = getStartIntent(context, taskId, taskListId, Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);
        context.startActivity(starter, bundle);
    }

    /**
     * Get the Intent template used to start this activity from outside the application.
     * This is part of the process used when starting this activity from the widget.
     *
     * @param context    context
     * @param taskListId task list identifier
     * @return an incomplete Intent that needs to be completed by adding the extra
     * {@link #EXTRA_TASK_ID} before being used to start this activity
     */
    public static Intent getIntentTemplate(Context context, String taskListId) {
        Intent starter = new Intent(context, TaskDetailActivity.class);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        starter.setAction(ACTION_SKIP_ANIMATION);
        return starter;
    }

    public static Intent getStartIntent(Context context, String taskId, String taskListId, boolean skipAnimation) {
        Intent starter = new Intent(context, TaskDetailActivity.class);
        starter.putExtra(EXTRA_TASK_ID, taskId);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        if (skipAnimation)
            starter.setAction(ACTION_SKIP_ANIMATION);
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(this).userComponent().inject(this);
        taskDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_task_detail);
        taskDetailPresenter.bindView(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getEnterTransition().addListener(new AnimUtils.TransitionListener() {
                @Override public void onTransitionEnd(Transition transition) {
                    ViewCompat.animate(taskDetailBinding.fab)
                            .scaleX(1.0F)
                            .scaleY(1.0F)
                            .alpha(1.0F)
                            .setInterpolator(new OvershootInterpolator())
                            .withLayer().start();
                }
            });
        }

        String action = getIntent().getAction();
        if (savedInstanceState != null || action != null && action.equals(ACTION_SKIP_ANIMATION)) {
            // Skip the animation if requested or if the activity is getting recreated
            skipEnterAnimation();
        } else {
            enterAnimation();
        }

        // Add a context menu to the task header
        registerForContextMenu(taskDetailBinding.taskHeader);

        // Get the task
        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskListId = getIntent().getStringExtra(EXTRA_TASK_LIST_ID);
        taskDetailPresenter.getTask(taskId);
        taskDetailPresenter.getTaskList(taskListId);
    }

    @Override
    public void onTaskLoaded(TTask task) {
        taskDetailBinding.setTask(task);
    }

    @Override
    public void onTaskLoadError() {
        Toast.makeText(this, R.string.error_task_loading, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onTaskListLoaded(TaskList taskList) {
        taskDetailBinding.setTaskList(taskList);
    }

    @Override
    public void onTaskListLoadError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskUpdated() {
        TasksWidgetProvider.updateWidgets(this, taskListId);
        onBackPressed();
    }

    @Override
    public void onTaskDeleted() {
        TasksWidgetProvider.updateWidgets(this, taskListId);
        Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show();
        onBackPressed();
    }

    public void onFabClicked(View v) {
        taskDetailPresenter.updateCompletionStatus();
    }

    public void onBackClicked(View v) {
        onBackPressed();
    }

    public void onEditClicked(View v) {
        EditTaskActivity.startEdit(this, taskId, taskListId, null);
    }

    public void onOverflowClicked(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setGravity(Gravity.END);
        popup.inflate(R.menu.menu_task_detail);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.delete:
                    DialogInterface.OnClickListener dialogClickListener = (dialog, choice) -> {
                        switch (choice) {
                            case DialogInterface.BUTTON_POSITIVE:
                                taskDetailPresenter.deleteTask();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    };
                    new AlertDialog.Builder(this)
                            .setMessage("Delete this task?")
                            .setPositiveButton(android.R.string.yes, dialogClickListener)
                            .setNegativeButton(android.R.string.no, dialogClickListener)
                            .show();
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void enterAnimation() {
        AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
        fadeInAnimation.setDuration(200);
        fadeInAnimation.setStartOffset(100);
        taskDetailBinding.back.startAnimation(fadeInAnimation);
        taskDetailBinding.edit.startAnimation(fadeInAnimation);
        taskDetailBinding.more.startAnimation(fadeInAnimation);
        taskDetailBinding.taskTitle.startAnimation(fadeInAnimation);
        taskDetailBinding.taskListTitle.startAnimation(fadeInAnimation);
    }

    /**
     * Skip the enter animation.<br>
     * Used on versions below Lollipop because the lack of content transitions
     * makes the animations look like the app is slow.<br>
     * Also used when starting this activity from the widget, again because
     * there are no content transitions.
     */
    private void skipEnterAnimation() {
        taskDetailBinding.fab.setScaleX(1f);
        taskDetailBinding.fab.setScaleY(1f);
        taskDetailBinding.fab.setAlpha(1f);
    }

    @Override
    public void onBackPressed() {
        // Remove the transition name so that the reverse shared transition doesn't play
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            taskDetailBinding.taskHeader.setTransitionName(null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        taskDetailPresenter.unbindView(this);
    }
}
