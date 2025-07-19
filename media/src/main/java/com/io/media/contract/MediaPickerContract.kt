package com.io.media.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import com.io.media.MediaActivity
import com.io.media.model.MediaModel
import com.io.media.picker.MediaPicker

class MediaPickerContract : ActivityResultContract<MediaPicker.MediaConfig, List<MediaModel>?>() {

    override fun createIntent(context: Context, input: MediaPicker.MediaConfig): Intent {
        val intent = Intent(context, MediaActivity::class.java)
        intent.putExtra("data", input)
        return intent

    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<MediaModel> {
        if (resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val ok = intent?.getSerializableExtra("data", ArrayList::class.java)
            } else {
                intent?.getSerializableExtra("data") as List<*>

            }
        }

        return arrayListOf()
    }
}