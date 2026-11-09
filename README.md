# Proton AI — Android Phone Automation

Proton AI is an on-device Android automation starter app. It provides a branded command center, voice/text commands, installed-app discovery, app launching, and accessibility-based navigation.

## Supported in this build
- Discover launchable apps installed on the device.
- Open apps such as YouTube, Facebook, Instagram, TikTok, WhatsApp, Messenger, Telegram, Gmail, Chrome, Settings, Photos, Camera, Spotify and Maps when installed.
- Voice input (Android speech recognition) and typed commands in Bangla/English.
- Accessibility actions: Back, Home, Recent Apps, scroll up/down, tap a visible UI label, and type into the currently focused text field.
- Destructive or consequential buttons such as Send, Post, Publish, Pay, Purchase and Delete are blocked by the basic text-tap guard.

## Setup
1. Install the APK.
2. Open Proton AI and tap Open accessibility settings.
3. Enable Proton AI Automation.
4. Return to Proton AI and try a text or voice command.

## Important limitations
- This is a local command parser, not a connected LLM. The existing backend has intentionally not been changed.
- App launching is broadly supported; actions inside third-party apps vary by app version, screen, login state and Android restrictions.
- This build does not send messages, publish posts, make purchases, enter passwords/OTPs, or bypass security prompts.
- QUERY_ALL_PACKAGES is used to list launchable apps. Google Play distribution requires a valid policy declaration; sideloaded builds are not subject to Play review.
- Accessibility access is powerful. It is user-enabled in Android Settings and should remain limited to explicit commands.

## Build
Run ./gradlew assembleDebug with Android SDK 34 and JDK 17.
