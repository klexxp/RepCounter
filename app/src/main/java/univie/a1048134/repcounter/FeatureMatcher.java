package univie.a1048134.repcounter;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.util.List;

/**
 * Created by klexx on 24.06.2015.
 */
public class FeatureMatcher {
    private static final String LOG_TAG = "FeatureMatcher";

    private FeatureDetector mDetector;
    private DescriptorExtractor mExtractor;
    private DescriptorMatcher mMatcher;

    public FeatureMatcher(){
        mDetector = FeatureDetector.create(FeatureDetector.ORB);
        mExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        mMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        Log.d(LOG_TAG, "FeatureMatcher created");
    }

    public List<DMatch> match(Mat primeFrame, Mat matchFrame){
        Log.d(LOG_TAG, "matching primeFrame with matchFrame");
        synchronized (this){
            MatOfDMatch matches = new MatOfDMatch();
            mMatcher.match(getDescriptor(primeFrame), getDescriptor(matchFrame), matches);
            Log.d(LOG_TAG, "matching prime and compare frame complete");
            return matches.toList();
        }
    }

    private Mat getDescriptor(Mat frame){
        Log.d(LOG_TAG, "getting descriptor");
        Mat descriptor = new Mat();
        mExtractor.compute(frame, getKeypoints(frame), descriptor);
        return descriptor;
    }

    private MatOfKeyPoint getKeypoints(Mat frame){
        Log.d(LOG_TAG, "getting keypoints");
        MatOfKeyPoint kp = new MatOfKeyPoint();
        mDetector.detect(frame, kp);
        return kp;
    }
}
