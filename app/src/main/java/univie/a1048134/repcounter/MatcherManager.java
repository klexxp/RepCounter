package univie.a1048134.repcounter;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
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

        Log.d(LOG_TAG,"Queue and Pool set up.");

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                Log.d(LOG_TAG,"Handling message.");
                switch (message.what){
                    case FrameMatcher.MATCHING_COMPLETE:
                        Log.d(LOG_TAG, "Matching Complete.");
                        mFrameView.setImageBitmap((Bitmap)message.obj);
                        Log.d(LOG_TAG,"Bitmap set");
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
        Log.d(LOG_TAG,"Handling state");
        switch (state) {
            case FrameMatcher.MATCHING_COMPLETE:
                Log.d(LOG_TAG,"Matching complete.");
                mCount = mCount + result;
                Message msg = mHandler.obtainMessage(FrameMatcher.MATCHING_COMPLETE, image);
                msg.sendToTarget();
                Log.d(LOG_TAG, "Cleaning up matcher");
                matcher.cleanup();
                Log.d(LOG_TAG,"Offering frameMatcher thread to pool");
                mFrameMatcherQueue.offer(matcher);
                break;
            default:
                break;
        }
    }

    public int getCount(){
        Log.d(LOG_TAG, "Returning count.");
        return mCount;
    }
    public Mat getPrimeFrame(){
        Log.d(LOG_TAG,"Returning prime frame.");
        return mPrimeFrame;
    }
    public void setPrimeFrame(Mat frame){
        Log.d(LOG_TAG,"Setting prime frame.");
        mPrimeFrame = frame;
    }

    public void matchToFrame(Mat frame){
        Log.d(LOG_TAG,"Starting to match frames.");
        FrameMatcher frameMatcher = (FrameMatcher) sInstance.mFrameMatcherQueue.poll();
        if(null == frameMatcher){
            frameMatcher = new FrameMatcher();
        }
        Log.d(LOG_TAG,"New frame matcher received.");
        frameMatcher.initialize(MatcherManager.sInstance, mPrimeFrame, frame);
        Log.d(LOG_TAG, "Frame matcher initialized. executing");
        mFrameMatcherThreadPool.execute(frameMatcher);
    }

    public void cancelAll(){
        Log.d(LOG_TAG,"Canceling all threads.");
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
        Log.d(LOG_TAG,"All threads cancelled");
    }
}
