package zeronzerot.auorob;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;

public class CVWindowFragment extends Fragment implements CvCameraViewListener2 {
    private static final String TAG = "RunActivityFragment: ";

    //Image modes
    public static final int COLOR_FILTER = 0;
    public static final int CANNY = 1;
    public static final int LINE_DETECTOR = 2;
    public static final int CIRCLE_DETECTOR = 3;
    public static final int THRESHOLD = 5;
    public static final int LUT = 90;
    public static final int NLD = 998;

    //Line following modes
    public static final int LF_FORWARD = 120;
    public static final int LF_BACKWARD = 321;
    public static final int LF_NA = 234;

    //UI components
    private CameraBridgeViewBase cvCameraPreview;

    //Listener
    private ImageProcessingListener mListener;

    //Mode identifier
    //Single function mode
    private boolean mCannyEnabled = false;
    private boolean mColorFilterEnabled = false;
    private boolean mCircleDetectorEnabled = false;
    private boolean mLineDetectorEnabled = false;
    private boolean mThresholdEnabled = false;
    private boolean mLUTEnabled = false;
    private boolean mNLDEnabled = false;
    private boolean mLineFollowingEnabled = false;
    private boolean mNutSearchingEnabled = false;

    //Processing parameters
    private int mThreshold = 0;
    //houghCircle
    private double mDp = 2.0;
    private double mMinDist = 10.0;
    private double mParam1 = 100; //default = 100
    private double mParam2 = 80; //accumulator threshold, default = 100
    private int mMinRadius = 0; //default = 0
    private int mMaxRadius = 50; //default = 0
    //houghLine
    private double mDistanceRes = 1.0;
    private double mAngleRes = 2*Math.PI/180;
    private int mHLThreshold = 30;
    private double mMinLineLength = 60;
    private double mMaxLineGap = 60;
    //Magnet points
    private Point mCenter = new Point();
    //Settings fields
    private Point mVert = new Point();
//    private boolean mForward = true;
    private int mDriveHeight = 70;
    private int mType = LF_FORWARD;
    private boolean mTop = true;
    private int xBack = 200;
    private int xFront = 120;
    private int yTop = 20;
    private int yBottom = 220;

//    some Convention and drawing stuffs
    DecimalFormat mDf = new DecimalFormat("#0.00");
    Scalar mColorRed = new Scalar(255, 0, 0);
    Scalar mColorGreen = new Scalar(0, 255, 0);
    Scalar mColorBlue = new Scalar(0, 0, 255);
    int fontType = Core.FONT_HERSHEY_PLAIN;


    //calculated internal values
    private Scalar mLowerBound = new Scalar(0,0,0);
    private Scalar mUpperBound = new Scalar(0,0,0);

    //Mat objects
    //Globally defined because of slow Mat objects initialization
    //Shared between functions (ass. all sequential)
    private Mat mFrameLMat;
    private Mat mFrameMat;
    private Mat mProcessMat4C;
    private Mat mProcessMat4C2;
    private Mat mProcessMat1C;
    private Mat mProcessMat1C2;
    private Mat mProcessMat1C3;
    private Mat mProcessMat1C4;
    private Mat mProcessMat3C;
    private Mat mVecMat;
    //Filter and stuffs
    private Mat mLUT;
    private Mat mBackLineMask;
    private Mat mFrontLineMask;
    private Mat mTopLineMask;
    private Mat mBottomLineMask;

    //Required empty constructor
    public CVWindowFragment() {
    }

    //Fragment default methods
    //"on"-ers
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cv, container, false);
        cvCameraPreview = (CameraBridgeViewBase) v.findViewById(R.id.camera_preview);
        cvCameraPreview.setVisibility(SurfaceView.VISIBLE);
        cvCameraPreview.setMaxFrameSize(640, 480);
        cvCameraPreview.setCvCameraViewListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        readSharedPreference();
        cvCameraPreview.enableView();
    }

    private void readSharedPreference() {
        SharedPreferences sP = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final double x = Double.parseDouble(sP.getString(getString(R.string.key_magnet_position_x), "160"));
        final double y = Double.parseDouble(sP.getString(getString(R.string.key_magnet_position_y), "120"));
        mCenter.x = x;
        mCenter.y = y;
        //Hough Circle
        final double dp = Double.parseDouble(sP.getString(getString(R.string.key_hough_circle_dp), "2.0"));
        final double minDist = Double.parseDouble(sP.getString(getString(R.string.key_hough_circle_mindist), "10.0"));
        final double param1 = Double.parseDouble(sP.getString(getString(R.string.key_hough_circle_param1), "100"));
        final double param2 = Double.parseDouble(sP.getString(getString(R.string.key_hough_circle_param2), "80"));
        final int minRadius = Integer.parseInt(sP.getString(getString(R.string.key_hough_circle_minradius), "0"));
        final int maxRadius = Integer.parseInt(sP.getString(getString(R.string.key_hough_circle_maxradius), "50"));
        mDp = dp;
        mMinDist = minDist;
        mParam1 = param1;
        mParam2 = param2;
        mMinRadius = minRadius;
        mMaxRadius = maxRadius;
        //Hough Line
        final double distanceRes = Double.parseDouble(sP.getString(getString(R.string.key_hough_line_rho), "1.0"));
        final double angleRes = Double.parseDouble(sP.getString(getString(R.string.key_hough_line_theta), "2.0"));
        final int HLThreshold = Integer.parseInt(sP.getString(getString(R.string.key_hough_line_threshold), "30"));
        final double minLineLength = Double.parseDouble(sP.getString(getString(R.string.key_hough_line_minlinelength), "60"));
        final double maxLineGap = Double.parseDouble(sP.getString(getString(R.string.key_hough_line_maxlinegap), "60"));
        mDistanceRes = distanceRes;
        mAngleRes = Math.toRadians(angleRes);
        mHLThreshold = HLThreshold;
        mMinLineLength = minLineLength;
        mMaxLineGap = maxLineGap;
        //Color Filter
        final double lowerBoundH = Double.parseDouble(sP.getString(getString(R.string.key_color_filter_h_low), "0"));
        final double lowerBoundS = Double.parseDouble(sP.getString(getString(R.string.key_color_filter_s_low), "0"));
        final double lowerBoundV = Double.parseDouble(sP.getString(getString(R.string.key_color_filter_v_low), "0"));
        final double upperBoundH = Double.parseDouble(sP.getString(getString(R.string.key_color_filter_h_high), "0"));
        final double upperBoundS = Double.parseDouble(sP.getString(getString(R.string.key_color_filter_s_high), "0"));
        final double upperBoundV = Double.parseDouble(sP.getString(getString(R.string.key_color_filter_v_high), "0"));
        mLowerBound.set(new double[] {lowerBoundH, lowerBoundS, lowerBoundV});
        mUpperBound.set(new double[] {upperBoundH, upperBoundS, upperBoundV});
        xBack = Integer.parseInt(sP.getString(getString(R.string.key_back_line_position), "200"));
        xFront = Integer.parseInt(sP.getString(getString(R.string.key_front_line_position), "120"));
        yTop = Integer.parseInt(sP.getString(getString(R.string.key_top_line_position), "20"));
        yBottom = Integer.parseInt(sP.getString(getString(R.string.key_bottom_line_position), "220"));
        final int driveHeight = Integer.parseInt(sP.getString(getString(R.string.key_drive_height), "70"));
        mDriveHeight = driveHeight;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (cvCameraPreview != null)
            cvCameraPreview.disableView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cvCameraPreview != null)
            cvCameraPreview.disableView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ImageProcessingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement ImageProcessingListener");
        }
    }

    // Methods from CvCameraViewListener2
    public void onCameraViewStarted(int widthORI, int heightORI) {
        //Initiate all Mats
        final int width = widthORI / 2;
        final int height = heightORI / 2;
        mFrameLMat = new Mat(heightORI, widthORI, CvType.CV_8UC4);
        mFrameMat = new Mat(height, width, CvType.CV_8UC4);
        mProcessMat1C = new Mat(height, width, CvType.CV_8UC1);
        mProcessMat1C2 = new Mat(height, width, CvType.CV_8UC1);
        mProcessMat1C3 = new Mat(height, width, CvType.CV_8UC1);
        mProcessMat1C4 = new Mat(height, width, CvType.CV_8UC1);
        mProcessMat3C = new Mat(height, width, CvType.CV_8UC3);
        mProcessMat4C = new Mat(height, width, CvType.CV_8UC4);
        mProcessMat4C2 = new Mat(height, width, CvType.CV_8UC4);
        mVecMat = new Mat(height, width, CvType.CV_8UC4);

        //FFilter and stuffs
        mLUT = new Mat(1, 256, CvType.CV_8UC1);
        for (int i=0; i<128; i++) {
            mLUT.put(0, i, 0);
        }
        for (int i=128; i<256; i++) {
            mLUT.put(0, i, 255);
        }
        mBackLineMask = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.line(mBackLineMask, new Point(xBack, 0), new Point(xBack, height), new Scalar(255));
        mFrontLineMask = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.line(mFrontLineMask, new Point(xFront, 0), new Point(xFront, height), new Scalar(255));
        mTopLineMask = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.line(mTopLineMask, new Point(0, yTop), new Point(width, yTop), new Scalar(255));
        mBottomLineMask = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.line(mBottomLineMask, new Point(0, yBottom), new Point(width, yBottom), new Scalar(255));
    }

    public void onCameraViewStopped() {
        mFrameMat.release();
        mFrameLMat.release();
        mProcessMat1C.release();
        mProcessMat1C2.release();
        mProcessMat1C3.release();
        mProcessMat1C4.release();
        mProcessMat3C.release();
        mProcessMat4C.release();
        mProcessMat4C2.release();
        mVecMat.release();

        mLUT.release();
        mBackLineMask.release();
        mFrontLineMask.release();
        mTopLineMask.release();
        mBottomLineMask.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //"LOCK"
        final boolean cannyEnabled = mCannyEnabled;
        final boolean colorFilterEnabled = mColorFilterEnabled;
        final boolean circleDetectorEnabled = mCircleDetectorEnabled;
        final boolean lineDetectorEnabled = mLineDetectorEnabled;
        final boolean lineFollowingEnabled = mLineFollowingEnabled;
        final boolean nutSearchingEnabled = mNutSearchingEnabled;
        final boolean thresholdEnabled = mThresholdEnabled;
        final boolean LUTEnabled = mLUTEnabled;
        final boolean NLDEnabled = mNLDEnabled;

        //get camera output
        //only subset of it will be processed
//        mFrameMat = inputFrame.rgba();
        Imgproc.resize(inputFrame.rgba(), mFrameMat, new Size(mFrameMat.width(), mFrameMat.height()));

        //State-based methods
        if (lineFollowingEnabled) {
            lineFollowing(mFrameMat);
        }
        if (nutSearchingEnabled) {
            nutSearching(mFrameMat);
        }

        //Debug level 2 (image processing) toggles
        //Simpler CV Processing
        if (LUTEnabled) {
            simplify(mFrameMat);
        }
        if (colorFilterEnabled) {
            colorFilter(mFrameMat, mLowerBound, mUpperBound);
//            yellowFilter(mFrameMat, mLowerBound, mUpperBound);
//            singleLineDetectorX(mFrameMat, mFrameMat, 200);
        }
        if (NLDEnabled) {
//            singleLineDetectorX(mFrameMat, mFrameMat, 200);
            Imgproc.cvtColor(mFrameMat, mProcessMat1C, Imgproc.COLOR_RGB2GRAY);
            threshold(mProcessMat1C, 128);
            mProcessMat1C2.setTo(new Scalar(0));
            mProcessMat1C.copyTo(mProcessMat1C2, mBackLineMask);
            lineDetector(mProcessMat1C2, mFrameMat, ANGLIAN);
        }
        if (thresholdEnabled) {
            threshold(mFrameMat, mThreshold);
        }
        if (lineDetectorEnabled) {
            org.opencv.core.Size s = new Size(3,3);
            Imgproc.GaussianBlur(mFrameMat, mFrameMat, s, 2);
            Imgproc.Canny(mFrameMat, mProcessMat1C, 80, 100);
//            lineDetector(mFrameMat, mFrameMat);
        }
        if (cannyEnabled) {
            org.opencv.core.Size s = new Size(3,3);
            Imgproc.GaussianBlur(mFrameMat, mFrameMat, s, 2);
            Imgproc.Canny(mFrameMat, mProcessMat1C, 80, 100);
            Imgproc.cvtColor(mProcessMat1C, mFrameMat, Imgproc.COLOR_GRAY2RGB, 3);
        }
        drawTarget(mFrameMat, mCenter);
//        return mFrameMat;
        Imgproc.resize(mFrameMat, mFrameLMat, new Size(mFrameLMat.width(), mFrameLMat.height()), 0, 0, Imgproc.INTER_NEAREST);
        return mFrameLMat;
    }

    //Image processing methods
    //Contain chained OpenCV processes
    private void colorFilter(Mat mat, final Scalar lowerBound, final Scalar upperBound) {
        Imgproc.cvtColor(mat, mProcessMat3C, Imgproc.COLOR_RGB2HSV, 0);
        //color "threshold" > Output: 1-channel B/W
        Core.inRange(mProcessMat3C, lowerBound, upperBound, mProcessMat1C);
        Imgproc.cvtColor(mProcessMat1C, mat, Imgproc.COLOR_GRAY2RGBA, 0);
    }

    private void colorFilter(Mat mat, Mat output, final Scalar lowerBound, final Scalar upperBound) {
        Imgproc.cvtColor(mat, mProcessMat3C, Imgproc.COLOR_RGB2HSV, 0);
        //color "threshold" > Output: 1-channel B/W
        Core.inRange(mProcessMat3C, lowerBound, upperBound, output);
    }

    private void simplify(Mat mat) {
        Core.LUT(mat, mLUT, mat);
    }

    private void threshold(Mat mat, int threshold) {
        Imgproc.threshold(mat, mat, threshold, 255, Imgproc.THRESH_BINARY);
    }

    //Draw target cross
    private void drawTarget(Mat output, Point center) {
        final Point startH = new Point(0, center.y);
        final Point endH = new Point(output.cols(), center.y);
        final Point startV = new Point(center.x, 0);
        final Point endV = new Point(center.x, output.rows());
        final Scalar lineColor = new Scalar(0,255,0);
        final int lineThickness = 1;
        //Draw horizontal line
        Imgproc.line(output, startH, endH, lineColor, lineThickness);
        //Draw vertical line
        Imgproc.line(output, startV, endV, lineColor, lineThickness);
    }

    //Draw circle on circles
    private double[] circleDetector(Mat input, Mat drawMat) {
        final double weightX = 3.0;
        final double weightY = 1.0;

        final Scalar circleColor = new Scalar(0, 110, 30);
        final Scalar blmCircleColor = new Scalar(0, 255, 0);

        int bottomLeftMost = 0;
        double bottomLeftMostScore = 0;

        double[] ex = new double[] {0, 0, 0, 0};

        //Hough circle settings
        final double dp = mDp;
        final double minDist = mMinDist;
        final double param1 = mParam1; //default = 100
        final double param2 = mParam2; //accumulator threshold, default = 100
        final int minRadius = mMinRadius; //default = 0
        final int maxRadius = mMaxRadius; //default = 0

        final Scalar textColor = new Scalar(0,220,255);

        final int fontType = Core.FONT_HERSHEY_PLAIN;

        Imgproc.HoughCircles(input, mVecMat, Imgproc.CV_HOUGH_GRADIENT,
                dp, minDist, param1, param2, minRadius, maxRadius);

        //Draw all circles
        if (mVecMat.cols() > 0) {
            ex[0] =  mVecMat.cols();
            //Search bottom-left most circle
            for (int x = 0; x < mVecMat.cols(); x++) {
                double vCircle[] = mVecMat.get(0, x);

                if (vCircle == null)
                    break;

                double score = weightX * vCircle[0] + weightY * vCircle[1];
                if (score > bottomLeftMostScore) {
                    bottomLeftMostScore = score;
                    bottomLeftMost = x;
                }
            }

            for (int x = 0; x < mVecMat.cols(); x++) {
                double vCircle[] = mVecMat.get(0, x);

                if (vCircle == null)
                    break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int) Math.round(vCircle[2]);
                // draw found circles
                // outer circle
                if (x == bottomLeftMost) {
                    ex[1] = vCircle[0] - mCenter.x;
                    ex[2] = vCircle[1] - mCenter.y;
                    ex[3] = vCircle[2];
                    Imgproc.circle(drawMat, pt, radius, blmCircleColor, 2);
                } else {
                    Imgproc.circle(drawMat, pt, radius, circleColor, 2);
                }
                //inner circle
                Imgproc.circle(drawMat, pt, 3, new Scalar(0, 0, 255), 2);
                //print circle coordinates
                Imgproc.putText(drawMat, Integer.toString(mVecMat.cols()) + " " + Integer.toString((int) vCircle[0]) + " " + Integer.toString((int) vCircle[1]),
                        pt, fontType, 0.5, textColor);
            }
        }
        return ex;
    }


    private static final int ANGLIAN = 202;
    private static final int DISTANDAL = 303;
    //Detect and Draw line on lines
    private double[] lineDetector(Mat input, Mat drawMat, int mode) {
        return lineDetector(input, drawMat, mode, true, mAngleRes, mMinLineLength, mMaxLineGap, mHLThreshold, mColorRed);
    }
    private double[] lineDetector(Mat input, Mat drawMat, int mode, boolean draw) {
        return lineDetector(input, drawMat, mode, draw, mAngleRes, mMinLineLength, mMaxLineGap, mHLThreshold, mColorRed);
    }
    private double[] lineDetector(Mat input, Mat drawMat, int mode, Scalar lineColor) {
        return lineDetector(input, drawMat, mode, true, mAngleRes, mMinLineLength, mMaxLineGap, mHLThreshold, lineColor);
    }
    private double[] lineDetector(Mat input, Mat drawMat, int mode,
                                  double angleRes, double minLineLength, double maxLineGap, int threshold, Scalar lineColor) {
        return lineDetector(input, drawMat, mode, true, Math.toRadians(angleRes), minLineLength, maxLineGap, threshold, lineColor);
    }
    private double[] lineDetector(Mat input, Mat drawMat, int mode, boolean draw,
                                  final double angleRes, final double minLineLength, final double maxLineGap,
                                  final int threshold,
                                  Scalar lineColor) {
        //Hough Transform Parameters
        final double distanceRes = mDistanceRes;
        //Line draw Parameters
        final Scalar textColor = new Scalar(0,220,255);
        final int lineThickness = 1;
        final double textSize = 0.6;

        final int fontType = Core.FONT_HERSHEY_PLAIN;

        //Process
        if (input.channels() == 1) {
            Imgproc.HoughLinesP(input, mVecMat, distanceRes, angleRes, threshold, minLineLength, maxLineGap);
        } else {
            return new double[] {0};
        }

        //Calc
        switch(mode) {
            case ANGLIAN:
                //Calculation fields
                int nA = 0;
                double sumA = 0;
                double avgA = 0;

                if (mVecMat.rows() > 0) {

                    for (int x = 0; x < mVecMat.rows(); x++) {
                        //Extract points
                        double[] vec = mVecMat.get(x, 0);
                        double x1 = vec[0],
                                y1 = vec[1],
                                x2 = vec[2],
                                y2 = vec[3];
                        Point start = new Point(x1, y1);
                        Point end = new Point(x2, y2);
                        Point mid = new Point(((x1 + x2) / 2) - 3.0, ((y1 + y2) / 2) - 3.0);

                        //Calculate line angle
                        final double deltaX = x1 - x2;
                        final double deltaY = y1 - y2;
                        double angle = (Math.atan(deltaY / deltaX));

                        //Take if angle < 45deg
                        if (Math.abs(angle) < (Math.PI / 4)) {
                            sumA += angle;
                            nA += 1;
                        }

                        if (draw) {
                            //Draw line angles
                            Imgproc.putText(drawMat, "A:" + mDf.format(Math.toDegrees(angle)),
                                    mid, fontType, textSize, textColor);
                            //Draw line
                            Imgproc.line(drawMat, start, end, lineColor, lineThickness);
                        }
                    }
                    avgA = sumA / nA;
                }
                return new double[] {avgA};
            case DISTANDAL:
                double[] ex = new double[] {0, 0, 0, 0};
//                Log.e(TAG, mDf.format(mVecMat.rows()));
                if (mVecMat.rows() ==  1) {
                    //Extract points
                    double[] vec = mVecMat.get(0, 0);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];
                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);
                    ex[0] = mVecMat.rows();
                    ex[1] = Math.sqrt((y1 - y2) * (y1 - y2) + (x1 - x2) * (x1 - x2));
                    if (y1 == y2) { //if horizontal line
                        ex[1] = (x2 - x1);
                    } else {
                        ex[1] = (y1 - y2);
                    }
                    ex[2] = (x1 + x2) / 2 ;
                    ex[3] = (y1 + y2) / 2;
                    //Draw line
                    Imgproc.line(drawMat, start, end, lineColor, lineThickness);
                }
                return ex;
            default:
                //TODO:proper handling
                return new double[] {0};
        }
    }

    private class MovingAverage {
        private double[] r_moving = {0, 0, 0, 0, 0};
        private int cursor = 0;

        public MovingAverage() {

        }

        void add(double value) {
            r_moving[cursor] = value;
            cursor = (cursor + 1) % 5;
        }

        double get() {
            double sum = 0;
            for (int i = 0; i < 5; i++) {
                sum += r_moving[i];
            }
            return sum / 5;
        }
    }

    //Compound process methods


    boolean hasDoneBump = false;
    boolean eol = false;

    private void lineFollowing(Mat frame) {
        //Drawing
        final Point nOfLinesText = new Point(0.0,235.0);
        final Point nOfLinesText2 = new Point(0.0,215.0);
        final Scalar textColor = new Scalar(0,220,255);
        final double textSize = 0.6;
        final int fontType = Core.FONT_HERSHEY_PLAIN;
//        final boolean forward = mForward;
        final boolean top = mTop;
        final int type = mType;

        //copy image of current frame
        frame.copyTo(mProcessMat4C);

        //Preprocess
        colorFilter(mProcessMat4C, mProcessMat1C, mLowerBound, mUpperBound);

        //Line angle calculator
        Imgproc.Canny(mProcessMat1C, mProcessMat1C2, 80, 100);
        final double A = lineDetector(mProcessMat1C2, frame, ANGLIAN)[0];
        //Calculate R and M
        //front line
        mProcessMat1C2.setTo(new Scalar(0));
        mProcessMat1C.copyTo(mProcessMat1C2, mBackLineMask);
        final double[] backLine = lineDetector(mProcessMat1C2, frame, DISTANDAL, 90, 20, 60, 20, mColorGreen);
        //back line
        mProcessMat1C2.setTo(new Scalar(0));
        mProcessMat1C.copyTo(mProcessMat1C2, mFrontLineMask);
        final double[] frontLine = lineDetector(mProcessMat1C2, frame, DISTANDAL, 90, 20, 60, 20, mColorGreen);

        final double sumOfLines = frontLine[0] + backLine[0];
//
        final double d = (backLine[1] + frontLine[1]) / sumOfLines;
        final double X = (backLine[2] + frontLine[2]) / sumOfLines;
        final double Y = (backLine[3] + frontLine[3]) / sumOfLines;

        final double M = Y - Math.abs(X - mCenter.x) * Math.tan(A) - mCenter.y;
        final double R = d * Math.cos(A);

        Imgproc.putText(frame, Integer.toString(mVecMat.rows()) + " R:" + mDf.format(R) + " M: " + mDf.format(M) + " A:" + mDf.format(Math.toDegrees(A)),
                nOfLinesText, fontType, textSize, textColor);

        //Vertical
        if (type == LF_NA) {
            mListener.onCVDataAvailable(VERTICAL, 40);
        } else {
            if (!Double.isNaN(R)) {
                mListener.onCVDataAvailable(VERTICAL, R - mDriveHeight);
            } else {
                mListener.onCVDataAvailable(VERTICAL, 0);
            }
        }
        //Lateral
        if (!Double.isNaN(M)) {
            mListener.onCVDataAvailable(LATERAL, M);
        } else {
            mListener.onCVDataAvailable(LATERAL, 0);
        }
        //Angle (except nut area, of which this funtion is replaced by gyroscope
        if (type != LF_NA) {
            if (!Double.isNaN(A)) {
                mListener.onCVDataAvailable(YAW, Math.toDegrees(A));
            } else {
                mListener.onCVDataAvailable(YAW, 0);
            }
        }

        //Line triggers
        if (type == LF_FORWARD) {
            //Detect Na line
            mProcessMat1C2.setTo(new Scalar(0));
            if (top) {
                mProcessMat1C.copyTo(mProcessMat1C2, mTopLineMask);
            } else {
                mProcessMat1C.copyTo(mProcessMat1C2, mBottomLineMask);
            }
            final double[] nutAreaLine = lineDetector(mProcessMat1C2, frame, DISTANDAL, 90, 20, 60, 20, mColorGreen);
            if (nutAreaLine[0] > 0 && frontLine[0] == 0) {
                //Arrived in nut area
                Imgproc.putText(frame, "NA", nOfLinesText2, fontType, textSize, textColor);
                mListener.onCVDataAvailable(NA, 0);
            }
        } else if (type == LF_NA) {
            //all on line?
            if (backLine[0] + frontLine[0] == 2) {
                Imgproc.putText(frame, "BOL", nOfLinesText2, fontType, textSize, textColor);
                mListener.onCVDataAvailable(BOL, 0);
            }
        } else if (type == LF_BACKWARD){
            //arrived in storage area?
            if (backLine[0] == 0 && hasDoneBump) {
                //to eol
                eol = true;
            }
            if (frontLine[0] == 0 && !hasDoneBump) {
                hasDoneBump = true;
            }

            if (eol) {
                Imgproc.putText(frame, "EOL", nOfLinesText2, fontType, textSize, textColor);
                mListener.onCVDataAvailable(EOL, 0);
            } else if (hasDoneBump) {
                Imgproc.putText(frame, "HDB", nOfLinesText2, fontType, textSize, textColor);
            }
        }
    }

    private void nutSearching(Mat frame) {
        final double textSize = 0.6;
        final Point nOfLinesText = new Point(0.0,215.0);

        final Scalar textColor = new Scalar(0,220,255);

        final int fontType = Core.FONT_HERSHEY_PLAIN;
        final boolean top = mTop;

        //Copy frame image
        frame.copyTo(mProcessMat4C);

        //Preproccess
        org.opencv.core.Size s = new Size(3,3);
        Imgproc.GaussianBlur(mProcessMat4C, mProcessMat4C2, s, 2);
        Imgproc.Canny(mProcessMat4C2, mProcessMat1C, 80, 100);

        //search circle
        final double[] targetCircle = circleDetector(mProcessMat1C, frame);

        final double nCircle = targetCircle[0];
        final double dx = targetCircle[1];
        final double dy = targetCircle[2];
        final double radius = targetCircle[3];

        if (Math.sqrt(dx*dx + dy*dy) < radius) {
            mListener.onCVDataAvailable(HIT, 0);
        }

        if (nCircle > 0) {
            mListener.onCVDataAvailable(N_CIRCLE, nCircle);
            mListener.onCVDataAvailable(LONG, dx);
            mListener.onCVDataAvailable(LATERAL, dy);
        } else {
            mListener.onCVDataAvailable(N_CIRCLE, 0);
            mListener.onCVDataAvailable(LONG, 0);
            mListener.onCVDataAvailable(LATERAL, 0);
        }

        Imgproc.putText(frame, "dx=" + mDf.format(dx) + " dy=" + mDf.format(dy),
                nOfLinesText, fontType, textSize, textColor);

//        Line presence detector
//        Calculate R and M
//        for line #1
        colorFilter(mProcessMat4C, mProcessMat1C, mLowerBound, mUpperBound);

        mProcessMat1C2.setTo(new Scalar(0));
        if (top) {
            mProcessMat1C.copyTo(mProcessMat1C2, mTopLineMask);
        } else {
            mProcessMat1C.copyTo(mProcessMat1C2, mBottomLineMask);
        }
        final double[] sideLine = lineDetector(mProcessMat1C2, frame, DISTANDAL, 90, 20, 60, 20, mColorGreen);

        mProcessMat1C2.setTo(new Scalar(0));
        mProcessMat1C.copyTo(mProcessMat1C2, mBackLineMask);
        final double[] backLine = lineDetector(mProcessMat1C2, frame, DISTANDAL, 90, 20, 60, 20, mColorGreen);

        final double sumOfLines = sideLine[0] + backLine[0];
//
        final double d = (backLine[1] + sideLine[1]) / sumOfLines;

        Imgproc.putText(frame, "dx=" + mDf.format(dx) + " dy=" + mDf.format(dy) + " d=" + mDf.format(d),
                nOfLinesText, fontType, textSize, textColor);

        if (!Double.isNaN(d)) {
            mListener.onCVDataAvailable(VERTICAL, d - mDriveHeight);
        } else {
            mListener.onCVDataAvailable(VERTICAL, 0);
        }
    }


    //Parameter control methods
    //Mode enabling toggles
    //"set"-ers
    public void setProcess(int what, final boolean isEnabled) {
        switch (what) {
            case CANNY:
                mCannyEnabled = isEnabled;
                break;
            case COLOR_FILTER:
                mColorFilterEnabled = isEnabled;
                break;
            case THRESHOLD:
                mThresholdEnabled = isEnabled;
                break;
            case CIRCLE_DETECTOR:
                mCircleDetectorEnabled = isEnabled;
                break;
            case LINE_DETECTOR:
                mLineDetectorEnabled = isEnabled;
                break;
            case LUT:
                mLUTEnabled = isEnabled;
                break;
            case NLD:
                mNLDEnabled = isEnabled;
                break;

        }
    }

    public void setLineFollowing(boolean enabled) {
        setLineFollowing(enabled, mTop, mType);
    }
    public void setLineFollowing(boolean enabled, boolean top, int type) {
        mLineFollowingEnabled = enabled;
        mType = type;
        mTop = top;
        hasDoneBump = false;
        eol = false;
    }

    public void setNutSearching(boolean enabled, boolean top) {
        mNutSearchingEnabled = enabled;
        mTop = top;
    }

    //Threshold parameter
    public void setThreshold(int threshold) {
        mThreshold = threshold;
    }

    public static final int EOL = 919;
    public static final int VERTICAL = 343;
    public static final int LATERAL = 90;
    public static final int YAW = 123;
    public static final int NA = 333;
    public static final int BOL = 717;

    public static final int N_CIRCLE = 500;
    public static final int HIT = 643;
    public static final int LONG = 210;

    public interface ImageProcessingListener {
        void onCVDataAvailable(int type, double data);
    }

}
