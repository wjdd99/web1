import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:perfect_circle/features/game/logic/circle_scorer.dart';
import 'package:perfect_circle/features/game/models/stroke_point.dart';

List<StrokePoint> synthesize({
  required Offset center,
  required double radius,
  required int samples,
  double jitter = 0.0,
  double sweepFraction = 1.0,
}) {
  final rng = math.Random(42);
  final points = <StrokePoint>[];
  for (int i = 0; i < samples; i++) {
    final t = (i / (samples - 1)) * sweepFraction * 2 * math.pi;
    final j = (rng.nextDouble() - 0.5) * jitter;
    final r = radius + j;
    final dx = center.dx + r * math.cos(t);
    final dy = center.dy + r * math.sin(t);
    points.add(StrokePoint(Offset(dx, dy), DateTime.now()));
  }
  return points;
}

void main() {
  test('perfect circle scores near 100', () {
    final pts = synthesize(center: const Offset(100, 100), radius: 80, samples: 200);
    final result = CircleScorer.score(pts);
    expect(result.valid, isTrue);
    expect(result.score, greaterThan(95));
  });

  test('noisy circle scores lower', () {
    final pts = synthesize(
      center: const Offset(100, 100),
      radius: 80,
      samples: 200,
      jitter: 20,
    );
    final result = CircleScorer.score(pts);
    expect(result.valid, isTrue);
    expect(result.score, lessThan(80));
  });

  test('partial stroke has closure penalty', () {
    final pts = synthesize(
      center: const Offset(100, 100),
      radius: 80,
      samples: 200,
      sweepFraction: 0.7,
    );
    final result = CircleScorer.score(pts);
    expect(result.valid, isTrue);
    expect(result.closureError, greaterThan(0.1));
  });

  test('too few points is invalid', () {
    final pts = [
      StrokePoint(const Offset(0, 0), DateTime.now()),
      StrokePoint(const Offset(10, 10), DateTime.now()),
    ];
    final result = CircleScorer.score(pts);
    expect(result.valid, isFalse);
  });

  test('tiny circle is invalid', () {
    final pts = synthesize(center: const Offset(50, 50), radius: 8, samples: 80);
    final result = CircleScorer.score(pts);
    expect(result.valid, isFalse);
  });
}
