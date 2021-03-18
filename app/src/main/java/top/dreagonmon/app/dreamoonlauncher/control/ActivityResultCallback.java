package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.Intent;

import androidx.annotation.Nullable;

public interface ActivityResultCallback {
    void onActivityResult(int resultCode,@Nullable Intent data);
}
