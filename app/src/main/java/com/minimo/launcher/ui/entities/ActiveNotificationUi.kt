package com.minimo.launcher.ui.entities

import android.app.Notification
import android.app.PendingIntent

data class ActiveNotificationUi(
    val key: String,
    val packageName: String,
    val className: String,
    val userHandle: Int,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isAutoCancel: Boolean,
    val contentIntent: PendingIntent?,
    val replyAction: Notification.Action?
)
