package com.example.wallpapers

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.OutputStream
import androidx.core.view.isGone

class WallpaperDetailActivity : AppCompatActivity() {

    lateinit var imageView: ImageView
    lateinit var buttonContainer: LinearLayout
    lateinit var horizontalScroll: HorizontalScrollView
    lateinit var backButton: ImageButton
    lateinit var saveButton: ImageButton
    lateinit var shareButton: ImageButton
    lateinit var fullscreenButton: ImageButton

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_detail)

        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""

        bindViews()
        setupImageView()
        setupButtons(imageUrl)
        loadWallpaper(imageUrl)
    }

    private fun bindViews() {
        imageView = findViewById(R.id.iv_wallpaper)
        buttonContainer = findViewById(R.id.button_container)
        horizontalScroll = findViewById(R.id.horizontal_scroll)
        backButton = findViewById(R.id.btn_back)
        saveButton = findViewById(R.id.btn_save)
        shareButton = findViewById(R.id.btn_share)
        fullscreenButton = findViewById(R.id.btn_fullscreen)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupImageView() {
        imageView.setOnClickListener {
            if (buttonContainer.isGone) {
                exitFullscreen()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupButtons(imageUrl: String) {
        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener { saveImageToGallery(imageUrl) }

        shareButton.setOnClickListener { showShareSheet(imageUrl) }

        fullscreenButton.setOnClickListener { enterFullscreen() }
    }

    private fun loadWallpaper(url: String) {
        Glide.with(this)
            .load(url)
            .into(imageView)

        horizontalScroll.post {
            val centerX = (imageView.width - horizontalScroll.width) / 2
            if (centerX > 0) {
                horizontalScroll.scrollTo(centerX, 0)
            }
        }
    }

    private fun saveImageToGallery(url: String) {
        Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()

        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val filename = "Wallpaper_${System.currentTimeMillis()}.jpg"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    uri?.let {
                        val fos: OutputStream? = contentResolver.openOutputStream(it)
                        fos?.use { stream ->
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            Toast.makeText(this@WallpaperDetailActivity, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
    }

    private fun showShareSheet(imageUrl: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this wallpaper: $imageUrl")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun enterFullscreen() {
        val controller = window.decorView.windowInsetsController
        val systemBars = android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()

        val params = imageView.layoutParams

        buttonContainer.visibility = View.GONE
        controller?.hide(systemBars)
        controller?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        params.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        params.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        imageView.scaleType = ImageView.ScaleType.FIT_XY

        imageView.layoutParams = params
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun exitFullscreen() {
        val controller = window.decorView.windowInsetsController
        val systemBars = android.view.WindowInsets.Type.statusBars() or
                android.view.WindowInsets.Type.navigationBars()

        buttonContainer.visibility = View.VISIBLE
        buttonContainer.alpha = 0f
        buttonContainer.animate().alpha(1f).duration = 300

        controller?.show(systemBars)

        val params = imageView.layoutParams as android.widget.FrameLayout.LayoutParams
        params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        params.gravity = android.view.Gravity.CENTER

        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = params

        horizontalScroll.scrollTo(0, 0)
    }

}