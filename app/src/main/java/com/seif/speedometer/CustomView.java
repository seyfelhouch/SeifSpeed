package com.seif.speedometer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CustomView extends View {
    private boolean isSpeedometer = true;
    private Paint paint;
    private float speed = 0;
    private float rpm = 0;
    private Paint textPaint;
    private IDataGenerator dataGeneratorService;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dataGeneratorService = IDataGenerator.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataGeneratorService = null;
        }
    };
    private float initialX, initialY;

    public CustomView(Context context) {
        super(context);
        init();
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        // Initialize the text paint object
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);
        Intent intent = new Intent(getContext(), DataGeneratorService.class);
        getContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // Generate speed and RPM as a sum of sines
                    long time = System.currentTimeMillis();
                    speed = (float) Math.abs(50 * Math.sin(time * 0.002) + 50 * Math.sin(time * 0.001));
                    rpm = (float) Math.abs(3000 * Math.sin(time * 0.002) + 2000 * Math.sin(time * 0.001));

                    postInvalidate();

                    try {
                        Thread.sleep(16); // To maintain ~60 FPS
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (dataGeneratorService != null) {
                speed = dataGeneratorService.getSpeed();
                rpm = dataGeneratorService.getRPM();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (isSpeedometer) {
            drawSpeedometer(canvas);
        } else {
            drawTachometer(canvas);
        }
        if (isSpeedometer) {
            canvas.drawText("Speed: " + Math.round(speed) + " km/h", getWidth() / 2 - 100, getHeight() / 2 - 50, textPaint);
        } else {
            canvas.drawText("RPM: " + Math.round(rpm), getWidth() / 2 - 100, getHeight() / 2 - 50, textPaint);
        }
    }

    private void drawSpeedometer(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        // Draw the rim
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, 300, paint);

        // Draw needle
        float needleLength = 280;
        float angle = speed * 240 / 100 - 120;
        float x = (float) (getWidth() / 2 + needleLength * Math.cos(Math.toRadians(angle)));
        float y = (float) (getHeight() / 2 + needleLength * Math.sin(Math.toRadians(angle)));
        canvas.drawLine(getWidth() / 2, getHeight() / 2, x, y, paint);
    }

    private void drawTachometer(Canvas canvas) {
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        // Draw the rim
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, 300, paint);

        // Draw needle
        float needleLength = 280;
        float angle = rpm * 240 / 4000 - 120;
        float x = (float) (getWidth() / 2 + needleLength * Math.cos(Math.toRadians(angle)));
        float y = (float) (getHeight() / 2 + needleLength * Math.sin(Math.toRadians(angle)));
        canvas.drawLine(getWidth() / 2, getHeight() / 2, x, y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        if (pointerCount == 2) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    initialX = (event.getX(0) + event.getX(1)) / 2;
                    initialY = (event.getY(0) + event.getY(1)) / 2;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float currentX = (event.getX(0) + event.getX(1)) / 2;
                    float currentY = (event.getY(0) + event.getY(1)) / 2;
                    float dx = currentX - initialX;
                    float dy = currentY - initialY;

                    if (Math.abs(dx) > 100 || Math.abs(dy) > 100) {
                        isSpeedometer = !isSpeedometer;
                        postInvalidate();
                        initialX = currentX;
                        initialY = currentY;
                    }
                    break;
            }
        }
        return true;
    }
}
