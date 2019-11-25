package com.teo.ttasks.ui.activities.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MenuItem
import com.androidhuman.rxfirebase2.auth.rxSignOut
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.teo.ttasks.UserManager
import com.teo.ttasks.data.local.PrefHelper
import com.teo.ttasks.ui.base.MvpView
import com.teo.ttasks.ui.base.Presenter
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class MainActivityPresenter(
    private val context: Context,
    private val prefHelper: PrefHelper,
    private val userManager: UserManager,
    private val firebaseAuth: FirebaseAuth
) : Presenter<MvpView>() {

    private open class ProfileIconTarget(private val targetFile: File) : Target {

        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            Timber.e(e, "Failed to load profile icon")
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    }

    private var profileIconTarget: ProfileIconTarget? = null

    internal fun loadProfilePicture(menuItem: MenuItem) {
        firebaseAuth.currentUser?.let { firebaseUser ->
            val photoFile = File(context.cacheDir, firebaseUser.uid)
            val photoUrl = firebaseUser.photoUrl.toString()
            when {
                photoUrl != prefHelper.userPhoto -> {
                    Timber.v("New profile photo: %s", photoUrl)
                    profileIconTarget = object : ProfileIconTarget(photoFile) {
                        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                            super.onBitmapLoaded(bitmap, from)

                            // Load successful, save the URL to preferences
                            prefHelper.userPhoto = photoUrl

                            // Apply the bitmap
                            menuItem.icon = BitmapDrawable(context.resources, bitmap)

                            profileIconTarget = null
                        }
                    }.also { target ->
                        Picasso.get()
                            .load(photoUrl)
                            .into(target)
                    }
                }
                photoFile.exists() -> {
                    Timber.v("Loading profile photo from disk")
                    menuItem.icon = BitmapDrawable(context.resources, photoFile.absolutePath)
                }
                else -> {
                    Timber.w("Profile photo not found on disk, resetting photo URL in preferences")
                    prefHelper.userPhoto = null
                }
            }
        }
    }

    internal fun signOut() {
        val disposable = firebaseAuth.rxSignOut()
            .onErrorComplete {
                Timber.e(it, "There was an error signing out from Firebase, ignoring")
                return@onErrorComplete true
            }
            .andThen(userManager.signOut())
            .onErrorComplete {
                Timber.e(it, "There was an error signing out from Google, ignoring")
                return@onErrorComplete true
            }
            .subscribe({
                Timber.d("Signed out")
            }, {
                Timber.e(it, "Could not sign out")
            })
        disposeOnUnbindView(disposable)
    }
}
