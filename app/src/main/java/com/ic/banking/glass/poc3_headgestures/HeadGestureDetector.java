package com.ic.banking.glass.poc3_headgestures;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

import java.util.Iterator;
import java.util.Queue;

public class HeadGestureDetector {

    private static final String TAG = HeadGestureDetector.class.getSimpleName();

    private static final int ANGLES_QUEUE_SIZE = 100;

    private static final Float NOD_LOW_ANGLE = new Float(-0.3);
    private static final Float NOD_HIGH_ANGLE = new Float(0.3);
    private static final Float HEAD_SHAKE_LOW_ANGLE = new Float(-1.0);
    private static final Float HEAD_SHAKE_HIGH_ANGLE = new Float(1.0);

    private SensorManager sensorManager;

    private SensorEventListener sensorEventListener;
    private Context context;

    private HeadGestureListener headGestureListener;
    private Queue<Float> nodAngles = Queues.synchronizedQueue(EvictingQueue.<Float>create(ANGLES_QUEUE_SIZE));
    private Queue<Float> headShakeAngles = Queues.synchronizedQueue(EvictingQueue.<Float>create(ANGLES_QUEUE_SIZE));

    private Thread checkThread;
    private boolean check;

    private float angleA;
    private float angleB;
    private float angleNod;

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
                    try {
                        Thread.sleep(250);
                        nodAngles.add(angleNod);
                    }
                    catch (InterruptedException e) {
                    }

                    Log.i(TAG, "AngleNod: " + angleNod);
                    checkIfNodOrHeadShake();
                }
            }
        });
    }

    public void stopListening() {
        if (this.sensorEventListener != null) {
            this.sensorManager.unregisterListener(this.sensorEventListener);
        }

        this.check = false;
    }

    public void startListening() {
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
                    test(event);
                    //calculateNewAngles(event);
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
        boolean isHeadShake = false;//isHeadShake();

        if (isNod && !isHeadShake) {
            logNodValues();
            emptyNodAnglesQueue();
            this.headGestureListener.onNod();
        }

        if (!isNod && isHeadShake) {
            this.headGestureListener.onHeadShake();
        }
    }

    private boolean isNod() {
        int lowPoints = 0;
        int highPoints = 0;

        for (Float nodAngle : nodAngles) {
            if (nodAngle > NOD_HIGH_ANGLE) {
                highPoints++;
            }
            if (nodAngle < NOD_LOW_ANGLE) {
                lowPoints++;
            }
        }
        return lowPoints >= 2 && highPoints >= 2;
    }

    private boolean isHeadShake() {
        int lowPoints = 0;
        int highPoints = 0;

        for (Float headShakeAngle : headShakeAngles) {
            if (headShakeAngle > HEAD_SHAKE_HIGH_ANGLE) {
                highPoints++;
            }
            if (headShakeAngle < HEAD_SHAKE_LOW_ANGLE) {
                lowPoints++;
            }
        }
        return lowPoints >= 2 && highPoints >= 2;
    }

    private Float calculateHeadShakeAngle(SensorEvent event) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private Float calculateNodAngle(SensorEvent event) {
        float angle = (float) -Math.atan(event.values[2]
                / Math.sqrt(event.values[1] * event.values[1] + event.values[0] * event.values[0]));

        return angle;
    }

    private Float computeOrientationA(SensorEvent event) {
        float angle = (float) -Math.atan(event.values[0]
                / Math.sqrt(event.values[1] * event.values[1] + event.values[2] * event.values[2]));

        return angle;
    }

    private Float computeOrientationB(SensorEvent event) {
        float angle = (float) -Math.atan(event.values[1]
                / Math.sqrt(event.values[2] * event.values[2] + event.values[0] * event.values[0]));

        return angle;
    }

    private void emptyNodAnglesQueue() {
        synchronized (nodAngles) {
            while (!nodAngles.isEmpty()) {
                nodAngles.poll();
            }
        }
    }

    private void test(SensorEvent event) {
        angleA = computeOrientationA(event);
        angleB = computeOrientationB(event);
        angleNod = calculateNodAngle(event);
    }

    private void logNodValues() {
        synchronized (nodAngles) {
            StringBuilder values = new StringBuilder();
            Iterator<Float> i = nodAngles.iterator();
            while (i.hasNext()) {
                if (values.length() > 0) {
                    values.append(';');
                }
                values.append(i.next());
            }
            Log.i(TAG, values.toString());
        }
    }
}