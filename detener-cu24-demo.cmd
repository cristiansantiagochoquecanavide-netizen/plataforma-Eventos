@echo off
setlocal
cd /d "%~dp0"
call docker compose -p cu24-demo -f docker-compose.yml -f docker-compose.local.yml stop
echo Los datos de la demo se conservan para el siguiente inicio.
pause
