package com.io.media

import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.io.media.base.BaseActivity
import com.io.media.databinding.ActivityMediaPlayerBinding

class MediaPlayerActivity : BaseActivity<ActivityMediaPlayerBinding>() {

    private lateinit var player: ExoPlayer

    override val layoutId: Int
        get() = R.layout.activity_media_player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getDataFromIntent()
    }


    fun getDataFromIntent() {

        val url = intent.getStringExtra("url") ?: ""

        Log.d("TAG", "getDataFromIntent: $url")

        player= ExoPlayer.Builder(this).build()

        binding.mPlayer.player = player


        player.addListener(object : Player.Listener{
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.d("TAG", "onPlayerError: ${error.message}")
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                Log.d("TAG", "onPlayerErrorChanged: ${error?.message}")
            }
        })





        val mediaItem =
            MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

     


    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()

    }

}