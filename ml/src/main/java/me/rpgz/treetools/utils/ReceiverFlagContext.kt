package me.rpgz.treetools.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat

class ReceiverFlagContext(app: Context) : ContextWrapper(app) {
    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter
    ): Intent? {
        // Android 13+ 要求显式选择，优先不导出
        return if (Build.VERSION.SDK_INT >= 33) {
            // 优先使用兼容库，能在各版本下做对
            ContextCompat.registerReceiver(
                this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            super.registerReceiver(receiver, filter)
        }
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?, filter: IntentFilter, broadcastPermission: String?, scheduler: Handler?
    ): Intent? {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.registerReceiver(
                this, receiver, filter, broadcastPermission, scheduler,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            super.registerReceiver(receiver, filter, broadcastPermission, scheduler)
        }
    }
}
