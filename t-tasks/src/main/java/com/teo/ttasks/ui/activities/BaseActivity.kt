package com.teo.ttasks.ui.activities

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.teo.ttasks.R
import dagger.android.support.DaggerAppCompatActivity

abstract class BaseActivity : DaggerAppCompatActivity() {

    private var toolbar: Toolbar? = null

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupToolbar()
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        setupToolbar()
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        super.setContentView(view, params)
        setupToolbar()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        toolbar?.let { toolbar -> setSupportActionBar(toolbar) }
    }

    fun toolbar(): Toolbar? = toolbar
}
