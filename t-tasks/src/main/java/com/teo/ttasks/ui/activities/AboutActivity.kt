package com.teo.ttasks.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat

import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.ui.LibsActivity
import com.mikepenz.aboutlibraries.util.Colors
import com.teo.ttasks.R
import com.teo.ttasks.util.NightHelper

class AboutActivity : LibsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val night = NightHelper.isNight(this)

        val colors = Colors(
                ResourcesCompat.getColor(resources, if (night) R.color.md_grey_850 else R.color.colorPrimary, null),
                ResourcesCompat.getColor(resources, if (night) R.color.md_grey_900 else R.color.colorPrimaryDark, null)
        )

        intent = LibsBuilder()
                //Pass the fields of your application to the lib so it can find all external lib information
                .withFields(R.string::class.java.fields)
                .withAboutAppName(resources.getString(R.string.app_name))
                .withAboutVersionShownName(true)
                .withAboutDescription(getString(R.string.about_description))
                .withAboutIconShown(true)
                .withActivityStyle(if (night) Libs.ActivityStyle.DARK else Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withActivityColor(colors)
                .withLicenseShown(true)
                .withActivityTitle(getString(R.string.about))
                .intent(this)

        super.onCreate(savedInstanceState)
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }
}
