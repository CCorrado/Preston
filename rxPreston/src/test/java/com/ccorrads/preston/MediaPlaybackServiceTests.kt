package com.ccorrads.preston

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioManager
import com.ccorrads.preston.media.MediaPlaybackService
import com.ccorrads.preston.media.MediaPlayerObserver
import com.ccorrads.preston.models.MediaPlayerState
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations


@RunWith(JUnit4::class)
class MediaPlaybackServiceTests : TestCase() {

    private lateinit var observer: MediaPlayerObserver
    private lateinit var state: MediaPlayerState
    private lateinit var sourcePath: String

    @Mock
    private lateinit var context: Context

    @Mock
    private var assetManager: AssetManager? = null

    @Mock
    private var assetFileDescriptor: AssetFileDescriptor? = null

    @Mock
    private var audioManager: AudioManager? = null

    /**
     * Set up each test.
     */
    @Before
    fun setUpTests() {
        MockitoAnnotations.initMocks(this)
        context = Mockito.mock(Context::class.java)
        audioManager = Mockito.mock(AudioManager::class.java)

        doReturn(assetManager).`when`(context).assets
        assetFileDescriptor = Mockito.mock(AssetFileDescriptor::class.java)
        doReturn(assetFileDescriptor).`when`(assetManager)?.openFd(anyString())
        doReturn(audioManager).`when`(context).getSystemService(AUDIO_SERVICE)

        observer = MediaPlayerObserver()
        sourcePath = this.javaClass.classLoader.getResource("3_1_startup.mp3").path

        Assert.assertNotNull(observer)
    }

    /**
     * Assert that the observable is valid on the subscription.
     */
    @Test
    fun testPlaybackServiceSubscription() {
        //Sanity Checks
        val service = MediaPlaybackService().create(context, sourcePath,
                MediaPlaybackService.StateAction.ACTION_PLAY, false)
        assertNotNull(service)
        Assert.assertEquals(MediaPlaybackService.StateAction.ACTION_PLAY, service.stateAction)
        Assert.assertEquals(false, service.isMuted)
        Assert.assertEquals(sourcePath, service.mediaSource)

        val observable = service.mediaPlayerObservable.observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .test()
        Assert.assertTrue(observable.hasSubscription())

        observable.assertOf({
            Consumer<MediaPlayerState> {
                service.mediaPlayerEmitter.onNext(it)
            }
        })
    }

    /**
     * Assert that the MP is not null if you play.
     */
    @Test
    fun testPlay() {
        setStateToPlaying()
        Assert.assertNotNull(observer.mediaPlayer)
    }

    /**
     * Assert that the MP is null if you pause without playing.
     */
    @Test
    fun testPauseNotPlaying() {
        setStateToPaused()
        Assert.assertNull(observer.mediaPlayer)
    }

    /**
     * Assert that the MP is not null if you pause after playing.
     */
    @Test
    fun testPauseWhenPlaying() {
        setStateToPlaying()
        Assert.assertNotNull(observer.mediaPlayer)

        setStateToPaused()
        Assert.assertNotNull(observer.mediaPlayer)
    }

    /**
     * Assert that the media player is null when muting
     */
    @Test
    fun testMuteWhenNotPlaying() {
        setStateToMuted()
        Assert.assertTrue(observer.stateMuted)
        Assert.assertNull(observer.mediaPlayer)
    }

    /**
     * Assert the media player is never null when muting
     */
    @Test
    fun testMuteWhenPlaying() {
        setStateToPlaying()
        Assert.assertFalse(observer.stateMuted)
        Assert.assertNotNull(observer.mediaPlayer)

        setStateToMuted()
        Assert.assertTrue(observer.stateMuted)
        Assert.assertNotNull(observer.mediaPlayer)
    }

    /**
     * Assert the media player is never null when muting/unmuting
     */
    @Test
    fun testMuteThenUnmuteWhenPlaying() {
        setStateToPlaying()
        Assert.assertNotNull(observer.mediaPlayer)

        setStateToMuted()
        Assert.assertNotNull(observer.mediaPlayer)

        setStateToUnmuted()
        Assert.assertNotNull(observer.mediaPlayer)
    }

    /**
     * Assert that the media player is null before and after this call.
     */
    @Test
    fun testStopWhenNotPlaying() {
        Assert.assertNull(observer.mediaPlayer)
        setStateToStopped()
        Assert.assertNull(observer.mediaPlayer)
    }

    /**
     * Assert that we've stopped the media player and set it to null.
     */
    @Test
    fun testStopWhenPlaying() {
        setStateToPlaying()
        Assert.assertNotNull(observer.mediaPlayer)

        setStateToStopped()
        Assert.assertNull(observer.mediaPlayer)
    }

    private fun setStateToPlaying() {
        val playService = MediaPlaybackService().create(context, sourcePath,
                MediaPlaybackService.StateAction.ACTION_PLAY, false)

        state = MediaPlayerState(context, MediaPlaybackService.StateAction.ACTION_PLAY,
                sourcePath, playService, false)
        observer.onNext(state)
    }

    private fun setStateToPaused() {
        val pauseService = MediaPlaybackService().create(context, sourcePath,
                MediaPlaybackService.StateAction.ACTION_PAUSE_OR_RESUME, false)

        state = MediaPlayerState(context, MediaPlaybackService.StateAction.ACTION_PAUSE_OR_RESUME,
                sourcePath, pauseService, false)
        observer.onNext(state)
    }

    private fun setStateToStopped() {
        val stopService = MediaPlaybackService().create(context, sourcePath,
                MediaPlaybackService.StateAction.ACTION_STOP, false)

        state = MediaPlayerState(context, MediaPlaybackService.StateAction.ACTION_STOP,
                sourcePath, stopService, false)
        observer.onNext(state)
    }

    private fun setStateToMuted() {
        val muteService = MediaPlaybackService().create(context, sourcePath,
                MediaPlaybackService.StateAction.ACTION_MUTE, false)

        state = MediaPlayerState(context, MediaPlaybackService.StateAction.ACTION_MUTE,
                sourcePath, muteService, true)
        observer.onNext(state)
    }

    private fun setStateToUnmuted() {
        val unmuteService = MediaPlaybackService().create(context, sourcePath,
                MediaPlaybackService.StateAction.ACTION_UNMUTE, false)

        state = MediaPlayerState(context, MediaPlaybackService.StateAction.ACTION_UNMUTE,
                sourcePath, unmuteService, false)
        observer.onNext(state)
    }
}