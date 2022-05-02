package com.example.auto_selfie;

import static org.opencv.imgproc.Imgproc.connectedComponents;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Comparator;
import java.util.LinkedList;

public class VideoShowActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{
    JavaCameraView javaCameraView;
    Mat mRGBA, mRGBAT, dst, bgr, hsv;
    boolean STPflag = true , RCOFflag = false;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(VideoShowActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status)
            {
                case BaseLoaderCallback.SUCCESS:
                {
                    javaCameraView.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_show);

        javaCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        javaCameraView.setCameraIndex(1);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(VideoShowActivity.this);


    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height,width, CvType.CV_8UC4);
        mRGBAT = new Mat();
        dst = new Mat();
        hsv = new Mat();
        bgr = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    public void sendImageToPython(Mat bImg1){
        Mat bImg2 = new Mat();
        Imgproc.resize(bImg1, bImg2, new Size(0, 0), 0.25, 0.25, Imgproc.INTER_AREA);
        Mat labeled_img = new Mat();
        connectedComponents(bImg2, labeled_img);
        int cols = labeled_img.cols();
        int rows = labeled_img.rows();
        LinkedList<Integer> ll = new LinkedList<Integer>();
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
               ll.add((int)(labeled_img.get(i,j)[0]));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ll.sort(Comparator.reverseOrder());
        }
        int max1=0,max1id=0,max2=0,max2id=0,current=-1,count=0;
        for (int i=0;i<ll.size();i++){
            if(ll.get(i) != 0)
            {
                if (ll.get(i) != current) {
                    if (count > max1) {
                        max2 = max1;
                        max2id = max1id;
                        max1 = count;
                        max1id = current;
                    }
                    else if(count > max2){
                        max2 = count;
                        max2id = current;
                    }
                    count=0;
                }
                current = ll.get(i);
                count++;
            }
        }
        System.out.println("biggest: "+max1id+" size: "+max1+"\n second biggest: "+max2id+" size: "+max2);
        for (int i=0;i<rows;i++){
            for (int j=0;j<cols;j++){
                if ((int)(labeled_img.get(i,j)[0]) != max1id || (int)(labeled_img.get(i,j)[0]) != max2id)
                {
                    labeled_img.put(i,j,0);
                }
                else
                {
                    labeled_img.put(i,j,1);
                }
            }
        }
    }

    public boolean readContentsOfFile(){

        return false; }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        Core.transpose(mRGBA, mRGBAT);
        Core.flip(mRGBAT, mRGBAT, 1);
        Imgproc.resize(mRGBAT, dst, mRGBA.size());
        mRGBA.release();
        mRGBAT.release();
        Imgproc.cvtColor(dst,bgr,Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(bgr,hsv,Imgproc.COLOR_BGR2HSV);
        Core.inRange(hsv,new Scalar(0,20,60),new Scalar(17,110,255),hsv);
        dst.release();
        bgr.release();
        if (STPflag){
            sendImageToPython(hsv);
        }
        if(RCOFflag){
            readContentsOfFile();
        }
        return hsv;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug())
        {
            Log.d("OPENCV","OpenCv is Configured or Connected successfully.");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
        else
        {
            Log.d("OPENCV","OpenCv is not Working or Loaded.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }
}