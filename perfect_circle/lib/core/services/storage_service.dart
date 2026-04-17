import 'package:shared_preferences/shared_preferences.dart';

import '../constants/app_constants.dart';

class StorageService {
  final SharedPreferences _prefs;

  StorageService(this._prefs);

  static Future<StorageService> create() async {
    final prefs = await SharedPreferences.getInstance();
    return StorageService(prefs);
  }

  double get bestScore => _prefs.getDouble(AppConstants.bestScoreKey) ?? 0.0;

  Future<void> setBestScore(double value) async {
    await _prefs.setDouble(AppConstants.bestScoreKey, value);
  }

  int get playCount => _prefs.getInt(AppConstants.playCountKey) ?? 0;

  Future<void> incrementPlayCount() async {
    await _prefs.setInt(AppConstants.playCountKey, playCount + 1);
  }

  bool get soundEnabled => _prefs.getBool(AppConstants.soundEnabledKey) ?? true;

  Future<void> setSoundEnabled(bool value) async {
    await _prefs.setBool(AppConstants.soundEnabledKey, value);
  }

  bool get hapticsEnabled => _prefs.getBool(AppConstants.hapticsEnabledKey) ?? true;

  Future<void> setHapticsEnabled(bool value) async {
    await _prefs.setBool(AppConstants.hapticsEnabledKey, value);
  }
}
