package univie.a1048134.repcounter;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.Image;
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
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.io.IOException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CounterActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    static{ System.loadLibrary("opencv_java"); }

    private static final String LOG_TAG = "CounterActivity";
    private MatcherManager mMatcherManager;

    private boolean isRunning = false;

    private CameraBridgeViewBase mOpenCvCameraView;

    private TextView mCountdownTimerText;
    private Button mControlButton;
    private Button mToggleButton;

    private GetReadyTimer mTimer;
    private MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle stateBundle) {
        super.onCreate(stateBundle);
        setContentView(R.layout.activity_counter);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.openCvCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mPlayer = MediaPlayer.create(this, R.raw.cd_beep);

        Bundle settings = getIntent().getExtras();

        String detector = settings.getString("Detector");
        String extractor = settings.getString("Extractor");
        String matcher = settings.getString("Matcher");
        Double distance = settings.getDouble("Distance");
        int matches = settings.getInt("Matches");
        int preFrames = settings.getInt("PreFrames");
        int countdown = settings.getInt("Countdown");

        mMatcherManager = new MatcherManager((ImageView) findViewById(R.id.primeView), (ImageView) findViewById(R.id.compareView), (TextView) findViewById(R.id.repetition_count), (TextView) findViewById(R.id.average), detector, extractor, matcher, distance, matches, preFrames);

        mTimer = new GetReadyTimer(countdown * 1000, 1000);
        mCountdownTimerText = (TextView) findViewById(R.id.countdownTimer_text);

        mControlButton = (Button) findViewById(R.id.control_button);
        mControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRunning){
                    mControlButton.setBackgroundResource(R.drawable.stop);
                    mCountdownTimerText.setText("10");
                    mCountdownTimerText.setVisibility(View.VISIBLE);
                    mTimer.start();
                }else{
                    isRunning = false;
                    mTimer.cancel();
                    (findViewById(R.id.control_button)).setBackgroundResource(R.drawable.play);
                }
            }
        });

        mToggleButton = (Button) findViewById(R.id.toggleKp_button);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMatcherManager.toggleKeypoints();
            }
        });

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if(isRunning) {
            mMatcherManager.handleFrame(inputFrame.rgba());
            Log.d(LOG_TAG, "matching is running");
        }

        return inputFrame.rgba();
    }

    public void onResume(){
        super.onResume();

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
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
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
                case LoaderCallbackInterface.INIT_FAILED:
                    Log.i(LOG_TAG,"Init Failed");
                    break;
                case LoaderCallbackInterface.INSTALL_CANCELED:
                    Log.i(LOG_TAG,"Install Cancelled");
                    break;
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                    Log.i(LOG_TAG,"Incompatible Version");
                    break;
                case LoaderCallbackInterface.MARKET_ERROR:
                    Log.i(LOG_TAG,"Market Error");
                    break;
                default:
                    Log.i(LOG_TAG,"OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

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
            isRunning = true;
        }
    }
}
