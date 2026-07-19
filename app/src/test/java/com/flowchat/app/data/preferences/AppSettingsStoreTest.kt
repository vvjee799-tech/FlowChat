package com.flowchat.app.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsStoreTest {
    @Test
    fun updatesToolAndMemoryControlsWithoutChangingOtherDefaults() {
        val store = AppSettingsStore(AppSettings(installId = "test-install"))

        assertTrue(store.state.value.memoryEnabled)
        assertTrue(store.state.value.openAppToolEnabled)
        assertFalse(store.state.value.appUsageToolEnabled)
        assertFalse(store.state.value.deviceAssistantEnabled)
        assertFalse(store.state.value.forceStopToolEnabled)

        store.setMemoryEnabled(false)
        store.setAppUsageToolEnabled(true)
        store.setOpenAppToolEnabled(false)
        store.setDeviceAssistantEnabled(true)
        store.setForceStopToolEnabled(true)

        assertFalse(store.state.value.memoryEnabled)
        assertTrue(store.state.value.appUsageToolEnabled)
        assertFalse(store.state.value.openAppToolEnabled)
        assertTrue(store.state.value.deviceAssistantEnabled)
        assertTrue(store.state.value.forceStopToolEnabled)
        assertFalse(store.state.value.webSearchDisclosureAccepted)
    }
}
