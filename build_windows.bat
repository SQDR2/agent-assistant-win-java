@echo off
echo ==========================================
echo Building Agent Assistant (Windows Java)
echo ==========================================

echo.
echo [0/3] Setting up Environment...
call dart pub global activate protoc_plugin 21.1.2
set "PATH=%PATH%;%APPDATA%\Pub\Cache\bin;%LOCALAPPDATA%\Pub\Cache\bin"

echo.
echo [1/3] Generating Protobuf for Client...
if not exist "client\lib\proto" mkdir "client\lib\proto"
protoc --dart_out=client/lib/proto -Iserver/src/main/proto server/src/main/proto/agentassist.proto
if %errorlevel% neq 0 (
    echo Failed to generate Protobuf. Ensure 'protoc' is in your PATH.
    echo If 'protoc-gen-dart' is missing, ensure Dart SDK is installed.
    pause
    exit /b %errorlevel%
)

echo.
echo [2/3] Building Java Server...
cd server
call gradlew.bat bootJar
if %errorlevel% neq 0 (
    echo Failed to build Server.
    cd ..
    pause
    exit /b %errorlevel%
)
copy "build\libs\server-1.0.0.jar" "build\libs\server-1.0.0-all.jar" /Y
cd ..

echo.
echo [3/3] Building Flutter Client...
cd client
call flutter pub get
call flutter build windows
if %errorlevel% neq 0 (
    echo Failed to build Client.
    cd ..
    pause
    exit /b %errorlevel%
)
cd ..

echo.
echo ==========================================
echo Build Success!
echo Server: server\build\libs\server-1.0.0.jar
echo Client: client\build\windows\runner\Release\client.exe
echo ==========================================
pause
