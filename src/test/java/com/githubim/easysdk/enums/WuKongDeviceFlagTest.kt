package com.githubim.easysdk.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class WuKongDeviceFlagTest {
    @Test
    fun `values match WuKongIM protocol`() {
        assertEquals(0, WuKongDeviceFlag.APP.value)
        assertEquals(1, WuKongDeviceFlag.WEB.value)
        assertEquals(2, WuKongDeviceFlag.DESKTOP.value)
    }
}
