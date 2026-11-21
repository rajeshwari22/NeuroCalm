import 'package:flutter/material.dart';
import 'screens/login_screen.dart';
import 'utils/app_theme.dart';

void main() => runApp(const NeuroCalmApp());

class NeuroCalmApp extends StatelessWidget {
  const NeuroCalmApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'NeuroCalm',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        home: const LoginScreen(),
      );
}
