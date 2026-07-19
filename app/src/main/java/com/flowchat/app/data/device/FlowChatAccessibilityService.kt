package com.flowchat.app.data.device

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.flowchat.app.domain.device.DeviceScreenSnapshot
import com.flowchat.app.domain.device.DeviceSwipeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlowChatAccessibilityService : AccessibilityService() {
    private val indexedNodes = linkedMapOf<Int, AccessibilityNodeInfo>()
    private var cachedSnapshot: DeviceScreenSnapshot? = null
    private var snapshotInvalidated = true

    override fun onServiceConnected() {
        instance = this
        snapshotInvalidated = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> snapshotInvalidated = true
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        clearNodeCache()
        if (instance === this) instance = null
        super.onDestroy()
    }

    suspend fun observeScreen(forceRefresh: Boolean = true): DeviceScreenSnapshot? =
        withContext(Dispatchers.Main.immediate) {
            if (!forceRefresh && !snapshotInvalidated) return@withContext cachedSnapshot
            val root = rootInActiveWindow ?: return@withContext null
            try {
                val activityName = windows.firstOrNull { it.isActive }?.title?.toString().orEmpty()
                val readResult = AccessibilityScreenReader.read(root, activityName)
                clearNodeCache()
                indexedNodes.putAll(readResult.indexedNodes)
                cachedSnapshot = readResult.snapshot
                snapshotInvalidated = false
                readResult.snapshot
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        }

    suspend fun tapUiElement(index: Int, longPress: Boolean): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val node = indexedNodes[index] ?: return@withContext false
            if (!node.isEnabled || !node.refresh()) return@withContext false
            val action = if (longPress) {
                AccessibilityNodeInfo.ACTION_LONG_CLICK
            } else {
                AccessibilityNodeInfo.ACTION_CLICK
            }
            if (node.performAction(action)) {
                snapshotInvalidated = true
                return@withContext true
            }
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (bounds.width() <= 0 || bounds.height() <= 0) return@withContext false
            dispatchPointGesture(
                x = bounds.centerX().toFloat(),
                y = bounds.centerY().toFloat(),
                durationMillis = if (longPress) LongPressDurationMillis else TapDurationMillis
            ).also { accepted -> if (accepted) snapshotInvalidated = true }
        }

    suspend fun inputText(index: Int, text: String, clearExisting: Boolean): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val node = indexedNodes[index] ?: return@withContext false
            if (!node.isEnabled || !node.isEditable || node.isPassword || !node.refresh()) {
                return@withContext false
            }
            val normalizedText = text.take(MaxInputTextLength)
            val value = if (clearExisting) normalizedText else node.text?.toString().orEmpty() + normalizedText
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments).also { changed ->
                if (changed) snapshotInvalidated = true
            }
        }

    suspend fun swipeScreen(direction: DeviceSwipeDirection, distancePercent: Int): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val width = resources.displayMetrics.widthPixels.toFloat()
            val height = resources.displayMetrics.heightPixels.toFloat()
            val distance = distancePercent.coerceIn(MinSwipePercent, MaxSwipePercent) / 100f
            val centerX = width / 2f
            val centerY = height / 2f
            val horizontalDelta = width * distance / 2f
            val verticalDelta = height * distance / 2f
            val coordinates = when (direction) {
                DeviceSwipeDirection.Up -> floatArrayOf(centerX, centerY + verticalDelta, centerX, centerY - verticalDelta)
                DeviceSwipeDirection.Down -> floatArrayOf(centerX, centerY - verticalDelta, centerX, centerY + verticalDelta)
                DeviceSwipeDirection.Left -> floatArrayOf(centerX + horizontalDelta, centerY, centerX - horizontalDelta, centerY)
                DeviceSwipeDirection.Right -> floatArrayOf(centerX - horizontalDelta, centerY, centerX + horizontalDelta, centerY)
            }
            val path = Path().apply {
                moveTo(coordinates[0], coordinates[1])
                lineTo(coordinates[2], coordinates[3])
            }
            dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, SwipeDurationMillis))
                    .build(),
                null,
                null
            ).also { accepted -> if (accepted) snapshotInvalidated = true }
        }

    suspend fun pressBack(): Boolean = withContext(Dispatchers.Main.immediate) {
        performGlobalAction(GLOBAL_ACTION_BACK).also { accepted -> if (accepted) snapshotInvalidated = true }
    }

    suspend fun pressHome(): Boolean = withContext(Dispatchers.Main.immediate) {
        performGlobalAction(GLOBAL_ACTION_HOME).also { accepted -> if (accepted) snapshotInvalidated = true }
    }

    private fun dispatchPointGesture(x: Float, y: Float, durationMillis: Long): Boolean {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMillis))
                .build(),
            null,
            null
        )
    }

    private fun clearNodeCache() {
        indexedNodes.values.forEach { node ->
            @Suppress("DEPRECATION")
            node.recycle()
        }
        indexedNodes.clear()
    }

    companion object {
        @Volatile
        private var instance: FlowChatAccessibilityService? = null

        fun current(): FlowChatAccessibilityService? = instance

        const val MaxObservedElements = AccessibilityScreenReader.MaxObservedElements
        private const val MaxInputTextLength = 4_000
        private const val TapDurationMillis = 80L
        private const val LongPressDurationMillis = 650L
        private const val SwipeDurationMillis = 350L
        private const val MinSwipePercent = 20
        private const val MaxSwipePercent = 80
    }
}
