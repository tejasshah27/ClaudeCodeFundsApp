@echo off
echo Stopping Angular frontend (port 4200)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :4200 ^| findstr LISTENING 2^>nul') do (
    echo Killing PID %%a
    taskkill /PID %%a /F >nul 2>&1
)
echo Frontend stopped.
