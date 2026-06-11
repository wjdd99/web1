package com.beolddeok.alarm.data

import kotlinx.serialization.Serializable

/** 깨우기 미션 종류 */
@Serializable
enum class MissionType {
    /** Claude 와 또렷하게 대화해야 꺼짐 */
    AI_CONVERSATION,

    /** 등록한 집안 장소 사진을 찍어 비슷하면 꺼짐 (침대 탈출) */
    PHOTO_ESCAPE,

    /** 몸을 움직여야 꺼짐 (흔들기 / 걷기) */
    PHYSICAL,
}

@Serializable
data class Alarm(
    val id: Long,
    /** 0~23 */
    val hour: Int,
    /** 0~59 */
    val minute: Int,
    val label: String = "",
    /** 0(일)~6(토). 비어 있으면 한 번만 울림 */
    val days: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val mission: MissionType = MissionType.AI_CONVERSATION,
    /** 미션 난이도/반복 (대화 라운드 수, 사진 장소 수, 걸음 수 등) */
    val missionStrength: Int = 3,
    val vibrate: Boolean = true,
    /** PHOTO_ESCAPE 전용: 등록한 기준 사진들의 average-hash (16자리 hex = 64bit) */
    val photoHashes: List<String> = emptyList(),
) {
    val timeText: String get() = "%02d:%02d".format(hour, minute)
}
