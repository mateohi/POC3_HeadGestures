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

/**
 * Calculando que se tardan entre 1 y 2 segundos realizar un nod, se guardan los 50 valores del
 * angulo correspondiente cada 50 milisegundos. Esto es, se guardan los intervalos cada 50
 * milisegundos de los ultimos 2,5 segundos.
 *
 * A partir de esto, se analiza que se tengan dos picos "high" y "low" entre los valores, lo que
 * representaria dos subidas y bajadas de cabeza.
 *
 *  */

 public class HeadGestureDetector {

    private static final String TAG = HeadGestureDetector.class.getSimpleName();

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

    private float angleA;
    private float angleB;
    private float angleNod;

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
                        nodAngles.add(angleNod);
                        checkIfNodOrHeadShake();
                    }
                    catch (InterruptedException e) { }

                    Log.i(TAG, "AngleNod: " + angleNod); // BORRAR
                    // Log.i(TAG, "AngleA: " + angleA); // BORRAR
                    // Log.i(TAG, "AngleB: " + angleB); // BORRAR
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
        this.checkThread.start();
    }

    private SensorEventListener createSensorEventListener() {
        SensorEventListener sensorEventListener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    test(event); // BORRAR
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
    }

    private void calculateNewAngles(SensorEvent event) {
        this.angleNod = calculateNodAngle(event);
        //this.headShakeAngle = calculateHeadShakeAngle(event);
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

    private void test(SensorEvent event) {
        angleA = computeOrientationA(event);
        angleB = computeOrientationB(event);
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
}
