package com.zebra.rfid.demo.sdksample;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SoundUtils {
    private static final String TAG = "SoundUtils";

    public static void playBarcodeBeep() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 150, 250);
    }

    public static void playConnectBeep() {
        playBarcodeBeep();
        new Handler(Looper.getMainLooper()).postDelayed(SoundUtils::playBarcodeBeep, 250);
    }

    public static void playDisconnectAlarm() {
        playTone(ToneGenerator.TONE_SUP_ERROR, 500, 600);
    }

    private static void playTone(int toneType, int durationMs, int releaseDelayMs) {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGen.startTone(toneType, durationMs);
            new Handler(Looper.getMainLooper()).postDelayed(toneGen::release, releaseDelayMs);
        } catch (Exception e) {
            Log.e(TAG, "Error playing tone", e);
        }
    }
}
