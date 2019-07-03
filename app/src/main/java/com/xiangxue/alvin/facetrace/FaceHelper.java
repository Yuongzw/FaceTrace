package com.xiangxue.alvin.facetrace;

import android.view.Surface;

/**
 * Created by alvin on 2018/7/9.
 * 享学课堂版权所有
 */

public class FaceHelper {

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    //这里不做成类了 static方便调用了
    public static native void loadModel(String detectModel);

    public static native void startTracking();

    public static native void stopTracking();

    public static native void setSurface(Surface surface, int w, int h);

    public static native void detectorFace(byte[] data, int w, int h, int rotation, int cameraId);

    public static native void destory();

}
