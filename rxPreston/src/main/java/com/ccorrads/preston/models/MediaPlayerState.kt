package com.ccorrads.preston.models

import android.content.Context
import com.ccorrads.preston.media.MediaPlaybackService

data class MediaPlayerState(val appContext: Context, val mediaPlayerState: String,
                            val mediaSource: String, val mediaPlaybackService: MediaPlaybackService,
                            val isMuted: Boolean)