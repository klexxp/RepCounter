package univie.a1048134.repcounter;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by klexx on 22.06.2015.
 */
public class Matcher implements Runnable {
    private final static String LOG_TAG = "MatcherRunnable";

    public static final int MATCH_FOUND = 2;
    public static final int MATCH_NOT_FOUND = 1;
    public static final int MATCHING_FAILED = 0;

    private Mat mPrimeFrame = new Mat();
    private Mat mMatchFrame = new Mat();

    private MatcherManager mManager;

    private float mDistance;
    private int mMatches;

    private List<DMatch> mMatchesList;

    private FeatureDetector mDetector;
    private DescriptorExtractor mExtractor;
    private DescriptorMatcher mMatcher;

    private MatOfKeyPoint primeKp;
    private MatOfKeyPoint matchKp;
    private Mat mPrimeDescriptor;
    private Mat mMatchDescriptor;
    private Mat mResultFrame;

    private ArrayList<DMatch> mGoodMatches;
    private List<KeyPoint> mKpPrimeList;
    private List<KeyPoint> mKpMatchList;
    private LinkedList<Point> mPrimePoints;
    private LinkedList<Point> mMatchPoints;

    private double mMinDist = 0, mMaxDist = 100;
    private int mFrameCount;
    private double mAvg = 0.0;

    public Matcher(){
        mDetector = FeatureDetector.create(FeatureDetector.ORB);
        mExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        mMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
    }

    public void init(MatcherManager manager, int frameCount, Mat primeFrame, Mat matchFrame, double distance, int matches){
        mManager = manager;
        mPrimeFrame = primeFrame;
        mMatchFrame = matchFrame;
        mDistance = (float) distance;
        mMatches = matches;
        mFrameCount = frameCount;

        Log.d(LOG_TAG, "Matcher initialized");
    }
    @Override
    public void run(){
        Log.d(LOG_TAG, "Matcher Thread running");
        try {
            if(Thread.interrupted()){
                return;
            }

            primeKp = getKeypoints(mPrimeFrame);
            matchKp = getKeypoints(mMatchFrame);

            mPrimeDescriptor = getDescriptor(mPrimeFrame, primeKp);
            mMatchDescriptor = getDescriptor(mMatchFrame, matchKp);

            if(mPrimeDescriptor.type()!= CvType.CV_32F){
                mPrimeDescriptor.convertTo(mPrimeDescriptor, CvType.CV_32F);
            }

            if(mMatchDescriptor.type()!= CvType.CV_32F){
                mMatchDescriptor.convertTo(mMatchDescriptor, CvType.CV_32F);
            }
            mMatchesList = matchFrames(mPrimeDescriptor, mMatchDescriptor);

            mGoodMatches = new ArrayList<>();
            double max_dist = 0; double min_dist = 100;
            for( int i = 0; i < mPrimeDescriptor.rows(); i++ )
            { double dist = mMatchesList.get(i).distance;
                if( dist < min_dist ) min_dist = dist;
                if( dist > max_dist ) max_dist = dist;
            }

            int i = 0;
            for(; i < mMatchesList.size();){
                Double dist = (double) mMatchesList.get(i).distance;

                if(dist <= Math.max(min_dist, 0.02)){
                    mAvg += dist;
                    mGoodMatches.add(mMatchesList.get(i));
                }
                i++;
            }
            mAvg = mAvg / i;
            Log.d(LOG_TAG, "Frame " + mFrameCount + " AvgDist: " + Double.toString(mAvg));

        }catch(CvException e){
            Log.e(LOG_TAG, "Matcher caught exception: " + e.toString());

            if(Thread.interrupted()){
                return;
            }
            MatchResult result = new MatchResult(MATCHING_FAILED, mFrameCount, null, null, mMatchFrame, mAvg);
            mManager.handleMatch(result);
        }finally {
            Log.d(LOG_TAG, "Matcher finished");
            if (Thread.interrupted()) {
                return;
            }

            if(null != mGoodMatches) {
                Log.d(LOG_TAG, "Matches not null / size: " + mGoodMatches.size());
                if (mGoodMatches.size() >= mMatches) {
                    Log.d(LOG_TAG, "Match found!");
                    MatchResult result = new MatchResult(MATCH_FOUND, mFrameCount, mGoodMatches, matchKp, mMatchFrame, mAvg);
                    mManager.handleMatch(result);
                } else {
                    Log.d(LOG_TAG, "No match found!");
                    MatchResult result = new MatchResult(MATCH_NOT_FOUND, mFrameCount, mGoodMatches, matchKp, mMatchFrame,mAvg);
                    mManager.handleMatch(result);
                }
            }else{
                Log.d(LOG_TAG, "Not enogh good matches: " + mGoodMatches.size());
            }
        }
    }

    public List<DMatch> matchFrames(Mat primeDescriptor, Mat matchDescriptor){
        Log.d(LOG_TAG, "matching primeFrame with matchFrame");

        MatOfDMatch matches = new MatOfDMatch();
        mMatcher.match(primeDescriptor, matchDescriptor, matches);

        Log.d(LOG_TAG, "matching prime and compare frame complete");

        return matches.toList();
    }

    private Mat getDescriptor(Mat frame, MatOfKeyPoint frameKeypoints){
        Log.d(LOG_TAG, "getting descriptor");

        Mat descriptor = new Mat();
        mExtractor.compute(frame, frameKeypoints, descriptor);

        return descriptor;
    }

    private MatOfKeyPoint getKeypoints(Mat frame){
        Log.d(LOG_TAG, "getting keypoints");

        MatOfKeyPoint kp = new MatOfKeyPoint();
        mDetector.detect(frame, kp);

        return kp;
    }
}
