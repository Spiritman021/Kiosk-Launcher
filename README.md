# Kiosk Launcher - BlockIt Style

A robust Android kiosk mode launcher with timer-based sessions and aggressive app blocking, similar to the BlockIt app.

## Features

- ⏱️ **Timer-Based Sessions**: Set duration and start kiosk mode
- 🔒 **Aggressive App Blocking**: Only whitelisted apps accessible
- 📱 **Phone App Always Accessible**: Emergency calls always allowed
- 🔄 **Dual Detection**: 100ms polling + Accessibility Service
- 💾 **Persistent Sessions**: Survives app restarts and device reboots
- 🎨 **Material 3 UI**: Modern Jetpack Compose interface

## Blocking Modes

- **REDIRECT**: Immediately return to launcher
- **SCREEN_OFF**: Lock screen using Device Admin
- **BOTH**: Lock screen, then redirect (BlockIt-style)

## Quick Start

### Build

```bash
./gradlew assembleDebug
```

### Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Grant Permissions

The app requires:
- Usage Stats Access
- Display Over Other Apps
- Accessibility Service
- Device Administrator
- Battery Optimization Exemption

Use the in-app permission setup screen for step-by-step guidance.

## Architecture

- **MVVM Pattern** with Jetpack Compose
- **Room Database** for persistence
- **Hilt** for dependency injection
- **Kotlin Coroutines** for async operations
- **StateFlow** for reactive state management

## Testing

See [walkthrough.md](file:///Users/vanand/.gemini/antigravity/brain/6446006c-c409-4da0-bc03-17a43df3d68d/walkthrough.md) for detailed testing instructions.

## Implementation Status

✅ Core kiosk mode functionality  
✅ Timer-based sessions  
✅ App blocking (redirect + screen-off)  
✅ Permission management  
✅ Boot persistence  
⏳ Whitelist management UI (manual DB editing required)  
⏳ Admin settings UI  
⏳ Block overlay service  

## License

[Your License Here]
