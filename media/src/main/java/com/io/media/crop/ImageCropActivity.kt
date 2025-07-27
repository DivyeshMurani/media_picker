package com.io.media.crop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.io.media.R
import com.io.media.databinding.ActivityImageCropBinding
import java.io.File
import java.io.FileOutputStream

class ImageCropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageCropBinding
    private var originalImagePath: String? = null
    private var croppedImagePath: String? = null

    companion object {
        private const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_CROPPED_IMAGE_PATH = "extra_cropped_image_path"

        fun start(context: Context, imagePath: String) {
             val intent = Intent(context, ImageCropActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_PATH, imagePath)
            }

            if (context is Activity) {
                context.startActivityForResult(intent, CROP_REQUEST_CODE)
            } else {
                context.startActivity(intent)
            }
        }

        const val CROP_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCropView()
        setupControls()
        loadImage()


    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupCropView() {
        // Initialize crop view
        binding.cropImageView.setOnClickListener {
            // Handle crop view clicks if needed
        }

        binding.cropImageView.setAspectRatio(1f)

    }

    private fun setupControls() {
        // Aspect ratio controls
        binding.aspectRatioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btnFree -> binding.cropImageView.setAspectRatio(0f)
                R.id.btnSquare -> binding.cropImageView.setAspectRatio(1f)
                R.id.btn16x9 -> binding.cropImageView.setAspectRatio(16f / 9f)
                R.id.btn4x3 -> binding.cropImageView.setAspectRatio(4f / 3f)
            }
        }

        // Default to square (1:1) aspect ratio
        binding.btnSquare.isChecked = true

        // Action buttons
        binding.btnReset.setOnClickListener {
            binding.cropImageView.resetCrop()
        }

        binding.btnRotate.setOnClickListener {
            binding.cropImageView.rotateImage()
        }

        binding.btnCrop.setOnClickListener {
            performCrop()
        }
    }

    private fun loadImage() {
        originalImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)

        Log.e("developer", "originalImagePath ::: $originalImagePath")

        originalImagePath?.let { path ->
            when {
                path.startsWith("content://") -> {
                    // Load from content URI
                    loadImageFromContentUri(path)
                }

                path.startsWith("http://") || path.startsWith("https://") -> {
                    // Load from URL using Glide
                    loadImageFromUrl(path)
                }

                else -> {
                    // Load from local file path
                    loadImageFromFile(path)
                }
            }
        } ?: run {
            showError("No image path provided")
        }
    }


    private fun loadImageFromUrl(url: String) {
        Glide.with(this).asBitmap().load(url).listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap>,
                    isFirstResource: Boolean
                ): Boolean {
                    showError("Failed to load image from URL: ${e?.message}")
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Bitmap>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.cropImageView.setImageBitmap(resource)
                    return false
                }
            }).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // This will be handled by the listener
                }
            })
    }

    private fun loadImageFromContentUri(uri: String) {
        try {
            val contentUri = android.net.Uri.parse(uri)
            val inputStream = contentResolver.openInputStream(contentUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {

                binding.cropImageView.viewTreeObserver.addOnGlobalLayoutListener {
                    // Now width and height are available
//                    cropImageView.setImageBitmap(bitmap)
                    binding.cropImageView.setImageBitmap(bitmap)

                }

//                binding.cropImageView.setImageBitmap(bitmap)
            } else {
                showError("Failed to load image from content URI")
            }
        } catch (e: Exception) {
            showError("Error loading image from content URI: ${e.message}")
        }
    }

    private fun loadImageFromFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    binding.cropImageView.setImageBitmap(bitmap)
                } else {
                    showError("Failed to load image")
                }
            } else {
                showError("Image file not found")
            }
        } catch (e: Exception) {
            showError("Error loading image: ${e.message}")
        }
    }

    private fun performCrop() {
        try {
            val croppedBitmap = binding.cropImageView.getCroppedBitmap()

            if (croppedBitmap != null) {
                // Save cropped image
                croppedImagePath = saveCroppedImage(croppedBitmap)

                if (croppedImagePath != null) {
                    // Return the cropped image path
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_CROPPED_IMAGE_PATH, croppedImagePath)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    showError("Failed to save cropped image")
                }
            } else {
                showError("Failed to crop image")
            }
        } catch (e: Exception) {
            showError("Error cropping image: ${e.message}")
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap): String? {
        return try {
            val outputDir = File(cacheDir, "cropped_images")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, "cropped_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cropImageView.viewTreeObserver.removeOnGlobalLayoutListener {

        }
    }
} 