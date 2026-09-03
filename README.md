# KYRO E.V. — Android assistant

A starter Android app for a personal voice assistant that uses Gemini to interpret commands and Android mechanisms to execute them.

## Current v0.1
- Voice input with Android SpeechRecognizer.
- Gemini command planning.
- YouTube open/search.
- Accessibility-based YouTube controls: play, pause, resume, next, back, scroll.
- Android 10+ minimum.

## Setup
1. Open this project in Android Studio.
2. Create `local.properties` in the project root (do not commit it):
   `GEMINI_API_KEY=YOUR_KEY_HERE`
   `GEMINI_MODEL=gemini-3.8-flash`
3. Sync Gradle and build/install the app.
4. Grant microphone permission.
5. Open **Settings → Accessibility → E.V. Android Control** and explicitly enable it.
6. Open E.V. and press **TALK TO E.V.**.

## Example commands
- Open YouTube
- Search YouTube for Minecraft survival
- Pause the video
- Resume
- Next video
- Scroll down
- Go back

## Important architecture note
Gemini decides the action; Android performs it. The Gemini API key is included in the local build for this prototype, which is not suitable for a public production APK because keys embedded in client apps can be extracted. A later production version should put Gemini behind a small backend and add authentication/rate limits.

## Background operation
Android controls microphone/background behavior. This starter does not secretly listen 24/7. A later version can add an explicit, user-visible foreground service/wake-word flow that follows Android's microphone rules.
