package com.teo.ttasks.ui.activities.task_detail;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.teo.ttasks.BuildConfig;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ActivityTaskDetailBinding;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.util.AnimUtils.TaskDetailAnim;

import javax.inject.Inject;

public class TaskDetailActivity extends AppCompatActivity implements TaskDetailView {

    public static final String EXTRA_TASK_ID = "taskId";
    public static final String EXTRA_TASK_LIST_ID = "taskListId";

    private static final String ACTION_SKIP_ANIMATION = BuildConfig.APPLICATION_ID + "SKIP_ANIMATION";

    @Inject TaskDetailPresenter mTaskDetailPresenter;

    private ActivityTaskDetailBinding mBinding;

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
        mTaskDetailPresenter.bindView(this);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_task_detail);


        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_SKIP_ANIMATION))
            skipEnterAnimation();
        else
            enterAnimation();

        // Add a context menu to the task header
        registerForContextMenu(mBinding.taskHeader);

        // Get the task
        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskListId = getIntent().getStringExtra(EXTRA_TASK_LIST_ID);
        mTaskDetailPresenter.getTask(taskId);
        mTaskDetailPresenter.getTaskList(taskListId);
    }

    @Override
    public void onTaskLoaded(TTask task) {
        mBinding.setTask(task);
    }

    @Override
    public void onTaskLoadError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskListLoaded(TaskList taskList) {
        mBinding.setTaskList(taskList);
    }

    @Override
    public void onTaskListLoadError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskUpdated() {
        onBackPressed();
    }

    @Override
    public void onTaskDeleted() {
        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
        onBackPressed();
    }

    public void onFabClicked(View v) {
        mTaskDetailPresenter.updateCompletionStatus(taskListId);
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
                                mTaskDetailPresenter.deleteTask(taskListId, taskId);
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
        ViewPropertyAnimatorCompat backAnimator = TaskDetailAnim.animate(mBinding.back);
        ViewPropertyAnimatorCompat editAnimator = TaskDetailAnim.animate(mBinding.edit);
        ViewPropertyAnimatorCompat moreAnimation = TaskDetailAnim.animate(mBinding.more);
        ViewPropertyAnimatorCompat titleAnimator = TaskDetailAnim.animate(mBinding.taskTitle);
        ViewPropertyAnimatorCompat taskListTitleAnimator = TaskDetailAnim.animate(mBinding.taskListTitle);
        ViewPropertyAnimatorCompat fabAnimator = ViewCompat.animate(mBinding.fab)
                .scaleX(1.0F)
                .scaleY(1.0F)
                .alpha(1.0F)
                .setStartDelay(400)
                .setInterpolator(new OvershootInterpolator())
                .withLayer();
        backAnimator.start();
        editAnimator.start();
        moreAnimation.start();
        titleAnimator.start();
        taskListTitleAnimator.start();
        fabAnimator.start();
    }

    /**
     * Skip the enter animation.<br>
     * Used on versions below Lollipop because the lack of content transitions
     * makes the animations look like the app is slow.<br>
     * Also used when starting this activity from the widget, again because
     * there are no content transitions.
     */
    private void skipEnterAnimation() {
        mBinding.back.setAlpha(1f);
        mBinding.edit.setAlpha(1f);
        mBinding.more.setAlpha(1f);
        mBinding.taskTitle.setAlpha(1f);
        mBinding.taskListTitle.setAlpha(1f);
        mBinding.fab.setScaleX(1f);
        mBinding.fab.setScaleY(1f);
        mBinding.fab.setAlpha(1f);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBinding.taskHeader.setTransitionName(null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskDetailPresenter.unbindView(this);
    }
}
