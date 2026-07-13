# Reproducible Android build environment for GymLogger.
#   nix-shell --run './gradlew :app:assembleDebug'
# Provides JDK 17 + Android SDK (platform 36, build-tools 36.0.0) and wires up
# the aapt2 override so Gradle uses the SDK-provided aapt2 (the Maven one will
# not run on NixOS due to the dynamic linker).
{ pkgs ? import <nixpkgs> {
    config.allowUnfree = true;
    config.android_sdk.accept_license = true;
  }
}:

let
  buildToolsVersion = "36.0.0";
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "36" ];
    buildToolsVersions = [ buildToolsVersion ];
    includeEmulator = false;
    includeSystemImages = false;
    includeNDK = false;
    includeSources = false;
  };
  androidSdk = androidComposition.androidsdk;
  sdkRoot = "${androidSdk}/libexec/android-sdk";
in
pkgs.mkShell {
  buildInputs = [
    pkgs.jdk17
    androidSdk
  ];

  ANDROID_HOME = sdkRoot;
  ANDROID_SDK_ROOT = sdkRoot;
  JAVA_HOME = "${pkgs.jdk17}";

  # Force Gradle/AGP to use the SDK aapt2 instead of the unrunnable Maven binary.
  GRADLE_OPTS =
    "-Dorg.gradle.project.android.aapt2FromMavenOverride=${sdkRoot}/build-tools/${buildToolsVersion}/aapt2";
}
