package com.teo.ttasks.ui.fragments.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.KoinComponent
import org.koin.core.inject

class AccountsInfoViewModel: ViewModel(), KoinComponent {

    private val firebaseAuth: FirebaseAuth by inject()

    private val _accountName = MutableLiveData<String>()
    val accountName: LiveData<String>
        get() = _accountName

    private val _accountEmail = MutableLiveData<String>()
    val accountEmail: LiveData<String>
        get() = _accountEmail

    init {
        firebaseAuth.currentUser?.let {
            it.displayName?.let { name ->
                _accountName.value = name
            }
            _accountEmail.value = it.email
        }
    }
}
