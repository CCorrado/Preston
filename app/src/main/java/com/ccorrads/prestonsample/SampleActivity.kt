package com.ccorrads.prestonsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.ccorrads.prestonsample.mediacards.MediaItem
import com.ccorrads.prestonsample.mediacards.SampleRVAdapter
import kotlinx.android.synthetic.main.activity_sample.*

class SampleActivity : AppCompatActivity() {

    private var adapter: SampleRVAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val sourcePaths = resources.getStringArray(R.array.source_paths)
        val sourceTitles = resources.getStringArray(R.array.source_titles)
        val mediaItems = ArrayList<MediaItem>()

        for ((index, path) in sourcePaths.withIndex()) {
            val item = MediaItem(path, sourceTitles[index])
            mediaItems.add(item)
        }

        sample_rv.layoutManager = LinearLayoutManager(this)
        adapter = SampleRVAdapter(this, mediaItems)
        sample_rv.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        adapter?.onPauseResume()
    }

    override fun onResume() {
        super.onResume()
        adapter?.onPauseResume()
    }
}