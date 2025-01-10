package extremeviolence;

import java.util.Random;

public class QRand {
    // I'll make this faster later. Just setting up the black-box interface for now
    public static Random rng = new Random();

    public static int randInt(int n) {
        return rng.nextInt(n);
    }

    public static double randDouble() {
        return rng.nextDouble();
    }

    public static int choice(double[] weights) {
        double sum = 0;
        for (double x : weights) {
            sum += x;
        }
        final double threshold = rng.nextDouble(sum);
        sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            if (sum >= threshold) {
                return i;
            }
        }
        return -1;
    }
}
