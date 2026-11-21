import 'package:flutter/material.dart';

class AppColors {
  static const primary = Color(0xFF4B3FC9);
  static const deep = Color(0xFF2A2272);
  static const lavender = Color(0xFFE6E1FF);
  static const lightBlue = Color(0xFFD6E6FF);
  static const bg = Color(0xFFF3F1FF);
  static const danger = Color(0xFFD32F4F);
  static const ok = Color(0xFF2E9E5B);
}

class AppTheme {
  static ThemeData get light => ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: AppColors.primary),
        scaffoldBackgroundColor: AppColors.bg,
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.primary,
            minimumSize: const Size(64, 48),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          ),
        ),
      );

  static List<BoxShadow> get softShadow => [
        BoxShadow(color: AppColors.primary.withValues(alpha: 0.12), blurRadius: 14, offset: const Offset(0, 6)),
      ];
}
