@echo off
echo Stopping Spring Boot backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    echo Killing PID %%a
    taskkill /PID %%a /F >nul 2>&1
)
echo Backend stopped.
