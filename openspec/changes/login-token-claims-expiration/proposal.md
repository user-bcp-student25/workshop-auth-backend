# Propuesta

## Por qué

El Issue [#11](https://github.com/user-bcp-student25/workshop-auth-backend/issues/11) identifica que el token de login no expresa una identidad, un cliente ni una expiracion verificables, y solicita evitar la exposicion de secretos y tokens en logs. Sin un contrato preciso, los consumidores no pueden confiar en la identidad ni determinar de forma coherente la vigencia del token.

## Qué cambia

- Definir el contrato verificable del token emitido por `POST /auth/login`, con los claims `sub`, `client`, `iat` y `exp`.
- Validar el `client` recibido en la solicitud. Para este alcance, solo `angular-workshop` es valido; un valor ausente o no permitido se rechaza como solicitud invalida (`400`).
- Exigir que `expiresIn` coincida con `exp - now` con una diferencia maxima de 1 segundo.
- Establecer que los tokens expirados no son aceptados por la validacion de tokens y que los escenarios de autenticacion invalida y expiracion tienen evidencia de prueba.
- Exigir que `AUTH_TOKEN_SECRET` quede documentado como configuracion sensible y que ningun secreto ni token aparezca en los logs. Cualquier secreto predeterminado, si se conserva, debe estar identificado y limitado al sandbox de laboratorio.
- Registrar, en una etapa posterior de diseño, la decision entre conservar HMAC o adoptar una biblioteca JWT. Esta propuesta no toma esa decision.

## Capacidades

### Capacidades nuevas

- `auth-token`: contrato observable de emision, claims, cliente y expiracion de los tokens usados por el flujo de autenticacion.

### Capacidades modificadas

Ninguna. El repositorio no tiene specs existentes; se agrega una capacidad nueva.

## Impacto

- Contrato documentado de `POST /auth/login` y comportamiento del token para sus consumidores.
- Configuracion y documentacion de `AUTH_TOKEN_SECRET`, ademas de la evidencia de pruebas y logs.
- `docs/ENDPOINTS.md` es la linea base: se preservan la ruta de login, el formato JSON, los campos existentes de `user` (`username`, `displayName`, `role`) y el resultado `401` para credenciales invalidas. El `client` actualmente se acepta pero se ignora; exigir y validar `angular-workshop` es el cambio intencional comprometido por el Issue #11. La respuesta mantiene `token`, `expiresIn` y los demas campos existentes salvo que una spec delta justifique expresamente un cambio.
- Fuera de alcance: agregar rutas o capacidades nuevas, cambiar las credenciales o el formato de `user`, elegir o diseñar la tecnologia de firma, y modificar otros endpoints.