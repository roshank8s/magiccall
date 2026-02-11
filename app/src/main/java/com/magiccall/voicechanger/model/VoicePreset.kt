package com.magiccall.voicechanger.model

import com.magiccall.voicechanger.audio.VoiceEffect
import com.magiccall.voicechanger.audio.effects.*

data class VoicePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val iconRes: String,
    val creditCost: Int,
    val category: Category,
    val effectFactory: () -> VoiceEffect
) {
    enum class Category {
        PITCH, MODULATION, ENVIRONMENT, SPECIAL
    }

    companion object {
        fun getAll(): List<VoicePreset> = listOf(
            VoicePreset(
                id = "female",
                displayName = "Female",
                description = "Higher pitched feminine voice",
                iconRes = "ic_female",
                creditCost = 1,
                category = Category.PITCH,
                effectFactory = { PitchShiftEffect("Female", 1.6f) }
            ),
            VoicePreset(
                id = "male_deep",
                displayName = "Deep Male",
                description = "Deep masculine voice",
                iconRes = "ic_male",
                creditCost = 1,
                category = Category.PITCH,
                effectFactory = { PitchShiftEffect("Deep Male", 0.7f) }
            ),
            VoicePreset(
                id = "child",
                displayName = "Child",
                description = "High-pitched child voice",
                iconRes = "ic_child",
                creditCost = 1,
                category = Category.PITCH,
                effectFactory = { PitchShiftEffect("Child", 1.8f) }
            ),
            VoicePreset(
                id = "helium",
                displayName = "Helium",
                description = "Super high chipmunk voice",
                iconRes = "ic_helium",
                creditCost = 2,
                category = Category.PITCH,
                effectFactory = { PitchShiftEffect("Helium", 2.2f) }
            ),
            VoicePreset(
                id = "monster",
                displayName = "Monster",
                description = "Deep scary monster voice",
                iconRes = "ic_monster",
                creditCost = 2,
                category = Category.PITCH,
                effectFactory = { PitchShiftEffect("Monster", 0.5f) }
            ),
            VoicePreset(
                id = "robot",
                displayName = "Robot",
                description = "Metallic robotic voice",
                iconRes = "ic_robot",
                creditCost = 2,
                category = Category.MODULATION,
                effectFactory = { RobotVoiceEffect(150f) }
            ),
            VoicePreset(
                id = "echo",
                displayName = "Echo",
                description = "Voice with echo/delay",
                iconRes = "ic_echo",
                creditCost = 1,
                category = Category.ENVIRONMENT,
                effectFactory = { EchoEffect(300, 0.5f, 0.4f) }
            ),
            VoicePreset(
                id = "reverb",
                displayName = "Hall",
                description = "Concert hall reverb",
                iconRes = "ic_reverb",
                creditCost = 1,
                category = Category.ENVIRONMENT,
                effectFactory = { ReverbEffect(0.7f, 0.35f) }
            ),
            VoicePreset(
                id = "whisper",
                displayName = "Whisper",
                description = "Soft whispering voice",
                iconRes = "ic_whisper",
                creditCost = 2,
                category = Category.SPECIAL,
                effectFactory = { WhisperEffect(0.6f) }
            ),
            VoicePreset(
                id = "tremor",
                displayName = "Tremor",
                description = "Shaky trembling voice",
                iconRes = "ic_tremor",
                creditCost = 1,
                category = Category.SPECIAL,
                effectFactory = { TremorEffect(6f, 0.5f) }
            ),
            VoicePreset(
                id = "alien",
                displayName = "Alien",
                description = "Extraterrestrial voice",
                iconRes = "ic_alien",
                creditCost = 3,
                category = Category.SPECIAL,
                effectFactory = { RobotVoiceEffect(300f) }
            ),
            VoicePreset(
                id = "old_man",
                displayName = "Old Man",
                description = "Elderly trembling voice",
                iconRes = "ic_old",
                creditCost = 2,
                category = Category.PITCH,
                effectFactory = { PitchShiftEffect("Old Man", 0.8f) }
            )
        )
    }
}
