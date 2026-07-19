package com.flowchat.app.domain.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceUiModelsTest {
    @Test
    fun formatsCompactIndexedElementsForATextOnlyModel() {
        val snapshot = DeviceScreenSnapshot(
            packageName = "com.android.settings",
            activityName = "Settings",
            elements = listOf(
                DeviceUiElement(
                    index = 3,
                    className = "android.widget.TextView",
                    text = "Battery",
                    contentDescription = null,
                    resourceId = "android:id/title",
                    bounds = DeviceBounds(24, 240, 500, 320),
                    clickable = true,
                    editable = false,
                    scrollable = false,
                    enabled = true
                )
            )
        )

        val content = snapshot.asToolContent()

        assertTrue(content.contains("package=com.android.settings"))
        assertTrue(content.contains("[3] TextView \"Battery\""))
        assertTrue(content.contains("clickable"))
        assertTrue(content.contains("bounds=24,240,500,320"))
    }

    @Test
    fun sensitiveSnapshotsNeverExposeNodeText() {
        val snapshot = DeviceScreenSnapshot(
            packageName = "com.example.pay",
            activityName = "Checkout",
            elements = listOf(
                DeviceUiElement(
                    index = 0,
                    className = "android.widget.EditText",
                    text = "123456",
                    contentDescription = "Payment password",
                    resourceId = null,
                    bounds = DeviceBounds(20, 200, 600, 300),
                    clickable = true,
                    editable = true,
                    scrollable = false,
                    enabled = true
                )
            ),
            sensitive = true,
            sensitivityReason = "password_or_payment"
        )

        val content = snapshot.asToolContent()

        assertTrue(content.contains("sensitive_screen_blocked"))
        assertFalse(content.contains("123456"))
        assertFalse(content.contains("Payment password"))
    }
}
