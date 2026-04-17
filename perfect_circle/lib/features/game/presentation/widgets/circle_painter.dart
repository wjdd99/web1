import 'dart:math' as math;
import 'package:flutter/material.dart';

import '../../models/game_result.dart';
import '../../models/stroke_point.dart';

class CirclePainter extends CustomPainter {
  final List<StrokePoint> points;
  final GameResult? result;
  final Color strokeColor;
  final Color guideColor;
  final Color referenceColor;

  CirclePainter({
    required this.points,
    required this.result,
    required this.strokeColor,
    required this.guideColor,
    required this.referenceColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final guidePaint = Paint()
      ..color = guideColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.2;
    canvas.drawCircle(center, 6, guidePaint);
    canvas.drawLine(
      Offset(center.dx - 14, center.dy),
      Offset(center.dx + 14, center.dy),
      guidePaint,
    );
    canvas.drawLine(
      Offset(center.dx, center.dy - 14),
      Offset(center.dx, center.dy + 14),
      guidePaint,
    );

    if (points.length >= 2) {
      final strokePaint = Paint()
        ..color = strokeColor
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..strokeWidth = 4.0;
      final path = Path()..moveTo(points.first.position.dx, points.first.position.dy);
      for (int i = 1; i < points.length; i++) {
        path.lineTo(points[i].position.dx, points[i].position.dy);
      }
      canvas.drawPath(path, strokePaint);
    }

    final r = result;
    if (r != null && r.valid) {
      final refPaint = Paint()
        ..color = referenceColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.0;
      canvas.drawCircle(r.center, r.meanRadius, refPaint);

      final centerDot = Paint()
        ..color = referenceColor
        ..style = PaintingStyle.fill;
      canvas.drawCircle(r.center, 4, centerDot);

      final bandPaint = Paint()
        ..color = referenceColor.withOpacity(0.15)
        ..style = PaintingStyle.stroke
        ..strokeWidth = math.max(2.0, r.radiusStdDev * 2);
      canvas.drawCircle(r.center, r.meanRadius, bandPaint);
    }
  }

  @override
  bool shouldRepaint(covariant CirclePainter oldDelegate) {
    return oldDelegate.points != points || oldDelegate.result != result;
  }
}
