package com.neurocalm.dto;

import java.time.Instant;

/** One PPG sample sent by Flutter. "value" may be null if the sensor read failed. */
public record PpgDataDTO(Instant timestamp, Double value, Double bvp) { }
