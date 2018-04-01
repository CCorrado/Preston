package com.ccorrads.preston.media

import android.content.Context
import android.media.MediaPlayer
import android.support.annotation.VisibleForTesting
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class MediaPlaybackService : MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private lateinit var context: Context

    @VisibleForTesting
    lateinit var mediaSource: String

    @VisibleForTesting
    lateinit var mediaPlayerEmitter: ObservableEmitter<MediaPlayerState>

    @VisibleForTesting
    lateinit var mediaPlayerObservable: Observable<MediaPlayerState>

    @VisibleForTesting
    var isMuted: Boolean = false

    @VisibleForTesting
    lateinit var stateAction: StateAction

    fun create(appContext: Context, source: String, action: StateAction, muted: Boolean): MediaPlaybackService {
        mediaSource = source
        isMuted = muted
        context = appContext
        stateAction = action
        mediaPlayerObservable = createObservable()
        return this
    }

    @Synchronized
    private fun createObservable(): Observable<MediaPlayerState> {
        return Observable.create { observableEmitter ->
            mediaPlayerEmitter = observableEmitter
            mediaPlayerEmitter.onNext(configureMediaPlayerState(stateAction, mediaSource, isMuted))
        }
    }

    override fun onPrepared(player: MediaPlayer) {
        mediaPlayerEmitter.onNext(configureMediaPlayerState(StateAction.ON_PREPARED, mediaSource, isMuted))
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
    private fun configureMediaPlayerState(intentAction: StateAction, mediaSource: String,
                                          isMuted: Boolean): MediaPlayerState {
        return MediaPlayerState(context, intentAction, mediaSource, this, isMuted)
    }

    enum class StateAction {
        ACTION_PLAY,
        ACTION_PAUSE_OR_RESUME,
        ACTION_STOP,
        ACTION_MUTE,
        ON_PREPARED,
        ACTION_UNMUTE
    }
}