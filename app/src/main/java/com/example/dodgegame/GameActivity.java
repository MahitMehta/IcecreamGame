package com.example.dodgegame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    GameSurface gameSurface;
    List<Bitmap> scoops = new ArrayList<>();
    int scoopD = 150;
    MediaPlayer backgroundMusicMP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);
        setContentView(gameSurface);

        scoops.add(getResizedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cottoncandyscoop), scoopD*7/6, scoopD));
        scoops.add(getResizedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.greenscoop), scoopD*7/6, scoopD));
        scoops.add(getResizedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.purplescoop), scoopD*7/6, scoopD));
        scoops.add(getResizedBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.yellowithbrownspotsscoop), scoopD*7/6, scoopD));
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

    @Override
    protected void onStop() {
        super.onStop();
        backgroundMusicMP.reset();
        backgroundMusicMP.release();
        backgroundMusicMP = null;
    }

    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener {
        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap background, cone;
        Paint paintProperty;
        int screenWidth, screenHeight;
        float coneTop;

        private SensorManager mSensorManager;
        private Sensor mAccelerometer;

        private float ax, ay, az;

        private double Pitch;

        private final double RAD_TO_DEG = 180d / Math.PI;

        private long lastSpawnTime;
        private long spawnCooldown = 1500;
        private final long timeLimit = 60000;
        private long countDownStart;
        private long animStartMS;
        private long animLength = 500;
        private float animEnd, animStart;



        public GameSurface(Context ctx) {
            super(ctx);

            holder = getHolder();



            Bitmap t = BitmapFactory.decodeResource(getResources(), R.drawable.cone_transparent);
            double aRatio = (double)t.getHeight()/t.getWidth();
            cone = getResizedBitmap(t, scoopD,  (int) (scoopD * aRatio));


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

                Pitch = Math.atan2(-ax, Math.sqrt(ay*ay + az*az)) * RAD_TO_DEG;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
        private List<Scoop> scoopsFalling = new ArrayList<>();
        private List<Scoop> scoopsOnCone = new ArrayList<>();

        @Override
        public void run() {
            Canvas canvas;


            float coneXOffset = screenWidth/2f - cone.getWidth();
            float offsetSpeed;

            Drawable d = getResources().getDrawable(R.drawable.background, null);

            double ratio = (double)screenWidth/d.getIntrinsicWidth();

            lastSpawnTime = System.currentTimeMillis();
            coneTop = screenHeight * 7/8f;
            animEnd = coneTop;
            countDownStart = System.currentTimeMillis();
            int finalScore = -1;

            backgroundMusicMP = MediaPlayer.create(GameActivity.this, R.raw.backgroundmusic);
            backgroundMusicMP.start();



            while(running){
                if(!holder.getSurface().isValid()) continue;
                canvas = holder.lockCanvas(null);

                //game over

                if(timeLimit + countDownStart - System.currentTimeMillis() < 0){
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    canvas.drawRect(new RectF(0, 0, getRight(), getBottom()), paint);
                    if(finalScore < 0)
                        finalScore = scoopsOnCone.size();
                    Paint paint1 = new Paint();
                    paint1.setTextSize(100);
                    paint1.setColor(Color.RED);
                    paint1.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("Score: " + scoopsOnCone.size(), screenWidth/2f, screenHeight/2f, paint1);
                    canvas.drawText("Game Over", screenWidth/2f, screenHeight/2f - 150, paint1);
                    holder.unlockCanvasAndPost(canvas);
                    continue;
                }


                int shopTop = getBottom() - (int)(ratio * d.getIntrinsicHeight()) + (scoopsOnCone.size() * scoopD/4);
                d.setBounds(getLeft(), shopTop, screenWidth, shopTop + (int)(ratio * d.getIntrinsicHeight()));
                d.draw(canvas);




                double resultRed = 255 + (scoopsOnCone.size() / 40f) * (15 - 255);
                double resultGreen = 255 + (scoopsOnCone.size() / 40f) * (52 - 255);
                double resultBlue = 255 + (scoopsOnCone.size() / 40f) * (115 - 255);
                Shader shader;
                if(shopTop < screenHeight)
                    shader = new LinearGradient(0, 0, 0, shopTop, Color.argb(255, (int) resultRed, (int) resultGreen, (int) resultBlue), Color.WHITE, Shader.TileMode.CLAMP);
                else shader = new LinearGradient(0, 0, 0, shopTop, Color.argb(255, (int) resultRed, (int) resultGreen, (int) resultBlue), Color.WHITE, Shader.TileMode.CLAMP);
                Paint paint = new Paint();
                paint.setShader(shader);
                canvas.drawRect(new RectF(0, 0, getRight(), shopTop), paint);

                paint = new Paint();
                paint.setTextSize(50);
                paint.setColor(Color.BLACK);
                canvas.drawText("Score: "+scoopsOnCone.size(), 0, 100, paint);
                if(System.currentTimeMillis() - countDownStart > timeLimit + 10) paint.setColor(Color.RED);
                canvas.drawText((timeLimit + (countDownStart - System.currentTimeMillis())) / 1000f+"s", 0, 50, paint);






                //smoothing adding the scoops
                if(System.currentTimeMillis() - animStartMS >= animLength){
                    animStartMS = -1;
                    coneTop = animEnd;
                }

                offsetSpeed = (float) (Pitch/9f);
                if(coneXOffset > screenWidth - cone.getWidth() && offsetSpeed > 0) offsetSpeed = 0;
                if(coneXOffset < 0 && offsetSpeed < 0) offsetSpeed = 0;
                coneXOffset += offsetSpeed;

                if(animStartMS < 0) canvas.drawBitmap(cone, coneXOffset, coneTop, null);
                else{
                    coneTop = animStart + (animEnd - animStart) * (System.currentTimeMillis() - animStartMS)/animLength;
                    canvas.drawBitmap(cone, coneXOffset, coneTop, null);
                }

                //spawner
                if(System.currentTimeMillis() - lastSpawnTime > spawnCooldown){
                    scoopsFalling.add(new Scoop(scoops.get((int)(Math.random() * scoops.size())), (int)(Math.random() * (screenWidth - scoopD)), -scoopD));
                    lastSpawnTime = System.currentTimeMillis();
                }

                //hitbox detection thingy

                for(int i = 0; i < scoopsFalling.size(); i++){
                    Scoop s = scoopsFalling.get(i);
                    s.bottom +=5;
                    int scoopRight = s.left + s.image.getWidth();
                    int scoopLeft = s.left;
                    int coneRight = (int)coneXOffset + cone.getWidth();
                    int coneLeft = (int)coneXOffset;
                    int scoopBottom = s.bottom;

                    float boundHeight = coneTop - (scoopsOnCone.size() + 1) * scoopD / 2f;
                    if(scoopRight > coneLeft && scoopLeft < coneRight
                            && boundHeight - 5 < scoopBottom && boundHeight > scoopBottom){
                        addScoop(i);
                        i--;
                    }

                    canvas.drawBitmap(s.image, s.left, s.bottom, null);
                }

                for(int i = 0; i < scoopsOnCone.size(); i++){
                    Scoop s = scoopsOnCone.get(i);
                    canvas.drawBitmap(s.image, coneXOffset - (scoopD * 7/6f - cone.getWidth())/2f, coneTop - (i+1) * scoopD/2f, null);
                }

                holder.unlockCanvasAndPost(canvas);

            }
        }
        public void addScoop(int s){
            Scoop scoop = scoopsFalling.get(s);
            if(scoopsOnCone.size()>1){
                animStartMS = System.currentTimeMillis();
                animStart = coneTop;
                animEnd +=scoopD/2f;
            }
            MediaPlayer soundEffectsMP = MediaPlayer.create(GameActivity.this, R.raw.plop);
            soundEffectsMP.setLooping(false);
            soundEffectsMP.start();

            scoopsFalling.remove(s);
            scoopsOnCone.add(scoop);
            for(int i = 0; i < scoopsOnCone.size(); i++){
                scoopsOnCone.get(i).bottom+=scoopD/2f;
            }
            Log.d("scoop", "scoopAdded");
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
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }






}