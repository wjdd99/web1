package com.beolddeok.alarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

internal val DAY_NAMES = listOf("일", "월", "화", "수", "목", "금", "토")

/** 리플 없이 클릭 가능하게 (간단 버전) */
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
