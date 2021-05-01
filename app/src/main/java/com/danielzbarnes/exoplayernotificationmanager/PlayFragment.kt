package com.danielzbarnes.exoplayernotificationmanager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView

private const val TAG = "PlayFragment"

class PlayFragment: Fragment() {

    private lateinit var playerControlView: PlayerControlView
    private lateinit var backgroundPlayService: BackgroundPlayService

    private lateinit var mediaObj: MediaObject  // this is intended to be initialized from a database, etc

    private lateinit var title: TextView
    private lateinit var album: TextView

    private val connection = object : ServiceConnection{

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected()")

            if (service is BackgroundPlayService.BackgroundServiceBinder){

                if (service.getPlayingMediaID() != mediaObj.id){
                    Log.d(TAG, "mediaObj id has changed, play the new media")
                    backgroundPlayService = service.getService()
                    backgroundPlayService.updatePlayer(mediaObj)
                }


                backgroundPlayService = service.getService()
                playerControlView.player = service.getExoplayer()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected()")
            unBindService()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()")

        val view = inflater.inflate(R.layout.fragment_play, container, false)

        playerControlView = view.findViewById(R.id.player_control_view)
        title = view.findViewById(R.id.song_title)
        album = view.findViewById(R.id.album_title)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated()")

        // title.text = getTitleFun()
        // album.text = getAlbumFun()
        // album art, etc

        initService()

    }

    private fun initService() {
        Log.d(TAG, "initService()")

        val intent = Intent(requireContext(), BackgroundPlayService::class.java)
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun unBindService() {
        Log.d(TAG, "unBindService()")
        backgroundPlayService.stopSelf()
        requireActivity().unbindService(connection)
    }

    companion object {

        fun newInstance(): PlayFragment{
            return PlayFragment()
        }

    }

}
