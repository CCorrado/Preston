package com.ccorrads.prestonsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ccorrads.preston.media.MediaPlaybackService
import com.ccorrads.preston.media.MediaPlayerObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_sample.*

class SampleActivity : AppCompatActivity() {

    private val mediaService: MediaPlaybackService = MediaPlaybackService()
    private val mediaPlayerObserver: MediaPlayerObserver = MediaPlayerObserver()

    private val audioPath = "sample_audio/might_be_coffee.mp3"

    private var muted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        action_stop.setOnClickListener { stopMedia() }

        action_replay.setOnClickListener { playMedia(muted) }

        play_pause_toggle.setOnClickListener { pauseOrResumeMedia(muted) }

        mute_toggle.setOnCheckedChangeListener { _, checked ->
            muted = checked
            if (muted) {
                muteCurrentMP()
            } else {
                unmuteCurrentMP()
            }
        }
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
    private fun playMedia(muted: Boolean) {
        mediaService.create(applicationContext, audioPath,
                MediaPlaybackService.StateAction.ACTION_PLAY, muted)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Pause or Resume a running Media Player
     */
    private fun pauseOrResumeMedia(muted: Boolean) {
        mediaService.create(applicationContext, audioPath,
                MediaPlaybackService.StateAction.ACTION_PAUSE_OR_RESUME, muted)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Stop a Media Player.
     */
    private fun stopMedia() {
        mediaService.create(applicationContext, audioPath,
                MediaPlaybackService.StateAction.ACTION_STOP, false)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Muting a running Media Player without playing or pausing.
     */
    private fun muteCurrentMP() {
        mediaService.create(applicationContext, audioPath,
                MediaPlaybackService.StateAction.ACTION_MUTE, true)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Unmuting a running Media Player without playing or pausing.
     */
    private fun unmuteCurrentMP() {
        mediaService.create(applicationContext, audioPath,
                MediaPlaybackService.StateAction.ACTION_UNMUTE, false)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
}