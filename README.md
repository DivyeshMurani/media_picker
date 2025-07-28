📸 **Media Picker Library for Android**

A lightweight and easy-to-use Android library for selecting images and videos (single or multiple) with built-in video playback support.

🎥 Features:

✅ Pick single/multiple images
✅ Pick single/multiple videos
✅ Image Crop
✅ Video playback preview
✅ Image/video file path access
✅ Modern Material UI
✅ Supports runtime permissions
✅ Android API 16–34+ supported


💻Usage
      
     
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private fun initMediaPicker() {
        scope.launch {
            val item =
                MediaPicker.Builder(this@MainActivity)
                    .setMediaType(MediaPicker.MediaType.VIDEO)
                    .enableMultiSelect()
                    .setMaxItems(5)
                    .pickMedia()

            var path = ""
            item.forEach {
                path += "\n ${it.path}"
                write("$path")
            }

            findViewById<TextView>(R.id.mTextView).text = path
        }

    }

Image Crop

      ImageCropActivity.start(
                this@MainActivity,
                path)

      override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImageCropActivity.CROP_REQUEST_CODE && resultCode == RESULT_OK) {
            val croppedImagePath = data?.getStringExtra(ImageCropActivity.EXTRA_CROPPED_IMAGE_PATH)

            Glide.with(this@MainActivity).load(croppedImagePath).into(findViewById(R.id.aci))
        }
    }
