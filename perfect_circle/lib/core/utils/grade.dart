import 'package:flutter/material.dart';

class Grade {
  final String label;
  final Color color;
  final String message;

  const Grade({required this.label, required this.color, required this.message});

  static Grade fromScore(double score) {
    if (score >= 98) {
      return const Grade(
        label: 'S',
        color: Color(0xFFFFD166),
        message: 'Inhuman. Are you a compass?',
      );
    }
    if (score >= 92) {
      return const Grade(
        label: 'A',
        color: Color(0xFF06D6A0),
        message: 'Masterful circle!',
      );
    }
    if (score >= 80) {
      return const Grade(
        label: 'B',
        color: Color(0xFF5B8DEF),
        message: 'Very round.',
      );
    }
    if (score >= 65) {
      return const Grade(
        label: 'C',
        color: Color(0xFF118AB2),
        message: 'Decent attempt.',
      );
    }
    if (score >= 45) {
      return const Grade(
        label: 'D',
        color: Color(0xFFEF476F),
        message: 'Needs more practice.',
      );
    }
    return const Grade(
      label: 'F',
      color: Color(0xFFBF3145),
      message: 'Was that a circle?',
    );
  }
}
