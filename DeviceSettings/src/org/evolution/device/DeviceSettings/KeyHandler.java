/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.evolution.device.DeviceSettings;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;

import org.evolution.device.DeviceSettings.Constants;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final int GESTURE_REQUEST = 1;

    private static final SparseIntArray sSupportedSliderZenModes = new SparseIntArray();
    private static final SparseIntArray sSupportedSliderRingModes = new SparseIntArray();
    private static final SparseIntArray sSupportedSliderHaptics = new SparseIntArray();
    static {
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_TOTAL_SILENCE, Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_SILENT, Settings.Global.ZEN_MODE_OFF);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_PRIORTY_ONLY, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_VIBRATE, Settings.Global.ZEN_MODE_OFF);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_NORMAL, Settings.Global.ZEN_MODE_OFF);

        sSupportedSliderRingModes.put(Constants.KEY_VALUE_TOTAL_SILENCE, AudioManager.RINGER_MODE_NORMAL);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_SILENT, AudioManager.RINGER_MODE_SILENT);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_PRIORTY_ONLY, AudioManager.RINGER_MODE_NORMAL);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_VIBRATE, AudioManager.RINGER_MODE_VIBRATE);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_NORMAL, AudioManager.RINGER_MODE_NORMAL);

        sSupportedSliderHaptics.put(Constants.KEY_VALUE_TOTAL_SILENCE, VibrationEffect.EFFECT_THUD);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_SILENT, VibrationEffect.EFFECT_HEAVY_CLICK);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_PRIORTY_ONLY, VibrationEffect.EFFECT_TICK);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_VIBRATE, VibrationEffect.EFFECT_DOUBLE_CLICK);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_NORMAL, -1);
    }

    private final Context mContext;
    private final PowerManager mPowerManager;
    private final NotificationManager mNotificationManager;
    private final AudioManager mAudioManager;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private Vibrator mVibrator;
    WakeLock mProximityWakeLock;
    WakeLock mGestureWakeLock;
    private int mProximityTimeOut;
    private boolean mProximityWakeSupported;

    public KeyHandler(Context context) {
        mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mNotificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock");

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    public KeyEvent handleKeyEvent(KeyEvent event) {
        int scanCode = event.getScanCode();
        String keyCode = Constants.sKeyMap.get(scanCode);
        int keyCodeValue = 0;

        try {
            keyCodeValue = Constants.getPreferenceInt(mContext, keyCode);
        } catch (Exception e) {
             return event;
        }

        if (!hasSetupCompleted()) {
            return event;
        }

        // We only want ACTION_UP event
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return null;
        }

        mAudioManager.setRingerModeInternal(sSupportedSliderRingModes.get(keyCodeValue));
        mNotificationManager.setZenMode(sSupportedSliderZenModes.get(keyCodeValue), null, TAG);
        doHapticFeedback(sSupportedSliderHaptics.get(keyCodeValue));
        return null;
    }

    private void doHapticFeedback(int effect) {
        if (mVibrator != null && mVibrator.hasVibrator() && effect != -1) {
            mVibrator.vibrate(VibrationEffect.get(effect));
        }
    }

    public void handleNavbarToggle(boolean enabled) {
        // do nothing
    }

    public boolean canHandleKeyEvent(KeyEvent event) {
        return false;
    }
}
