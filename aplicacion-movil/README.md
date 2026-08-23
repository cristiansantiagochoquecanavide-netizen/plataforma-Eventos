# Aplicacion Movil

Cliente Flutter de Sprint 0 para participantes y staff.

Incluye inicio de sesion, almacenamiento seguro de tokens, renovacion de sesion, perfil propio y cambio de clave. Las operaciones administrativas permanecen en el panel Angular.

## Ejecucion

```powershell
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080/api
```

`10.0.2.2` apunta al host desde el emulador Android. Para un dispositivo fisico, usar la IP local del equipo que ejecuta el backend.
