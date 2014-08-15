package com.ic.banking.glass.poc3_headgestures;

import java.util.ArrayList;
import java.util.List;

public class HeadGestureUtils {

    private static final int ANGLE_DIFFERENCE_AMOUNT = 3;
    private static final Float ANGLE_DEGREES = new Float(15);

    public static boolean isNod(Float[] values) {
        return check(values, ANGLE_DEGREES, ANGLE_DIFFERENCE_AMOUNT);
    }

    public static boolean isHeadShake(Float[] values) {
        return check(values, ANGLE_DEGREES, ANGLE_DIFFERENCE_AMOUNT);
    }

    private static boolean check(Float[] values, Float angle, int amount) {
        List<Float> simplifiedValues = simplifiedValues(values);
        List<Float> differences = differenciateValues(simplifiedValues);

        int actual = 0;

        for (Float difference : differences) {
            if (difference >= angle) {
                actual++;
            }
        }

        return actual >= amount;
    }

    private static List<Float> differenciateValues(List<Float> values) {
        List<Float> differences = new ArrayList<Float>();
        Float[] angles = values.toArray(new Float[values.size()]);

        for (int i = 0; i < angles.length - 1; i++) {
            differences.add(Math.abs(angles[i] - angles[i + 1]));
        }

        return differences;
    }

    private static List<Float> simplifiedValues(Float[] values) {
        List<Float> simplifiedValues = new ArrayList<Float>();
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i] <= values[i + 1]) {
                // Si estoy en una subida, agarro el mas alto de la subida
                float max = values[i];
                for (int j = i; j < values.length - 1; j++, i++) {
                    if (values[j] > values[j + 1]) {
                        // Llegue a la cima
                        i--;
                        break;
                    }
                    if (values[j] < max) {
                        max = values[j];
                    }
                }
                simplifiedValues.add(max);
            }
            else {
                // Si estoy en una bajada, agarro el menor de la bajada
                float min = values[i];
                for (int j = i; j < values.length - 1; j++, i++) {
                    if (values[j] < values[j + 1]) {
                        // Llegue al valle
                        i--;
                        break;
                    }
                    if (values[j] > min) {
                        min = values[j];
                    }
                }
                simplifiedValues.add(min);
            }
        }
        simplifiedValues.add(values[values.length - 1]);
        return simplifiedValues;
    }
}
