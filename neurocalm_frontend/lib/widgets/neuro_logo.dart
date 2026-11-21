import 'package:flutter/material.dart';
import '../utils/app_theme.dart';

class NeuroLogo extends StatelessWidget {
  final double size;
  const NeuroLogo({super.key, this.size = 48});
  @override
  Widget build(BuildContext context) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: const LinearGradient(colors: [AppColors.primary, Color(0xFF7B6CF0)]),
          boxShadow: AppTheme.softShadow,
        ),
        child: Icon(Icons.psychology, color: Colors.white, size: size * 0.62),
      );
}
