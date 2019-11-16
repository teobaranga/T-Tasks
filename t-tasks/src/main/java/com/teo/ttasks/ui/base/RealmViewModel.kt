package com.teo.ttasks.ui.base

import androidx.lifecycle.ViewModel
import io.realm.Realm

abstract class RealmViewModel : ViewModel() {

    protected val realm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    override fun onCleared() {
        realm.close()
        super.onCleared()
    }
}
