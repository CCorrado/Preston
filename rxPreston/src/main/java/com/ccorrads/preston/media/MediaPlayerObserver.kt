package com.ccorrads.preston.media

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.MediaPlayer
import android.os.Build
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import android.util.Log
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.io.Serializable
import java.lang.IllegalStateException
import java.math.BigDecimal

/**
 * Observer to subscribe to when needing to play a media file.
 */
class MediaPlayerObserver : Observer<MediaPlayerState>, Serializable {

    @Transient
    @VisibleForTesting
    var mediaPlayer: MediaPlayer? = null

    @Transient
    private lateinit var appContext: Context
    @Transient
    private var focusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    @VisibleForTesting
    var stateMuted: Boolean = false

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
            MediaPlaybackService.StateAction.ON_PREPARED -> {
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                    mediaPlayer!!.reset()
                    return
                }
                this.focusChangeListener = createAudioFocusListener()
                mediaPlayer?.start()
                if (stateMuted) {
                    performMute()
                } else {
                    performUnmute()
                }
            }
            MediaPlaybackService.StateAction.ACTION_PLAY -> {
                mediaPlay(mediaSource, mediaPlayerState.mediaPlaybackService)
            }
            MediaPlaybackService.StateAction.ACTION_PAUSE_OR_RESUME -> mediaPause()
            MediaPlaybackService.StateAction.ACTION_STOP -> {
                releaseMediaPlayer(mediaPlayer)
            }
            MediaPlaybackService.StateAction.ACTION_MUTE -> performMute()
            MediaPlaybackService.StateAction.ACTION_UNMUTE -> performUnmute()
        }
    }

    private fun performMute() {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                abandonAudioFocus(focusChangeListener)
                mediaPlayer?.setVolume(0f, 0f)
            }
            //if the mediaPlayer is not currently set up to mute, log this exception safely.
        } catch (err: IllegalStateException) {
            Log.e(TAG, err.message, err)
        }
    }

    private fun performUnmute() {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                this.focusChangeListener = createAudioFocusListener()
                mediaPlayer?.setVolume(BigDecimal.valueOf(1).toFloat(),
                        BigDecimal.valueOf(1).toFloat())
            }
            //if the mediaPlayer is not currently set up to unmute, log this exception safely.
        } catch (err: IllegalStateException) {
            Log.e(TAG, err.message, err)
        }
    }

    private fun createAudioFocusListener(): AudioManager.OnAudioFocusChangeListener {
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (mediaPlayer != null
                        && mediaPlayer!!.isPlaying && !stateMuted) {
                    mediaPlayer!!.setVolume(BigDecimal.valueOf(AUDIO_VOL_LOSS_LEVEL).toFloat(),
                            BigDecimal.valueOf(AUDIO_VOL_LOSS_LEVEL).toFloat())
                }
                AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, AudioManager.AUDIOFOCUS_GAIN,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> if (mediaPlayer != null
                        && mediaPlayer!!.isPlaying && !stateMuted) {
                    mediaPlayer!!.setVolume(BigDecimal.valueOf(1).toFloat(),
                            BigDecimal.valueOf(1).toFloat())
                }
            }
        }
        abandonAudioFocus(focusChangeListener)
        val am = appContext.getSystemService(AUDIO_SERVICE) as AudioManager

        //Android O and above Audio Focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()

            val focusRequestBuilder = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .setAudioAttributes(audioAttributes)

            am.requestAudioFocus(focusRequestBuilder.build())

        } else {
            am.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
        return focusChangeListener
    }

    /**
     * Method to control what happens when ACTION PAUSE is received.
     */
    private fun mediaPause() {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                abandonAudioFocus(focusChangeListener)
            }
            statePaused = if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
                true
            } else {
                if (statePaused) {
                    mediaPlayer?.start()
                    if (stateMuted) {
                        performMute()
                    } else {
                        performUnmute()
                    }
                }
                false
            }
            //If the media player tries to pause/resume after completion without getting reset.
        } catch (err: IllegalStateException) {
            Log.e(TAG, err.message, err)
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
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        try {
            appContext.assets.openFd(mediaSource).use({ assetFileDescriptor ->

                mediaPlayer?.setOnPreparedListener(mediaPlaybackService)
                mediaPlayer?.setOnErrorListener(mediaPlaybackService)
                mediaPlayer?.setOnCompletionListener(mediaPlaybackService)
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length)
                mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            })
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        }

        try {
            mediaPlayer?.prepareAsync()
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
        abandonAudioFocus(null)
        mp?.stop()
        mp?.reset()
        mp?.release()
        mediaPlayer = null
    }

    private fun abandonAudioFocus(focusChangeListener: AudioManager.OnAudioFocusChangeListener?) {
        val am = appContext.getSystemService(AUDIO_SERVICE) as AudioManager
        am.abandonAudioFocus(focusChangeListener)
    }

    companion object {

        private val TAG = MediaPlayerObserver::class.java.simpleName

        private const val AUDIO_VOL_LOSS_LEVEL = 0.2

        private var statePaused: Boolean = false
    }
}
