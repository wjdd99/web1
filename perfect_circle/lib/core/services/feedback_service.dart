import 'package:flutter/services.dart';
import 'package:vibration/vibration.dart';

class FeedbackService {
  bool hapticsEnabled;
  bool soundEnabled;

  FeedbackService({this.hapticsEnabled = true, this.soundEnabled = true});

  Future<void> tap() async {
    if (!hapticsEnabled) return;
    HapticFeedback.selectionClick();
  }

  Future<void> success() async {
    if (!hapticsEnabled) return;
    if ((await Vibration.hasVibrator()) ?? false) {
      Vibration.vibrate(pattern: [0, 40, 60, 80]);
    } else {
      HapticFeedback.mediumImpact();
    }
  }

  Future<void> failure() async {
    if (!hapticsEnabled) return;
    HapticFeedback.heavyImpact();
  }
}
