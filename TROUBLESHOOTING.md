# BlockIt-Style Kiosk Mode - Troubleshooting Guide

## ✅ Code Status

**All BlockIt-style code is already implemented!**

Git commits show:
- `e0f6b2f` - "similar to blockit app"
- `10949c3` - "True lockdown kiosk mode"

Files confirmed:
- ✅ `StatusBarBlockerService.kt` - System-wide overlay (created Jan 29)
- ✅ `KioskAccessibilityService.kt` - Explicit launcher redirection
- ✅ `AppLaunchMonitor.kt` - Explicit launcher redirection
- ✅ `KioskService.kt` - Integrated with StatusBarBlockerService

## 🐛 Why It's Not Working

Since the code is correct, the issue is likely **permissions or configuration**. Here's what to check:

### 1. **Critical Permissions** (MOST COMMON ISSUE)

#### ✅ Display Over Other Apps
```
Settings → Apps → Kiosk Launcher → 
Advanced → Display over other apps → ALLOW
```
**Why:** StatusBarBlockerService needs this to create overlay

#### ✅ Accessibility Service
```
Settings → Accessibility → 
Kiosk Launcher → ENABLE
```
**Why:** Real-time app blocking requires this

#### ✅ Usage Stats
```
Settings → Apps → Special access → 
Usage access → Kiosk Launcher → ALLOW
```
**Why:** AppLaunchMonitor needs this to detect foreground app

### 2. **Verify Services Are Running**

Check if services are active:
```bash
adb shell dumpsys activity services | grep kiosk
```

Should show:
- `KioskService` - Running
- `StatusBarBlockerService` - Running
- `KioskAccessibilityService` - Running

### 3. **Check Logs**

View real-time logs:
```bash
adb logcat | grep -E "KioskAccessibility|AppLaunchMonitor|StatusBarBlocker"
```

**What to look for:**
- `"Allowing whitelisted app"` - Good, whitelisted apps allowed
- `"BLOCKING non-whitelisted app"` - Good, blocking is working
- `"Blocked app - launcher brought to front"` - Good, redirection working
- `"Status bar blocker overlay added"` - Good, overlay active

### 4. **Common Issues**

#### Issue: Apps Not Being Blocked
**Cause:** Accessibility service not enabled  
**Fix:** Enable in Settings → Accessibility

**Verify:**
```bash
adb shell settings get secure enabled_accessibility_services
```
Should include: `com.kv.kiosklauncher/.service.KioskAccessibilityService`

#### Issue: Status Bar Still Accessible
**Cause:** Display over other apps permission not granted  
**Fix:** Grant permission in Settings

**Verify:**
```bash
adb shell appops get com.kv.kiosklauncher SYSTEM_ALERT_WINDOW
```
Should show: `SYSTEM_ALERT_WINDOW: allow`

#### Issue: Slow Blocking Response
**Cause:** AppLaunchMonitor not running or slow interval  
**Fix:** Check if Usage Stats permission granted

#### Issue: Launcher Not Default
**Cause:** Another launcher is default  
**Fix:** This shouldn't matter - our code uses explicit LauncherActivity intent

### 5. **Test Each Layer**

#### Test Accessibility Service
1. Enable kiosk mode
2. Try to open Settings
3. Check logcat for: `"BLOCKING non-whitelisted app: com.android.settings"`
4. Should redirect to launcher immediately

#### Test App Monitor
1. If accessibility doesn't catch it
2. App monitor should catch within 500ms
3. Check logcat for: `"BLOCKING non-whitelisted app"`

#### Test Status Bar Blocker
1. Open whitelisted app
2. Try to pull notification bar
3. Should be blocked
4. Check logcat for: `"Status bar blocker overlay added"`

## 🔧 Quick Fix Steps

### Step 1: Reinstall APK
```bash
adb uninstall com.kv.kiosklauncher
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Grant All Permissions
```bash
# Display over other apps (requires manual grant in Settings)
# Accessibility (requires manual enable in Settings)

# Usage Stats
adb shell appops set com.kv.kiosklauncher GET_USAGE_STATS allow
```

### Step 3: Enable Kiosk Mode
1. Open app
2. Long press 3 seconds
3. Enter password
4. Enable kiosk mode

### Step 4: Verify Services
```bash
adb shell dumpsys activity services | grep -A 5 KioskService
adb shell dumpsys activity services | grep -A 5 StatusBarBlockerService
```

### Step 5: Test
1. Try opening non-whitelisted app
2. Watch logcat for blocking messages
3. Should redirect to launcher

## 📊 Expected Logcat Output

When working correctly:
```
KioskAccessibilityService: Window state changed: com.someapp
KioskAccessibilityService: BLOCKING non-whitelisted app: com.someapp
KioskAccessibilityService: Blocked app - launcher brought to front
StatusBarBlocker: Status bar blocker overlay added (system-wide)
AppLaunchMonitor: BLOCKING non-whitelisted app: com.someapp
AppLaunchMonitor: Launcher brought to front
```

## 🎯 If Still Not Working

### Enable Debug Logging

The code already has extensive logging. Just watch logcat:
```bash
adb logcat -c  # Clear logs
adb logcat | grep -E "Kiosk|Accessibility|StatusBar|AppLaunch"
```

### Check Device Compatibility

Some manufacturers (Xiaomi, Huawei, Oppo) have aggressive battery optimization:

**Disable Battery Optimization:**
```
Settings → Battery → Battery optimization → 
Kiosk Launcher → Don't optimize
```

**Disable MIUI Optimization (Xiaomi):**
```
Settings → Additional settings → Developer options → 
Turn off MIUI optimization
```

### Last Resort: Use Device Owner Mode

For maximum reliability:
```bash
# Factory reset device
# Before adding Google account:
adb shell dpm set-device-owner com.kv.kiosklauncher/.receiver.KioskDeviceAdmin

# Then enable kiosk mode
```

## 📱 Device-Specific Issues

### Samsung
- Disable "Smart Stay"
- Disable "Edge panels"

### Xiaomi/MIUI
- Disable "MIUI Optimization"
- Disable "Battery Saver"
- Enable "Autostart"

### Huawei/EMUI
- Disable "PowerGenie"
- Enable "Autostart"

## ✅ Verification Checklist

- [ ] Display over other apps permission granted
- [ ] Accessibility service enabled
- [ ] Usage stats permission granted
- [ ] KioskService running
- [ ] StatusBarBlockerService running
- [ ] KioskAccessibilityService running
- [ ] Battery optimization disabled
- [ ] Kiosk mode enabled in app
- [ ] Tested with logcat monitoring

## 🎯 Contact Info

If still not working after all checks, provide:
1. Logcat output when trying to open non-whitelisted app
2. Output of permission check commands
3. Device model and Android version
4. Screenshot of Settings → Accessibility
