import 'package:flutter/material.dart';

class GameResult {
  final double score;
  final Offset center;
  final double meanRadius;
  final double radiusStdDev;
  final double closureError;
  final bool valid;
  final String? invalidReason;

  const GameResult({
    required this.score,
    required this.center,
    required this.meanRadius,
    required this.radiusStdDev,
    required this.closureError,
    required this.valid,
    this.invalidReason,
  });

  factory GameResult.invalid(String reason) {
    return GameResult(
      score: 0,
      center: Offset.zero,
      meanRadius: 0,
      radiusStdDev: 0,
      closureError: 0,
      valid: false,
      invalidReason: reason,
    );
  }
}
