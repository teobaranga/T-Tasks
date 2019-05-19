package com.teo.ttasks.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.ui.LibsActivity
import com.teo.ttasks.R
import com.teo.ttasks.util.NightHelper

class AboutActivity : LibsActivity() {

    companion object {
        fun start(context: Context) = context.startActivity(Intent(context, AboutActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        intent = LibsBuilder()
            //Pass the fields of your application to the lib so it can find all external lib information
            .withFields(R.string::class.java.fields)
            .withAboutAppName(resources.getString(R.string.app_name))
            .withAboutVersionShownName(true)
            .withAboutDescription(getString(R.string.about_description))
            .withAboutIconShown(true)
            .withActivityTheme(R.style.Theme_About)
            .withLicenseShown(true)
            .withActivityTitle(getString(R.string.about))
            .intent(this)

        super.onCreate(savedInstanceState)

        if (!NightHelper.isNight(this)) {
            val toolbar = findViewById<Toolbar>(com.mikepenz.aboutlibraries.R.id.toolbar)
            toolbar.setTitleTextColor(Color.WHITE)
            toolbar.setSubtitleTextColor(Color.WHITE)
        }
    }
}
