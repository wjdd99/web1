import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../state/game_state.dart';
import '../widgets/circle_painter.dart';
import '../widgets/score_card.dart';
import 'settings_screen.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(gameControllerProvider);
    final controller = ref.read(gameControllerProvider.notifier);
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _TopBar(bestScore: state.bestScore),
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onPanStart: (d) => controller.startStroke(d.localPosition),
                onPanUpdate: (d) => controller.appendPoint(d.localPosition),
                onPanEnd: (_) => controller.endStroke(),
                child: Stack(
                  children: [
                    Positioned.fill(
                      child: CustomPaint(
                        painter: CirclePainter(
                          points: state.points,
                          result: state.result,
                          strokeColor: theme.colorScheme.primary,
                          guideColor: theme.colorScheme.outlineVariant,
                          referenceColor: theme.colorScheme.tertiary,
                        ),
                      ),
                    ),
                    if (state.phase == GamePhase.idle && state.points.isEmpty)
                      Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.touch_app_outlined,
                              size: 56,
                              color: theme.colorScheme.outline,
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Draw a circle with one finger.',
                              style: theme.textTheme.titleMedium?.copyWith(
                                color: theme.colorScheme.outline,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
            if (state.phase == GamePhase.finished && state.result != null)
              Padding(
                padding: const EdgeInsets.all(16),
                child: ScoreCard(
                  result: state.result!,
                  bestScore: state.bestScore,
                  onRetry: controller.reset,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  final double bestScore;
  const _TopBar({required this.bestScore});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 12, 12, 8),
      child: Row(
        children: [
          Text(
            'Perfect Circle',
            style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
          const Spacer(),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: theme.colorScheme.primaryContainer,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.emoji_events_outlined,
                  size: 18,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
                const SizedBox(width: 6),
                Text(
                  'Best ${bestScore.toStringAsFixed(1)}',
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: theme.colorScheme.onPrimaryContainer,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'Settings',
            icon: const Icon(Icons.settings_outlined),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const SettingsScreen()),
              );
            },
          ),
        ],
      ),
    );
  }
}
