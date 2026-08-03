package com.quietphoto.clock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PowerStatePolicyTest {
    @Test
    public void keepAwakeModesHaveSafeDefaults() {
        assertTrue(PowerStatePolicy.shouldKeepScreenOn(
                PowerStatePolicy.KEEP_AWAKE_ALWAYS, true, false, true));
        assertTrue(PowerStatePolicy.shouldKeepScreenOn(
                PowerStatePolicy.KEEP_AWAKE_CHARGING, true, true, true));
        assertFalse(PowerStatePolicy.shouldKeepScreenOn(
                PowerStatePolicy.KEEP_AWAKE_CHARGING, true, false, true));
        assertFalse(PowerStatePolicy.shouldKeepScreenOn(
                PowerStatePolicy.KEEP_AWAKE_SYSTEM, true, true, true));
        assertTrue(PowerStatePolicy.shouldKeepScreenOn(
                PowerStatePolicy.KEEP_AWAKE_CHARGING, true, false, false));
    }

    @Test
    public void lowBatteryGuardUsesChargingAndBatteryPresence() {
        assertTrue(PowerStatePolicy.shouldForceEco(true, true, false, 20));
        assertFalse(PowerStatePolicy.shouldForceEco(true, true, true, 10));
        assertFalse(PowerStatePolicy.shouldForceEco(true, false, false, 1));
        assertTrue(PowerStatePolicy.shouldPauseAutomaticPhotos(true, true, false, 10));
        assertFalse(PowerStatePolicy.shouldPauseAutomaticPhotos(true, true, false, 11));
    }

    @Test
    public void lowBatteryRecoveryHasHysteresis() {
        assertFalse(PowerStatePolicy.shouldResumeAutomaticPhotos(14, false));
        assertTrue(PowerStatePolicy.shouldResumeAutomaticPhotos(15, false));
        assertFalse(PowerStatePolicy.shouldResumeForcedEco(24, false));
        assertTrue(PowerStatePolicy.shouldResumeForcedEco(25, false));
        assertTrue(PowerStatePolicy.shouldResumeForcedEco(-1, true));
    }
}
