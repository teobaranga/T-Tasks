package com.teo.ttasks.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.teo.ttasks.R
import com.teo.ttasks.util.dpToPx


class AccountInfoDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(): AccountInfoDialogFragment {
            return AccountInfoDialogFragment()
        }
    }

    interface AccountInfoListener {
        fun onSignOut()

        fun onSettingsShow()

        fun onAboutShow()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_account_info, container, false)

        view.findViewById<View>(R.id.settings).setOnClickListener {
            dismiss()
            (activity as AccountInfoListener).onSettingsShow()
        }

        view.findViewById<View>(R.id.about).setOnClickListener {
            dismiss()
            (activity as AccountInfoListener).onAboutShow()
        }

        view.findViewById<View>(R.id.sign_out).setOnClickListener {
            dismiss()
            (activity as AccountInfoListener).onSignOut()
        }

        return view
    }

    override fun onResume() {
        // Get existing layout params for the window
        val params = dialog!!.window!!.attributes
        params.gravity = Gravity.TOP
        params.y = 72.dpToPx()
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
        super.onResume()
    }
}
