package com.teo.ttasks

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import com.teo.ttasks.injection.component.DaggerTestApplicationComponent
import com.teo.ttasks.injection.component.TestApplicationComponent

class DaggerAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        // Inject the Test component instead of the real component
        (app as TTasksApp).applicationComponent =
                DaggerTestApplicationComponent.builder().create(app) as TestApplicationComponent
        super.callApplicationOnCreate(app)
    }
}
