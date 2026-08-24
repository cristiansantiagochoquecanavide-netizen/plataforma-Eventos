# Aplicacion Movil

Cliente Flutter de Sprint 0 para participantes y staff.

Incluye registro de participante, inicio de sesion, almacenamiento seguro de tokens, renovacion de sesion, recuperacion de clave, perfil propio y cambio de clave. Las operaciones administrativas permanecen en el panel Angular.

## Ejecucion

```powershell
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

`10.0.2.2` apunta al host desde el emulador Android. Para un dispositivo fisico, usar la IP local del equipo que ejecuta el backend.

## Android local

La aplicacion compila con Android SDK 37 por compatibilidad con `flutter_secure_storage`. Para pruebas locales sobre HTTP, el manifiesto permite trafico sin cifrar exclusivamente en desarrollo. Usar un emulador estable API 35 y mantener los contenedores locales activos con `docker compose up --detach`.

## Recuperacion de clave local

El backend envía los mensajes a Mailpit, disponible en `http://localhost:8025`, y acepta una solicitud por cuenta cada 60 segundos. El enlace `eventos://recuperar-clave?token=...` abre la pantalla de restablecimiento en Android y precarga el token. Si el sistema no puede abrir el enlace, la pantalla `Olvide mi clave` permite pegar el token incluido en el correo.

El esquema `eventos://` es solo para desarrollo. Produccion debe usar Android App Links HTTPS con un dominio verificado.
