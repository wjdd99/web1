package com.beolddeok.alarm.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.scale

/**
 * 사진 인증용 average-hash (aHash).
 * 8x8 그레이스케일 → 평균보다 밝으면 1 → 64bit 지문.
 * 두 지문의 해밍 거리가 작으면 "같은 장소"로 간주.
 */
object ImageHash {

    /** 64bit aHash 를 16자리 hex 문자열로 반환 */
    fun compute(src: Bitmap): String {
        val small = src.scale(8, 8)
        val gray = IntArray(64)
        var sum = 0L
        for (y in 0 until 8) for (x in 0 until 8) {
            val p = small.getPixel(x, y)
            val g = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
            gray[y * 8 + x] = g
            sum += g
        }
        val avg = sum / 64
        var bits = 0L
        for (i in 0 until 64) if (gray[i] >= avg) bits = bits or (1L shl i)
        return "%016x".format(bits)
    }

    /** 두 해시의 해밍 거리(0~64). 작을수록 비슷함. */
    fun distance(a: String, b: String): Int {
        val x = a.toULong(16) xor b.toULong(16)
        return java.lang.Long.bitCount(x.toLong())
    }

    /** 거리 ≤ threshold 면 일치로 판정 (기본 10/64) */
    fun matches(a: String, b: String, threshold: Int = 10): Boolean =
        distance(a, b) <= threshold
}
