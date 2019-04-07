package com.ccorrads.preston.media

import android.annotation.TargetApi
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
import android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
import android.media.MediaPlayer
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.io.Serializable

/**
 * Observer to subscribe to when needing to play a media file.
 */
open class MediaPlayerObserver : Observer<MediaPlayerState>, Serializable {

    @Transient
    @VisibleForTesting
    var mediaPlayer: MediaPlayer? = null

    @Transient
    private lateinit var appContext: Context

    @Transient
    private var focusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    @Transient
    private var focusRequest: AudioFocusRequest? = null

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
                mediaPlayer?.let { mp ->

                    if (mp.isPlaying) {
                        mp.stop()
                        mp.reset()
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
            }
            MediaPlaybackService.StateAction.ACTION_PLAY -> {
                mediaPlay(mediaSource, mediaPlayerState.mediaPlaybackService)
            }
            MediaPlaybackService.StateAction.ACTION_PAUSE_OR_RESUME -> mediaPause()
            MediaPlaybackService.StateAction.ACTION_STOP -> releaseMediaPlayer(mediaPlayer)
            MediaPlaybackService.StateAction.ACTION_MUTE -> performMute()
            MediaPlaybackService.StateAction.ACTION_UNMUTE -> performUnmute()
        }
    }

    private fun performMute() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    abandonAudioFocus(focusChangeListener, focusRequest)
                    mp.setVolume(0f, 0f)
                }
            }
            //if the mediaPlayer is not currently set up to mute, log this exception safely.
        } catch (err: IllegalStateException) {
            Log.e(TAG, err.message, err)
        }
    }

    private fun performUnmute() {
        val am = appContext.getSystemService(AUDIO_SERVICE) as? AudioManager
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    this.focusChangeListener = createAudioFocusListener()
                    val currentVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val leftVol = currentVol ?: 1
                    val rightVol = currentVol ?: 1
                    mp.setVolume(leftVol.toFloat(), rightVol.toFloat())
                }
            }
            //if the mediaPlayer is not currently set up to unmute, log this exception safely.
        } catch (err: IllegalStateException) {
            Log.e(TAG, err.message, err)
        }
    }

    private fun createAudioFocusListener(): AudioManager.OnAudioFocusChangeListener {
        val am = appContext.getSystemService(AUDIO_SERVICE) as? AudioManager
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying && !stateMuted) {
                            mp.setVolume(0.2F, 0.2F)
                        }
                    }

                AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ->
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying && !stateMuted) {
                            val currentVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val leftVol = currentVol?.toFloat() ?: AUDIO_VOL_LOSS_LEVEL
                            val rightVol = currentVol?.toFloat() ?: AUDIO_VOL_LOSS_LEVEL
                            mp.setVolume(leftVol, rightVol)
                        }
                    }
            }
        }
        abandonAudioFocus(focusChangeListener, focusRequest)
        //Android O and above Audio Focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = getFocusRequest(focusChangeListener)
            am?.requestAudioFocus(focusRequest)
        } else {
            am?.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
        return focusChangeListener
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun getFocusRequest(focusChangeListener: AudioManager.OnAudioFocusChangeListener?): AudioFocusRequest {
        return AudioFocusRequest.Builder(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setAudioAttributes(getAudioAttributes()).build()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    }

    /**
     * Method to control what happens when ACTION PAUSE is received.
     */
    private fun mediaPause() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    abandonAudioFocus(focusChangeListener, focusRequest)
                }
                statePaused = if (mp.isPlaying) {
                    mp.pause()
                    true
                } else {
                    if (statePaused) {
                        mp.start()
                        if (stateMuted) {
                            performMute()
                        } else {
                            performUnmute()
                        }
                    }
                    false
                }
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
            appContext.assets.openFd(mediaSource).use { assetFileDescriptor ->

                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.setOnPreparedListener(mediaPlaybackService)
                    mp.setOnErrorListener(mediaPlaybackService)
                    mp.setOnCompletionListener(mediaPlaybackService)

                    mp.reset()
                    mp.setDataSource(assetFileDescriptor.fileDescriptor,
                            assetFileDescriptor.startOffset,
                            assetFileDescriptor.length)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mp.setAudioAttributes(getAudioAttributes())
                    } else {
                        mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        }

        try {
            mediaPlayer?.prepareAsync()
        } catch (error: IllegalStateException) {
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
        abandonAudioFocus(null, null)
        mp?.stop()
        mp?.reset()
        mp?.release()
        mediaPlayer = null
    }

    private fun abandonAudioFocus(focusChangeListener: AudioManager.OnAudioFocusChangeListener?,
                                  focusRequest: AudioFocusRequest?) {
        val am = appContext.getSystemService(AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest != null) {
                am.abandonAudioFocusRequest(focusRequest)
            }
        } else {
            am.abandonAudioFocus(focusChangeListener)
        }
    }

    companion object {

        private val TAG = MediaPlayerObserver::class.java.simpleName

        private const val AUDIO_VOL_LOSS_LEVEL = 0.4F

        private var statePaused: Boolean = false
    }
}
