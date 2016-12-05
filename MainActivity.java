package com.example.gaby.imageprocessingv2;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import android.app.Activity;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.imgcodecs.*;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;


public class MainActivity extends Activity implements CvCameraViewListener2 {

    double edge_factor=0.5 ,col_factor=0.5;
    int sim=70;
    static int counter=0;
    static int period=300; //nombres de frames par periode
    static int crit=75;
    static int SR=150; //2 fois valeurs critiques ou dans ce cas images sent
    static int SRmin=10;
    static int SRmax=150;
    static int saut=(period/SR);
    static double d=0.2; // minimum pourcentage required to detect a change

    static double F_up=Math.min((1 + d) * crit, SR / 2);
    static double F_down=(1-d)*crit;

    static double th_up=crit*(1 + d/2);
    static double th_down=crit*(1 - d/2);
    static int nbrframessentperiod=0;
    static int h=2;
    static int h1=0;
    static int h2=0;
    static int nbrframessenttotal=0;
    int wi=720;
    int de=1280;
    static int T=18000;
    static int n=60; //number of periods

    double E_distance, ColSim ,Similarity;
    static CvCameraViewFrame img1;
    static Bitmap toCompare,img2;
    static int countn=0;
    static int countf=0;
    static int o=0;
    static int lock=0;
    private static final String  TAG                 = "OCVSample::Activity";
    static Mat tcframe;  //to compare frame
    static int rows;
    static int cols;



    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            System.loadLibrary("org/opencv/imgcodecs/Imgcodecs.java");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    private static void SaveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();

        String fname = "Image-"+ o +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();

        rows=frame.rows(); 
        cols=frame.cols();

        //transform it to Bitmap
        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame, bmp);
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        //save it to phone gallery
        SaveImage(bmp);
        Process(inputFrame);
        return frame;
    }

    public static void Process(CvCameraViewFrame newFrame){
        if(countf==0){
            //trasnform the captured frame to Bitmap
            tcframe=newFrame.rgba();
            toCompare=null;
            try {
                toCompare= Bitmap.createBitmap(tcframe.cols(), tcframe.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(tcframe, toCompare);
            }catch(Exception ex){
                System.out.println(ex.getMessage());
            }
            nbrframessenttotal=nbrframessenttotal+1; //increment number of sent frame by one.
            nbrframessentperiod=nbrframessentperiod+1; //increment number of frame sent by period
            countf++;//number of frames captured
            countn++;//number of periods passed
            SaveImage(toCompare);
            //o=period*(countn-1)+1;
        } //end if countf==0

        if(countn<n){
            countf++;
            if(countf<period&&countf!=0){
                o = o + 1; //o used to generate unique file name
                if (countf%saut == 0 && o < T) {  //saut=period/SR(300/150=2) ,
                    Mat frame = newFrame.rgba();
                    img2=null;
                    try {
                        img2= Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(frame, img2);
                    }catch(Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                    double edsim=edgesim(toCompare,img2);
                    Double colsim=colorsim(toCompare, img2);
                    if (colsim>5){
                        SaveImage(img2);
                        toCompare=img2;
                        counter=counter+1;
                    }
                }
            }//end if countf<period
            else if(countf>period){
                countn++;
                countf=0;
                nbrframessentperiod=0;

                if(counter>th_up) {
                    h1 = h1 + 1;
                    h2 = 0;
                }else
                if (counter<th_down) {
                    h1 = 0;
                    h2 = h2 + 1;
                }
                if(h1>h || h2>h) {
                    crit = counter;
                    SR = 2 * counter; //2 fois valeurs critiques ou dans ce cas images sent
                    if (SR == 0)
                        SR = SRmin;

                    if (SR > SRmax)
                        SR = SRmax;

                    saut = period / SR;
                    if (saut == 0)
                        saut = 1;

                    F_up = Math.min((1 + d) * crit, SR / 2);
                    F_down = (1 - d) * crit;

                    th_up = crit * (1 + d / 2);
                    th_down = crit * (1 - d / 2);

                    h1 = 0;
                    h2 = 0;
                }
                counter=0;
                //o=0;
            }//end if countf>period//when count n reach 60 this means we reached 60 periods ,each 300 frames increase period by 1.

        }//end else if countn<n

        // lock=0;
    }//end Process

    public static double edgesim(Bitmap first, Bitmap second) {

        float matched_data = 0;
        float matched_data1 = 0;
        float white_points = 0;
        float black_points = 0;
        float total_data=0;
        double total_matched_percentage_edge=0;

        Bitmap image1;
        Bitmap image2;

        ///////////////transform back to Mat to be able to get Canny images//////////////////
        Mat img1=new Mat();
        Mat img2=new Mat();
        Utils.bitmapToMat(first,img1);
        Utils.bitmapToMat(second,img2);

        //mat gray img1 holder
        Mat imageGray1 = new Mat();

        //mat gray img2 holder
        Mat imageGray2 = new Mat();

        //mat canny image
        Mat imageCny1 = new Mat();

        //mat canny image
        Mat imageCny2 = new Mat();

        /////////////////////////////////////////////////////////////////

        //Convert img1 in to gray image
        Imgproc.cvtColor(img1, imageGray1, Imgproc.COLOR_BGR2GRAY);

        //Canny Edge Detection
        Imgproc.Canny(imageGray1, imageCny1, 10, 100, 3, true);

        ///////////////////////////////////////////////////////////////////

        //Convert img2 in to gray image
        Imgproc.cvtColor(img2, imageGray2, Imgproc.COLOR_BGR2GRAY);

        //Canny Edge Detection
        Imgproc.Canny(imageGray2, imageCny2, 10, 100, 3, true);

        //////////////////Transform Canny to Bitmap/////////////////////////////////////////
        image1= Bitmap.createBitmap(imageCny1.cols(), imageCny1.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageCny1, image1);

        image2= Bitmap.createBitmap(imageCny2.cols(), imageCny2.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageCny2, image2);

        int pixel1, pixel2;
        //white :255 and black :0
        for(int x = 0; x < rows; x++) {
            for(int y = 0; y < cols; y++) {
                pixel1 = image1.getPixel(y, x);
                pixel2 = image2.getPixel(y, x);
                int R1 = Color.red(pixel1); // R1=G1=B1 ==255||0 //bcz the image is black and white!
                int R2 = Color.red(pixel2);
                if(R1==255) //white
                    white_points++;
                else
                    black_points++;
            }
        }
        for(int x = 0; x < rows; x++) {
            for(int y = 0; y < cols; y++) {
                pixel1 = image1.getPixel(y, x);
                pixel2 = image2.getPixel(y, x);
                int R1 = Color.red(pixel1); // R1=G1=B1 ==255||0 //bcz the image is black and white!
                int R2 = Color.red(pixel2);

                if(R1==255&&R2==255) //white
                    matched_data++;
                else
                    matched_data1++;
            }
        }

        Log.i("test","Wp"+white_points);
        Log.i("test","MD"+matched_data);

        total_data = white_points;
        total_matched_percentage_edge = (matched_data/total_data)*100;
        Log.i("test","Matched"+total_matched_percentage_edge);

        return total_matched_percentage_edge;
    }

    public static double colorsim(Bitmap img1, Bitmap img2) {

        int rows = img1.getHeight();
        int cols = img1.getWidth();

        float[][]rHist1 = new float[rows][cols];
        float[][]rHist2 =new float[rows][cols];
        float[][]gHist1 =new float[rows][cols];
        float[][]gHist2 =new float[rows][cols];
        float[][]bHist1 =new float[rows][cols];
        float[][]bHist2 =new float[rows][cols];

        int no_of_pixels = rows * cols;

        int pixel1, pixel2;
        for(int x = 0; x < rows; x++) {
            for(int y = 0; y < cols; y++) {

                pixel1 = img1.getPixel(y, x);
                pixel2 = img2.getPixel(y, x);

                float A1 = Color.alpha(pixel1);
                float R1 = Color.red(pixel1);
                float G1 = Color.green(pixel1);
                float B1 = Color.blue(pixel1);
                rHist1[x][y]=R1/no_of_pixels;
                gHist1[x][y]=G1/no_of_pixels;
                bHist1[x][y]=B1/no_of_pixels;


                float A2 = Color.alpha(pixel2);
                float R2 = Color.red(pixel2);
                float G2 = Color.green(pixel2);
                float B2 = Color.blue(pixel2);
                rHist2[x][y]=R2/no_of_pixels;
                gHist2[x][y]=G2/no_of_pixels;
                bHist2[x][y]=B2/no_of_pixels;


            }
        }


        float n1=0, n2=0, n3=0;

        for(int x = 0; x < rows; x++) {
            for(int y = 0; y < cols; y++) {
                n1 += Math.sqrt(Math.pow(rHist2[x][y] - rHist1[x][y], 2));
                n2 += Math.sqrt(Math.pow(gHist2[x][y] - gHist1[x][y], 2));
                n3 += Math.sqrt(Math.pow(bHist2[x][y] - bHist1[x][y], 2));
            }
        }
        Log.i(TAG, "n1 : "+n1);
        Log.i(TAG, "n2 : "+n2);
        Log.i(TAG, "n3 : "+n3);
        Log.i(TAG, "Similarity : "+((n1+n2+n3))/10);
        return ((n1+n2+n3)/10);

    }
}
