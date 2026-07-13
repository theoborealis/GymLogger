# Headless Android emulator environment for testing/profiling GymLogger.
#   nix-shell emulator.nix --run './scripts/emulator.sh up'
# Provides JDK 17 + Android SDK 34 + emulator + an x86_64 system image. Separate
# from shell.nix so day-to-day builds don't pull the (large) emulator image.
{ pkgs ? import <nixpkgs> {
    config.allowUnfree = true;
    config.android_sdk.accept_license = true;
  }
}:

let
  buildToolsVersion = "34.0.0";
  composition = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "34" ];
    buildToolsVersions = [ buildToolsVersion ];
    includeEmulator = true;
    includeSystemImages = true;
    systemImageTypes = [ "google_apis" ];
    abiVersions = [ "x86_64" ];
    includeNDK = false;
    includeSources = false;
  };
  sdk = composition.androidsdk;
  sdkRoot = "${sdk}/libexec/android-sdk";
in
pkgs.mkShell {
  buildInputs = [ pkgs.jdk17 sdk ];

  ANDROID_HOME = sdkRoot;
  ANDROID_SDK_ROOT = sdkRoot;
  JAVA_HOME = "${pkgs.jdk17}";
  GRADLE_OPTS =
    "-Dorg.gradle.project.android.aapt2FromMavenOverride=${sdkRoot}/build-tools/${buildToolsVersion}/aapt2";

  # Keep the AVD + its writable userdata inside the project (gitignored), not ~/.android.
  ANDROID_AVD_HOME = toString ./.emulator/avd;
  ANDROID_USER_HOME = toString ./.emulator/.android;
  # Headless: no host display / GL needed.
  QT_QPA_PLATFORM = "offscreen";
}
