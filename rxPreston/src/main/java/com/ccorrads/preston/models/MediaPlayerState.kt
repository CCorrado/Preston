package com.ccorrads.preston.models

import android.content.Context
import com.ccorrads.preston.media.MediaPlaybackService

data class MediaPlayerState(val appContext: Context, val mediaPlayerState: MediaPlaybackService.StateAction,
                            val mediaSource: String, val mediaPlaybackService: MediaPlaybackService,
                            val isMuted: Boolean)