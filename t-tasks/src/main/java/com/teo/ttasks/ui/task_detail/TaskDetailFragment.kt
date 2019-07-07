package com.teo.ttasks.ui.task_detail

import android.content.Context.WINDOW_SERVICE
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.teo.ttasks.R
import com.teo.ttasks.databinding.FragmentTaskDetailBinding
import com.teo.ttasks.util.ARG_TASK_ID
import com.teo.ttasks.util.ARG_TASK_LIST_ID
import com.teo.ttasks.util.dpToPx
import timber.log.Timber


class TaskDetailFragment : BottomSheetDialogFragment() {

    companion object {
        /** Create a new instance of this fragment */
        fun newInstance(taskId: String, taskListId: String): TaskDetailFragment {
            val tasksFragment = TaskDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TASK_ID, taskId)
                    putString(ARG_TASK_LIST_ID, taskListId)
                }
            }
            Timber.v("New TaskDetail fragment: ${tasksFragment.arguments}")
            return tasksFragment
        }
    }

    lateinit var fragmentTaskDetailBinding: FragmentTaskDetailBinding

    lateinit var taskId: String

    lateinit var taskListId: String

    lateinit var taskDetailViewModel: TaskDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use a theme with a transparent background
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)

        arguments?.let {
            it.getString(ARG_TASK_ID)?.let { id -> taskId = id }
            it.getString(ARG_TASK_LIST_ID)?.let { id -> taskListId = id }
        }

        taskDetailViewModel = activity?.run {
            ViewModelProviders.of(this)[TaskDetailViewModel::class.java]
        } ?: throw IllegalStateException("ViewModel could not be loaded")

        taskDetailViewModel.loadTask(taskId)
        taskDetailViewModel.loadTaskList(taskListId)

        taskDetailViewModel.task.observe(this, Observer {
            fragmentTaskDetailBinding.task = it
        })
        taskDetailViewModel.taskList.observe(this, Observer {
            fragmentTaskDetailBinding.taskList = it
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentTaskDetailBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_detail, container, false)

        return fragmentTaskDetailBinding.root
    }

    override fun onResume() {
        super.onResume()

        // Resize bottom sheet dialog so it doesn't span the entire width past a particular measurement
        val wm = activity!!.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels - 32.dpToPx()
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.setLayout(width, height)
    }
}
