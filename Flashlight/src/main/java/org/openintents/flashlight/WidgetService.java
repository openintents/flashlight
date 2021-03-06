package org.openintents.flashlight;

import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.os.IBinder;
import android.widget.Toast;

public class WidgetService extends Service {

    private Camera mCamera;
    private boolean isOn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (!isOn) {
            mCamera = Camera.open();
            if (mCamera != null) {
                Toast.makeText(this, "Turning On", Toast.LENGTH_SHORT).show();
                Camera.Parameters params = mCamera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(params);
                isOn = true;
            }
        } else {
            Toast.makeText(this, "Turning off", Toast.LENGTH_SHORT).show();
            mCamera.release();
            mCamera = null;
            isOn = false;
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
