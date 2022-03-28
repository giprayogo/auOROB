package zeronzerot.auorob;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;

//OpenCV settings manual toggles
public class DebugFragment extends Fragment {
    //states
    public static final int LF_FW = 100;
    public static final int LF_BW = 101;
    public static final int LF_NA = 102;
    public static final int NS = 123;
    public static final int NT = 134;
    public static final int ROT = 900;
    public static final int OFF = 911;
    public static final int MAGNET = 777;

    private Switch debugSwitch;
    private Switch magnetSwitch;
    private Switch cannySwitch;
    private Switch colorFilterSwitch;
    private Switch lineDetectorSwitch;
    private Switch circleDetectorSwitch;
    private Switch thresholdSwitch;
    private Switch lutSwitch;
    private Switch nldSwitch;

    private RadioButton offRadio;
    private RadioButton lfForwardRadio;
    private RadioButton lfBackwardRadio;
    private RadioButton lfNutAreaRadio;
    private RadioButton nutSearchingRadio;
    private RadioButton nutTakingRadio;
    private RadioButton rotateRadio;

    //Event Listeners
    private OnFragmentInteractionListener mListener;
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean checked = ((RadioButton) v).isChecked();
            if (checked) {
                switch (v.getId()) {
                    case R.id.radio_line_follower_fw:
                        mListener.onRadioMode(LF_FW);
                        break;
                    case R.id.radio_line_follower_bw:
                        mListener.onRadioMode(LF_BW);
                        break;
                    case R.id.radio_line_follower_na:
                        mListener.onRadioMode(LF_NA);
                        break;
                    case R.id.radio_nut_searching:
                        mListener.onRadioMode(NS);
                        break;
                    case R.id.radio_nut_taking:
                        mListener.onRadioMode(NT);
                        break;
                    case R.id.radio_rotate_90:
                        mListener.onRadioMode(ROT);
                        break;
                    case R.id.radio_off:
                        mListener.onRadioMode(OFF);
                }
            }
        }
    };
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch(buttonView.getId()) {
                case R.id.switch_debug:
                    if (isChecked) {
                        cannySwitch.setEnabled(true);
                        colorFilterSwitch.setEnabled(true);
                        magnetSwitch.setEnabled(true);
                        lineDetectorSwitch.setEnabled(true);
                        circleDetectorSwitch.setEnabled(true);
                        thresholdSwitch.setEnabled(true);
                        lutSwitch.setEnabled(true);
                        nldSwitch.setEnabled(true);
                        offRadio.setEnabled(true);
                        lfForwardRadio.setEnabled(true);
                        lfBackwardRadio.setEnabled(true);
                        lfNutAreaRadio.setEnabled(true);
                        nutSearchingRadio.setEnabled(true);
                        nutTakingRadio.setEnabled(true);
                        rotateRadio.setEnabled(true);
                    } else {
                        cannySwitch.setEnabled(false);
                        colorFilterSwitch.setEnabled(false);
                        magnetSwitch.setEnabled(false);
                        lineDetectorSwitch.setEnabled(false);
                        circleDetectorSwitch.setEnabled(false);
                        thresholdSwitch.setEnabled(false);
                        lutSwitch.setEnabled(false);
                        nldSwitch.setEnabled(false);
                        offRadio.setEnabled(false);
                        lfForwardRadio.setEnabled(false);
                        lfBackwardRadio.setEnabled(false);
                        lfNutAreaRadio.setEnabled(false);
                        nutSearchingRadio.setEnabled(false);
                        nutTakingRadio.setEnabled(false);
                        rotateRadio.setEnabled(false);
                    }
                    break;
                case R.id.switch_magnet:
                    mListener.onToggleSwitch(MAGNET, isChecked);
                    break;
                case R.id.switch_color_filter:
                    mListener.onToggleSwitch(CVWindowFragment.COLOR_FILTER, isChecked);
                    break;
                case R.id.switch_canny:
                    mListener.onToggleSwitch(CVWindowFragment.CANNY, isChecked);
                    break;
                case R.id.switch_line_detector:
                    mListener.onToggleSwitch(CVWindowFragment.LINE_DETECTOR, isChecked);
                    break;
                case R.id.switch_circle_detector:
                    mListener.onToggleSwitch(CVWindowFragment.CIRCLE_DETECTOR, isChecked);
                    break;
                case R.id.switch_threshold:
                    mListener.onToggleSwitch(CVWindowFragment.THRESHOLD, isChecked);
                    break;
                case R.id.switch_lut:
                    mListener.onToggleSwitch(CVWindowFragment.LUT, isChecked);
                    break;
                case R.id.switch_nld:
                    mListener.onToggleSwitch(CVWindowFragment.NLD, isChecked);
            }
        }
    };
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch (seekBar.getId()) {
                case R.id.seekbar_threshold:
                    mListener.onThresholdChanged(progress);

            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch(action) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            v.onTouchEvent(event);
            return true;
        }
    };

    public DebugFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_debug, container, false);

        debugSwitch = ((Switch) v.findViewById(R.id.switch_debug));
        debugSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        magnetSwitch = ((Switch) v.findViewById(R.id.switch_magnet));
        magnetSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        cannySwitch = ((Switch) v.findViewById(R.id.switch_canny));
        cannySwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        colorFilterSwitch = ((Switch) v.findViewById(R.id.switch_color_filter));
        colorFilterSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        lineDetectorSwitch = ((Switch) v.findViewById(R.id.switch_line_detector));
        lineDetectorSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        circleDetectorSwitch = ((Switch) v.findViewById(R.id.switch_circle_detector));
        circleDetectorSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        thresholdSwitch = ((Switch) v.findViewById(R.id.switch_threshold));
        thresholdSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        lutSwitch = ((Switch) v.findViewById(R.id.switch_lut));
        lutSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        nldSwitch = ((Switch) v.findViewById(R.id.switch_nld));
        nldSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);

        SeekBar ThresholdSeekBar = (SeekBar) v.findViewById(R.id.seekbar_threshold);
        ThresholdSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        ThresholdSeekBar.setOnTouchListener(mOnTouchListener);

        offRadio = (RadioButton) v.findViewById(R.id.radio_off);
        offRadio.setOnClickListener(mOnClickListener);
        lfForwardRadio = (RadioButton) v.findViewById(R.id.radio_line_follower_fw);
        lfForwardRadio.setOnClickListener(mOnClickListener);
        lfBackwardRadio = (RadioButton) v.findViewById(R.id.radio_line_follower_bw);
        lfBackwardRadio.setOnClickListener(mOnClickListener);
        lfNutAreaRadio = (RadioButton) v.findViewById(R.id.radio_line_follower_na);
        lfNutAreaRadio.setOnClickListener(mOnClickListener);
        nutSearchingRadio = (RadioButton) v.findViewById(R.id.radio_nut_searching);
        nutSearchingRadio.setOnClickListener(mOnClickListener);
        nutTakingRadio = (RadioButton) v.findViewById(R.id.radio_nut_taking);
        nutTakingRadio.setOnClickListener(mOnClickListener);
        rotateRadio = (RadioButton) v.findViewById(R.id.radio_rotate_90);
        rotateRadio.setOnClickListener(mOnClickListener);

        cannySwitch.setEnabled(false);
        colorFilterSwitch.setEnabled(false);
        lineDetectorSwitch.setEnabled(false);
        circleDetectorSwitch.setEnabled(false);
        thresholdSwitch.setEnabled(false);
        lutSwitch.setEnabled(false);
        nldSwitch.setEnabled(false);
        offRadio.setEnabled(false);
        lfForwardRadio.setEnabled(false);
        lfBackwardRadio.setEnabled(false);
        lfNutAreaRadio.setEnabled(false);
        nutSearchingRadio.setEnabled(false);
        nutTakingRadio.setEnabled(false);
        rotateRadio.setEnabled(false);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface OnFragmentInteractionListener{
        void onToggleSwitch(int mode, boolean isChecked);
        void onRadioMode(int mode);
        void onThresholdChanged(int threshold);
    }

}
