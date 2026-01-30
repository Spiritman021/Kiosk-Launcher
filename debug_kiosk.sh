#!/bin/bash

# Kiosk Launcher Debug Script
# This script helps diagnose why app blocking isn't working

echo "================================================"
echo "🔍 Kiosk Launcher Debugging Script"
echo "================================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Check if app is installed
echo "1️⃣  Checking if app is installed..."
if adb shell pm list packages | grep -q "com.kv.kiosklauncher"; then
    echo -e "${GREEN}✓ App is installed${NC}"
else
    echo -e "${RED}✗ App is NOT installed${NC}"
    echo "   Run: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    exit 1
fi
echo ""

# 2. Check permissions
echo "2️⃣  Checking permissions..."

# Usage Stats
if adb shell appops get com.kv.kiosklauncher GET_USAGE_STATS | grep -q "allow"; then
    echo -e "${GREEN}✓ Usage Stats permission granted${NC}"
else
    echo -e "${RED}✗ Usage Stats permission DENIED${NC}"
fi

# Overlay
if adb shell appops get com.kv.kiosklauncher SYSTEM_ALERT_WINDOW | grep -q "allow"; then
    echo -e "${GREEN}✓ Overlay permission granted${NC}"
else
    echo -e "${RED}✗ Overlay permission DENIED${NC}"
fi

# Battery optimization
if adb shell dumpsys deviceidle whitelist | grep -q "com.kv.kiosklauncher"; then
    echo -e "${GREEN}✓ Battery optimization disabled${NC}"
else
    echo -e "${YELLOW}⚠ Battery optimization enabled (may affect monitoring)${NC}"
fi

echo ""

# 3. Check if service is running
echo "3️⃣  Checking if monitoring service is running..."
if adb shell ps -A | grep -q "com.kv.kiosklauncher"; then
    echo -e "${GREEN}✓ App process is running${NC}"
    
    # Check for AppMonitorService specifically
    if adb shell dumpsys activity services | grep -q "AppMonitorService"; then
        echo -e "${GREEN}✓ AppMonitorService is active${NC}"
    else
        echo -e "${RED}✗ AppMonitorService is NOT running${NC}"
        echo "   This is the main issue - the monitoring service isn't started!"
    fi
else
    echo -e "${RED}✗ App is not running${NC}"
fi
echo ""

# 4. Check database for whitelisted apps
echo "4️⃣  Checking whitelisted apps in database..."
echo "   Querying database..."
WHITELIST_COUNT=$(adb shell "run-as com.kv.kiosklauncher sqlite3 /data/data/com.kv.kiosklauncher/databases/kiosk_launcher.db 'SELECT COUNT(*) FROM whitelisted_apps;'" 2>/dev/null)

if [ -z "$WHITELIST_COUNT" ]; then
    echo -e "${YELLOW}⚠ Could not read database (app may need to be debuggable)${NC}"
else
    if [ "$WHITELIST_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✓ Found $WHITELIST_COUNT whitelisted app(s)${NC}"
        echo "   Whitelisted apps:"
        adb shell "run-as com.kv.kiosklauncher sqlite3 /data/data/com.kv.kiosklauncher/databases/kiosk_launcher.db 'SELECT packageName, appName FROM whitelisted_apps;'" 2>/dev/null | while read line; do
            echo "   - $line"
        done
    else
        echo -e "${RED}✗ NO whitelisted apps found!${NC}"
        echo "   This is a critical issue - whitelist is empty!"
    fi
fi
echo ""

# 5. Check active session
echo "5️⃣  Checking for active kiosk session..."
SESSION_ACTIVE=$(adb shell "run-as com.kv.kiosklauncher sqlite3 /data/data/com.kv.kiosklauncher/databases/kiosk_launcher.db 'SELECT COUNT(*) FROM sessions WHERE isActive=1;'" 2>/dev/null)

if [ -z "$SESSION_ACTIVE" ]; then
    echo -e "${YELLOW}⚠ Could not read session data${NC}"
elif [ "$SESSION_ACTIVE" -gt 0 ]; then
    echo -e "${GREEN}✓ Active session found${NC}"
    adb shell "run-as com.kv.kiosklauncher sqlite3 /data/data/com.kv.kiosklauncher/databases/kiosk_launcher.db 'SELECT id, startTime, isIndefinite FROM sessions WHERE isActive=1;'" 2>/dev/null | while read line; do
        echo "   Session: $line"
    done
else
    echo -e "${RED}✗ NO active session${NC}"
    echo "   You need to start kiosk mode first!"
fi
echo ""

# 6. Live log monitoring
echo "6️⃣  Starting live log monitoring..."
echo "   Press Ctrl+C to stop"
echo "   ${YELLOW}Now try opening a non-whitelisted app (like Chrome)${NC}"
echo ""
echo "================================================"

adb logcat -c  # Clear logs
adb logcat | grep -E "AppMonitorService|BlockingAction|KioskAccessibility|SessionManager" --line-buffered | while read line; do
    if echo "$line" | grep -q "BLOCKING"; then
        echo -e "${RED}$line${NC}"
    elif echo "$line" | grep -q "allowed"; then
        echo -e "${GREEN}$line${NC}"
    else
        echo "$line"
    fi
done
