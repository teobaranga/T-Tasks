package com.teo.ttasks.ui.fragments.accounts

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.teo.ttasks.R
import com.teo.ttasks.databinding.DialogAccountInfoBinding
import com.teo.ttasks.ui.activities.main.MainViewModel
import com.teo.ttasks.util.dpToPx
import org.koin.android.viewmodel.ext.android.sharedViewModel

class AccountInfoDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(): AccountInfoDialogFragment {
            return AccountInfoDialogFragment()
        }
    }

    private lateinit var binding: DialogAccountInfoBinding

    private lateinit var accountsInfoViewModel: AccountsInfoViewModel

    private val viewModel by sharedViewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountsInfoViewModel = activity?.run {
            ViewModelProvider(this)[AccountsInfoViewModel::class.java]
        } ?: throw IllegalStateException("ViewModel could not be loaded")

        with(accountsInfoViewModel) {
            val owner = this@AccountInfoDialogFragment

            accountEmail.observe(owner, Observer {
                binding.accountEmail.text = it
            })

            accountName.observe(owner, Observer {
                binding.accountName.text = it
            })
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_account_info, container, false)
        binding.viewModel = viewModel
        return binding.root
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
