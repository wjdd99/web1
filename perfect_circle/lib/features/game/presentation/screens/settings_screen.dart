import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/services/storage_service.dart';
import '../../state/game_state.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final asyncStorage = ref.watch(storageProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: asyncStorage.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (storage) => _SettingsList(storage: storage),
      ),
    );
  }
}

class _SettingsList extends ConsumerStatefulWidget {
  final StorageService storage;
  const _SettingsList({required this.storage});

  @override
  ConsumerState<_SettingsList> createState() => _SettingsListState();
}

class _SettingsListState extends ConsumerState<_SettingsList> {
  late bool _sound;
  late bool _haptics;

  @override
  void initState() {
    super.initState();
    _sound = widget.storage.soundEnabled;
    _haptics = widget.storage.hapticsEnabled;
  }

  @override
  Widget build(BuildContext context) {
    final controller = ref.read(gameControllerProvider.notifier);
    return ListView(
      children: [
        SwitchListTile(
          title: const Text('Sound effects'),
          subtitle: const Text('Play short sounds on actions.'),
          value: _sound,
          onChanged: (v) {
            setState(() => _sound = v);
            controller.setSoundEnabled(v);
          },
        ),
        SwitchListTile(
          title: const Text('Haptics'),
          subtitle: const Text('Vibrate on scoring and taps.'),
          value: _haptics,
          onChanged: (v) {
            setState(() => _haptics = v);
            controller.setHapticsEnabled(v);
          },
        ),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.info_outline),
          title: const Text('About'),
          subtitle: const Text('Perfect Circle v1.0.0'),
        ),
      ],
    );
  }
}
