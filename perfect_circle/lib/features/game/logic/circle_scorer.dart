import 'dart:math' as math;
import 'package:flutter/material.dart';

import '../../../core/constants/app_constants.dart';
import '../models/game_result.dart';
import '../models/stroke_point.dart';

/// Scores a user-drawn stroke by how close it is to a perfect circle.
///
/// The algorithm:
/// 1. Reject strokes that are too short or too small.
/// 2. Compute centroid as the mean of all points.
/// 3. Compute mean radius (mean distance from centroid).
/// 4. Compute standard deviation of radii (roundness error).
/// 5. Compute closure error (gap between first and last point / mean radius).
/// 6. Combine into a 0..100 score where lower variance = higher score.
class CircleScorer {
  static GameResult score(List<StrokePoint> points) {
    if (points.length < AppConstants.minStrokePoints) {
      return GameResult.invalid('Draw a longer circle.');
    }

    double sumX = 0, sumY = 0;
    for (final p in points) {
      sumX += p.position.dx;
      sumY += p.position.dy;
    }
    final centroid = Offset(sumX / points.length, sumY / points.length);

    final radii = <double>[];
    for (final p in points) {
      radii.add((p.position - centroid).distance);
    }
    final meanRadius = radii.reduce((a, b) => a + b) / radii.length;

    if (meanRadius < AppConstants.minStrokeRadius) {
      return GameResult.invalid('Circle is too small.');
    }

    double varianceSum = 0;
    for (final r in radii) {
      final d = r - meanRadius;
      varianceSum += d * d;
    }
    final stdDev = math.sqrt(varianceSum / radii.length);
    final roundnessError = stdDev / meanRadius;

    final closureGap = (points.first.position - points.last.position).distance;
    final closureError = closureGap / meanRadius;

    // Non-linear scoring: punish errors more aggressively.
    final roundnessScore = 100.0 * math.exp(-roundnessError * 9.0);
    final closurePenalty = math.min(closureError / AppConstants.closureThreshold, 1.0) * 25.0;
    final raw = roundnessScore - closurePenalty;
    final clamped = raw.clamp(0.0, AppConstants.perfectScore);

    return GameResult(
      score: clamped,
      center: centroid,
      meanRadius: meanRadius,
      radiusStdDev: stdDev,
      closureError: closureError,
      valid: true,
    );
  }
}
