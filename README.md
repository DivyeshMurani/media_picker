📸 **Media Picker Library for Android**

A lightweight and easy-to-use Android library for selecting images and videos (single or multiple) with built-in video playback support.

🎥 Features:
✅ Pick single/multiple images

✅ Pick single/multiple videos

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
