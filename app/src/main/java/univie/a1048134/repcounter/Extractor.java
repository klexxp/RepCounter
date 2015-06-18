package univie.a1048134.repcounter;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

/**
 * Created by klexx on 09.06.2015.
 */
public final class Extractor {
    private static FeatureDetector mDetector;
    private static DescriptorExtractor mDescriptor;

    private static Extractor sInstance;

    static {
        sInstance = new Extractor();
    }

    private Extractor(){
        mDetector = FeatureDetector.create(FeatureDetector.ORB);
        mDescriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    }

    public static Extractor getInstance(){
        return sInstance;
    }

    public static synchronized Mat calcDescriptor(Mat frame, MatOfKeyPoint kp){
        Mat descriptor = new Mat();

        mDescriptor.compute(frame, kp, descriptor);

        return descriptor;
    }
    public static synchronized MatOfKeyPoint calcKeypoints(Mat frame){
        MatOfKeyPoint kp = new MatOfKeyPoint();
        mDetector.detect(frame, kp);

        return kp;
    }
}
