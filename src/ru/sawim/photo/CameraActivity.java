package ru.sawim.photo;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import ru.sawim.R;

public class CameraActivity extends Activity {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private Preview preview;
    private Button buttonClick;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo);
        int width = getIntent().getExtras().getInt("width", 1024);
        int height = getIntent().getExtras().getInt("height", 768);
        try {
            init(width, height);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        //if (null != preview) preview.destroyCamera();
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private void init(int width, int height) {
        preview = new Preview(this, width, height);
        ((FrameLayout) findViewById(R.id.preview)).addView(preview);

        final Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] jpeg, Camera camera) {
                try {
                    //preview.destroyCamera();
                    Activity it = CameraActivity.this;
                    Intent intent = new Intent();
                    intent.putExtra("photo", jpeg);
                    it.setResult(RESULT_OK, intent);
                    if (null != it.getParent()) {
                        it.getParent().setResult(RESULT_OK, intent);
                    }
                    it.finish();
                } catch (Exception e) {
                    sawim.modules.DebugLog.panic("photo", e);
                }
            }
        };
        buttonClick = (Button) findViewById(R.id.tablePhotoButtonClick);
        buttonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    preview.takePicture(jpegCallback);
                } catch (Exception e) {
                    sawim.modules.DebugLog.panic("click", e);
                }
            }
        });
    }
}