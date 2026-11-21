package com.neurocalm;

import static org.junit.jupiter.api.Assertions.*;

import com.neurocalm.service.PpgProcessingService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PpgProcessingServiceTest {

    private final PpgProcessingService service = new PpgProcessingService();

    /** Sine wave: 'frequencyHz' pulses per second sampled at 'sampleRate'. */
    static List<Double> sineWave(int count, double sampleRate, double frequencyHz) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(100 + 20 * Math.sin(2 * Math.PI * frequencyHz * i / sampleRate));
        }
        return values;
    }

    @Test
    void cleanSamples_removesNullNaNAndInfinite() {
        List<Double> clean = service.cleanSamples(Arrays.asList(1.0, null, Double.NaN, 2.0, Double.POSITIVE_INFINITY));
        assertEquals(List.of(1.0, 2.0), clean);
    }

    @Test
    void cleanSamples_nullInputGivesEmptyList() {
        assertTrue(service.cleanSamples(null).isEmpty());
    }

    @Test
    void estimateHeartRate_findsAbout72BpmForA1Point2HzPulse() {
        Integer bpm = service.estimateHeartRate(sineWave(250, 25, 1.2), 25);
        assertNotNull(bpm);
        assertEquals(72, bpm, 5);
    }

    @Test
    void estimateHeartRate_returnsNullForFlatSignal() {
        List<Double> flat = new ArrayList<>();
        for (int i = 0; i < 200; i++) flat.add(100.0);
        assertNull(service.estimateHeartRate(flat, 25));
    }

    @Test
    void estimateHeartRate_returnsNullWhenTooFewSamples() {
        assertNull(service.estimateHeartRate(sineWave(20, 25, 1.2), 25));
    }

    @Test
    void estimateSampleRate_usesTimestamps() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        List<Instant> times = new ArrayList<>();
        for (int i = 0; i < 51; i++) times.add(start.plusMillis(i * 20L)); // 50 Hz
        assertEquals(50.0, service.estimateSampleRate(times), 0.5);
    }

    @Test
    void estimateSampleRate_fallsBackToDefault() {
        assertEquals(PpgProcessingService.DEFAULT_SAMPLE_RATE_HZ, service.estimateSampleRate(List.of()));
    }

    @Test
    void derivePulse_hasZeroMeanAndKeepsInput() {
        List<Double> input = List.of(10.0, 20.0, 30.0);
        List<Double> pulse = service.derivePulse(input);
        assertEquals(0.0, service.mean(pulse), 1e-9);
        assertEquals(List.of(10.0, 20.0, 30.0), input);
    }
}
