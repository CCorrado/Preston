package com.ccorrads.preston.media

import android.content.Context
import android.media.MediaPlayer
import android.support.annotation.VisibleForTesting
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

/**
 * This Service is meant to route and delegate the Media Player callbacks to the managed
 * Observable/Emitter
 */
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

    /**
     * Method to create an initialized instance of this service.
     *
     * @param appContext -- Context to pass into the state for the Media Player
     * @param source     -- the Media Source (URI) passed in.
     * @param action     -- {@link StateAction} enum item to delegate to the media player.
     * @param muted      -- whether or not the audio should "play" muted
     */
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

    private fun configureMediaPlayerState(intentAction: StateAction, mediaSource: String,
                                          isMuted: Boolean): MediaPlayerState {
        return MediaPlayerState(context, intentAction, mediaSource, this, isMuted)
    }

    /**
     * Enum class to define the handled States accepted by the MediaPlayerObserver
     */
    enum class StateAction {
        ACTION_PLAY,
        ACTION_PAUSE_OR_RESUME,
        ACTION_STOP,
        ACTION_MUTE,
        ON_PREPARED,
        ACTION_UNMUTE
    }
}