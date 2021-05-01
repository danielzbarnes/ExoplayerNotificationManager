package com.danielzbarnes.exoplayernotificationmanager

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerNotificationManager

private const val TAG = "BackgroundPlayer"


// Android Developers Backstage Podcast Episode 162 chosen completely at random for an example audio source
private const val MEDIA_URL = "https://traffic.libsyn.com/secure/adbackstage/ADB162-1.5.mp3?dest-id=2710847"

class BackgroundPlayService: Service() {

    private val backgroundPlayIBinder: IBinder = BackgroundServiceBinder()

    private var exoPlayer: SimpleExoPlayer? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaObj: MediaObject // object that can hold details for the playing item, title/url/etc
    private lateinit var context: Context

    private val channelId = "Exoplayer Channel"
    private val notificationId: Int = 123987

    private var playbackPos: Long = 0
    private var playWhenReady = true

    override fun onCreate() {
        super.onCreate()
        context = this
    }

    private fun initializePlayer(context: Context){
        Log.d(TAG, "initializePlayer()")

        if (exoPlayer == null)
            exoPlayer = SimpleExoPlayer.Builder(context).build()

        val mediaItem = MediaItem.fromUri(MEDIA_URL)
        exoPlayer?.apply {

            setMediaItem(mediaItem)
            playWhenReady = true
            seekTo(playbackPos)
            prepare()
        }

        initPlayerNotificationManager()

        playerNotificationManager.setPlayer(exoPlayer)

    }

    private fun initPlayerNotificationManager() {

        Log.d(TAG, "initPlayerNotificationManager()")
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            this, channelId, R.string.channel_name, R.string.channel_desc,
            notificationId, object : PlayerNotificationManager.MediaDescriptionAdapter{

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return "media title"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(context, PlayFragment::class.java)
                    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return "reference/context"
                }

                override fun getCurrentSubText(player: Player): CharSequence? {
                    return "a subtitle"
                }

                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    return null
                }
            }, object : PlayerNotificationManager.NotificationListener{
                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    Log.d(TAG, "NotificationListener.onNotificationPosted(), startForeground()")
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    Log.d(TAG, "NotificationListener.onNotificationCancelled(), selfStop()")
                    stopSelf()
                    stopForeground(true)
                }
            }
        )
    }

    private fun releasePlayer(){
        Log.d(TAG, "releasePlayer()")

        playerNotificationManager.setPlayer(null)

        if (exoPlayer != null){
            playWhenReady = exoPlayer!!.playWhenReady
            playbackPos = exoPlayer!!.currentPosition

            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // method for updating the player used in conjunction with a list of media items
    fun updatePlayer(media: MediaObject){

        this.mediaObj = media
        val mediaItem = MediaItem.fromUri(mediaObj.mp3url)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.playWhenReady = true
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind()")
        initializePlayer(context)
        return backgroundPlayIBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind()")
        releasePlayer()
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        releasePlayer()
        stopSelf()
    }

    inner class BackgroundServiceBinder: Binder(){
        fun getExoplayer() = exoPlayer
        fun getService() =this@BackgroundPlayService
        fun getPlayingMediaID() = mediaObj.id
    }

}