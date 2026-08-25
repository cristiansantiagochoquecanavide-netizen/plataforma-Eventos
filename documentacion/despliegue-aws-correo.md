# Correo transaccional para CU4 en AWS

CU4 no muestra enlaces en pantalla, no genera codigos visibles y no simula envios. En produccion el backend debe conectarse a un SMTP transaccional real, por ejemplo Amazon SES.

## Variables obligatorias

Configurar estas variables en el servicio backend:

```text
URL_FRONTEND_WEB=https://app.tu-dominio.com
CORS_ORIGENES=https://app.tu-dominio.com
SMTP_HOST=email-smtp.us-east-1.amazonaws.com
SMTP_PORT=587
SMTP_USER=USUARIO_SMTP_DE_SES
SMTP_PASSWORD=PASSWORD_SMTP_DE_SES
SMTP_FROM=no-reply@tu-dominio.com
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
SMTP_STARTTLS_REQUIRED=true
SMTP_SSL_ENABLE=false
RECUPERACION_CLAVE_EXPIRACION_MINUTOS=30
```

`URL_FRONTEND_WEB` debe ser la URL publica del frontend desplegado. El enlace enviado por correo se genera como:

```text
${URL_FRONTEND_WEB}/restablecer-clave?token=<token>
```

## Amazon SES

1. Verificar en SES el dominio o el correo usado en `SMTP_FROM`.
2. Crear credenciales SMTP de SES para la misma region del endpoint.
3. Usar `email-smtp.<region>.amazonaws.com` como `SMTP_HOST`.
4. Usar puerto `587` con `STARTTLS` habilitado.
5. En produccion, sacar la cuenta de SES sandbox para enviar a destinatarios no verificados.
6. Guardar `SMTP_USER` y `SMTP_PASSWORD` como secretos del entorno de AWS, no en el repositorio.

Las credenciales SMTP de SES no son las mismas que las access keys normales de AWS.

## Docker Compose en AWS

Usar el override de produccion para no iniciar Mailpit:

```bash
docker compose -f docker-compose.yml -f docker-compose.aws.yml --env-file .env.production up --build -d
```

En local se puede seguir usando Mailpit con `.env` y `docker compose up --build`.

## Verificacion

1. Abrir el frontend publico.
2. Ejecutar "Olvidaste tu contrasena".
3. Confirmar en logs que SES acepta el mensaje.
4. Confirmar que el correo recibido apunta al dominio publico del frontend.
5. Abrir el enlace y restablecer la clave.
6. Iniciar sesion con la nueva clave.

Si SES rechaza el envio, el backend responde `503` con un error controlado y no marca como exitosa la solicitud.
