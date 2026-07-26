package com.minimo.launcher.utils

import android.app.Notification
import android.app.PendingIntent
import com.minimo.launcher.data.PreferenceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDotsNotifier @Inject constructor(
    preferenceHelper: PreferenceHelper
) {
    private val _activeNotifications = MutableStateFlow<List<ActiveNotification>>(emptyList())

    val notificationDots: Flow<List<NotificationDot>> = combine(
        _activeNotifications,
        preferenceHelper.getNotificationDot().distinctUntilChanged()
    ) { notifications, enable ->
        if (enable) {
            notifications.map { NotificationDot(it.packageName, it.userHandle) }.distinct()
        } else {
            emptyList()
        }
    }

    val activeNotifications: Flow<List<ActiveNotification>> = combine(
        _activeNotifications,
        preferenceHelper.getNotificationPanel().distinctUntilChanged()
    ) { notifications, enable ->
        if (enable) notifications.sortedByDescending { it.postTime } else emptyList()
    }

    suspend fun getNotificationDots(): List<NotificationDot> {
        return notificationDots.firstOrNull() ?: emptyList()
    }

    fun updateActiveNotifications(notifications: List<ActiveNotification>) {
        _activeNotifications.value = notifications
    }
}

data class NotificationDot(
    val packageName: String,
    val userHandle: Int
)

data class ActiveNotification(
    val key: String,
    val packageName: String,
    val userHandle: Int,
    val postTime: Long,
    val title: String,
    val text: String,
    val isAutoCancel: Boolean,
    val contentIntent: PendingIntent?,
    val replyAction: Notification.Action?
)
