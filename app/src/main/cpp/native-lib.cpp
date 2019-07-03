#include <jni.h>
#include <string>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>


#define LOG_TAG "native"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)




#include <vector>
#include <opencv2/opencv.hpp>


using namespace cv;
using namespace std;

extern "C" {

DetectionBasedTracker *tracker = 0;
CascadeClassifier *faceClassifier = 0;

ANativeWindow *nativeWindow;
int isTracking = 0;

class CascadeDetectorAdapter : public DetectionBasedTracker::IDetector {
public:
    CascadeDetectorAdapter(cv::Ptr<CascadeClassifier> detector) :
            IDetector(),
            detector(detector) {
        CV_Assert(detector);
    }

    void detect(const Mat &Image, vector<Rect> &objects) {
        detector->detectMultiScale(Image, objects, scaleFactor, minNeighbours, 0, minObjSize,
                                   maxObjSize);
    }

    ~CascadeDetectorAdapter() {
    }

private:
    CascadeDetectorAdapter();

    Ptr<CascadeClassifier> detector;
};


JNIEXPORT void JNICALL
Java_com_xiangxue_alvin_facetrace_FaceHelper_loadModel(JNIEnv *env, jclass type,
                                                       jstring detectModel_) {
    const char *detectModel = env->GetStringUTFChars(detectModel_, 0);
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(
            makePtr<CascadeClassifier>(detectModel));
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(
            makePtr<CascadeClassifier>(detectModel));
    DetectionBasedTracker::Parameters detectorParams;
    tracker = new DetectionBasedTracker(mainDetector, trackingDetector, detectorParams);
    env->ReleaseStringUTFChars(detectModel_, detectModel);
    faceClassifier = new CascadeClassifier(detectModel);
}

JNIEXPORT void JNICALL
Java_com_xiangxue_alvin_facetrace_FaceHelper_startTracking(JNIEnv *env, jclass type) {
    if (tracker && !isTracking) {
        isTracking = 1;
        tracker->run();
    }
}

JNIEXPORT void JNICALL
Java_com_xiangxue_alvin_facetrace_FaceHelper_stopTracking(JNIEnv *env, jclass type) {
    if (tracker && isTracking) {
        isTracking = 0;
        tracker->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_xiangxue_alvin_facetrace_FaceHelper_setSurface(JNIEnv *env, jclass type, jobject surface,
                                                        jint w, jint h) {

    if (surface && w && h) {
        if (nativeWindow) {
            LOGI("release old native window");
            ANativeWindow_release(nativeWindow);
            nativeWindow = 0;
        }
        LOGI("new native window");
        nativeWindow = ANativeWindow_fromSurface(env, surface);
        if (nativeWindow) {
            LOGI("set new native window buffer");
            ANativeWindow_setBuffersGeometry(nativeWindow, w, h,
                                             WINDOW_FORMAT_RGBA_8888);
        }
    } else {
        if (nativeWindow) {
            LOGI("release old native window");
            ANativeWindow_release(nativeWindow);
            nativeWindow = 0;
        }
    }

}

JNIEXPORT void JNICALL
Java_com_xiangxue_alvin_facetrace_FaceHelper_detectorFace(JNIEnv *env, jclass type,
                                                          jbyteArray data_, jint w, jint h,
                                                          jint rotation, jint cameraId) {

    /////////////////////////////////////////step1 将摄像头的数据转变成Native可以识别数据////////////////////////////////
    //摄像头数据
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    //把摄像头数据放入opencv的mat
    Mat nv21Mat(h + h / 2, w, CV_8UC1, data);
    Mat rgbMat;
    //摄像头是nv21 转成bgr
    cvtColor(nv21Mat, rgbMat, COLOR_YUV2BGR_NV21);
    //需要逆时针旋转90度
    if (rotation == 0) {
        int angle;
        //后置
        if (cameraId == 0) {
            angle = -90;
        } else {
            angle = 90;
        }
        //旋转
        Mat matrix = getRotationMatrix2D(Point2f(w / 2, h / 2), angle, 1);
        warpAffine(rgbMat, rgbMat, matrix, Size(w, h));
        //旋转后宽高交换 会有黑边 截取
//        Mat dst;
        getRectSubPix(rgbMat, Size(h, h),
                      Point2f(w / 2, h / 2), rgbMat);
        //下面显示前会resize的
    }
    //////////////////////////////////////////////////////step2 数字图像处理/////////////////////////////////////////////////////////////

    Mat grayMat;
    cvtColor(rgbMat, grayMat, COLOR_BGR2GRAY);
    //直方图均衡化 增强对比效果
    equalizeHist(grayMat, grayMat);
    vector<Rect> faces;
    //识别
    tracker->process(grayMat);
    //获得识别结果 人脸位置矩形
    tracker->getObjects(faces);
  //  faceClassifier->detectMultiScale(grayMat, faces);

    for (int i = 0; i < faces.size(); ++i) {
        Rect face = faces[i];
        rectangle(rgbMat, face.tl(), face.br(), Scalar(0, 255, 255));
    }
    //ANativeWindow直接画出图像
    if (!nativeWindow) {
        LOGI("native window null");
        goto end;
    }

    ANativeWindow_Buffer windowBuffer;
    //0才成功
    if (ANativeWindow_lock(nativeWindow, &windowBuffer, 0)) {
        LOGI("native window lock fail");
        goto end;
    }
    cvtColor(rgbMat, rgbMat, COLOR_BGR2GRAY);
    resize(rgbMat, rgbMat, Size(windowBuffer.width, windowBuffer.height));
    memcpy(windowBuffer.bits, rgbMat.data, windowBuffer.height * windowBuffer.stride * 4);
    ANativeWindow_unlockAndPost(nativeWindow);

    end:
    nv21Mat.release();
    rgbMat.release();
    grayMat.release();
    env->ReleaseByteArrayElements(data_, data, 0);
}

JNIEXPORT void JNICALL
Java_com_xiangxue_alvin_facetrace_FaceHelper_destory(JNIEnv *env, jclass type) {

    if (tracker)
        delete tracker;
    tracker = 0;
    if (nativeWindow)
        ANativeWindow_release(nativeWindow);
    nativeWindow = 0;

}
}