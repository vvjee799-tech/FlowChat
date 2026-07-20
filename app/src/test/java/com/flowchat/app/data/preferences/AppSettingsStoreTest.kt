package com.flowchat.app.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsStoreTest {
    @Test
    fun powerModeControlsEveryDeviceAssistantFeatureFromOneSetting() {
        val store = AppSettingsStore(AppSettings(installId = "test-install"))

        assertTrue(store.state.value.memoryEnabled)
        assertFalse(store.state.value.powerModeEnabled)

        store.setMemoryEnabled(false)
        store.setPowerModeEnabled(true)

        assertFalse(store.state.value.memoryEnabled)
        assertTrue(store.state.value.powerModeEnabled)
        assertFalse(store.state.value.webSearchDisclosureAccepted)

        store.setPowerModeEnabled(false)

        assertFalse(store.state.value.powerModeEnabled)
    }
}
