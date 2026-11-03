@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
set "ANDROID_HOME=C:\Users\workylappy\AppData\Local\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
cd /d C:\workspaces\FastShoppr-spike\phase0-android
C:\workspaces\software\gradle-8.8\bin\gradle.bat %*
