package com.ccorrads.preston.media

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.MediaPlayer
import android.text.TextUtils
import android.util.Log
import com.ccorrads.preston.media.MediaPlaybackService.Companion.ACTION_MUTE
import com.ccorrads.preston.media.MediaPlaybackService.Companion.ACTION_PAUSE
import com.ccorrads.preston.media.MediaPlaybackService.Companion.ACTION_PLAY
import com.ccorrads.preston.media.MediaPlaybackService.Companion.ACTION_STOP
import com.ccorrads.preston.media.MediaPlaybackService.Companion.ACTION_UNMUTE
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.io.Serializable
import java.math.BigDecimal

/**
 * Observer to subscribe to when needing to play a media file.
 */
class MediaPlayerObserver : Observer<MediaPlayerState>, Serializable {

    @Transient private val mediaPlayer: MediaPlayer = MediaPlayer()
    @Transient private lateinit var appContext: Context
    @Transient private var focusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    private var stateMuted: Boolean = false

    override fun onSubscribe(disposable: Disposable) {
        Log.d(TAG, "MP Observer Subscribed")
    }

    override fun onNext(mediaPlayerState: MediaPlayerState) {
        configureMediaPlayer(mediaPlayerState)
    }

    override fun onError(throwable: Throwable) {
        Log.e(TAG, throwable.message, throwable)

        //Release the Media Player upon error state.
        releaseMediaPlayer(mediaPlayer)
    }

    override fun onComplete() {
        Log.d(TAG, "MP Observer Finished")
        //Release the media player upon completion.
        releaseMediaPlayer(mediaPlayer)
    }

    /**
     * Method to control what happens when the media player is initiated.
     *
     * @param mediaPlayerState -- Model class to contain the instance of media player and data.
     */
    private fun configureMediaPlayer(mediaPlayerState: MediaPlayerState) {
        val mediaSource = mediaPlayerState.mediaSource
        this.appContext = mediaPlayerState.appContext
        this.stateMuted = mediaPlayerState.isMuted

        when (mediaPlayerState.mediaPlayerState) {
            ON_COMPLETED -> {
                abandonAudioFocus(focusChangeListener)
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.reset()
            }
            ON_ERROR -> releaseMediaPlayer(mediaPlayer)
            ON_PREPARED -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    return
                }
                this.focusChangeListener = createAudioFocusListener(mediaPlayer)
                mediaPlayer.start()
                if (stateMuted) {
                    performMute()
                } else {
                    performUnmute()
                }
            }
            ACTION_PLAY -> {
                mediaPlay(mediaSource, mediaPlayerState.mediaPlaybackService)
            }
            ACTION_PAUSE -> mediaPause()
            ON_RESUME -> if (mediaPlayer.isPlaying) {
                abandonAudioFocus(focusChangeListener)
                statePaused = true
                mediaPlayer.pause()
            }
            ACTION_STOP -> {
                releaseMediaPlayer(mediaPlayer)
            }
            ACTION_MUTE -> performMute()
            ACTION_UNMUTE -> performUnmute()
        }
    }

    private fun performMute() {
        abandonAudioFocus(focusChangeListener)
        if (mediaPlayer.isPlaying) {
            mediaPlayer.setVolume(0f, 0f)
        }
    }

    private fun performUnmute() {
        if (mediaPlayer.isPlaying) {
            this.focusChangeListener = createAudioFocusListener(mediaPlayer)
            mediaPlayer.setVolume(BigDecimal.valueOf(1).toFloat(),
                    BigDecimal.valueOf(1).toFloat())
        }
    }

    private fun createAudioFocusListener(mediaPlayer: MediaPlayer?): AudioManager.OnAudioFocusChangeListener {
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (!stateMuted) {
                    mediaPlayer?.setVolume(BigDecimal.valueOf(AUDIO_VOL_LOSS_LEVEL).toFloat(),
                            BigDecimal.valueOf(AUDIO_VOL_LOSS_LEVEL).toFloat())
                }
                AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, AudioManager.AUDIOFOCUS_GAIN,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> if (!stateMuted) {
                    mediaPlayer?.setVolume(BigDecimal.valueOf(1).toFloat(),
                            BigDecimal.valueOf(1).toFloat())
                }
            }
        }
        abandonAudioFocus(focusChangeListener)
        val am = appContext.getSystemService(AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        return focusChangeListener
    }

    /**
     * Method to control what happens when ACTION PAUSE is received.
     */
    private fun mediaPause() {
        abandonAudioFocus(focusChangeListener)
        statePaused = if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            true
        } else {
            if (statePaused) {
                mediaPlayer.start()
                if (stateMuted) {
                    performMute()
                } else {
                    performUnmute()
                }
            }
            false
        }
    }

    /**
     * Method to control what happens when ACTION PLAY is received.
     *
     * @param mediaSource -- the String URI of where the file is located on the device.
     */
    private fun mediaPlay(mediaSource: String, mediaPlaybackService: MediaPlaybackService) {
        if (TextUtils.isEmpty(mediaSource)) {
            return
        }
        try {
            appContext.resources.assets.openFd(mediaSource).use({ assetFileDescriptor ->

                mediaPlayer.setOnPreparedListener(mediaPlaybackService)
                mediaPlayer.setOnErrorListener(mediaPlaybackService)
                mediaPlayer.setOnCompletionListener(mediaPlaybackService)
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.reset()
                mediaPlayer.setDataSource(assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length)
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            })
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        }

        try {
            mediaPlayer.prepareAsync()
        } catch (error: Exception) {
            //catch possible "setDataSource" errors if the file is not found on the device.
            Log.e(TAG, error.message, error)
        }
    }

    /**
     * Method to release the Media Player.
     *
     * @param mp -- media player to nullify.
     */
    private fun releaseMediaPlayer(mp: MediaPlayer?) {
        abandonAudioFocus(focusChangeListener)
        if (mp != null && mp.isPlaying) {
            mp.stop()
        }
        mp?.reset()
        mp?.release()
    }

    private fun abandonAudioFocus(focusChangeListener: AudioManager.OnAudioFocusChangeListener?) {
        val am = appContext.getSystemService(AUDIO_SERVICE) as AudioManager
        am.abandonAudioFocus(focusChangeListener)
    }

    companion object {

        private val TAG = MediaPlayerObserver::class.java.simpleName

        val ON_PREPARED = "ON_PREPARED"
        private val ON_ERROR = "ON_ERROR"
        private val ON_COMPLETED = "ON_COMPLETED"
        private val ON_RESUME = "ON_RESUME"

        private val AUDIO_VOL_LOSS_LEVEL = 0.2

        private var statePaused: Boolean = false
    }
}
