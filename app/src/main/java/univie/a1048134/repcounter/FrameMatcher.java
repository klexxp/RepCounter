package univie.a1048134.repcounter;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by klexx on 03.06.2015.
 */
public class FrameMatcher{
    public enum Detector{
        ORB, FLAN
    }
    public enum Descriptor{
        ORB
    }

    private static final String LOG_TAG = "FrameMatcher";

    private FeatureDetector mDetector;
    private DescriptorExtractor mDescriptor;
    private DescriptorMatcher mMatcher;

    private Mat mPrimeFrame = new Mat();
    private MatOfKeyPoint mPrimeKp = new MatOfKeyPoint();
    private Mat mPrimeDescriptor = new Mat();

    private Mat mCompareFrame = new Mat();
    private MatOfKeyPoint mCompareKp = new MatOfKeyPoint();
    private Mat mCompareDescriptor = new Mat();

    private Bitmap mResultImage;

    private double mMinDistance = 0;
    private int mMinMatches = 0;

    int mCount;

    public FrameMatcher(){
    }

    public void compare(Mat frame){
        try {
            mCompareFrame = frame;

            mCompareKp = new MatOfKeyPoint();
            mCompareDescriptor = new Mat();

            mDetector.detect(mCompareFrame, mCompareKp);
            mDescriptor.compute(mCompareFrame, mCompareKp, mCompareDescriptor);

            MatOfDMatch matches = new MatOfDMatch();
            mMatcher.match(mPrimeDescriptor, mCompareDescriptor, matches);

            List<DMatch> descriptorMatches = matches.toList();
            List<DMatch> finalMatches = new ArrayList<>();

            for (int i = 0; i < descriptorMatches.size(); i++) {
                if (descriptorMatches.get(i).distance <= mMinDistance) {
                    finalMatches.add(descriptorMatches.get(i));
                }
            }

            if (finalMatches.size() >= mMinMatches) {
                mCount++;
            }
        }catch(CvException e){
            Log.e(LOG_TAG, e.toString());
        }
    }

    public void setPrimeFrame(Mat frame){
        mPrimeFrame = frame;
        mDetector.detect(frame, mPrimeKp);
        mDescriptor.compute(frame, mPrimeKp, mPrimeDescriptor);
    }
    public boolean primeFrameEmpty(){
        return mPrimeFrame.empty();
    }

    public int getCount(){
        return mCount;
    }
    public Bitmap getResultBitmap(){
        createBitmap();
        return mResultImage;
    }

    public void initialize(String detector, String descriptor, double minDist, int minMatch){

        mMinDistance = minDist;
        mMinMatches = minMatch;

        mDetector = FeatureDetector.create(FeatureDetector.ORB);
        mDescriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        mMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
    }

    private void createBitmap(){
        Mat outImage = mCompareFrame.clone();
        Imgproc.cvtColor(outImage, mCompareFrame, Imgproc.COLOR_RGBA2RGB);
        Features2d.drawKeypoints(mCompareFrame, mCompareKp, outImage);
        Imgproc.cvtColor(mCompareFrame, outImage, Imgproc.COLOR_RGB2RGBA);
        mResultImage = Bitmap.createBitmap(outImage.cols(), outImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outImage, mResultImage);
    }
}