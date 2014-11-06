package com.example.robin.creasypitch;

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
import android.view.Display;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    Thread t;
    int sr = 44100;
    boolean isRunning = true;
    double sliderval = 1;
    float savedX =0;
    float savedY =0;
    int caca = 20;
    int deviceHeight;
    int maxRange = 12;
    boolean dragging = false;
    double powUnDouze = 1.059463094359f;
    float octaveSwitch = 1;
    int deviceWidth;
    double xGyroscope;
    double yGyroscope;
    double zGyroscope;

    // Attribut de la classe pour calculer  l'orientation
    float[] acceleromterVector=new float[3];
    float[] gravityVector=new float[3];
    float[] baseGravityVector=new float[3];

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor gravity;
    //ImageView circleView;

    TextView noteDisplay;

    //RotateAnimation circleAnim;

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
            double margin = getNoteAsPowerOfTwo();
            margin -= Math.floor(margin);

            if( margin < 0.5f )
                noteAdjust = -0.2 * margin;
            else
                noteAdjust = 0.2 * (1-margin);
            findViewById (R.id.activity_main).invalidate();
            updateNoteLabel();
        }
    };
    private  ImageView crazyCircleAnim;
    private int statusBarHeight;
    private boolean crazyAllDay = true;
    private GestureDetector gestureDetector;
    private double noteAdjust = 0;

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            crazyAllDay = !crazyAllDay;
            //baseGravityVector = Arrays.copyOf(gravityVector, 3);
            return false;
        }
    }

    protected void onResume() {
        super.onResume();
        crazyAllDay = true;
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
        dragging = false;
        crazyAllDay = false;
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        setContentView(R.layout.activity_main);

        gestureDetector = new GestureDetector(getApplicationContext(), new GestureListener());

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
                        Thread.sleep(1000/60);
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
                int buffsize = 2;
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
                    if( !dragging && !crazyAllDay){
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            isRunning = false;
                        }
                        continue;
                    }

                    fr =  frBase * Math.pow(powUnDouze, getNoteAsPowerOfTwo());
                    for(int i=0; i < buffsize; i++) {
                        samples[i] = (short) (amp * (Math.sin(ph)));
                        ph += twopi * fr / sr;
                    }
                    audioTrack.write(samples, 0, buffsize);
                }
                audioTrack.stop();
                audioTrack.release();
            }
        };
        t.start();
/*
        // Step1 : create the  RotateAnimation object
        circleAnim = new RotateAnimation(0f, -360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        // Step 2:  Set the Animation properties
        circleAnim.setInterpolator(new LinearInterpolator());
        circleAnim.setRepeatCount(Animation.INFINITE);
        circleAnim.setDuration(2000);

        circleView = ((ImageView)findViewById(R.id.circle));
*/
        crazyCircleView = ((ImageView) findViewById(R.id.crazy_circle));

        //crazyCircleView.setVisibility(View.INVISIBLE);

        statusBarHeight = getStatusBarHeight();
/*
        if( crazyAllDay )
            ((ImageView)findViewById(R.id.circle)).startAnimation(circleAnim);
*/
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
        return sliderval+2*octaveSwitch+getGravityModifier()+getAccModifier()+noteAdjust;
    }

    private float getGravityModifier(){
        return 0.4f*(gravityVector[1]+gravityVector[2]+gravityVector[0]-baseGravityVector[1]-baseGravityVector[2]-baseGravityVector[0]);
    }

    private float getAccModifier(){
        return 0.1f*(acceleromterVector[1]+acceleromterVector[2]+acceleromterVector[0]);
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
        if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
            gravityVector=event.values;

        }else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acceleromterVector=event.values;
        }
        float shift = 0.8f;
        if( !dragging) {
            crazyCircleView.setX(crazyCircleView.getX() + shift * (-(gravityVector[0]-baseGravityVector[0])));
            crazyCircleView.setY(crazyCircleView.getY() + shift * (gravityVector[1]-baseGravityVector[1]));
        }

        if( crazyCircleView.getY()+crazyCircleView.getHeight() > deviceHeight){
            crazyCircleView.setY(deviceHeight - crazyCircleView.getHeight()- statusBarHeight);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean dispatchTouchEvent (MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);

        gestureDetector.onTouchEvent(event);

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                savedX = event.getX();
                savedY = event.getY();

                dragging = true;

                break;
            case MotionEvent.ACTION_MOVE:

                double drag =  Math.sqrt( Math.pow(Math.abs(savedX-event.getX()),2) - Math.pow(Math.abs(savedY-event.getY()),2));

                sliderval = maxRange * (1- event.getY() / deviceHeight);
                octaveSwitch = event.getX() / deviceWidth;

                if( sliderval == Double.NaN ){
                    sliderval = 0.5;
                }else if( sliderval < 0) {
                    sliderval = 0;
                }else if( sliderval > maxRange) {
                    sliderval = maxRange;
                }

                crazyCircleView.setX(event.getX()-crazyCircleView.getWidth()/2);
                crazyCircleView.setY(event.getY() - crazyCircleView.getHeight() - statusBarHeight);

                break;
            case MotionEvent.ACTION_UP:
            dragging = false;
/*
                if( !crazyAllDay )
                    ((ImageView)findViewById(R.id.circle)).setAnimation(null);
                    */
                break;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
        }
        return true;
    }

    private void updateNoteLabel() {
        if( !dragging && !crazyAllDay )
            return;
        double p2 = getNoteAsPowerOfTwo();
        noteDisplay.setText(notesLabel[(char) Math.floor(p2)%notesLabel.length]);
        getWindow().getDecorView().setBackgroundColor(Color.HSVToColor(new float[]{
                360 * (float)( ((0.2*Math.pow(p2,2))%notesLabel.length) / notesLabel.length ),
                1,
                1
        }));
/*
        circleView.setScaleX((float)(p2*0.4));
        circleView.setScaleY((float)(p2*0.4));
        */
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