package com.ccorrads.preston.media

import android.content.Context
import android.media.MediaPlayer
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class MediaPlaybackService : MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private lateinit var mediaSource: String
    private lateinit var mediaPlayerEmitter: ObservableEmitter<MediaPlayerState>
    private lateinit var context: Context

    lateinit var mediaPlayerObservable: Observable<MediaPlayerState>

    private var isMuted: Boolean = false

    fun create(appContext: Context, source: String, action: String, muted: Boolean): Observable<MediaPlayerState> {
        mediaSource = source
        isMuted = muted
        context = appContext
        return createObservable(action)
    }

    @Synchronized private fun createObservable(action: String): Observable<MediaPlayerState> {
        mediaPlayerObservable = Observable.create { observableEmitter ->
            mediaPlayerEmitter = observableEmitter
            mediaPlayerEmitter.onNext(configureMediaPlayerState(action, mediaSource, isMuted))
        }
        return mediaPlayerObservable
    }

    override fun onPrepared(player: MediaPlayer) {
        mediaPlayerEmitter.onNext(configureMediaPlayerState(MediaPlayerObserver.ON_PREPARED, mediaSource, isMuted))
    }

    override fun onCompletion(mp: MediaPlayer) {
        mediaPlayerEmitter.onComplete()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        mediaPlayerEmitter.onError(Throwable())
        return false
    }

    /**
     * Configure the MP State Model with data for the Observer.
     *
     * @param intentAction -- the Service intent action passed in from the activity.
     * @param mediaSource  -- the Media Source (URI) passed in from the activity.
     * @return -- a Configured MediaPlayerState object.
     */
    private fun configureMediaPlayerState(intentAction: String, mediaSource: String,
                                          isMuted: Boolean): MediaPlayerState {
        return MediaPlayerState(context, intentAction, mediaSource, this, isMuted)
    }

    companion object {

        val ACTION_PLAY = "ACTION_PLAY"
        val ACTION_PAUSE = "ACTION_PAUSE"
        val ACTION_STOP = "ACTION_STOP"
        val ACTION_MUTE = "ACTION_MUTE"
        val ACTION_UNMUTE = "ACTION_UNMUTE"
        val STATE_MUTED = "STATE_MUTED"
    }
}