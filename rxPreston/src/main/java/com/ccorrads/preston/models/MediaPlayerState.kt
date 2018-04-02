package com.ccorrads.preston.models

import android.content.Context
import com.ccorrads.preston.media.MediaPlaybackService

/**
 * Class to represent a requested state of the MediaPlayer. Required input to the MediaPlayerService
 */
data class MediaPlayerState(val appContext: Context, val mediaPlayerState: MediaPlaybackService.StateAction,
                            val mediaSource: String, val mediaPlaybackService: MediaPlaybackService,
                            val isMuted: Boolean)