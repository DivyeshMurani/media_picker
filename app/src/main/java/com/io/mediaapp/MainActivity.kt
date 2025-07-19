package com.io.mediaapp

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        findViewById<TextView>(R.id.mTextView).performClick()
    }

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
}