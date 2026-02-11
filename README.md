# MagicCall - Voice Changer App

A real-time voice changer Android application with credits-based system. Transform your voice with 12+ effects in real-time.

## Features

- **Real-time voice processing** - Uses AudioRecord/AudioTrack for low-latency audio
- **12 Voice Effects:**
  - Female, Deep Male, Child, Helium, Monster (pitch-based)
  - Robot, Alien (ring modulation)
  - Echo, Hall Reverb (environment)
  - Whisper, Tremor, Old Man (special)
- **Credits System** - Local credits with daily rewards and simulated purchases
- **Foreground Service** - Keeps processing active in background
- **Material Design 3** - Dark theme UI with modern components

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Architecture:** MVVM with LiveData
- **Audio:** Android AudioRecord + AudioTrack with custom DSP
- **UI:** Material Design 3, ViewBinding, RecyclerView

## Build

Open in Android Studio and build, or use command line:

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/magiccall/voicechanger/
├── audio/                  # Core audio engine
│   ├── AudioEngine.kt      # Real-time audio capture & playback
│   ├── VoiceEffect.kt      # Effect interface
│   └── effects/             # DSP effect implementations
│       ├── PitchShiftEffect.kt
│       ├── RobotVoiceEffect.kt
│       ├── EchoEffect.kt
│       ├── ReverbEffect.kt
│       ├── WhisperEffect.kt
│       └── TremorEffect.kt
├── model/
│   ├── VoicePreset.kt       # Voice preset definitions
│   └── CreditManager.kt     # Local credits system
├── ui/
│   ├── home/
│   │   ├── HomeFragment.kt  # Main screen
│   │   └── HomeViewModel.kt
│   └── effects/
│       └── EffectsAdapter.kt # Effects grid adapter
├── service/
│   └── VoiceChangerService.kt # Foreground audio service
├── MainActivity.kt
└── MagicCallApplication.kt
```

## License

GPL-3.0
