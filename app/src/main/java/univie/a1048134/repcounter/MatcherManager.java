package univie.a1048134.repcounter;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

import org.opencv.core.Mat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by klexx on 03.06.2015.
 */
public class MatcherManager {

    private final String LOG_TAG=("MatcherManager");
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static final int KEEP_ALIVE_TIME = 1;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    private final BlockingQueue<Runnable> mFrameMatcherQueue;
    private final ThreadPoolExecutor mFrameMatcherThreadPool;

    private Mat mPrimeFrame;

    private static MatcherManager sInstance = null;

    private ImageView mFrameView;

    private Handler mHandler;

    private int mCount = 0;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new MatcherManager();
    }

    private MatcherManager(){
        mPrimeFrame = null;
        mFrameMatcherQueue = new LinkedTransferQueue<>();
        mFrameMatcherThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mFrameMatcherQueue);

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                switch (message.what){
                    case FrameMatcher.MATCHING_COMPLETE:
                        mFrameView.setImageBitmap((Bitmap)message.obj);
                        break;
                }
            }
        };
    }

    public void passView(View view){
        mFrameView = (ImageView) view;
    }
    public static MatcherManager getInstance(){
        return sInstance;
    }

    public synchronized void handleState(FrameMatcher matcher, int state, int result, Bitmap image){
        switch (state) {
            case FrameMatcher.MATCHING_COMPLETE:
                mCount = mCount + result;
                Message msg = mHandler.obtainMessage(FrameMatcher.MATCHING_COMPLETE, image);
                msg.sendToTarget();
                matcher.cleanup();
                mFrameMatcherQueue.offer(matcher);
                break;
            default:
                break;
        }
    }

    public int getCount(){
        return mCount;
    }
    public Mat getPrimeFrame(){
        return mPrimeFrame;
    }
    public void setPrimeFrame(Mat frame){
        mPrimeFrame = frame;
    }

    public void matchToFrame(Mat frame){
        FrameMatcher frameMatcher = (FrameMatcher) sInstance.mFrameMatcherQueue.poll();
        if(null == frameMatcher){
            frameMatcher = new FrameMatcher();
        }
        frameMatcher.initialize(MatcherManager.sInstance, mPrimeFrame, frame);
        mFrameMatcherThreadPool.execute(frameMatcher);
    }

    public void cancelAll(){
        FrameMatcher[] matcherArray = new FrameMatcher[sInstance.mFrameMatcherQueue.size()];
        sInstance.mFrameMatcherQueue.toArray(matcherArray);
        int taskArraylen = matcherArray.length;
        synchronized (sInstance) {
            for (int taskArrayIndex = 0; taskArrayIndex < taskArraylen; taskArrayIndex++) {
                Thread thread = matcherArray[taskArrayIndex].getCurrentThread();
                if (null != thread) {
                    thread.interrupt();
                }
            }
        }
    }
}
