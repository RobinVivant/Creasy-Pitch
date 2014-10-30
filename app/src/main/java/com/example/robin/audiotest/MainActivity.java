package com.example.robin.audiotest;

import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    Thread t;
    int sr = 44100;
    boolean isRunning = true;
    double sliderval = 1;
    private static final String DEBUG_TAG = "Velocity";
    private VelocityTracker mVelocityTracker = null;
    float savedX =0;
    float savedY =0;
    int caca = 20;
    int deviceHeight;
    int maxRange = 12;
    boolean playAudio = false;
    double powUnDouze = 1.059463094359f;
    float octaveSwitch = 1;
    int deviceWidth;
    double xGyroscope;
    double yGyroscope;
    double zGyroscope;

    // Attribut de la classe pour calculer  l'orientation
    float[] acceleromterVector=new float[3];
    float[] magneticVector=new float[3];
    float[] resultMatrix=new float[9];
    float[] values=new float[3];

    private SensorManager mSensorManager;
    private Sensor magnetic;
    private Sensor accelerometer;
    ImageView circleView;

    TextView noteDisplay;

    RotateAnimation circleAnim;

    String[] notesLabel = {
            "A",
            "A#",
            "B",
            "C",
            "C#",
            "D",
            "D#",
            "E",
            "F",
            "F#",
            "G",
            "G#",
    };

    ImageView crazyCircleView;

    private final Handler updateNoteLabelHandle = new Handler();

    final Runnable updateNoteLabelRunnable = new Runnable() {
        public void run() {
            //call the activity method that updates the UI
            findViewById (R.id.activity_main).invalidate();
            updateNoteLabel();
        }
    };
    private  ImageView crazyCircleAnim;
    private int statusBarHeight;
    private boolean crazyAllDay = true;

    protected void onResume() {
        super.onResume();
        crazyAllDay = true;
        mSensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        playAudio = false;
        crazyAllDay = false;
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        // Instantiate the magnetic sensor and its max range
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
// Instantiate the accelerometer
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        setContentView(R.layout.activity_main);

        noteDisplay = (TextView)findViewById(R.id.noteText);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        deviceHeight = size.y;//Math.max(size.x, size.y);
        deviceWidth = size.x;

        t = new Thread(){
            public void run(){

                setPriority(Thread.MIN_PRIORITY);

                while(isRunning){
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        isRunning = false;
                        return;
                    }
                    updateNoteLabelHandle.post(updateNoteLabelRunnable);
                }
            }
        };
        t.start();
        // start a new thread to synthesise audio
        t = new Thread() {
            public void run() {
                // set process priority
                setPriority(Thread.MAX_PRIORITY);
                // set the buffer size
                /*
                int buffsize = AudioTrack.getMinBufferSize(sr,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        */
                int buffsize = 8;
                // create an audiotrack object
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sr, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, buffsize,
                        AudioTrack.MODE_STREAM);

                short samples[] = new short[buffsize];
                int amp = 10000;
                double twopi = 8.*Math.atan(1.);
                double fr;
                double frBase = 440.f;
                double ph = 0.0;

                // start audio
                audioTrack.play();

                // synthesis loop
                while(isRunning){
                    if( !playAudio && !crazyAllDay){
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            isRunning = false;
                        }
                        continue;
                    }

                    fr =  frBase * Math.pow(powUnDouze, getNoteAsPowerOfTwo());
                    for(int i=0; i < buffsize; i++){
                        samples[i] = (short) (amp*Math.sin(ph));
                        ph += twopi*fr/sr;
                    }
                    audioTrack.write(samples, 0, buffsize);
                }
                audioTrack.stop();
                audioTrack.release();
            }
        };
        t.start();

        // Step1 : create the  RotateAnimation object
        circleAnim = new RotateAnimation(0f, -360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        // Step 2:  Set the Animation properties
        circleAnim.setInterpolator(new LinearInterpolator());
        circleAnim.setRepeatCount(Animation.INFINITE);
        circleAnim.setDuration(2000);

        circleView = ((ImageView)findViewById(R.id.circle));

        crazyCircleView = ((ImageView) findViewById(R.id.crazy_circle));

        //crazyCircleView.setVisibility(View.INVISIBLE);

        statusBarHeight = getStatusBarHeight();

        if( crazyAllDay )
            ((ImageView)findViewById(R.id.circle)).startAnimation(circleAnim);

    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private double getNoteAsPowerOfTwo() {
        return sliderval+2*octaveSwitch+2*(values[1]+values[2]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.crazy_switch:
                crazyAllDay = !crazyAllDay;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acceleromterVector=event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticVector=event.values;
        }
// Demander au sensorManager la matric de Rotation (resultMatric)
        SensorManager.getRotationMatrix(resultMatrix, null, acceleromterVector, magneticVector);
// Demander au SensorManager le vecteur d'orientation associé (values)
        SensorManager.getOrientation(resultMatrix, values);
        float shift = 10;
        crazyCircleView.setX(crazyCircleView.getX()+shift*(values[2]));
        crazyCircleView.setY(crazyCircleView.getY() + shift * (-values[1]));

        if( crazyCircleView.getY()+crazyCircleView.getHeight() > deviceHeight){
            crazyCircleView.setY(deviceHeight - crazyCircleView.getHeight()- statusBarHeight);
        }

//Log.d("lol", "" + );
        /*
        getWindow().getDecorView().setBackgroundColor(Color.rgb(
                baseColor +(255-baseColor)*(int)Math.toDegrees(values[0])/360, // azimuth
                baseColor +(255-baseColor)*(int)Math.toDegrees(values[1])/360, // pitch
                baseColor +(255-baseColor)*(int)Math.toDegrees(values[2])/360)); // roll
                */
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean dispatchTouchEvent (MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                savedX = event.getX();
                savedY = event.getY();

                playAudio = true;

                ((ImageView)findViewById(R.id.circle)).startAnimation(circleAnim);

                if(mVelocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    // Reset the velocity tracker back to its initial state.
                    mVelocityTracker.clear();
                }
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(event);

                //crazyCircleView.setVisibility(View.VISIBLE);

                break;
            case MotionEvent.ACTION_MOVE:

                double drag =  Math.sqrt( Math.pow(Math.abs(savedX-event.getX()),2) - Math.pow(Math.abs(savedY-event.getY()),2));
                //sliderval =  drag / deviceHeight;
                //Log.d("MainActivity", "drag: " +drag+" slider: "+sliderval);

                mVelocityTracker.addMovement(event);
                // When you want to determine the velocity, call
                // computeCurrentVelocity(). Then call getXVelocity()
                // and getYVelocity() to retrieve the velocity for each pointer ID.
                mVelocityTracker.computeCurrentVelocity(1000, caca);
                //float x = Math.min( mVelocityTracker.getXVelocity(pointerId), caca);
                //double y = mVelocityTracker.getYVelocity(pointerId);
                // Log velocity of pixels per second
                // Best practice to use VelocityTrackerCompat where possible.

                //sliderval += y / caca;
/*
                sliderval = Math.ceil(maxRange * event.getY() / deviceHeight);
                octaveSwitch = (int)Math.ceil(2 * event.getX() / deviceWidth);
*/

                sliderval = maxRange * event.getY() / deviceHeight;
                octaveSwitch = event.getX() / deviceWidth;

                if( sliderval == Double.NaN ){
                    sliderval = 0.5;
                }else if( sliderval < 0) {
                    sliderval = 0;
                }else if( sliderval > maxRange) {
                    sliderval = maxRange;
                }

                crazyCircleView.setX(event.getX()-crazyCircleView.getWidth()/2);
                crazyCircleView.setY(event.getY()-crazyCircleView.getHeight()-statusBarHeight);

                break;
            case MotionEvent.ACTION_UP:
                playAudio = false;

                if( !crazyAllDay )
                    ((ImageView)findViewById(R.id.circle)).setAnimation(null);

                //crazyCircleView.setVisibility(View.INVISIBLE);
                break;
            case MotionEvent.ACTION_CANCEL:
                playAudio = false;
                // Return a VelocityTracker object back to be re-used by others.
                // mVelocityTracker.recycle();
                break;
        }
        return true;
    }

    private void updateNoteLabel() {
        if( ! playAudio && !crazyAllDay )
            return;
        double p2 = getNoteAsPowerOfTwo();
        noteDisplay.setText(notesLabel[(char) Math.floor(p2)%notesLabel.length]);
        /*
        getWindow().getDecorView().setBackgroundColor(Color.HSVToColor(new float[]{
                360 * (float)((sliderval+(maxRange/2)*(octaveSwitch-0.5)) / (maxRange+maxRange/2)),
                1,
                1
        }));
        */
        getWindow().getDecorView().setBackgroundColor(Color.HSVToColor(new float[]{
                360 * (float)( Math.min(p2,notesLabel.length) / notesLabel.length ),
                1,
                1
        }));


        circleView.setScaleX((float)(p2*0.4));

        circleView.setScaleY((float)(p2*0.4));
    }


    public void onDestroy(){
        super.onDestroy();
        isRunning = false;
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t = null;
    }
}