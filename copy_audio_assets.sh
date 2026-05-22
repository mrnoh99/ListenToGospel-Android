#!/bin/bash
# Copies audio files from the iOS project into Android assets.
# Run this once after cloning, from the project root.

ANDROID_ASSETS="app/src/main/assets/AudioFiles"

if [ -n "$1" ]; then
    IOS_AUDIO="$1"
elif [ -d "ListenToGospel/AudioFiles" ]; then
    IOS_AUDIO="ListenToGospel/AudioFiles"
elif [ -d "ListenToGospel/ListenToGospel/AudioFiles" ]; then
    IOS_AUDIO="ListenToGospel/ListenToGospel/AudioFiles"
else
    IOS_AUDIO="../ListenToGospel/ListenToGospel/AudioFiles"
fi

if [ ! -d "$IOS_AUDIO" ]; then
    echo "Error: iOS audio folder not found at '$IOS_AUDIO'"
    echo "Usage: ./copy_audio_assets.sh [path/to/AudioFiles]"
    exit 1
fi

echo "Copying audio files from: $IOS_AUDIO"
mkdir -p "$ANDROID_ASSETS"
cp -r "$IOS_AUDIO"/. "$ANDROID_ASSETS/"
echo "Done. $(find "$ANDROID_ASSETS" -name '*.m4a' | wc -l | tr -d ' ') audio files copied to $ANDROID_ASSETS"
