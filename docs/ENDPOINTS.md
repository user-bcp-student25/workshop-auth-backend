# Contrato de endpoints

## `POST /auth/login`

- **Content-Type:** `application/json`
- **Autenticación previa:** no requerida.
- **Solicitud:** `username` y `password` son cadenas requeridas; `client` es opcional y actualmente no altera el flujo.

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

- **`200 OK`:** devuelve `token`, `expiresIn` (segundos) y `user` (`username`, `displayName`, `role`); la respuesta actual también contiene `debug`.
- **`400 Bad Request`:** cuerpo JSON con `message` cuando la solicitud, el nombre de usuario o la contraseña son nulos/vacíos.
- **`401 Unauthorized`:** cuerpo JSON con `message` para credenciales no válidas.
- **`500 Internal Server Error`:** cuerpo JSON con `message` y `exception` si el flujo lanza una excepción.

## `GET /q/health`

- Endpoint de salud expuesto por la extensión Quarkus SmallRye Health.
- No requiere parámetros de solicitud según la configuración actual.