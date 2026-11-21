package com.neurocalm.service;

import com.neurocalm.enums.SignalQuality;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Simple rule-based signal quality check (no machine learning).
 * Each problem adds a "penalty": 0 = GOOD, 1 = MODERATE, 2+ = POOR.
 */
@Service
public class SignalQualityService {

    private static final int MIN_SAMPLES = 10;
    private static final double MAX_INVALID_RATIO = 0.3;
    private static final double SUDDEN_JUMP_RATIO = 0.6;   // biggest step compared to total range
    private static final double MAX_AMPLITUDE_CHANGE = 3.0; // between chunks of the block

    private final PpgProcessingService ppgProcessingService;

    public SignalQualityService(PpgProcessingService ppgProcessingService) {
        this.ppgProcessingService = ppgProcessingService;
    }

    /**
     * @param rawSamples  samples exactly as received (may contain nulls)
     * @param motorActive true if a motor was running during this block.
     *                    Motor activity alone does NOT lower quality; it only adds a penalty
     *                    when the signal already shows another problem.
     */
    public SignalQuality assess(List<Double> rawSamples, boolean motorActive) {
        if (rawSamples == null || rawSamples.size() < MIN_SAMPLES) {
            return SignalQuality.POOR;
        }
        List<Double> valid = ppgProcessingService.cleanSamples(rawSamples);
        double invalidRatio = 1.0 - valid.size() / (double) rawSamples.size();
        if (valid.size() < MIN_SAMPLES || invalidRatio > MAX_INVALID_RATIO) {
            return SignalQuality.POOR;
        }
        double range = rangeOf(valid);
        if (range < 1e-9) {
            return SignalQuality.POOR; // flat line: sensor not reading a pulse
        }

        int penalties = 0;
        if (invalidRatio > 0) {
            penalties++;                                   // some missing samples
        }
        if (largestJump(valid) / range > SUDDEN_JUMP_RATIO) {
            penalties++;                                   // sudden fluctuation
        }
        if (isAmplitudeInconsistent(valid)) {
            penalties++;                                   // signal strength changes a lot
        }
        if (motorActive && penalties > 0) {
            penalties++;                                   // motor running AND signal looks bad
        }
        return penalties == 0 ? SignalQuality.GOOD
                : penalties == 1 ? SignalQuality.MODERATE : SignalQuality.POOR;
    }

    private double rangeOf(List<Double> values) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return max - min;
    }

    private double largestJump(List<Double> values) {
        double biggest = 0;
        for (int i = 1; i < values.size(); i++) {
            biggest = Math.max(biggest, Math.abs(values.get(i) - values.get(i - 1)));
        }
        return biggest;
    }

    /** Splits the block into 4 chunks and compares their ranges. */
    private boolean isAmplitudeInconsistent(List<Double> values) {
        if (values.size() < 20) {
            return false;
        }
        int chunkSize = values.size() / 4;
        double smallest = Double.MAX_VALUE;
        double largest = 0;
        for (int c = 0; c < 4; c++) {
            double chunkRange = rangeOf(values.subList(c * chunkSize, (c + 1) * chunkSize));
            smallest = Math.min(smallest, chunkRange);
            largest = Math.max(largest, chunkRange);
        }
        return smallest < 1e-9 || largest / smallest > MAX_AMPLITUDE_CHANGE;
    }
}
