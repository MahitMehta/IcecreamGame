package com.example.dodgegame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameActivity extends AppCompatActivity {

    GameSurface gameSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);
        setContentView(gameSurface);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameSurface.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameSurface.resume();
    }

    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener {
        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap background, cone;
        int ballX;
        int x = 200;
        Paint paintProperty;
        int screenWidth, screenHeight;

        private SensorManager mSensorManager;
        private Sensor mAccelerometer;

        private float ax, ay, az;

        private double Roll, Pitch;

        private final double RAD_TO_DEG = 180d / Math.PI;


        public GameSurface(Context ctx) {
            super(ctx);

            holder = getHolder();

            cone = BitmapFactory.decodeResource(getResources(), R.drawable.cone_transparent);
            background = BitmapFactory.decodeResource(getResources(), R.drawable.background);

            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);
            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;

            paintProperty = new Paint();
        }


        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
                ax=event.values[0];
                ay=event.values[1];
                az=event.values[2];

                Roll = Math.atan2(ay, az) * RAD_TO_DEG;
                Pitch = Math.atan2(-ax, Math.sqrt(ay*ay + az*az)) * RAD_TO_DEG;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void run() {
            Canvas canvas = null;
            Drawable d = getResources().getDrawable(R.drawable.background, null);

            double ratio = background.getHeight() / (double) background.getWidth();
            int flip = 1;
            while(running){
                if(!holder.getSurface().isValid()) continue;
                canvas = holder.lockCanvas(null);
                d.setBounds(0, screenWidth * (int) ratio, screenWidth, getBottom());
                d.draw(canvas);

                float maxXOffset = cone.getWidth() / 2;
                Log.d("Pitch",  Pitch + "");
                float xOffset = maxXOffset * ((float) Pitch / 90f);

                canvas.drawBitmap(cone, xOffset, screenHeight/2, null);
               // if(ballX == screenWidth/2-cone.getWidth()/2 || ballX == -1*screenWidth/2+cone.getWidth()/2) flip*=-1;
               // ballX += flip;
                holder.unlockCanvasAndPost(canvas);
            }
        }

        public void resume() {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause() {
            mSensorManager.unregisterListener(this);
            running = false;
            while (true) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {

                }
            }
        }
    }

}