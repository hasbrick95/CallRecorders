package com.example.callrecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 📱 MainActivity
 * الشاشة الرئيسية - تطلب الصلاحيات وتعرض حالة التطبيق
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView statusText;
    private TextView recordingsCountText;

    // 📋 قائمة الصلاحيات اللي محتاجينها
    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔗 نربطو العناصر من الـ layout
        statusText = findViewById(R.id.statusText);
        recordingsCountText = findViewById(R.id.recordingsCountText);
        Button checkPermissionsBtn = findViewById(R.id.checkPermissionsBtn);
        Button viewRecordingsBtn = findViewById(R.id.viewRecordingsBtn);

        // 🖱️ زر طلب الصلاحيات
        checkPermissionsBtn.setOnClickListener(v -> checkAndRequestPermissions());

        // 🖱️ زر عرض التسجيلات
        viewRecordingsBtn.setOnClickListener(v -> showRecordingsInfo());

        // ✅ نتحققو من الصلاحيات عند الفتح
        checkAndRequestPermissions();
    }

    // ============================================
    // 🔐 نتحققو من الصلاحيات
    // ============================================
    private void checkAndRequestPermissions() {
        List<String> missingPermissions = new ArrayList<>();

        // نشوفو أشمن صلاحيات ناقصة
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) {
            // ✅ عندنا كل الصلاحيات
            onAllPermissionsGranted();
        } else {
            // ❌ ناقصين صلاحيات - نطلبوها
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toArray(new String[0]),
                PERMISSIONS_REQUEST_CODE
            );
        }
    }

    // ============================================
    // 📨 نتلقاو جواب المستخدم على الصلاحيات
    // ============================================
    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                onAllPermissionsGranted();
            } else {
                onPermissionsDenied();
            }
        }
    }

    // ============================================
    // ✅ كل الصلاحيات معطاة
    // ============================================
    private void onAllPermissionsGranted() {
        statusText.setText("✅ التطبيق شغال - غيتسجل تلقائياً عند كل مكالمة");
        statusText.setTextColor(getColor(android.R.color.holo_green_dark));
        showRecordingsInfo();
    }

    // ============================================
    // ❌ الصلاحيات مرفوضة
    // ============================================
    private void onPermissionsDenied() {
        statusText.setText("❌ محتاج تعطي الصلاحيات باش يخدم التطبيق\nاضغط الزر فالأسفل");
        statusText.setTextColor(getColor(android.R.color.holo_red_dark));
        Toast.makeText(this, "⚠️ الصلاحيات ضرورية لتسجيل المكالمات!", Toast.LENGTH_LONG).show();
    }

    // ============================================
    // 📁 نعرضو عدد التسجيلات
    // ============================================
    private void showRecordingsInfo() {
        File recordingsFolder = new File(
            getExternalFilesDir(null),
            "CallRecordings"
        );

        if (recordingsFolder.exists()) {
            File[] files = recordingsFolder.listFiles();
            int count = (files != null) ? files.length : 0;
            recordingsCountText.setText("📁 عدد التسجيلات: " + count);
        } else {
            recordingsCountText.setText("📁 مازال ماكاين حتى تسجيل");
        }
    }
}
