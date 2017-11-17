package com.ccorrads.preston

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ccorrads.preston.media.MediaPlaybackService
import com.ccorrads.preston.media.MediaPlayerObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SampleActivity : AppCompatActivity() {

    private val mediaService: MediaPlaybackService = MediaPlaybackService()
    private val mediaPlayerObserver: MediaPlayerObserver = MediaPlayerObserver()

    private val audioPath = "sample_audio/might_be_coffee.mp3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        playMedia(false)
    }

    override fun onPause() {
        super.onPause()
        pauseOrResumeMedia(false)
    }

    override fun onResume() {
        super.onResume()
        pauseOrResumeMedia(false)
    }

    /**
     * Play a Media Player with some assets path.
     */
    fun playMedia(muted: Boolean) {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_PLAY, muted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Pause or Resume a running Media Player
     */
    fun pauseOrResumeMedia(muted: Boolean) {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_PAUSE, muted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Stop a Media Player.
     */
    fun stopMedia() {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_STOP, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Muting a running Media Player without playing or pausing.
     */
    fun muteCurrentMP() {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_MUTE, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Unmuting a running Media Player without playing or pausing.
     */
    fun unmuteCurrentMP(context: Context) {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_UNMUTE, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
}