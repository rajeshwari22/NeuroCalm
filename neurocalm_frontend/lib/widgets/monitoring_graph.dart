import 'package:flutter/material.dart';
import '../utils/app_theme.dart';

/// Simple custom-painted line chart (values expected in roughly 0..100).
class MonitoringGraph extends StatelessWidget {
  final String title;
  final List<double> values;
  final Color color;
  const MonitoringGraph({super.key, required this.title, required this.values, required this.color});

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
          boxShadow: AppTheme.softShadow,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 6),
            SizedBox(
              height: 110,
              child: CustomPaint(size: Size.infinite, painter: _GraphPainter(values, color)),
            ),
          ],
        ),
      );
}

class _GraphPainter extends CustomPainter {
  final List<double> v;
  final Color color;
  _GraphPainter(this.v, this.color);

  @override
  void paint(Canvas canvas, Size s) {
    final grid = Paint()..color = Colors.grey.withValues(alpha: 0.25)..strokeWidth = 1;
    for (var i = 0; i <= 2; i++) {
      final y = s.height * i / 2;
      canvas.drawLine(Offset(0, y), Offset(s.width, y), grid);
    }
    if (v.length < 2) return;
    final lo = v.reduce((a, b) => a < b ? a : b);
    final hi = v.reduce((a, b) => a > b ? a : b);
    final range = (hi - lo) == 0 ? 1.0 : hi - lo;
    final path = Path();
    for (var i = 0; i < v.length; i++) {
      final x = s.width * i / (v.length - 1);
      final y = s.height - (v[i] - lo) / range * (s.height - 8) - 4;
      i == 0 ? path.moveTo(x, y) : path.lineTo(x, y);
    }
    final fill = Path.from(path)..lineTo(s.width, s.height)..lineTo(0, s.height)..close();
    canvas.drawPath(
      fill,
      Paint()..shader = LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [color.withValues(alpha: 0.3), color.withValues(alpha: 0)],
      ).createShader(Offset.zero & s),
    );
    canvas.drawPath(path, Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 2);
  }

  @override
  bool shouldRepaint(_GraphPainter old) => old.v != v || old.color != color;
}
