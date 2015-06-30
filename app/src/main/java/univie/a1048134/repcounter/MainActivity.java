package univie.a1048134.repcounter;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;


public class MainActivity extends Activity{
    private static final String LOG_TAG = "MainActivity";

    Spinner detectorSpinner;
    Spinner matcherSpinner;
    Spinner countdownSpinner;

    EditText distance_input;
    EditText minMatches_input;
    EditText preFrames_input;

    String mSelectedMatcher;
    String mSelectedDetector;
    int mSelectedCountdown;

    TextView mOpencvManagerStatus;
    Button mStartButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText distance_chooser = (EditText) findViewById(R.id.minDist_chooser);
        final EditText matches_chooser = (EditText) findViewById(R.id.matchThres_chooser);
        final EditText preFrames_chosser = (EditText) findViewById(R.id.preFrames_chooser);

        populateSpinners();

        mOpencvManagerStatus = (TextView) findViewById(R.id.text_opencvStatus);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, CounterActivity.class);
            Bundle settings = new Bundle();

            settings.putString("Detector", mSelectedDetector);
            settings.putString("Matcher", mSelectedMatcher);

            settings.putDouble("Distance", Double.valueOf(distance_chooser.getText().toString()));
            settings.putInt("Matches", Integer.valueOf(matches_chooser.getText().toString()));

            settings.putInt("PreFrames", Integer.valueOf(preFrames_chosser.getText().toString()));

            settings.putInt("Countdown", mSelectedCountdown);

            intent.putExtras(settings);
            startActivity(intent);
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        boolean installed = appInstalledOrNot("org.opencv.engine");
        if(installed) {
            mOpencvManagerStatus.setTextColor(getResources().getColor(R.color.green));
            mOpencvManagerStatus.setText(R.string.text_opencvStatusInstalled);
            System.out.println("OpenCV Manager is installed.");
        } else {
            mOpencvManagerStatus.setText(R.string.text_opencvStatusNotInstalled);
            System.out.println("OpenCV Manager is not installed.");
        }
    }

    private void populateSpinners() {
        detectorSpinner = (Spinner) findViewById(R.id.detectors_spinner);
        matcherSpinner = (Spinner) findViewById(R.id.matchers_spinner);
        countdownSpinner = (Spinner) findViewById(R.id.countdown_spinner);

        // Detector Spinner
        ArrayAdapter<CharSequence> detectorAdapter = ArrayAdapter.createFromResource(this, R.array.detector_array, android.R.layout.simple_spinner_dropdown_item);

        detectorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        detectorSpinner.setAdapter(detectorAdapter);
        mSelectedDetector = detectorSpinner.getSelectedItem().toString();
        detectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedDetector = detectorSpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Matcher spinner
        ArrayAdapter<CharSequence> matcherAdapter = ArrayAdapter.createFromResource(this, R.array.matcher_array, android.R.layout.simple_spinner_dropdown_item);

        matcherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        matcherSpinner.setAdapter(matcherAdapter);
        mSelectedMatcher = matcherSpinner.getSelectedItem().toString();
        matcherSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedMatcher = matcherSpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Countdown Spinner
        final String[] ints = {"1","3","5","10","15"};
        ArrayAdapter<String> countdownAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, ints
        );
        countdownSpinner = (Spinner) findViewById(R.id.countdown_spinner);
        countdownSpinner.setSelection(3);
        countdownSpinner.setAdapter(countdownAdapter);
        countdownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedCountdown = Integer.valueOf(ints[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }
}
