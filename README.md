# KYRO E.V. — Android assistant

A starter Android app for a personal voice assistant that uses Gemini to interpret commands and Android mechanisms to execute supported actions.

## Current v0.1
- Voice input with Android SpeechRecognizer
- Gemini command planning
- YouTube open/search
- Accessibility-based YouTube controls: play, pause, resume, next, back, scroll
- Android 10+ minimum

## Phone-only cloud build
This repository includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`.

1. Open the repository's **Actions** tab.
2. Select **Build E.V. APK**.
3. Tap **Run workflow** and choose `main`.
4. Wait for the run to finish.
5. Open the completed run and download the **EV-debug-apk** artifact.
6. Extract the ZIP and install `app-debug.apk` on the Android phone.

The workflow can use a repository secret named `GEMINI_API_KEY`. If it is not set, the APK can still compile, but Gemini commands will not work until a key is configured.

Optional repository variable:
- `GEMINI_MODEL` — defaults to `gemini-3.8-flash`.

## First-run phone setup
1. Install E.V.
2. Grant microphone permission.
3. Open **Settings → Accessibility → E.V. Android Control** and explicitly enable it.
4. Open E.V. and test: `E.V., open YouTube`.

## Important architecture note
Gemini decides which supported action to take; Android performs the action. Accessibility access is user-enabled and is not silently granted by the app.

The starter does not secretly listen 24/7. A future background voice/wake-word version must follow Android's microphone and foreground-service rules.

## API-key note
For this prototype, the Gemini key is supplied to the local build through `local.properties` / GitHub Actions secrets. Do not commit a real API key into the repository. A public production release should put Gemini behind a small authenticated backend because API keys embedded in client APKs can be extracted.
