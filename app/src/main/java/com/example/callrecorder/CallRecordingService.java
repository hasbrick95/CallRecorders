package com.example.callrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 🔴 CallRecordingService - Android 10+ Compatible
 * نستخدمو AudioRecord مع VOICE_RECOGNITION
 * هاد المصدر خدام على Android 10/11/12/13/14
 */
public class CallRecordingService extends Service {

    private static final String TAG = "CallRecordingService";
    private static final String CHANNEL_ID = "CallRecorderChannel";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private String outputFilePath;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if ("START_RECORDING".equals(action)) startRecording();
        else if ("STOP_RECORDING".equals(action)) stopRecording();
        return START_STICKY;
    }

    private void startRecording() {
        if (isRecording) return;

        startForeground(1, buildNotification("🔴 جاري التسجيل..."));
        outputFilePath = getOutputFilePath();

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;

        // VOICE_RECOGNITION = المصدر الوحيد خدام على Android 10+ بدون root
        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "❌ فشل تهيئة AudioRecord");
            stopSelf();
            return;
        }

        isRecording = true;
        audioRecord.startRecording();

        final int finalBufferSize = bufferSize;
        recordingThread = new Thread(() -> writeAudioToFile(finalBufferSize));
        recordingThread.start();

        Log.d(TAG, "▶️ التسجيل بدا → " + outputFilePath);
    }

    private void writeAudioToFile(int bufferSize) {
        byte[] audioBuffer = new byte[bufferSize];
        try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
            writeWavHeader(fos, 0); // header مؤقت
            long totalBytes = 0;
            while (isRecording) {
                int bytesRead = audioRecord.read(audioBuffer, 0, bufferSize);
                if (bytesRead > 0) {
                    fos.write(audioBuffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ خطأ: " + e.getMessage());
            return;
        }
        fixWavHeader(outputFilePath); // نصلحو الحجم الحقيقي
        Log.d(TAG, "✅ تحفظ: " + outputFilePath);
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        if (recordingThread != null) {
            try { recordingThread.join(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void writeWavHeader(FileOutputStream fos, long audioDataLength) throws IOException {
        long totalDataLen = audioDataLength + 36;
        long byteRate = SAMPLE_RATE * 16 / 8;
        byte[] header = new byte[44];
        header[0]='R'; header[1]='I'; header[2]='F'; header[3]='F';
        header[4]=(byte)(totalDataLen&0xff); header[5]=(byte)((totalDataLen>>8)&0xff);
        header[6]=(byte)((totalDataLen>>16)&0xff); header[7]=(byte)((totalDataLen>>24)&0xff);
        header[8]='W'; header[9]='A'; header[10]='V'; header[11]='E';
        header[12]='f'; header[13]='m'; header[14]='t'; header[15]=' ';
        header[16]=16; header[17]=0; header[18]=0; header[19]=0;
        header[20]=1; header[21]=0; header[22]=1; header[23]=0;
        header[24]=(byte)(SAMPLE_RATE&0xff); header[25]=(byte)((SAMPLE_RATE>>8)&0xff);
        header[26]=(byte)((SAMPLE_RATE>>16)&0xff); header[27]=(byte)((SAMPLE_RATE>>24)&0xff);
        header[28]=(byte)(byteRate&0xff); header[29]=(byte)((byteRate>>8)&0xff);
        header[30]=(byte)((byteRate>>16)&0xff); header[31]=(byte)((byteRate>>24)&0xff);
        header[32]=2; header[33]=0; header[34]=16; header[35]=0;
        header[36]='d'; header[37]='a'; header[38]='t'; header[39]='a';
        header[40]=(byte)(audioDataLength&0xff); header[41]=(byte)((audioDataLength>>8)&0xff);
        header[42]=(byte)((audioDataLength>>16)&0xff); header[43]=(byte)((audioDataLength>>24)&0xff);
        fos.write(header);
    }

    private void fixWavHeader(String filePath) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            long fileSize = raf.length();
            long audioLen = fileSize - 44;
            raf.seek(4);
            raf.write(longToBytes(fileSize - 8));
            raf.seek(40);
            raf.write(longToBytes(audioLen));
        } catch (IOException e) {
            Log.e(TAG, "❌ خطأ WAV header: " + e.getMessage());
        }
    }

    private byte[] longToBytes(long v) {
        return new byte[]{(byte)(v&0xff),(byte)((v>>8)&0xff),(byte)((v>>16)&0xff),(byte)((v>>24)&0xff)};
    }

    private String getOutputFilePath() {
        File folder = new File(getExternalFilesDir(null), "CallRecordings");
        if (!folder.exists()) folder.mkdirs();
        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
        return new File(folder, "call_" + ts + ".wav").getAbsolutePath();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "مسجل المكالمات", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎙️ مسجل المكالمات")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRecording) stopRecording();
    }
}
