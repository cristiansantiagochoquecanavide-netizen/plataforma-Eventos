@echo off
setlocal
cd /d "%~dp0"
docker info >nul 2>&1
if errorlevel 1 (
  echo Abre Docker Desktop, espera a que termine de iniciar y vuelve a ejecutar este archivo.
  pause
  exit /b 1
)
echo Iniciando la demo CU24. La primera construccion puede tardar varios minutos.
call docker compose -p cu24-demo -f docker-compose.yml -f docker-compose.local.yml up --build -d
if errorlevel 1 (
  echo No se pudo iniciar la demo. Conserva el mensaje de error para revisarlo.
  pause
  exit /b 1
)
echo Demo disponible en http://localhost:4200/iniciar-sesion
echo Organizador: demo.sur.organizador@plataformaeventos.local
echo Clave local: Demo123!
start "" "http://localhost:4200/iniciar-sesion"
pause
