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
| `client` | string | Obligatorio. Solo se acepta `angular-workshop`; un valor ausente, vacio o distinto responde `400 Bad Request` antes de consultar credenciales. |

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
  "expiresIn": 3599,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

`token` es un JWT firmado con HS256 y se debe tratar como opaco. Incluye los claims `sub` (usuario), `client`, `iat` y `exp`, en segundos desde epoch. `expiresIn` expresa los segundos restantes y puede diferir de `exp - now` como maximo en un segundo. El valor de `displayName` se escapa como HTML antes de devolverse.

### Secreto de firma

`AUTH_TOKEN_SECRET` es una configuración sensible que debe proporcionarse externamente fuera del perfil de pruebas. No existe un secreto de producción predeterminado en el código. El valor `%test.auth.token.secret` de `application.properties` es una clave ficticia, exclusiva del sandbox de pruebas, y no debe reutilizarse fuera de él.

### Respuestas de error

| Estado | Condicion | Estructura del cuerpo |
| --- | --- | --- |
| `400 Bad Request` | Solicitud nula, `client` ausente/vacio/no permitido, o `username`/`password` ausente o vacio. | `{ "message": "..." }` |
| `401 Unauthorized` | Las credenciales no corresponden a un usuario del workshop. | `{ "message": "Invalid user <username> or password" }` |
| `500 Internal Server Error` | Una excepcion escapa del flujo de login. | `{ "message": "...", "exception": "<exception-class>" }` |

Los mensajes exactos mostrados arriba reflejan la implementacion actual. En particular, la respuesta `500` puede exponer detalles de la excepcion; es comportamiento heredado, no un contrato de errores recomendado.

## `GET /q/health`

Endpoint agregado de Quarkus SmallRye Health. Este proyecto no define comprobaciones de salud especificas de la aplicacion. La extension de health genera la respuesta e informa el estado del servicio.

## Issue #11: cambios respecto a la linea base

El flujo actual valida `client=angular-workshop` antes de consultar credenciales y emite un JWT con `sub`, `client`, `iat` y `exp`. `expiresIn` se deriva del mismo instante de expiracion. Se mantienen `POST /auth/login`, las claves `token`, `expiresIn`, `user` y `debug`, los campos `user.username`, `user.displayName` y `user.role`, `401` para credenciales invalidas y `400` para campos obligatorios ausentes o vacios.