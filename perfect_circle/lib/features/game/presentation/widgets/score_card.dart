import 'package:flutter/material.dart';

import '../../../../core/utils/grade.dart';
import '../../models/game_result.dart';

class ScoreCard extends StatelessWidget {
  final GameResult result;
  final double bestScore;
  final VoidCallback onRetry;

  const ScoreCard({
    super.key,
    required this.result,
    required this.bestScore,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    if (!result.valid) {
      return _InfoCard(
        title: 'Try again',
        body: result.invalidReason ?? 'Draw a single round stroke.',
        onRetry: onRetry,
      );
    }
    final grade = Grade.fromScore(result.score);
    final scoreText = result.score.toStringAsFixed(1);
    return Card(
      elevation: 8,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: grade.color.withOpacity(0.15),
                  child: Text(
                    grade.label,
                    style: theme.textTheme.headlineMedium?.copyWith(
                      color: grade.color,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Score', style: theme.textTheme.labelMedium),
                      Text(
                        scoreText,
                        style: theme.textTheme.displaySmall?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: grade.color,
                        ),
                      ),
                    ],
                  ),
                ),
                FilledButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Retry'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(grade.message, style: theme.textTheme.bodyLarge),
            const SizedBox(height: 12),
            _MetricRow(
              label: 'Best',
              value: bestScore.toStringAsFixed(1),
            ),
            _MetricRow(
              label: 'Mean radius',
              value: '${result.meanRadius.toStringAsFixed(1)} px',
            ),
            _MetricRow(
              label: 'Roundness σ',
              value: result.radiusStdDev.toStringAsFixed(2),
            ),
            _MetricRow(
              label: 'Closure error',
              value: '${(result.closureError * 100).toStringAsFixed(1)}%',
            ),
          ],
        ),
      ),
    );
  }
}

class _MetricRow extends StatelessWidget {
  final String label;
  final String value;

  const _MetricRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: theme.textTheme.bodyMedium),
          Text(
            value,
            style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  final String title;
  final String body;
  final VoidCallback onRetry;

  const _InfoCard({required this.title, required this.body, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 6,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
        child: Row(
          children: [
            Icon(Icons.info_outline, color: theme.colorScheme.primary, size: 36),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: theme.textTheme.titleLarge),
                  const SizedBox(height: 4),
                  Text(body, style: theme.textTheme.bodyMedium),
                ],
              ),
            ),
            const SizedBox(width: 12),
            FilledButton.tonalIcon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }
}
