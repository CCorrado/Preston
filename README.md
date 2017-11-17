# Preston
Preston is a helpful wrapper around Android's MediaPlayer using RxJava.

### Why Preston?
Android's Media Player is a pain point for a lot of Android Developers. There is a lot to consider.
* `MediaPlayerObserver` Takes care of the Media Player "state" for you by allowing you to subscribe to an emitting Observable from `MediaPlaybackService`.
* Audio Focus is handled by the Observer which allows you to not worry about multiple audio streams.
* 

### API
The API is simple. There are a few different ways you can play back, pause (or resume), stop, mute (or unmute) an audio stream.

* Playing using Preston
```
    /**
     * Play a Media Player with some assets path.
     */
    fun playMedia(muted: Boolean) {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_PLAY, muted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
```
* Pause (or Resume) using Preston ... This is for use with onPause() and onResume() of your lifecycle.
```

    /**
     * Pause or Resume a running Media Player
     */
    fun pauseOrResumeMedia(muted: Boolean) {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_PAUSE, muted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
```
* Stop the MP.
```
    /**
     * Stop a Media Player.
     */
    fun stopMedia() {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_STOP, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
```
* Mute a running MP. This will attempt to mute the current media player, if it is playing.
```
    /**
     * Muting a running Media Player without playing or pausing.
     */
    fun muteCurrentMP() {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_MUTE, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
```
* Unmute a running MP. Similarly, this will attempt to unmute the current media player.
```
    /**
     * Unmuting a running Media Player without playing or pausing.
     */
    fun unmuteCurrentMP(context: Context) {
        mediaService.create(applicationContext, audioPath, MediaPlaybackService.ACTION_UNMUTE, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
```
