@echo off
echo Stopping any process on port 4200...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :4200 ^| findstr LISTENING 2^>nul') do (
    echo Killing PID %%a
    taskkill /PID %%a /F >nul 2>&1
)
echo Starting Angular frontend on port 4200...
call npx ng serve
