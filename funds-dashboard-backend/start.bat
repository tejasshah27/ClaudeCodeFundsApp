@echo off
echo Stopping any process on port 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    echo Killing PID %%a
    taskkill /PID %%a /F >nul 2>&1
)
echo Starting Spring Boot backend on port 8080...
call mvnw.cmd spring-boot:run
