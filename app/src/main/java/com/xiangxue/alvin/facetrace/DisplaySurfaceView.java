package com.xiangxue.alvin.facetrace;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Iterator;
import java.util.List;

/**
 * Created by avlin on 2018/7/9.
 * 享学课堂所有
 */

public class DisplaySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera
        .PreviewCallback {

    private final int DEFAULT_WIDTH = 640;
    private final int DEFAULT_HEIGHT = 480;

    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private Camera mCamera;
    private boolean mPreviewRunning;
    private int width;
    private int height;
    private byte[] mBuffer;
    private static final String TAG = "display";


    public DisplaySurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public DisplaySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public DisplaySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }


    private void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mPreviewRunning = false;
        FaceHelper.stopTracking();
    }


    private void setPreviewSize(Camera.Parameters parameters) {
        int area = DEFAULT_WIDTH * DEFAULT_HEIGHT;
        List<Camera.Size> supportedPreviewSizes = parameters
                .getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
//
        int m = Math.abs(size.height * size.width - area);
        supportedPreviewSizes.remove(0);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        while (iterator.hasNext()) {
            Camera.Size next = iterator.next();
            Log.d(TAG, "support " + next.width + "x" + next.height);
            int n = Math.abs(next.height * next.width - area);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        parameters.setPreviewSize(size.width, size.height);
        Log.d(TAG, "preview size width:" + size.width + " height:" + size.height);
    }

    private void startPreview() {
        if (mPreviewRunning) {
            return;
        }
        try {
            mCamera = Camera.open(mCameraId);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
//            parameters.setPreviewSize(640, 480);
            setPreviewSize(parameters);
            mCamera.setParameters(parameters);
            Camera.Size previewSize = parameters.getPreviewSize();
            width = previewSize.width;
            height = previewSize.height;
            //获得nv21的像素数
            //y : w*h
            //u : w*h/4
            //v : w*h/4
            mBuffer = new byte[width * height * 3 / 2];
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            SurfaceTexture mSurfaceTexture = new SurfaceTexture(10);
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
            mPreviewRunning = true;
            FaceHelper.startTracking();
            FaceHelper.setSurface(getHolder().getSurface(), width, height);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        startPreview();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay()
                .getRotation();
        FaceHelper.detectorFace(data, width, height, rotation, mCameraId);
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }


}
