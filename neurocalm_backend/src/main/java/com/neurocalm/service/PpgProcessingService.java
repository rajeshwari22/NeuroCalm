package com.neurocalm.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Basic PPG processing. Simple peak counting - NOT clinically validated.
 * Raw samples are never modified here; every method returns new lists.
 */
@Service
public class PpgProcessingService {

    public static final double DEFAULT_SAMPLE_RATE_HZ = 25.0;
    private static final int MIN_BPM = 40;
    private static final int MAX_BPM = 200;

    /** Removes null, NaN and infinite samples. */
    public List<Double> cleanSamples(List<Double> samples) {
        List<Double> clean = new ArrayList<>();
        if (samples == null) {
            return clean;
        }
        for (Double sample : samples) {
            if (sample != null && !sample.isNaN() && !sample.isInfinite()) {
                clean.add(sample);
            }
        }
        return clean;
    }

    /** Sample rate in Hz from the first and last timestamp; falls back to a default. */
    public double estimateSampleRate(List<Instant> timestamps) {
        if (timestamps == null || timestamps.size() < 2) {
            return DEFAULT_SAMPLE_RATE_HZ;
        }
        long millis = Duration.between(timestamps.get(0), timestamps.get(timestamps.size() - 1)).toMillis();
        if (millis <= 0) {
            return DEFAULT_SAMPLE_RATE_HZ;
        }
        double rate = (timestamps.size() - 1) / (millis / 1000.0);
        return (rate < 1 || rate > 1000) ? DEFAULT_SAMPLE_RATE_HZ : rate;
    }

    /**
     * Estimates heart rate (bpm) by finding pulse peaks.
     * Returns null when there is not enough signal (needs about 3 seconds).
     */
    public Integer estimateHeartRate(List<Double> samples, double sampleRateHz) {
        if (samples == null || samples.size() < sampleRateHz * 3) {
            return null;
        }
        List<Double> signal = smooth(derivePulse(samples));
        double deviation = standardDeviation(signal);
        if (deviation < 1e-9) {
            return null; // flat signal
        }
        List<Integer> peaks = findPeaks(signal, 0.3 * deviation, (int) (sampleRateHz * 0.3));
        if (peaks.size() < 2) {
            return null;
        }
        double averageInterval = (peaks.get(peaks.size() - 1) - peaks.get(0)) / (double) (peaks.size() - 1);
        int bpm = (int) Math.round(60.0 * sampleRateHz / averageInterval);
        return (bpm >= MIN_BPM && bpm <= MAX_BPM) ? bpm : null;
    }

    /** DERIVED pulse waveform: the samples with their average removed. */
    public List<Double> derivePulse(List<Double> samples) {
        double average = mean(samples);
        List<Double> result = new ArrayList<>();
        for (Double sample : samples) {
            result.add(sample - average);
        }
        return result;
    }

    public double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    public double standardDeviation(List<Double> values) {
        if (values.size() < 2) {
            return 0;
        }
        double average = mean(values);
        double sumSquares = 0;
        for (double v : values) {
            sumSquares += (v - average) * (v - average);
        }
        return Math.sqrt(sumSquares / values.size());
    }

    /** 3-point moving average to reduce small noise. */
    private List<Double> smooth(List<Double> values) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            int from = Math.max(0, i - 1);
            int to = Math.min(values.size() - 1, i + 1);
            double sum = 0;
            for (int j = from; j <= to; j++) {
                sum += values.get(j);
            }
            result.add(sum / (to - from + 1));
        }
        return result;
    }

    /** Local maxima above a threshold, at least minDistance samples apart. */
    private List<Integer> findPeaks(List<Double> signal, double threshold, int minDistance) {
        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < signal.size() - 1; i++) {
            boolean isPeak = signal.get(i) > threshold
                    && signal.get(i) > signal.get(i - 1)
                    && signal.get(i) >= signal.get(i + 1);
            if (!isPeak) {
                continue;
            }
            if (peaks.isEmpty() || i - peaks.get(peaks.size() - 1) >= minDistance) {
                peaks.add(i);
            } else if (signal.get(i) > signal.get(peaks.get(peaks.size() - 1))) {
                peaks.set(peaks.size() - 1, i); // keep the higher of two close peaks
            }
        }
        return peaks;
    }
}
