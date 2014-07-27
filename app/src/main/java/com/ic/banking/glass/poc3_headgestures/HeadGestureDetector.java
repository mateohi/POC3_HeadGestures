package com.ic.banking.glass.poc3_headgestures;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.common.collect.EvictingQueue;

import java.util.Queue;

public class HeadGestureDetector {

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;

    private Context context;
    private HeadGestureListener headGestureListener;

    public static final int ANGLES_QUEUE_SIZE = 50;
    private Queue<Float> nodAngles = EvictingQueue.create(ANGLES_QUEUE_SIZE);
    private Queue<Float> headShakeAngles = EvictingQueue.create(ANGLES_QUEUE_SIZE);

    private Thread checkThread;
    private boolean check;

    public HeadGestureDetector(Context context, HeadGestureListener headGestureListener) {
        this.context = context;
        this.headGestureListener = headGestureListener;

        this.sensorEventListener = createSensorEventListener();
        registerSensors();

        createCheckThread();
    }

    private void createCheckThread() {
        this.checkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (check) {
                    checkIfNodOrHeadShake();
                }
            }
        });

        this.check = true;
        this.checkThread.start();
    }

    public void stopListening() {
        if (this.sensorEventListener != null) {
            this.sensorManager.unregisterListener(this.sensorEventListener);
        }

        this.check = false;
    }

    public void continueListening() {
        registerSensors();

        this.check = true;
        this.checkThread.start();
    }

    private SensorEventListener createSensorEventListener() {
        SensorEventListener sensorEventListener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    calculateNewAngles(event);
                }
            }
        };
        return sensorEventListener;
    }

    private void registerSensors() {
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.sensorManager.registerListener(sensorEventListener,
                this.sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void calculateNewAngles(SensorEvent event) {
        Float nodAngle = calculateNodAngle(event);
        Float headShakeAngle = calculateHeadShakeAngle(event);

        this.nodAngles.add(nodAngle);
        this.headShakeAngles.add(headShakeAngle);
    }

    private void checkIfNodOrHeadShake() {
        boolean isNod = isNod();
        boolean isHeadShake = isHeadShake();

        if (isNod && !isHeadShake) {
            this.headGestureListener.onNod();
        }

        if (!isNod && isHeadShake) {
            this.headGestureListener.onHeadShake();
        }
    }

    private boolean isNod() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private boolean isHeadShake() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private Float calculateHeadShakeAngle(SensorEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private Float calculateNodAngle(SensorEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private Float computeOrientation(SensorEvent event) {
        float angle = (float) -Math.atan(event.values[0]
                / Math.sqrt(event.values[1] * event.values[1] + event.values[2] * event.values[2]));

        return angle;
    }
}
