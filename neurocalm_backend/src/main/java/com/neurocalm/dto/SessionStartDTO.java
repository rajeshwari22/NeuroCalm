package com.neurocalm.dto;

import com.neurocalm.enums.MassageIntensity;
import jakarta.validation.constraints.NotNull;

public record SessionStartDTO(@NotNull Long userId, MassageIntensity intensity, String rhythm) { }
