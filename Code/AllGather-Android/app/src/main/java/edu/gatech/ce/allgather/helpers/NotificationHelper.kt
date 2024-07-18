package edu.gatech.ce.allgather.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

import edu.gatech.ce.allgather.R


/**
 * NotificationHelper
 * @author Justin Lee
 * @date 2020/6/11
 */
class NotificationHelper(val context: Context) : ContextWrapper(context) {
    companion object {
        const val CHANNEL_ID = "allgather"
        private const val CHANNEL_NAME = "Allgather Channel"
        private const val CHANNEL_DESCRIPTION = "this is Allgather channel!"
    }

    private var notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var mNotificationChannel: NotificationChannel

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationChannel.description = CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(mNotificationChannel)
        }
    }

    fun getOngoingNotification(title: String, content: String = ""): NotificationCompat.Builder {
        var builder: NotificationCompat.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            builder = NotificationCompat.Builder(this)
            builder.priority = NotificationCompat.PRIORITY_DEFAULT
        }
        builder.setContentTitle(title)
        if (content.isNotEmpty())
            builder.setContentText(content)
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
        builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
        builder.setOngoing(true)
        return builder
    }

    fun notify(id: Int, builder: NotificationCompat.Builder) {
        notificationManager.notify(id, builder.build())
    }

    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openChannelSetting(channelId: String?) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openNotificationSetting() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) startActivity(intent)
    }
}