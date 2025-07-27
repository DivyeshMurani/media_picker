package com.io.mediaapp

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.io.media.crop.ImageCropActivity
import com.io.media.picker.MediaPicker
import com.io.media.util.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.mTextView).setOnClickListener {
            initMediaPicker()
        }

        findViewById<AppCompatImageView>(R.id.aci).setOnClickListener {
            initMediaPicker()

        }

        findViewById<TextView>(R.id.mTextView).performClick()
    }

    private fun initMediaPicker() {
        scope.launch {
            val item =
                MediaPicker.Builder(this@MainActivity).setMediaType(MediaPicker.MediaType.ALL)
                    .enableMultiSelect().setMediaExtension(MediaPicker.MediaExtension.ALL)
                    .setMaxItems(5).pickMedia()

            var path = ""
            item.forEach {
                path += "\n ${it.path}"
                write("$path")
            }

            ImageCropActivity.start(
                this@MainActivity,
                item[0].path ?: "")
            findViewById<TextView>(R.id.mTextView).text = path
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImageCropActivity.CROP_REQUEST_CODE && resultCode == RESULT_OK) {
            val croppedImagePath = data?.getStringExtra(ImageCropActivity.EXTRA_CROPPED_IMAGE_PATH)

            Glide.with(this@MainActivity).load(croppedImagePath).into(findViewById(R.id.aci))
        }
    }
}