package com.minimo.launcher.utils

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LauncherNotificationListenerService : NotificationListenerService() {
    @Inject
    lateinit var notificationDotsNotifier: NotificationDotsNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Cache to keep track of active notifications without making repeated IPC calls.
    // Maps notification key -> notification data
    private val activeNotificationsCache = mutableMapOf<String, ActiveNotification>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.d("onListenerConnected")
        instance = this
        syncNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Timber.d("onListenerDisconnected")
        instance = null
        activeNotificationsCache.clear()
        notificationDotsNotifier.updateActiveNotifications(emptyList())
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val isValid = sbn.isClearable && !sbn.isOngoing

        if (isValid) {
            activeNotificationsCache[sbn.key] = sbn.toActiveNotification()
        } else {
            activeNotificationsCache.remove(sbn.key)
        }

        notificationDotsNotifier.updateActiveNotifications(activeNotificationsCache.values.toList())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        if (activeNotificationsCache.remove(sbn.key) != null) {
            notificationDotsNotifier.updateActiveNotifications(activeNotificationsCache.values.toList())
        }
    }

    private fun syncNotifications() {
        serviceScope.launch {
            try {
                // activeNotifications makes a synchronous Binder IPC call to the System Server.
                // We fetch this result on the IO thread to prevent main thread blocking / UI jank.
                val notifications = withContext(Dispatchers.IO) {
                    activeNotifications
                } ?: return@launch

                // We switch back to the Main thread to update the cache.
                // This avoids race conditions with onNotificationPosted/Removed, which are invoked by the OS on the Main thread.
                activeNotificationsCache.clear()

                for (notification in notifications) {
                    if (notification.isClearable && !notification.isOngoing) {
                        activeNotificationsCache[notification.key] = notification.toActiveNotification()
                    }
                }

                notificationDotsNotifier.updateActiveNotifications(activeNotificationsCache.values.toList())
            } catch (exception: Exception) {
                Timber.e(exception)
                activeNotificationsCache.clear()
                notificationDotsNotifier.updateActiveNotifications(emptyList())
            }
        }
    }

    private fun StatusBarNotification.toActiveNotification(): ActiveNotification {
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val isAutoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        val replyAction = notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        }

        return ActiveNotification(
            key = key,
            packageName = packageName,
            userHandle = user.hashCode(),
            postTime = postTime,
            title = title,
            text = text,
            isAutoCancel = isAutoCancel,
            contentIntent = notification.contentIntent,
            replyAction = replyAction
        )
    }

    companion object {
        private var instance: LauncherNotificationListenerService? = null

        fun dismissNotification(key: String) {
            try {
                instance?.cancelNotification(key)
            } catch (exception: Exception) {
                Timber.e(exception)
            }
        }

        fun snoozeNotification(key: String, durationMs: Long) {
            try {
                instance?.snoozeNotification(key, durationMs)
            } catch (exception: Exception) {
                Timber.e(exception)
            }
        }
    }
}
