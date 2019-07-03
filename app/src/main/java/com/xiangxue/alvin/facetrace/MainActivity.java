package com.xiangxue.alvin.facetrace;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements Camera.PreviewCallback, View.OnTouchListener, SurfaceHolder.Callback {


    private ProgressDialog pd;
    private DisplaySurfaceView surfaceView;
    private CameraSurfaceView mCameraSurfaceView;
    Camera camera;
    SurfaceHolder holder;
    Camera.Parameters parameters;
    private int camWidth = 640;
    private int camHeight = 480;

    private void showLoading() {
        if (null == pd) {
            pd = new ProgressDialog(this);
            pd.setIndeterminate(true);
        }
        pd.show();
    }

    private void dismissLoading() {
        if (null != pd) {
            pd.dismiss();
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surfaceView);
        mCameraSurfaceView = findViewById(R.id.mCameraSurfaceView);
//        holder = surfaceView.getHolder();
//        surfaceView.setOnTouchListener(this);
//        holder.addCallback(this);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    File dir = new File(Environment.getExternalStorageDirectory(), "face");
                    File haar = copyAssetsFile("haarcascade_frontalface_alt.xml", dir);
                    FaceHelper.loadModel(haar.getAbsolutePath());
                    FaceHelper.startTracking();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPreExecute() {
                showLoading();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                dismissLoading();
            }
        }.execute();
    }


    private File copyAssetsFile(String name, File dir) throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, name);
        if (!file.exists()) {
            InputStream is = getAssets().open(name);
            FileOutputStream fos = new FileOutputStream(file);
            int len;
            byte[] buffer = new byte[2048];
            while ((len = is.read(buffer)) != -1)
                fos.write(buffer, 0, len);
            fos.close();
            is.close();
        }
        return file;
    }

    private void initCamera() {

        parameters = camera.getParameters();
        parameters.setFlashMode("off"); // 无闪光灯
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPictureSize(camWidth, camHeight);
        parameters.setPreviewSize(camWidth, camHeight);
//这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错
        camera.setParameters(parameters);
// 横竖屏镜头自动调整
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait"); //
            parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
            camera.setDisplayOrientation(90); // 在2.2以上可以使用
        } else// 如果是横屏
        {
            parameters.set("orientation", "landscape"); //
            camera.setDisplayOrientation(0); // 在2.2以上可以使用
        }

        byte[] buf = new byte[camWidth * camHeight * 3 / 2];
        camera.addCallbackBuffer(buf);
        camera.setPreviewCallback(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceHelper.destory();
    }

    public void switchCamera(View view) {
        if (mCameraSurfaceView != null) {
            CameraUtils.switchCamera(1 - CameraUtils.getCameraID(), mCameraSurfaceView.getHolder());
        }
        surfaceView.switchCamera();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (bytes == null) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        FaceHelper.detectorFace(bytes, camWidth, camHeight, rotation,  Camera.CameraInfo.CAMERA_FACING_BACK);
        if (this.camera != null) {
            byte[] buf = new byte[camWidth * camHeight * 3 / 2];
            this.camera.addCallbackBuffer(buf);
        }
        System.out.println("onPreviewFrame");
//        int ret = avcCodec.offerEncoder(data, h264);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {//按下时自动对焦
            camera.autoFocus(null);
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = Camera.open();
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            System.out.println("startPreview");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
