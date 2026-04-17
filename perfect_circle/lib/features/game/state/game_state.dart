import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../core/services/feedback_service.dart';
import '../../../core/services/storage_service.dart';
import '../logic/circle_scorer.dart';
import '../models/game_result.dart';
import '../models/stroke_point.dart';

enum GamePhase { idle, drawing, scoring, finished }

class GameState {
  final GamePhase phase;
  final List<StrokePoint> points;
  final GameResult? result;
  final double bestScore;

  const GameState({
    required this.phase,
    required this.points,
    required this.bestScore,
    this.result,
  });

  GameState copyWith({
    GamePhase? phase,
    List<StrokePoint>? points,
    GameResult? result,
    double? bestScore,
    bool clearResult = false,
  }) {
    return GameState(
      phase: phase ?? this.phase,
      points: points ?? this.points,
      result: clearResult ? null : (result ?? this.result),
      bestScore: bestScore ?? this.bestScore,
    );
  }

  factory GameState.initial(double bestScore) => GameState(
        phase: GamePhase.idle,
        points: const [],
        bestScore: bestScore,
      );
}

final sharedPreferencesProvider = FutureProvider<SharedPreferences>((ref) async {
  return SharedPreferences.getInstance();
});

final storageProvider = FutureProvider<StorageService>((ref) async {
  final prefs = await ref.watch(sharedPreferencesProvider.future);
  return StorageService(prefs);
});

final feedbackProvider = Provider<FeedbackService>((ref) {
  return FeedbackService();
});

final gameControllerProvider =
    StateNotifierProvider<GameController, GameState>((ref) {
  return GameController(ref);
});

class GameController extends StateNotifier<GameState> {
  final Ref _ref;

  GameController(this._ref) : super(GameState.initial(0)) {
    _loadBestScore();
  }

  Future<void> _loadBestScore() async {
    final storage = await _ref.read(storageProvider.future);
    state = state.copyWith(bestScore: storage.bestScore);
    final feedback = _ref.read(feedbackProvider);
    feedback.hapticsEnabled = storage.hapticsEnabled;
    feedback.soundEnabled = storage.soundEnabled;
  }

  void startStroke(Offset position) {
    state = state.copyWith(
      phase: GamePhase.drawing,
      points: [StrokePoint(position, DateTime.now())],
      clearResult: true,
    );
  }

  void appendPoint(Offset position) {
    if (state.phase != GamePhase.drawing) return;
    final updated = List<StrokePoint>.from(state.points)
      ..add(StrokePoint(position, DateTime.now()));
    state = state.copyWith(points: updated);
  }

  Future<void> endStroke() async {
    if (state.phase != GamePhase.drawing) return;
    state = state.copyWith(phase: GamePhase.scoring);
    final result = CircleScorer.score(state.points);
    final feedback = _ref.read(feedbackProvider);

    double bestScore = state.bestScore;
    if (result.valid) {
      if (result.score > bestScore) {
        bestScore = result.score;
        final storage = await _ref.read(storageProvider.future);
        await storage.setBestScore(bestScore);
      }
      final storage = await _ref.read(storageProvider.future);
      await storage.incrementPlayCount();
      if (result.score >= 80) {
        feedback.success();
      } else {
        feedback.tap();
      }
    } else {
      feedback.failure();
    }

    state = state.copyWith(
      phase: GamePhase.finished,
      result: result,
      bestScore: bestScore,
    );
  }

  void reset() {
    state = state.copyWith(
      phase: GamePhase.idle,
      points: const [],
      clearResult: true,
    );
  }

  Future<void> setSoundEnabled(bool value) async {
    final storage = await _ref.read(storageProvider.future);
    await storage.setSoundEnabled(value);
    _ref.read(feedbackProvider).soundEnabled = value;
  }

  Future<void> setHapticsEnabled(bool value) async {
    final storage = await _ref.read(storageProvider.future);
    await storage.setHapticsEnabled(value);
    _ref.read(feedbackProvider).hapticsEnabled = value;
  }
}
