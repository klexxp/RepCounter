package univie.a1048134.repcounter;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by klexx on 03.06.2015.
 */
public class FrameMatcher implements Runnable {
    private static final String LOG_TAG = "FrameMatcher";
    private static final long SLEEP_TIME_MILLISECONDS = 250;

    static final int MATCHING_COMPLETE = 0;
    private static final double sThreshold = 0.75;

    private Mat mPrimeDesc = null;
    private MatOfKeyPoint mKpPrime = null;

    private Mat mCompareFrame;
    private Mat mCompareDesc = null;
    private MatOfKeyPoint mKpCompare = null;

    private Mat mImageOut;
    private Bitmap mFinalImage;

    private DescriptorMatcher mMatcher;

    private static MatcherManager mManager;

    private Thread mThisThread;

    private int mResult = 0;
    private int mMinDistance = 10;
    private int mMinMatches = 750;

    public void initialize(MatcherManager manager, Mat primeFrame, Mat compareFrame){
        mManager = manager;
        mThisThread = Thread.currentThread();

        mCompareFrame = compareFrame;

        mKpPrime = Extractor.calcKeypoints(primeFrame);
        mPrimeDesc = Extractor.calcDescriptor(primeFrame, mKpPrime);

        if(Thread.interrupted()){
            return;
        }

        mKpCompare = Extractor.calcKeypoints(compareFrame);
        mCompareDesc = Extractor.calcDescriptor(compareFrame, mKpCompare);

        if(Thread.interrupted()){
            return;
        }

        mMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
    }

    @Override
    public void run() {
        mFinalImage = null;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        if(Thread.interrupted()){
            return;
        }

        try{
            MatOfDMatch matches = new MatOfDMatch();
            mMatcher.match(mPrimeDesc, mCompareDesc, matches);

            if(Thread.interrupted()){
                return;
            }

            List<DMatch> descriptorMatches = matches.toList();
            List<DMatch> finalMatches = new ArrayList<>();

            Log.d(LOG_TAG,"Matches Count: " + descriptorMatches.size());

            for(int i=0; i < descriptorMatches.size(); i++){
                if(descriptorMatches.get(i).distance <= mMinDistance){
                    finalMatches.add(descriptorMatches.get(i));
                }
            }
            Log.d(LOG_TAG, "Final Matches Count: " + finalMatches.size());
            if(finalMatches.size() >= mMinMatches){
                mResult = 1;
            }

        } catch (Throwable e) {
            Log.e(LOG_TAG, "Out of Memory in matcher stage. Throttling.");
            java.lang.System.gc();
            if(Thread.interrupted()){
                return;
            }
            try{
                Thread.sleep(SLEEP_TIME_MILLISECONDS);
            }catch(java.lang.InterruptedException interruptException){
                return;
            }
        }finally{
            createBitmap();
            mManager.handleState(this, MATCHING_COMPLETE, mResult, mFinalImage);
            Thread.interrupted();
        }
    }

    private void  createBitmap(){
        mImageOut = mCompareFrame.clone();
        Features2d.drawKeypoints(mCompareFrame, mKpCompare,mImageOut);
        mFinalImage = Bitmap.createBitmap(mImageOut.cols(), mImageOut.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mImageOut, mFinalImage);
    }

    public Thread getCurrentThread(){
        return mThisThread;
    }
    public void cleanup(){
        mFinalImage = null;
        mCompareFrame = null;
        mPrimeDesc = null;
        mCompareDesc = null;
        mKpCompare = null;
        mKpPrime = null;
        mManager = null;
        mMatcher = null;
        mImageOut = null;
    }
}
