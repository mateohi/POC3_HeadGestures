package com.ic.banking.glass.poc3_headgestures;

import java.util.ArrayList;
import java.util.List;

public class HeadGestureUtils {

    private static final Float NOD_LOW_ANGLE = new Float(-0.075);
    private static final Float NOD_HIGH_ANGLE = new Float(0.175);

    private static final Float HEAD_SHAKE_LOW_ANGLE = new Float(-1.0);
    private static final Float HEAD_SHAKE_HIGH_ANGLE = new Float(1.0);

    public static boolean isNod(Float[] values) {
        return check(values, NOD_HIGH_ANGLE, NOD_LOW_ANGLE, 2, 2);
    }

    public static boolean isHeadShake(Float[] values) {
        return check(values, HEAD_SHAKE_HIGH_ANGLE, HEAD_SHAKE_LOW_ANGLE, 2, 2);
    }

    private static boolean check(Float[] values, Float high, Float low, int minHighs, int minLows) {
        List<Float> simplifiedValues = simplifiedValues(values);

        int lows = 0;
        int highs = 0;

        for (Float value : simplifiedValues) {
            if (value >= high) {
                highs++;
            }
            if (value <= low) {
                lows++;
            }
        }

        return lows >= minLows && highs >= minHighs;
    }

    private static List<Float> simplifiedValues(Float[] values) {
        List<Float> simplifiedValues = new ArrayList<Float>();
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) {
                // Si estoy en uno negativo, agarro el menor y avanzo hasta el proximo positivo
                float min = values[i];
                for (int j = i; j < values.length; j++, i++) {
                    if (values[j] > 0) {
                        i--;
                        break;
                    }
                    if (values[j] < min) {
                        min = values[j];
                    }
                }
                simplifiedValues.add(min);
            }
            else {
                // Si estoy en uno positivo, agarro el mayor y avanzo hasta el proximo negativo
                float max = values[i];
                for (int j = i; j < values.length; j++, i++) {
                    if (values[j] < 0) {
                        i--;
                        break;
                    }
                    if (values[j] > max) {
                        max = values[j];
                    }
                }
                simplifiedValues.add(max);
            }
        }
        return simplifiedValues;
    }
}
