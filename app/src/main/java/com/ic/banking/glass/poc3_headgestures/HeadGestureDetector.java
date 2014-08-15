package com.ic.banking.glass.poc3_headgestures;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

import java.util.Iterator;
import java.util.Queue;

/*
 *      Calculo del Nod:
 * ------------------------------------------------------------------------------------------------
 * Calculando que se tardan entre 1 y 2 segundos realizar un nod, se guardan los 50 valores del
 * angulo correspondiente cada 50 milisegundos. Esto es, se guardan los intervalos cada 50
 * milisegundos de los ultimos 2,5 segundos.
 *
 * A partir de esto, se analiza que se tengan dos picos "high" y "low" entre los valores, lo que
 * representaria dos subidas y bajadas de cabeza.
 *------------------------------------------------------------------------------------------------
 *
 *      Calculo del headshake:
 * ------------------------------------------------------------------------------------------------
 * Explicacion
 * ------------------------------------------------------------------------------------------------
 *
 */

public class HeadGestureDetector {

    private static final String TAG = HeadGestureDetector.class.getSimpleName();

    private static final int ARM_DISPLACEMENT_DEGREES = 6;
    private static final int ANGLES_QUEUE_SIZE = 50;

    private Context context;
    private HeadGestureListener headGestureListener;

    private IntentFilter winkFilter;
    private BroadcastReceiver winkReceiver;

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;

    private Queue<Float> nodAngles = Queues.synchronizedQueue(EvictingQueue.<Float>create(ANGLES_QUEUE_SIZE));
    private Queue<Float> headShakeAngles = Queues.synchronizedQueue(EvictingQueue.<Float>create(ANGLES_QUEUE_SIZE));

    private Thread checkThread;
    private boolean check;

    private float nodAngle;
    private float headShakeAngle;

    public HeadGestureDetector(Context context, HeadGestureListener headGestureListener) {
        this.context = context;
        this.headGestureListener = headGestureListener;

        this.sensorEventListener = createSensorEventListener();
        registerSensors();

        createCheckThread();
        createWinkFilterAndReceiver();
    }

    private void createWinkFilterAndReceiver() {
        this.winkFilter = new IntentFilter("com.google.android.glass.action.EYE_GESTURE");
        this.winkFilter.setPriority(1000);
        this.winkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("WINK".equals(intent.getStringExtra("gesture"))) {
                    abortBroadcast();
                    Log.i(TAG, "Wink received");
                    headGestureListener.onWink();
                }
            }
        };
    }

    private void registerWinkReceiver() {
        this.context.registerReceiver(this.winkReceiver, this.winkFilter);
    }

    private void unregisterWinkReceiver() {
        this.context.unregisterReceiver(this.winkReceiver);
    }

    private void createCheckThread() {
        this.checkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (check) {
                    try {
                        Thread.sleep(50);
                        nodAngles.add(nodAngle);
                        headShakeAngles.add(headShakeAngle);
                        checkIfNodOrHeadShake();
                    }
                    catch (InterruptedException e) {
                        // Too bad
                    }

                    //Log.i("      nod: ", Float.toString(nodAngle)); // BORRAR
                    //Log.i("headshake: ", Float.toString(headShakeAngle)); // BORRAR
                }
            }
        });
    }

    public void stopListening() {
        unregisterWinkReceiver();
        if (this.sensorEventListener != null) {
            this.sensorManager.unregisterListener(this.sensorEventListener);
        }

        this.check = false;
        emptyNodAnglesQueue();
        emptyHeadShakeAnglesQueue();
    }

    public void startListening() {
        registerWinkReceiver();
        registerSensors();

        this.check = true;
        if (!this.checkThread.isAlive()) {
            this.checkThread.start();
        }
    }

    private SensorEventListener createSensorEventListener() {
        SensorEventListener sensorEventListener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    calculateNewAngles(event);
                }
            }
        };
        return sensorEventListener;
    }

    private void registerSensors() {
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.sensorManager.registerListener(this.sensorEventListener,
                this.sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        this.sensorManager.registerListener(this.sensorEventListener,
                this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_UI);
    }

    private void calculateNewAngles(SensorEvent event) {
        float[] rotationMatrix = new float[16];
        float[] orientation = new float[9];

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X,
                SensorManager.AXIS_Z, rotationMatrix);
        SensorManager.getOrientation(rotationMatrix, orientation);

        float magneticHeading = (float) Math.toDegrees(orientation[0]);
        this.headShakeAngle = mod(magneticHeading, 360.0f) - ARM_DISPLACEMENT_DEGREES;

        this.nodAngle = (float) Math.toDegrees(orientation[1]);
    }

    private void checkIfNodOrHeadShake() {
        Float[] nodAnglesArray = this.nodAngles.toArray(new Float[this.nodAngles.size()]);
        Float[] headShakeAnglesArray = this.headShakeAngles.toArray(new Float[this.headShakeAngles.size()]);

        boolean isNod = HeadGestureUtils.isNod(nodAnglesArray);
        boolean isHeadShake = HeadGestureUtils.isHeadShake(headShakeAnglesArray);

        if (isNod && !isHeadShake) {
            logNodValues(); // BORRAR
            emptyNodAnglesQueue();
            this.headGestureListener.onNod();
        }

        if (!isNod && isHeadShake) {
            logHeadShakeValues(); // BORRAR
            emptyHeadShakeAnglesQueue();
            this.headGestureListener.onHeadShake();
        }
    }

    private void emptyNodAnglesQueue() {
        synchronized (this.nodAngles) {
            while (!this.nodAngles.isEmpty()) {
                this.nodAngles.poll();
            }
        }
    }

    private void emptyHeadShakeAnglesQueue() {
        synchronized (this.headShakeAngles) {
            while (!this.headShakeAngles.isEmpty()) {
                this.headShakeAngles.poll();
            }
        }
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

    private void logHeadShakeValues() {
        synchronized (headShakeAngles) {
            StringBuilder values = new StringBuilder();
            Iterator<Float> i = headShakeAngles.iterator();
            while (i.hasNext()) {
                if (values.length() > 0) {
                    values.append(';');
                }
                values.append(i.next());
            }
            Log.i(TAG, values.toString());
        }
    }

    // Headshake

    public static float mod(float a, float b) {
        return (a % b + b) % b;
    }
}
