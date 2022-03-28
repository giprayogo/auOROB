package zeronzerot.auorob;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.text.DecimalFormat;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

//todo:delete replace fragment
//BluetoothFragment.OnFragmentInteractionListener
public class RunActivity extends Activity implements
        DebugFragment.OnFragmentInteractionListener,
        CVWindowFragment.ImageProcessingListener
    //TODO:reimplement listeners in runact
{
    private static final String TAG = "RunActivity: ";

    //UI Components
    private CVWindowFragment mCVFragment;
    private Fragment mRightFragment;
    //@ Toolbar
    private Toolbar mToolbar;
    private TextView mNutCountText;
    private TextView mCountdownText;
    private TextView mAccText;
    private TextView mGyroText;

    protected String numText = new String();

    //Robotic program fields
    private Roboto mRoboto = new Roboto();
    //For delaying robot race sequence starts
    private CountDownTimer mCountDownTimer;
    private boolean mRaceStarted = false;
    private int mTimerLength = 5;
    DecimalFormat timerDf = new DecimalFormat("0.0");
    //Sensors
    //For detecting collisions
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    //Bluetooth
    //for communication with Arduino
    BluetoothSPP mBt = new BluetoothSPP(this);

    //Callback and listeners
    //OpenCV Listeners
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    // Load preview
                    mCVFragment = new CVWindowFragment();
                    mRightFragment = new DebugFragment();
                    replaceFragment(mCVFragment, R.id.content_frame);
                    replaceFragment(mRightFragment, R.id.right_drawer);

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);

        mNutCountText = (TextView) findViewById(R.id.toolbar_nut_count);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        readSharedPreferences();

        //Toolbar setup
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(mToolbar);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
//        getActionBar().setHomeButtonEnabled(true);
//        getActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

        //Countdown Timer
        mCountdownText = (TextView) findViewById(R.id.toolbar_race_countdown);
        mCountdownText.setText(timerDf.format(mTimerLength / 1000));
        mAccText = (TextView) findViewById(R.id.toolbar_acc_text);
        mGyroText = (TextView) findViewById(R.id.toolbar_gyro_text);

        //Setup bletooth listener
        mBt.setBluetoothConnectionListener(mRoboto.btConnectionListener);

        mCountDownTimer = new CountDownTimer(mTimerLength, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                double seconds = (double) millisUntilFinished / 1000;
                mCountdownText.setText(timerDf.format(seconds));
            }

            @Override
            public void onFinish() {
                //Reset timer
                mCountdownText.setText(timerDf.format(mTimerLength / 1000));
                //mRaceStarted = false;
                setBarColor(R.color.toolbar_background, R.color.status_bar);
                //Start Robot
                mRoboto.start();
            }
        };

        //Sensor setup
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mRoboto.accelerometerListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mRoboto.gyroscopeListener, mGyroscope, SensorManager.SENSOR_DELAY_UI);
        //Resume OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        readSharedPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCountDownTimer.cancel();
        mSensorManager.unregisterListener(mRoboto.accelerometerListener);
        mSensorManager.unregisterListener(mRoboto.gyroscopeListener);
        mRoboto.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case BluetoothState.REQUEST_CONNECT_DEVICE:
                    mBt.connect(data);
                    break;
                case BluetoothState.REQUEST_ENABLE_BT:
                    mBt.setupService();
                    mBt.startService(BluetoothState.DEVICE_OTHER);
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                    break;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_run, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        //Enable bluetooth
        if (id == R.id.action_bluetooth) {
            //Check Bluetooth State
            if (mBt.isBluetoothAvailable()) {
                if (mBt.isBluetoothEnabled()) {
                    mBt.setupService();
                    mBt.startService(BluetoothState.DEVICE_OTHER);
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, BluetoothState.REQUEST_ENABLE_BT);
                }
            } else {
                makeToast("Bluetooth not available");
            }
        }
        //Trigger timer
        if (id == R.id.action_run) {
            mRoboto.setCurrentState(Roboto.PAUSED);
            if (mRaceStarted) {
                //reset timer
                mRaceStarted = false;
                mRoboto.stop();
                mCountdownText.setText(timerDf.format(mTimerLength / 1000));
                mToolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_background));
                getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar));
                mCountDownTimer.cancel();
            } else {
                //start timer
                mRaceStarted = true;
                mToolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_background_timing));
                getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_timing));

                mCountDownTimer.start();
            }
        }

        return super.onOptionsItemSelected(item);
    }



    private void replaceFragment(Fragment fragment, int resourceId) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(resourceId, fragment);
        transaction.commit();
    }

    //All Listeners
    //Send OpenCV Data over Bluetooth
    @Override
    public void onCVDataAvailable(int type, double data) {
        mRoboto.cvListener.onCVDataAvailable(type, data);
    }

    //Fragment listeners.
    //Or "fragment communication highway"
    //****OpenCVFragment Listener implementations
    @Override
    public void onToggleSwitch(int mode, boolean isChecked) {
        if (mode == DebugFragment.MAGNET) {
            mRoboto.setMagnet(isChecked);
        } else {
            mCVFragment.setProcess(mode, isChecked);
        }
    }

    @Override
    public void onRadioMode(int mode) {
        switch (mode) {
            case DebugFragment.LF_FW:
                mRoboto.setForward(true);
                mRoboto.setCurrentState(Roboto.LINE_FOLLOWING);
                break;
            case DebugFragment.LF_BW:
                mRoboto.setForward(false);
                mRoboto.setCurrentState(Roboto.LINE_FOLLOWING);
                break;
            case DebugFragment.LF_NA:
                mRoboto.setForward(false);
                mRoboto.setCurrentState(Roboto.NA_LINE_FOLLOWING);
                break;
            case DebugFragment.NS:
                mRoboto.setCurrentState(Roboto.NUT_SEARCHING);
                break;
            case DebugFragment.NT:
                mRoboto.setCurrentState(Roboto.NUT_TAKING);
                break;
            case DebugFragment.ROT:
                mRoboto.setCurrentState(Roboto.ROTATE_90);
                break;
            case DebugFragment.OFF:
                mRoboto.setCurrentState(Roboto.PAUSED);
        }
    }

    @Override
    public void onThresholdChanged(int threshold) {
        mCVFragment.setThreshold(threshold);
    }

    //Quick and dirty methods***
    //Make toast from anywhere!!!
    private void makeToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    //channge toolbar and statusbar color from anywheere!!
    private void setBarColor(final int toolBarColor, final int statusBarColor) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToolbar.setBackgroundColor(getResources().getColor(toolBarColor));
                getWindow().setStatusBarColor(getResources().getColor(statusBarColor));
            }
        });
    }

    private void readSharedPreferences() {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(this);
        mRoboto.setnNut(Integer.parseInt(sP.getString(getString(R.string.key_n_nut), "16")));
        numText = "0 / " + sP.getString(getString(R.string.key_n_nut), "0");
        mNutCountText.setText(numText);
        mRoboto.setNutTakeTimeout(Integer.parseInt(sP.getString(getString(R.string.key_nut_take_timeout), "5")));
        mRoboto.setLongThreshold(Double.parseDouble(sP.getString(getString(R.string.key_long_acc_threshold), "3.0")));
        mRoboto.setVertThreshold(Double.parseDouble(sP.getString(getString(R.string.key_vert_acc_threshold), "3.0")));
        mRoboto.setGyroThreshold(Double.parseDouble(sP.getString(getString(R.string.key_gyro_threshold), "300")));
        mRoboto.setVertSpeed(Integer.parseInt(sP.getString(getString(R.string.key_vertical_speed), "40")));
        mRoboto.setYawSpeed(Integer.parseInt(sP.getString(getString(R.string.key_yaw_speed), "40")));
        mRoboto.setTop(sP.getBoolean(getString(R.string.key_start_top_on), true));
        //in milliseconds
        mTimerLength = 1000 * Integer.parseInt(sP.getString(getString(R.string.key_countdown_length), "5"));
    }
    //TODO: makeSnackbar

    //Control class
    private class Roboto {
        //state constants
        static final int PAUSED = 66;
        static final int LINE_FOLLOWING = 787;      //standard line following
        static final int NA_LINE_FOLLOWING = 22;   //line following for eol cases
        static final int NUT_SEARCHING = 32;
        static final int NUT_TAKING = 40;
        static final int ROTATE_90 = 9090;

        //Header declarations
        //Initialisation
        final int HEADER_BIT    = 0x80;
        //Sign and directions
        final int SIGN          = 0x02; //0b0000 0010
        final int FORWARD = 0x20; //0b0010 0000
        final int MAGNET        = 0x01; //0b0000 0001
        //Line Following & nut searching errors
        final int ERR_LATERAL	= 0x10; //0b000 100 00
        final int ERR_VERTICAL  = 0x08; //0b000 010 00
        //Line following only
        final int ERR_YAW       = 0x18; //0b000 110 00
        //Nut searching only
        final int ERR_LONG      = 0x04; //0b000 001 00
        //Other commands
        final int V_TRANSLATION = 0x1C; //0b000 111 00
        final int YAW = 0x0C; //0b000 011 00

        //Robot controller select
        final int OFF_CTRL      = 0x00; //0b0 00 00000
        final int NS_CTRL       = 0x20; //0b0 01 00000
        final int LF_CTRL       = 0x40; //0b0 10 00000

        //State Fields
        private int mNutTaken = 0;
        private boolean mBTConnected = false;
        private int mCurrentState = PAUSED;
        private boolean mDebug = true;
        private boolean changeStateLegal = true;
        private boolean mTop = true;

        //State change parameters
        private int nNut = 16;
        private int nutTakeTimeout = 5;
        private double longThreshold = 3.0;
        private double vertThreshold = 3.0;
        private double gyroThreshold = 300;
        private byte yawSpeed = 40;
        private byte vertSpeed = 40;

        //state values
        private double gyroAccumulator = 0;
        private boolean mForward = true;
        private boolean mMagnet = false;

        //Formatting thingss
        DecimalFormat df = new DecimalFormat("#0.00");

        //Event Listeners
        //Accelerometer and gyroscope
        //Used for internal (Android) state switching
        //and nut area yaw detection
        protected SensorEventListener gyroscopeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                final float gyroValue = event.values[2];
                final boolean btConnected = mBTConnected;
                final boolean magnet = mMagnet;
                final int currentState = mCurrentState;
                final boolean forward = mForward;

                mGyroText.setText(df.format(gyroAccumulator));

                switch (currentState) {
                    case ROTATE_90:
                        gyroAccumulator += gyroValue * 0.27;
                        if (Math.abs(gyroAccumulator) > gyroThreshold) {
                            nextState();
                            gyroAccumulator = 0;
                            break;
                        }
                        if (btConnected) {
                            int packetHeader = 0xFF & (HEADER_BIT + OFF_CTRL + YAW);
                            if (magnet) {
                                packetHeader += MAGNET;
                            }
                            mBt.send(new byte[] {(byte) packetHeader, yawSpeed}, false);
                        }
                        break;
                    case NUT_TAKING:
                        gyroAccumulator += gyroValue * 0.27;
                        if (btConnected) {
                            int packetHeader = 0xFF & (HEADER_BIT + OFF_CTRL + ERR_YAW);
                            int packetData = (int) gyroAccumulator;
                            if (packetData < 0) {
                                packetHeader += SIGN; //set negative sign
                            }
                            if (magnet) {
                                packetHeader += MAGNET;
                            }
                            if (Math.abs(packetData) > 127) {
                                packetData = 127;
                            }
                            mBt.send(new byte[]{(byte) (0xFF & packetHeader), (byte) (0x7F & packetData)}, false);
                        }
                        break;
                    case NA_LINE_FOLLOWING:
                        gyroAccumulator += gyroValue * 0.27;
                        if (btConnected) {
                            int packetHeader = 0xFF & (HEADER_BIT + LF_CTRL + ERR_YAW);
                            int packetData = (int) gyroAccumulator;
                            if (packetData < 0) {
                                packetHeader += SIGN; //set negative sign
                            }
                            if (forward) {
                                packetHeader += FORWARD;
                            }
                            if (magnet) {
                                packetHeader += MAGNET;
                            }
                            if (Math.abs(packetData) > 127) {
                                packetData = 127;
                            }
                            mBt.send(new byte[]{(byte) (0xFF & packetHeader), (byte) (0x7F & packetData)}, false);
                        }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        protected SensorEventListener accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                final float longAcceleration = event.values[1];
                final float vertAcceleration = event.values[2];
                final boolean btConnected = mBTConnected;
                final boolean magnet = mMagnet;
                final int currentState = mCurrentState;

                mAccText.setText(df.format(vertAcceleration));

                switch (currentState) {
                    //use accelerometer to "detect" walls > change robot direction
                    case LINE_FOLLOWING:
                        if (longAcceleration > longThreshold) {
                            if (!mForward) {
                                nextState();
                            }
//                            mForward = !mForward;
//                            setCurrentState(LINE_FOLLOWING);
                        }
                        break;
                    //use accelerometer to "detect" floor
                    case NUT_TAKING:
                        if (vertAcceleration > vertThreshold) {
                            nextState(); //go to next state
                        }
                        //send data
                        if (btConnected) {
                            int packetHeader = 0xFF & (HEADER_BIT + OFF_CTRL + V_TRANSLATION);
                            if (magnet) {
                                packetHeader += MAGNET;
                            }
                            mBt.send(new byte[] {(byte) packetHeader, vertSpeed}, false);
                        }
                    default:
                        //do nothing
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        //CV Listeners
        //Send error data to robot
        protected CVWindowFragment.ImageProcessingListener cvListener = new CVWindowFragment.ImageProcessingListener() {
            @Override
            public void onCVDataAvailable(int type, double data) {
                final boolean btConnected = mBTConnected;
                final boolean magnet = mMagnet;
                final int currentState = mCurrentState;
                final boolean forward = mForward;

                switch (currentState) {
                    case NA_LINE_FOLLOWING:
                        if (type == CVWindowFragment.BOL) {
                            nextState();
                        }
                    case LINE_FOLLOWING:
                        //send angle data, mid data, and width data
                        switch (type) {
                            case CVWindowFragment.NA:
                                nextState();
                                break;
                            case CVWindowFragment.EOL:
//                                nextState();
                                break;
                            case CVWindowFragment.VERTICAL:
                                if (btConnected) {
                                    int packetHeader = 0xFF & (HEADER_BIT + LF_CTRL + ERR_VERTICAL);
                                    int packetData = (int) data;
                                    if (packetData < 0) {
                                        packetHeader += SIGN; //set negative sign
                                    }
                                    if (forward) {
                                        packetHeader += FORWARD;
                                    }
                                    if (magnet) {
                                        packetHeader += MAGNET;
                                    }
                                    if (Math.abs(data) > 127) {
                                        packetData = 127;
                                    }
                                    mBt.send(new byte[]{(byte) packetHeader, (byte) (0x7F & packetData)}, false);
                                }
                                break;
                            case CVWindowFragment.LATERAL:
                                if (btConnected) {
                                    int packetHeader = 0xFF & (HEADER_BIT + LF_CTRL + ERR_LATERAL);
                                    int packetData = (int) data;
                                    if (packetData < 0) {
                                        packetHeader += SIGN; //set negative sign
                                    }
                                    if (forward) {
                                        packetHeader += FORWARD;
                                    }
                                    if (magnet) {
                                        packetHeader += MAGNET;
                                    }
                                    if (Math.abs(data) > 127) {
                                        packetData = 127;
                                    }
                                    mBt.send(new byte[]{(byte) (0xFF & packetHeader), (byte) (0x7F & packetData)}, false);
                                }
                                break;
                            case CVWindowFragment.YAW:
                                if (btConnected) {
                                    int packetHeader = 0xFF & (HEADER_BIT + LF_CTRL + ERR_YAW);
                                    int packetData = (int) data;
                                    if (packetData < 0) {
                                        packetHeader += SIGN; //set negative sign
                                    }
                                    if (forward) {
                                        packetHeader += FORWARD;
                                    }
                                    if (magnet) {
                                        packetHeader += MAGNET;
                                    }
                                    if (Math.abs(data) > 127) {
                                        packetData = 127;
                                    }
                                    mBt.send(new byte[]{(byte) (0xFF & packetHeader), (byte) (0x7F & packetData)}, false);
                                }
                                break;
                        }
                        break;
                    case NUT_SEARCHING:
                        //send circle displacement
                        switch (type) {
                            case CVWindowFragment.LONG:
                                //if negative, movement also negative
                                if (btConnected) {
                                    int packetHeader = 0xFF & (HEADER_BIT + NS_CTRL + ERR_LONG);
                                    int packetData = (int) data;
                                    if (packetData < 0) {
                                        packetHeader += SIGN; //set negative sign
                                    }
                                    if (magnet) {
                                        packetHeader += MAGNET;
                                    }
                                    //limit
                                    if (Math.abs(data) > 127) {
                                        packetData = 127;
                                    }
                                    mBt.send(new byte[]{(byte) (0xFF & packetHeader), (byte) (0x7F & packetData)}, false);
                                }
                                break;
                            case CVWindowFragment.LATERAL:
                                if (btConnected) {
                                    int packetHeader = 0xFF & (HEADER_BIT + NS_CTRL + ERR_LATERAL);
                                    int packetData = (int) data;
                                    if (packetData < 0) {
                                        packetHeader += SIGN; //set negative sign
                                    }
                                    if (magnet) {
                                        packetHeader += MAGNET;
                                    }
                                    //limit
                                    if (Math.abs(data) > 127) {
                                        packetData = 127;
                                    }
                                    mBt.send(new byte[]{(byte) (0xFF & packetHeader), (byte) (0x7F & packetData)}, false);
                                }
                                break;
                            case CVWindowFragment.VERTICAL:
                                if (btConnected) {
                                    int packetHeader = 0xFF & (HEADER_BIT + NS_CTRL + ERR_VERTICAL);
                                    int packetData = (int) data;
                                    if (packetData < 0) {
                                        packetHeader += SIGN; //set negative sign
                                    }
                                    if (magnet) {
                                        packetHeader += MAGNET;
                                    }
                                    if (Math.abs(data) > 127) {
                                        packetData = 127;
                                    }
                                    mBt.send(new byte[]{(byte) packetHeader, (byte) (0x7F & packetData)}, false);
                                }
                                break;
                            case CVWindowFragment.HIT:
                                nextState();
                        }
                        break;
                }
            }

        };

        //Bluetooth connection listener
        //TODO: change to bluetooth state listener
        protected BluetoothSPP.BluetoothConnectionListener btConnectionListener = new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                // Do something when successfully connected
                mBTConnected = true;
            }

            public void onDeviceDisconnected() {
                // Do something when connection was disconnected
                makeToast("Bluetooth device disconnected");
                mBTConnected = false;
            }

            public void onDeviceConnectionFailed() {
                // Do something when connection failed
                makeToast("Connection Failed");
                mBTConnected = false;
            }
        };

        //Helper Objects
        //nut taking timeout
        CountDownTimer nutTakeTimer = new CountDownTimer(3300, 100) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (mCurrentState == NUT_TAKING) {
                    nextState();
                }
            }
        };
        //state change delay
        CountDownTimer delayTimer = new CountDownTimer(200, 100) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                changeStateLegal = true;
            }
        };

        Roboto() {
        }

        //PLaying with state
        private int mCursor = 0;
//        private int[] loop = new int[] {LINE_FOLLOWING, NUT_SEARCHING, NUT_TAKING, NA_LINE_FOLLOWING, LINE_FOLLOWING};
//        private boolean[] loopForward = new boolean[] {true, false, false, false, false};
//        private boolean[] loopMagnet = new boolean[] {false, false, true, true, true};
        private int[] loop = new int[] {LINE_FOLLOWING, NUT_TAKING, NA_LINE_FOLLOWING, LINE_FOLLOWING};
        private boolean[] loopForward = new boolean[] {true, false, false, false};
        private boolean[] loopMagnet = new boolean[] {false, true, true, true};
        private boolean rotateFlag = false;
        private boolean halfFlag = false;

        void start() {
            mCursor = 0;
            mDebug = false;
            mForward = loopForward[mCursor];
            mMagnet = loopMagnet[mCursor];
            setCurrentState(loop[mCursor]);
            makeToast("START");
        }
        void stop() {
            mCursor = 0;
            mDebug = true;
            mForward = loopForward[mCursor];
            mMagnet = loopMagnet[mCursor];
            mNutTaken = 0;
            setCurrentState(PAUSED);
        }

        void nextState() {
            final boolean debug = mDebug;
            gyroAccumulator = 0;
            if (debug) {
                //Don't switch state
            } else {
//                makeToast("NXT");
//                mCursor = (mCursor + 1) % loop.length;
                mCursor += 1;
                if (mCursor >= loop.length) {  //one phase completed
                    mNutTaken += 1;
                    mCursor = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            numText = Integer.toString(mNutTaken) + " / " + Integer.toString(nNut);
                            mNutCountText.setText(numText);
                        }
                    });
                    if (mNutTaken == nNut) {
                        stop();
                        return;
                    }
                }
                if (mNutTaken == nNut/2 && !halfFlag) {   //halfway done
                    rotateFlag = true;
                    halfFlag = true;
                }
                if (rotateFlag && mCurrentState == NUT_TAKING) {
                    setCurrentState(ROTATE_90);
                    mCursor--;
                    rotateFlag = false; //clear rotate flag
                    mTop = !mTop;
                    return;
                }
                mForward = loopForward[mCursor];
                mMagnet = loopMagnet[mCursor];
                setCurrentState(loop[mCursor]);
            }
        }

        void prevState() {
            final boolean debug = mDebug;

            if (debug) {
                //Don't switch state
            } else {
                mCursor = (mCursor - 1) % loop.length;
                setCurrentState(loop[mCursor]);
            }
        }

        //setters
        void setCurrentState(final int currentState) {
            final boolean forward = mForward;
            final boolean top = mTop;
            final boolean btConnected = mBTConnected;
            gyroAccumulator = 0;

            if (!changeStateLegal) {
                return;
            }
            switch (currentState) {
                case LINE_FOLLOWING:
                    if (forward) {
                        setBarColor(R.color.toolbar_background_lf_forward, R.color.status_bar_lf_forward);
                        mCVFragment.setLineFollowing(true, top, CVWindowFragment.LF_FORWARD);
                    } else {
                        setBarColor(R.color.toolbar_background_lf_backward, R.color.status_bar_lf_backward);
                        mCVFragment.setLineFollowing(true, top, CVWindowFragment.LF_BACKWARD);
                    }
                    //Set OpenCV
                    mCVFragment.setNutSearching(false, top);
                    break;
                case NA_LINE_FOLLOWING:
                    setBarColor(R.color.toolbar_background_slow_backward, R.color.status_bar_lf_nut_area);
                    //SetCV
                    mCVFragment.setLineFollowing(true, top, CVWindowFragment.LF_NA);
                    mCVFragment.setNutSearching(false, top);
                    break;
                case NUT_SEARCHING:
                    setBarColor(R.color.toolbar_background_nut_searching, R.color.status_bar_nut_searching);
                    //SetCV
                    mCVFragment.setLineFollowing(false);
                    mCVFragment.setNutSearching(true, top);
                    break;
                case NUT_TAKING:
                    setBarColor(R.color.toolbar_background_nut_taking, R.color.status_bar_nut_taking);
                    nutTakeTimer.start();
                    //Set CV
                    mCVFragment.setLineFollowing(false);
                    mCVFragment.setNutSearching(false, top);
                    break;
                case ROTATE_90:
                    setBarColor(R.color.toolbar_background_rotate_90, R.color.status_bar_rotate_90);
                    //Set CV
                    mCVFragment.setLineFollowing(false);
                    mCVFragment.setNutSearching(false, top);
                    mTop = !mTop;
                    break;
                case PAUSED:
                    setBarColor(R.color.toolbar_background, R.color.status_bar);
                    //SetCV
                    mCVFragment.setLineFollowing(false);
                    mCVFragment.setNutSearching(false, top);
                    if (btConnected) {
                        int packetHeader = 0xFF & (HEADER_BIT + OFF_CTRL + YAW);
                        mBt.send(new byte[] {(byte) packetHeader, 0}, false);
                        packetHeader = 0xFF & (HEADER_BIT + OFF_CTRL + V_TRANSLATION);
                        mBt.send(new byte[] {(byte) packetHeader, 0}, false);
                    }
            }
            this.mCurrentState = currentState;
            //to prevent 2 close activations
            changeStateLegal = false;
            delayTimer.start();
        }

        //Preference handles
        //setters
        public void setTop(final boolean top) {
            this.mTop = top;
        }
        public void setLongThreshold(final double longThreshold) {
            this.longThreshold = longThreshold;
        }
        public void setVertThreshold(final double vertThreshold) {
            this.vertThreshold = vertThreshold;
        }
        public void setGyroThreshold(final double gyroThreshold) {
            this.gyroThreshold = gyroThreshold;
        }
        public void setnNut(final int nNut) {
            this.nNut = nNut;
        }
        public void setNutTakeTimeout(final int nutTakeTimeout) {
            this.nutTakeTimeout = nutTakeTimeout;
        }
        public void setVertSpeed(final int vertSpeed) {
            if (vertSpeed > 127) {
                this.vertSpeed = 127;
            } else if (vertSpeed < -128) {
                this.vertSpeed = -128;
            } else {
                this.vertSpeed = (byte) vertSpeed;
            }
        }
        public void setYawSpeed(final int yawSpeed) {
            if (yawSpeed > 127) {
                this.yawSpeed = 127;
            } else if (yawSpeed < -128) {
                this.yawSpeed = -128;
            } else {
                this.yawSpeed = (byte) yawSpeed;
            }
        }
        public void setForward(final boolean forward) {
            this.mForward = forward;
        }
        public void setMagnet(final boolean mMagnet) {
            this.mMagnet = mMagnet;
        }
        public void setDebug(final boolean debug) {
            if (debug == mDebug) {
                return;
            } else if (debug == true) {

            } else if (debug == false) {

            }
        }
    }

}