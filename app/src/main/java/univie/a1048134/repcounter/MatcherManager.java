package univie.a1048134.repcounter;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by klexx on 24.06.2015.
 */
public class MatcherManager {
    public static final String LOG_TAG = "MatcherManager";
    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    // Sets the initial threadpool size to 8
    private static final int CORE_POOL_SIZE = 8;
    // Sets the maximum threadpool size to 8
    private static final int MAXIMUM_POOL_SIZE = 8;

    private static final int FRAME = 3;
    private static final int MATCHED = 4;
    private static final int FGMASK = 5;
    private static final int AVG = 6;

    private LinkedBlockingQueue<Runnable> mMatcherQueue;
    private ThreadPoolExecutor mMatcherPool;

    private FeatureMatcher mFeatureMatcher;

    private Handler mHandler;

    private ImageView mPrimeView;
    private ImageView mCompareView;
    private TextView mRepCounter;
    private TextView mAverage;

    private double mDistance;
    private int mMatches;
    private int mPreFrames;

    private int mFrameCount = 0;
    private int mMatchCount = 0;

    private Mat mPrimeFrame = new Mat();
    private BackgroundSubtractorMOG2 mBs;
    private Mat mFGMask = new Mat();

    private boolean bMatchingRunning = false;
    private boolean bPrimeFrameSet = false;
    private boolean bDrawKeypoints = false;

    private Map<Integer, MatchResult> matchResults;
    private ArrayList<Integer> frameList;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    }

    public MatcherManager(ImageView primeView, ImageView compareView, TextView repetitionCounter, TextView average, String detector, String extractor, String matcher, double distance, int matches, int preFrames){

        mPrimeView = primeView;
        mCompareView = compareView;
        mRepCounter = repetitionCounter;
        mAverage = average;
        mDistance = distance;
        mMatches = matches;
        mPreFrames = preFrames;
        matchResults = new HashMap<>();
        frameList = new ArrayList<>();

        mBs = new BackgroundSubtractorMOG2(preFrames,16,true);

        mMatcherQueue = new LinkedBlockingQueue<>();
        mMatcherPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mMatcherQueue);

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                switch(message.what){
                    case Matcher.MATCH_FOUND:
                        mRepCounter.setText(Integer.toString(mMatchCount));
                        mCompareView.setImageBitmap((Bitmap) message.obj);
                        break;
                    case Matcher.MATCH_NOT_FOUND:
                        mCompareView.setImageBitmap((Bitmap) message.obj);
                    case Matcher.MATCHING_FAILED:
                        break;
                    case FRAME:
                        mCompareView.setImageBitmap((Bitmap) message.obj);
                        break;
                    case FGMASK:
                        mPrimeView.setImageBitmap((Bitmap) message.obj);
                        break;
                    case AVG:
                        mAverage.setText(Double.toString((double)message.obj));
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public void handleMatch(MatchResult result){
        synchronized (this) {
            Log.d(LOG_TAG, "Handling Match");
            Log.d(LOG_TAG, result.toString());
            Message msg = mHandler.obtainMessage(AVG,result.mAvg);
            msg.sendToTarget();
            matchResults.put(result.mFrameCount, result);
            outputFrame();
        }
    }

    private void outputFrame(){
        Log.d(LOG_TAG,"Outputting Frame");
        if(frameList.size() > 0 && matchResults.size() > 0) {
            MatchResult result = matchResults.remove(frameList.remove(0));
            if (null != result) {
                Log.d(LOG_TAG,"Processing Result");
                Message msg;
                Mat image;
                Bitmap resultImage;

                switch (result.mState) {
                    case Matcher.MATCHING_FAILED:
                        Log.d(LOG_TAG, "Matching Failed");
                        msg = mHandler.obtainMessage(Matcher.MATCHING_FAILED, null);
                        msg.sendToTarget();
                        break;
                    case Matcher.MATCH_FOUND:
                        Log.d(LOG_TAG, "Match found");
                        mMatchCount++;
                        Log.d(LOG_TAG,"Match count: " + mMatchCount);
                        image = result.mMatchFrame;
                        if (bDrawKeypoints) {
                            image = drawKp(result.mKp, image);
                        }
                        resultImage = getBitmap(image, result.mFrameCount);
                        msg = mHandler.obtainMessage(Matcher.MATCH_FOUND, resultImage);
                        msg.sendToTarget();
                        break;
                    case Matcher.MATCH_NOT_FOUND:
                        Log.d(LOG_TAG, "Match not found");
                        image = result.mMatchFrame;
                        if (bDrawKeypoints) {
                            image = drawKp(result.mKp, image);
                        }
                        resultImage = getBitmap(image, result.mFrameCount);
                        msg = mHandler.obtainMessage(Matcher.MATCH_NOT_FOUND, resultImage);
                        msg.sendToTarget();
                    default:
                        break;
                }
            }
        }else{
            Log.d(LOG_TAG,"No Frames in List!");
        }
    }

    private void removeBackground(Mat frame){
        mBs.apply(frame, mFGMask,  1.0 / mPreFrames);
    }

    private void smoothFgMask(){
        Imgproc.erode(mFGMask, mFGMask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        Imgproc.dilate(mFGMask, mFGMask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
    }

    public void handleFrame(Mat frame) {
        removeBackground(frame);

        if (mFrameCount <= mPreFrames){
            smoothFgMask();
            Message msg = mHandler.obtainMessage(FGMASK, getBitmap(mFGMask));
            msg.sendToTarget();
        }else if(mPrimeFrame.empty()){
            smoothFgMask();
            mPrimeFrame = mFGMask;
            Message msg = mHandler.obtainMessage(FGMASK, getBitmap(mFGMask));
            msg.sendToTarget();
        }else{
            smoothFgMask();
            frameList.add(mFrameCount);
            matchFrames(mFrameCount, mFGMask);
        }

        mFrameCount++;

        /*
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Mat mIntermediateMat = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC4);

        Imgproc.Canny(mFGMask, mIntermediateMat, 80, 100);
        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
    /* Mat drawing = Mat.zeros( mIntermediateMat.size(), CvType.CV_8UC3 );
     for( int i = 0; i< contours.size(); i++ )
     {
    Scalar color =new Scalar(Math.random()*255, Math.random()*255, Math.random()*255);
     Imgproc.drawContours( drawing, contours, i, color, 2, 8, hierarchy, 0, new Point() );
     }*/
        //hierarchy.release();
        //Imgproc.drawContours(mFGMask, contours, -1, new Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255));//, 2, 8, hierarchy, 0, new Point());
        /*
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {

            Imgproc.drawContours(frame, contours, contourIdx, new Scalar(0, 0, 255), -1);
        }
    */
        //Imgproc.drawContours(frame, contours, -1, new Scalar(0,255,0), -1, 8, hierarchy, 0, new Point());
        // Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);





        /*
        removeBackground(frame);
        Mat image = createForegroundImage(frame);
        Message msg = mHandler.obtainMessage(FGMASK, getBitmap(image));
        msg.sendToTarget();
        */
        /*
        Log.d(LOG_TAG, "recieved frame");
        if(mFrameCount <= mPreFrames){
            Log.d(LOG_TAG, "Creating FGMask -> PreFrames: " + mPreFrames + " -> FrameCount: " + mFrameCount);

            removeBackground(frame);

            Message msg = mHandler.obtainMessage(FGMASK, getBitmap(mFGMask));
            msg.sendToTarget();
        }else if(mPrimeFrame.empty()){
            mPrimeFrame = createForegroundImage(frame);
            Message msg = mHandler.obtainMessage(FGMASK, getBitmap(mPrimeFrame));
            msg.sendToTarget();
        }else{
            matchFrames(mFrameCount, createForegroundImage(frame));
            frameList.add(mFrameCount);
        }

        mFrameCount++;*/
    }

    private Mat drawKp(MatOfKeyPoint kp, Mat frame){
        Mat result = new Mat();
        Features2d.drawKeypoints(frame, kp, result);
        return result;
    }

    private Bitmap getBitmap(Mat frame, int frameCount){
        Log.d(LOG_TAG, "getting Bitmap");
        if(frame != null) {
            //Core.putText(frame, String.valueOf(frameCount),new Point(100,100), Core.FONT_HERSHEY_PLAIN, 3,new Scalar(0,255,0,0),3,8,false);
            Bitmap image = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame, image);
            return image;
        }
        return null;
    }

    private Bitmap getBitmap(Mat frame){
        Log.d(LOG_TAG,"getting Bitmap");
        Bitmap image = null;
        if(frame != null) {
            image = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame, image);
            return image;
        }
        return image;
    }

    public void matchFrames(int frameCount, Mat matchFrame){
        Log.d(LOG_TAG,"Matching frames");
        Matcher matcher = (Matcher) mMatcherQueue.poll();
        if(null == matcher){
            matcher = new Matcher();
        }
        matcher.init(this,frameCount, mPrimeFrame, matchFrame, mDistance, mMatches);
        mMatcherPool.execute(matcher);
    }

    public void toggleKeypoints(){
        if(!bDrawKeypoints){
            bDrawKeypoints = true;
        }else{
            bDrawKeypoints = false;
        }
    }
}
