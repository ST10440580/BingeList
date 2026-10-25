package com.example.bingelist.ui.account

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.bingelist.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Profile photos live in the app's private storage, one file per signed-in user. */
object ProfilePhotoStore {

    private const val SIZE = 512

    private fun file(context: Context, uid: String): File {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        return File(dir, "$uid.jpg")
    }

    fun exists(context: Context, uid: String) = file(context, uid).exists()

    fun remove(context: Context, uid: String) {
        file(context, uid).delete()
    }

    /** Crops to a square, shrinks to 512 px, fixes rotation (Glide) and saves it. */
    suspend fun save(context: Context, uid: String, source: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = Glide.with(context.applicationContext)
                    .asBitmap().load(source).override(SIZE, SIZE).centerCrop()
                    .submit().get()
                FileOutputStream(file(context, uid)).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    /** Shows the user's round photo, or the default profile icon when there isn't one. */
    fun bind(view: ImageView, uid: String?, placeholderPaddingDp: Int) {
        val context = view.context
        val photo = uid?.let { file(context, it) }

        if (photo != null && photo.exists()) {
            view.setPadding(0, 0, 0, 0)
            view.background = null
            view.imageTintList = null
            Glide.with(view).load(photo)
                .signature(ObjectKey(photo.lastModified()))
                .circleCrop().into(view)
        } else {
            Glide.with(view).clear(view)
            val pad = (placeholderPaddingDp * context.resources.displayMetrics.density).toInt()
            view.setPadding(pad, pad, pad, pad)
            view.setBackgroundResource(R.drawable.rating_badge_background)
            view.setImageResource(R.drawable.ic_profile)
            view.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }
}