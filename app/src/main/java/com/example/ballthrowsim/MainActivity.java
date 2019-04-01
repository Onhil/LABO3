package com.example.ballthrowsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private TextView score;
    private TextView highScore;
    private boolean eventSlots = true;
    private List<Double> readings;

    private boolean reachedTop = false;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        score = findViewById(R.id.score);
        highScore = findViewById(R.id.high_score);
        readings = new ArrayList<>();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            getAccelerometer(event);
        }

    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        double x = values[0];
        double y = values[1];
        double z = values[2];

        double ACC = Math.sqrt(x * x + y * y + z * z);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int sens = prefs.getInt("sens_key", 10);

        if (ACC > sens && eventSlots) //
        {
            readings.add(ACC);
            if (readings.size() >= 20) {
                eventSlots = false;
                Throw(Collections.max(readings));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
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

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void Throw(double ACC) {
        double timeToHighest = ACC / SensorManager.GRAVITY_EARTH;
        Log.d("Main", "Acceleration: " + Double.toString(Math.floor(ACC)));

        new ThrowSim(timeToHighest, ACC).execute();



        reachedTop = false;

    }

    private class ThrowSim extends AsyncTask<Void, Void, Void> {
        private  long currentTime;
        private final double timeToTop;
        private double peak = 0;
        private double height;
        private double timeDiff;
        private final double ACC;

        private MediaPlayer ping;
        private MediaPlayer crash;
        private DecimalFormat numberFormat;



        private ThrowSim(double timeToTop, double ACC) {
            this.timeToTop = timeToTop;
            this.ACC = ACC;
        }


        @Override
        protected Void doInBackground(Void... params) {

            ping = MediaPlayer.create(MainActivity.this, R.raw.ping);
            crash = MediaPlayer.create(MainActivity.this, R.raw.glassbreak);

            final long timeStart = System.currentTimeMillis();
            currentTime = System.currentTimeMillis();

            timeDiff = (currentTime - timeStart) / 1000.f;
            numberFormat = new DecimalFormat("#.00");

            while(timeDiff <= timeToTop*2){

                currentTime = System.currentTimeMillis();
                timeDiff = (currentTime - timeStart) / 1000.f;
                height = ACC * timeDiff - SensorManager.GRAVITY_EARTH / 2 * Math.pow(timeDiff, 2);

                SystemClock.sleep(1);
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){

                        Log.d("throwSim", "Time to top: " + Double.toString(timeToTop) + " timeDiff: " + Double.toString(timeDiff));

                        if (height  > peak) {
                            peak = height;
                            highScore.setText("Peak Height: " + numberFormat.format(peak) + "m");
                        }


                        if(timeDiff >= timeToTop && !reachedTop){
                            ping.start();
                            reachedTop = true;
                        }

                        if(height >= 0) {
                            score.setText("Current height: " + numberFormat.format(height) + "m");
                        }

                    }

                });
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    crash.start();
                    score.setText("Current height: " + "0" + "m");
                }
            });



            return null;

        }

        @Override
        protected void onPostExecute(Void result) {
            eventSlots = true;
            readings.clear();
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

}
