package univie.a1048134.repcounter;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;

import java.util.List;

/**
 * Created by klexx on 28.06.2015.
 */
public class MatchResult {
    List<DMatch> mMatches;
    MatOfKeyPoint mKp;
    Mat mMatchFrame;
    int mFrameCount;
    int mState;
    double mAvg;

    public MatchResult(int state, int frameCount, List<DMatch> matches, MatOfKeyPoint kp, Mat matchFrame, double avg){
        mMatches = matches;
        mKp = kp;
        mMatchFrame = matchFrame;
        mFrameCount = frameCount;
        mState = state;
        mAvg = avg;
    }

    public String toString(){
        return "FrameCount: " + mFrameCount + " / Avg: " + mAvg + " / State: " + mState + " / Matches: " + mMatches.size() + " / Keypoints: " + mKp.size();
    }
}
