@echo off
REM Gradle Wrapper for Windows
set GRADLE_VERSION=8.5
set GRADLE_DISTRIBUTION_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip

if not exist "gradle/wrapper/gradle-wrapper.jar" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri '%GRADLE_DISTRIBUTION_URL%' -OutFile 'gradle.zip'; Expand-Archive -Path 'gradle.zip' -DestinationPath 'gradle_temp'; Move-Item -Path 'gradle_temp/gradle-%GRADLE_VERSION%' -Destination 'gradle'; Remove-Item 'gradle.zip', 'gradle_temp' -Recurse -Force"
)

if exist "gradle/bin/gradle.bat" (
    call gradle/bin/gradle.bat %*
) else (
    echo Gradle not found. Please run 'gradlew.bat' once to download, or install Gradle %GRADLE_VERSION%+ manually.
    exit /b 1
)