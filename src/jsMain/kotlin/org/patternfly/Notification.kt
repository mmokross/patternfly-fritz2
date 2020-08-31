package org.patternfly

import dev.fritz2.binding.RootStore
import dev.fritz2.binding.SimpleHandler
import dev.fritz2.binding.handledBy
import dev.fritz2.dom.html.Button
import dev.fritz2.dom.html.HtmlElements
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLButtonElement
import kotlin.js.Date

// ------------------------------------------------------ dsl

fun HtmlElements.pfNotificationBadge(classes: String? = null): NotificationBadge =
    register(NotificationBadge(classes), {})

fun HtmlElements.pfNotificationBadge(modifier: Modifier): NotificationBadge =
    register(NotificationBadge(modifier.value), {})

// ------------------------------------------------------ tag

class NotificationBadge(classes: String?) :
    PatternFlyComponent<HTMLButtonElement>,
    Button(baseClass = classes(ComponentType.NotificationBadge, classes)) {

    init {
        markAs(ComponentType.NotificationBadge)
        Notification.store.unread.map { unread ->
            if (unread) "Unread notifications" else "Notifications"
        }.bindAttr("aria-label")
        span(baseClass = "notification-badge".component()) {
            classMap = Notification.store.unread.map { unread ->
                mapOf(Modifier.read.value to !unread, Modifier.unread.value to unread)
            }
            pfIcon("bell".fas())
        }
    }
}

// ------------------------------------------------------ store

data class Notification(
    val severity: Severity,
    val text: String,
    val details: String? = null,
    internal val read: Boolean = false,
    internal val timestamp: Long = Date.now().toLong()
) {
    companion object {
        val store = NotificationStore()

        fun error(text: String, details: String? = null) = send(Notification(Severity.DANGER, text, details))
        fun info(text: String, details: String? = null) = send(Notification(Severity.INFO, text, details))
        fun success(text: String, details: String? = null) = send(Notification(Severity.SUCCESS, text, details))
        fun warning(text: String, details: String? = null) = send(Notification(Severity.WARNING, text, details))

        private fun send(notification: Notification) = flowOf(notification) handledBy store.add
    }
}

class NotificationStore : RootStore<List<Notification>>(listOf()) {
    val add: SimpleHandler<Notification> = handle { notifications, notification ->
        notifications + notification
    }

    val clear = handle { listOf() }

    val latest = data
        .map {
            it.maxByOrNull { n -> n.timestamp }
        }
        .filterNotNull()
        .distinctUntilChanged()

    val unread = data.map { it.any { n -> !n.read } }.distinctUntilChanged()
}
