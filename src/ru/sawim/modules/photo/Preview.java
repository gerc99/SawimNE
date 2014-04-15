package ru.sawim.modules.photo;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

class Preview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera camera;
    private int width;
    private int height;

    Preview(Context context, int width, int height) {
        super(context);
        this.width = width;
        this.height = height;
        camera = getCameraInstance();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        if (null == camera) throw new RuntimeException("camera 0");
        camera.setDisplayOrientation(90);
        SurfaceHolder mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (null == camera) return;
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.panic("surfaceCreated", e);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        destroyCamera();
    }

    public Camera.Size getPref(List<Camera.Size> sizes, int w, int h) {
        Camera.Size pref = null;
        if (null != sizes) {
            for (Camera.Size size : sizes) {
                ru.sawim.modules.DebugLog.println("size " + size.width + " " + size.height);
                if ((size.width <= w) && (size.height <= h)) {
                    if (null == pref) {
                        pref = size;
                    } else if ((pref.width <= size.width) && (pref.height <= size.height)) {
                        pref = size;
                    }
                }
            }
        }
        pref = (null == pref) ? camera.new Size(w, h) : pref;
        ru.sawim.modules.DebugLog.println("_ " + pref.width + " " + pref.height + " " + w + " " + h);
        return pref;
    }

    private void setParameters(List<Camera.Size> preview, List<Camera.Size> picture, int w, int h) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.set("orientation", "portrait");
        parameters.setRotation(90);
        Camera.Size prefPreview = getPref(preview, w, h);
        parameters.setPreviewSize(prefPreview.width, prefPreview.height);
        Camera.Size prefPicture = getPref(picture, width, height);
        parameters.setPictureSize(prefPicture.width, prefPicture.height);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        camera.setParameters(parameters);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        if (holder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        if (null == camera) return;
        camera.stopPreview();

        Camera.Parameters parameters = camera.getParameters();
        try {
            setParameters(parameters.getSupportedPreviewSizes(), null, w, h);
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.panic("surfaceChanged def", e);
            try {
                setParameters(parameters.getSupportedPreviewSizes(), parameters.getSupportedPictureSizes(), w, h);
            } catch (Exception ex) {
                ru.sawim.modules.DebugLog.panic("surfaceChanged opt", ex);
            }
        }
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.panic("surfaceChanged camera", e);
        }
    }

    public void takePicture(final PictureCallback jpegCallback) {
        if (null == camera) return;
        camera.takePicture(null, null, jpegCallback);
    }

    private Camera getCameraInstance() {
        try {
            return Camera.open();
        } catch (Exception e) {
            return null;
        }
    }

    public void destroyCamera() {
        if (null == camera) return;
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // do nothing
        }
        try {
            camera.release();
        } catch (Exception e) {
            // do nothing
        }
        camera = null;
    }
}