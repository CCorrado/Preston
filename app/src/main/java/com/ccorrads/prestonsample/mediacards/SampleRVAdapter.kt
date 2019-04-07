package com.ccorrads.prestonsample.mediacards

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ccorrads.preston.media.MediaPlaybackService
import com.ccorrads.preston.media.MediaPlayerObserver
import com.ccorrads.prestonsample.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.card_media_item.view.*

class SampleRVAdapter(private var context: Context, private var mediaItems: List<MediaItem>) : RecyclerView.Adapter<MediaViewHolder>() {

    private val mediaService: MediaPlaybackService = MediaPlaybackService()
    private val mediaPlayerObserver: MediaPlayerObserver = MediaPlayerObserver()

    private var muted: Boolean = false

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MediaViewHolder {
        val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.card_media_item,
                viewGroup, false)
        return MediaViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem: MediaItem = mediaItems[position]
        holder.itemView.song_title.text = mediaItem.title

        holder.itemView.action_stop.setOnClickListener { stopMedia(mediaItem.sourcePath) }

        holder.itemView.action_replay.setOnClickListener { playMedia(muted, mediaItem.sourcePath) }

        holder.itemView.play_pause_toggle.setOnClickListener { pauseOrResumeMedia(muted, mediaItem.sourcePath) }

        holder.itemView.mute_toggle.setOnCheckedChangeListener { _, checked ->
            muted = checked
            if (muted) {
                muteCurrentMP(mediaItem.sourcePath)
            } else {
                unmuteCurrentMP(mediaItem.sourcePath)
            }
        }
    }

    override fun getItemCount(): Int {
        val meals: List<MediaItem> = mediaItems
        return meals.size
    }

    fun onPauseResume() {
        pauseOrResumeMedia(false, "")
    }

    /**
     * Play a Media Player with some assets path.
     */
    private fun playMedia(muted: Boolean, audioPath: String) {
        mediaService.create(context, audioPath,
                MediaPlaybackService.StateAction.ACTION_PLAY, muted)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Pause or Resume a running Media Player
     */
    private fun pauseOrResumeMedia(muted: Boolean, audioPath: String) {
        mediaService.create(context, audioPath,
                MediaPlaybackService.StateAction.ACTION_PAUSE_OR_RESUME, muted)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Stop a Media Player.
     */
    private fun stopMedia(audioPath: String) {
        mediaService.create(context, audioPath,
                MediaPlaybackService.StateAction.ACTION_STOP, false)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Muting a running Media Player without playing or pausing.
     */
    private fun muteCurrentMP(audioPath: String) {
        mediaService.create(context, audioPath,
                MediaPlaybackService.StateAction.ACTION_MUTE, true)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }

    /**
     * Unmuting a running Media Player without playing or pausing.
     */
    private fun unmuteCurrentMP(audioPath: String) {
        mediaService.create(context, audioPath,
                MediaPlaybackService.StateAction.ACTION_UNMUTE, false)
                .mediaPlayerObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaPlayerObserver)
    }
}