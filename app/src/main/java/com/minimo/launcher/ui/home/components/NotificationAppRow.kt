package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R
import com.minimo.launcher.ui.entities.ActiveNotificationUi
import com.minimo.launcher.ui.theme.Dimens

private val NOTIFICATION_ICON_SIZE = 22.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAppRow(
    notification: ActiveNotificationUi,
    expanded: Boolean,
    textColor: Color,
    textSize: TextUnit,
    verticalPadding: Dp,
    loadIcon: suspend () -> ImageBitmap?,
    onToggleExpand: () -> Unit,
    onOpen: () -> Unit,
    onMarkAsRead: () -> Unit,
    onSnooze: () -> Unit,
    onReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onToggleExpand()
            }
            // Always spring back — this swipe only toggles expansion, it never removes the row.
            false
        }
    )

    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = notification.key) {
        value = loadIcon()
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {}
    ) {
        if (expanded) {
            ExpandedNotificationContent(
                notification = notification,
                icon = icon,
                textColor = textColor,
                textSize = textSize,
                verticalPadding = verticalPadding,
                onOpen = onOpen,
                onCollapse = onToggleExpand,
                onMarkAsRead = onMarkAsRead,
                onSnooze = onSnooze,
                onReply = onReply
            )
        } else {
            CollapsedNotificationContent(
                notification = notification,
                icon = icon,
                textColor = textColor,
                textSize = textSize,
                verticalPadding = verticalPadding,
                onClick = onOpen
            )
        }
    }
}

@Composable
private fun CollapsedNotificationContent(
    notification: ActiveNotificationUi,
    icon: ImageBitmap?,
    textColor: Color,
    textSize: TextUnit,
    verticalPadding: Dp,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = verticalPadding),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(NOTIFICATION_ICON_SIZE)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${notification.appName} · ${formatRelativeTime(notification.postTime)}",
                color = textColor.copy(alpha = 0.6f),
                fontSize = textSize.times(0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    color = textColor,
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = textSize.times(0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ExpandedNotificationContent(
    notification: ActiveNotificationUi,
    icon: ImageBitmap?,
    textColor: Color,
    textSize: TextUnit,
    verticalPadding: Dp,
    onOpen: () -> Unit,
    onCollapse: () -> Unit,
    onMarkAsRead: () -> Unit,
    onSnooze: () -> Unit,
    onReply: (String) -> Unit
) {
    var replyText by remember(notification.key) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = verticalPadding)
    ) {
        Column(modifier = Modifier.clickable(onClick = onOpen)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(NOTIFICATION_ICON_SIZE)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = notification.appName,
                    color = textColor,
                    fontSize = textSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.collapse_notification),
                        tint = textColor
                    )
                }
            }

            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    color = textColor,
                    fontSize = textSize.times(0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = textSize.times(0.75f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onMarkAsRead,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    contentColor = textColor
                )
            ) {
                Text(stringResource(R.string.mark_as_read))
            }
            Button(
                onClick = onSnooze,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    contentColor = textColor
                )
            ) {
                Text(stringResource(R.string.mute_notification))
            }
        }

        if (notification.replyAction != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text(stringResource(R.string.reply)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReply(replyText)
                            replyText = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.reply),
                        tint = textColor
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(postTime: Long): String {
    val minutes = ((System.currentTimeMillis() - postTime).coerceAtLeast(0)) / 60_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        else -> "${minutes / (60 * 24)}d"
    }
}
