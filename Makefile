# CarMenu build/release/publish targets. Mirror of DeviceMonitor/Makefile;
# refer to that one for any procedure detail not covered here.

JAVA_HOME    ?= /opt/android-studio/jbr
ANDROID_HOME ?= $(HOME)/Android/Sdk
KEYSTORE     ?= keystore.jks
KEY_ALIAS    ?= carmenu

VARIANT      ?= debug
APK_OUT      := app/build/outputs/apk/$(VARIANT)/app-$(VARIANT).apk
APK_DEBUG    := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE  := app/build/outputs/apk/release/app-release.apk
AAB_RELEASE  := app/build/outputs/bundle/release/app-release.aab

VC := $(shell git rev-list --count HEAD 2>/dev/null || echo 1)
VN := $(shell git describe --tags --always --dirty 2>/dev/null || echo 0.1)

AAPT    := $(shell ls -1 $(ANDROID_HOME)/build-tools/*/aapt 2>/dev/null | tail -1)
KEYTOOL := $(JAVA_HOME)/bin/keytool
ADB     := $(ANDROID_HOME)/platform-tools/adb

.PHONY: help debug release bundle install dhu version keygen keyfingerprint test clean

help:
	@echo "make debug                 — build app-debug.apk"
	@echo "make release               — build app-release.apk (signed)"
	@echo "make bundle                — build app-release.aab (Play upload)"
	@echo "make install               — adb install -r the debug APK"
	@echo "make test                  — run JVM unit tests (no device/emulator needed)"
	@echo "make version               — print versionCode + versionName"
	@echo "make keygen                — generate keystore.jks + keystore.properties"
	@echo "make keyfingerprint        — print SHA-256 of the current keystore"
	@echo "make clean                 — gradle clean"

version:
	@echo "versionCode = $(VC)"
	@echo "versionName = $(VN)"

debug:
	JAVA_HOME=$(JAVA_HOME) ANDROID_HOME=$(ANDROID_HOME) ./gradlew assembleDebug

test:
	JAVA_HOME=$(JAVA_HOME) ANDROID_HOME=$(ANDROID_HOME) ./gradlew testDebugUnitTest --console=plain

release:
	JAVA_HOME=$(JAVA_HOME) ANDROID_HOME=$(ANDROID_HOME) ./gradlew assembleRelease

bundle:
	JAVA_HOME=$(JAVA_HOME) ANDROID_HOME=$(ANDROID_HOME) ./gradlew bundleRelease
	@echo "AAB → $(AAB_RELEASE)"

install: debug
	$(ADB) install -r $(APK_DEBUG)

# Open DHU + forward — see BUILD.md for the full DHU procedure.
dhu:
	@echo "Run: cd ~/Android/Sdk/extras/google/auto && ./desktop-head-unit"
	@echo "Then on phone: Android Auto > ⋮ > Start head unit server"
	$(ADB) forward tcp:5277 tcp:5277

keygen:
	@if [ -f $(KEYSTORE) ]; then echo "$(KEYSTORE) already exists — refusing to overwrite"; exit 1; fi
	@read -sp "Keystore password: " STOREPW; echo; \
	$(KEYTOOL) -genkeypair -v -keystore $(KEYSTORE) -alias $(KEY_ALIAS) \
		-keyalg RSA -keysize 2048 -validity 10000 \
		-storepass $$STOREPW -keypass $$STOREPW \
		-dname "CN=CarMenu, O=Personal, L=Stockholm, C=SE"; \
	cp keystore.properties.example keystore.properties; \
	sed -i "s/storePassword=CHANGE_ME/storePassword=$$STOREPW/" keystore.properties; \
	sed -i "s/keyPassword=CHANGE_ME/keyPassword=$$STOREPW/" keystore.properties; \
	echo "Wrote $(KEYSTORE) + keystore.properties (gitignored)."

keyfingerprint:
	@$(KEYTOOL) -list -v -keystore $(KEYSTORE) -alias $(KEY_ALIAS) 2>/dev/null \
		| grep -E "SHA1:|SHA256:" || echo "Need keystore password — re-run: $(KEYTOOL) -list -v -keystore $(KEYSTORE) -alias $(KEY_ALIAS)"

clean:
	./gradlew clean
