# Endpoints de la API

Este documento registra el contrato HTTP observable en la implementacion actual. Es una linea base, no afirma que ya este implementado el comportamiento de seguridad solicitado en el Issue #11 de GitHub.

## Servicio

- URL base local: `http://localhost:8080`
- Formato de solicitudes y respuestas: JSON
- En local, el servicio escucha en `127.0.0.1`. El perfil de produccion se enlaza a `0.0.0.0`.

## `POST /auth/login`

Autentica a un usuario del workshop y devuelve un token y datos basicos del usuario.

### Solicitud

Tipo de contenido: `application/json`

| Campo | Tipo | Comportamiento actual |
| --- | --- | --- |
| `username` | string | Obligatorio; no puede ser nulo ni estar vacio. |
| `password` | string | Obligatorio; no puede ser nulo ni estar vacio. |
| `client` | string | Se acepta, pero actualmente no se valida ni se utiliza. El Issue #11 compromete a validarlo e incluirlo en el token. |

Ejemplo:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

La configuracion actual de Jackson ignora los campos JSON desconocidos.

### Respuesta exitosa: `200 OK`

La respuesta contiene `token`, `expiresIn`, `user` y el campo `debug` heredado que existe actualmente.

```json
{
  "token": "<opaque-token>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

`expiresIn` se expresa en segundos. El token actual utiliza un formato heredado y no constituye un contrato JWT; los clientes deben tratarlo como opaco. El valor de `displayName` se escapa como HTML antes de devolverse.

### Respuestas de error

| Estado | Condicion | Estructura del cuerpo |
| --- | --- | --- |
| `400 Bad Request` | Solicitud nula, o `username`/`password` ausente o vacio. | `{ "message": "..." }` |
| `401 Unauthorized` | Las credenciales no corresponden a un usuario del workshop. | `{ "message": "Invalid user <username> or password" }` |
| `500 Internal Server Error` | Una excepcion escapa del flujo de login. | `{ "message": "...", "exception": "<exception-class>" }` |

Los mensajes exactos mostrados arriba reflejan la implementacion actual. En particular, la respuesta `500` puede exponer detalles de la excepcion; es comportamiento heredado, no un contrato de errores recomendado.

## `GET /q/health`

Endpoint agregado de Quarkus SmallRye Health. Este proyecto no define comprobaciones de salud especificas de la aplicacion. La extension de health genera la respuesta e informa el estado del servicio.

## Issue #11: cambios comprometidos respecto a la linea base

[GitHub Issue #11](https://github.com/user-bcp-student25/workshop-auth-backend/issues/11) solicita cambiar el contrato del token de login: un token valido debe incluir `sub`, `client`, `iat` y `exp`; `client` debe obtenerse de la solicitud y validarse; y `expiresIn` debe coincidir con `exp - now` dentro de una tolerancia documentada. Tambien requiere pruebas de login exitoso, credenciales invalidas y tokens expirados, documentacion de `AUTH_TOKEN_SECRET`, logs redactados y una decision registrada sobre HMAC frente a una biblioteca JWT.

Estos son requisitos objetivo, no el comportamiento actual. La solicitud ya acepta `client`, pero lo ignora; el token actual no cumple el contrato de claims requerido y la respuesta utiliza un `expiresIn` fijo de 3600 segundos. El issue no solicita cambiar la ruta de login ni los campos existentes de la respuesta `user`; se preservan como parte del comportamiento actual, salvo que se revise el alcance del issue.