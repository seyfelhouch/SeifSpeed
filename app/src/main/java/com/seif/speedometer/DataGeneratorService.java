package com.seif.speedometer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class DataGeneratorService extends Service {

    private float speed = 0;
    private float rpm = 0;

    private final IDataGenerator.Stub binder = new IDataGenerator.Stub() {
        @Override
        public float getSpeed() throws RemoteException {
            return speed;
        }

        @Override
        public float getRPM() throws RemoteException {
            return rpm;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // Start the data generation thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    long time = System.currentTimeMillis();
                    speed = (float) Math.abs(50 * Math.sin(time * 0.002) + 50 * Math.sin(time * 0.001));
                    rpm = (float) Math.abs(3000 * Math.sin(time * 0.002) + 2000 * Math.sin(time * 0.001));

                    try {
                        Thread.sleep(16); // To maintain ~60 FPS
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        return binder;
    }
}
