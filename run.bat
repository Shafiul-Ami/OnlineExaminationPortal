@echo off
REM Compiles and starts the ExamSphere online examination portal.
cd /d "%~dp0"
if not exist config.properties (
  copy config.example.properties config.properties >nul
  echo Created config.properties - set db.password in it, then run again.
  pause
  exit /b 1
)
if not exist build\classes mkdir build\classes
dir /s /b src\portal\*.java > build\sources.txt
javac -encoding UTF-8 -d build\classes -cp "lib\*" @build\sources.txt
if errorlevel 1 (
  echo.
  echo Compilation failed.
  pause
  exit /b 1
)
java -cp "build\classes;lib\*" portal.Main
