package univie.a1048134.repcounter;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class CounterActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    static{ System.loadLibrary("opencv_java"); }
    private static final String LOG_TAG = "CounterActivity";

    private CameraBridgeViewBase mOpenCvCameraView;

    private Button mControlButton;

    private MatcherManager mMatcherManager;
    private Handler mHandler;
    View mCounterView;

    private TextView mCountText;
    private boolean isRunning = false;

    private TextView mCountdownTimerText;
    private GetReadyTimer mTimer;

    private MediaPlayer mPlayer;
    Intent mIntent;

    private int mFrameCount = 0;

    @Override
    protected void onCreate(Bundle stateBundle) {
        super.onCreate(stateBundle);
        mCounterView = getLayoutInflater().inflate(R.layout.activity_counter, null);
        setContentView(mCounterView);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.openCvCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mPlayer = MediaPlayer.create(this, R.raw.cd_beep);

        mIntent = getIntent();
        mMatcherManager = MatcherManager.getInstance();
        mMatcherManager.passView(findViewById(R.id.frameView));

        mTimer = new GetReadyTimer(10000, 1000);
        mCountdownTimerText = (TextView) findViewById(R.id.countdownTimer_text);

        mCountText = (TextView) findViewById(R.id.repetition_count);

        mControlButton = (Button) findViewById(R.id.control_button);
        mControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRunning){
                    (findViewById(R.id.control_button)).setBackgroundResource(R.drawable.start_button_pressed);
                    mCountdownTimerText.setText("10");
                    mCountdownTimerText.setVisibility(View.VISIBLE);
                    mTimer.start();
                }else{
                    stopMatching();
                }
            }
        });

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                mCountText.setText(String.valueOf(message.what));
            }
        };
    }

    public void onResume(){
        super.onResume();

        mFrameCount = 0;

        if(!OpenCVLoader.initDebug()){
            Log.d(LOG_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d(LOG_TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
        if(mMatcherManager != null){
            mMatcherManager.cancelAll();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
        if(mMatcherManager != null){
            mMatcherManager.cancelAll();
        }
    }

    @Override
    protected void onStop() {
        mMatcherManager.cancelAll();
        super.onStop();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.gray();
        if(isRunning) {
            Log.d(LOG_TAG,"Matching started to run.");
            if(null == mMatcherManager.getPrimeFrame()){
                mMatcherManager.setPrimeFrame(frame);
            }
            if(mFrameCount % 25 == 0) {
                mMatcherManager.matchToFrame(frame);
            }
            int count = mMatcherManager.getCount();
            mHandler.sendEmptyMessage(count);
        }
        mFrameCount++;
        return inputFrame.rgba();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(LOG_TAG, "OpenCV loaded successfully.");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void startMatching(){
        isRunning = true;
    }

    private void stopMatching(){
        isRunning = false;
        mMatcherManager.cancelAll();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_counter, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class GetReadyTimer extends CountDownTimer{

        public GetReadyTimer(long startTime, long interval){
            super(startTime, interval);
        }
        @Override
        public void onTick(long l) {
            mPlayer.start();
            mCountdownTimerText.setText(String.valueOf(l/1000));
        }

        @Override
        public void onFinish() {
            mCountdownTimerText.setVisibility(View.GONE);
            startMatching();
        }
    }
}
