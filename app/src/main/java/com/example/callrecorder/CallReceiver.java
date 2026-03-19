package com.example.callrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * 📡 CallReceiver
 * هذا الكلاس كيستمع لحالة التليفون
 * كيمكننا نعرفو واش كاين اتصال جاي أو خارج
 */
public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";

    // 🔄 نحفظو الحالة السابقة باش نعرفو متى تبدلات
    private static String lastState = TelephonyManager.EXTRA_STATE_IDLE;

    @Override
    public void onReceive(Context context, Intent intent) {

        // ✅ نجيبو حالة التليفون دابا
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        Log.d(TAG, "📞 حالة التليفون: " + state);

        // ============================================
        // 📲 كاين اتصال جاي (رنين)
        // ============================================
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            Log.d(TAG, "🔔 التليفون كيرن...");
            // مازال ما بدانا نسجلو - نتصنتو فقط
        }

        // ============================================
        // ✅ بدا الاتصال (حيد أو خرج)
        // ============================================
        else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {

            // تأكد مكنانوش نسجلو ديجا
            if (!TelephonyManager.EXTRA_STATE_OFFHOOK.equals(lastState)) {
                Log.d(TAG, "▶️ بدا الاتصال - غنبدا التسجيل!");

                // 🚀 نطلق سيرفيس التسجيل
                Intent serviceIntent = new Intent(context, CallRecordingService.class);
                serviceIntent.setAction("START_RECORDING");
                context.startForegroundService(serviceIntent);
            }
        }

        // ============================================
        // ❌ خلص الاتصال (رجع للوضع العادي)
        // ============================================
        else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {

            // واش كنا فاتصال؟
            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(lastState)
                    || TelephonyManager.EXTRA_STATE_RINGING.equals(lastState)) {

                Log.d(TAG, "⏹️ خلص الاتصال - غنوقف التسجيل!");

                // 🛑 نوقفو سيرفيس التسجيل
                Intent serviceIntent = new Intent(context, CallRecordingService.class);
                serviceIntent.setAction("STOP_RECORDING");
                context.startService(serviceIntent);
            }
        }

        // 💾 نحفظو الحالة الحالية باش نستخدموها المرة الجاية
        lastState = state;
    }
}
