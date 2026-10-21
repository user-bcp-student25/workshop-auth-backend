# Spec Delta

## Purpose

Define el contrato observable de los tokens emitidos durante el login, incluidos el cliente autenticado, la identidad, la expiracion y las restricciones de seguridad asociadas.

## ADDED Requirements

### Requirement: El token de login identifica al usuario y al cliente

El sistema MUST emitir, para un login exitoso, un token con los claims `sub`, `client`, `iat` y `exp`. `sub` MUST identificar al usuario autenticado y `client` MUST coincidir con el cliente validado de la solicitud.

#### Scenario: Login exitoso incluye claims requeridos
- **WHEN** se envia un login con credenciales validas y `client` igual a `angular-workshop`
- **THEN** la respuesta es `200 OK` y el token contiene `sub`, `client`, `iat` y `exp`
- **THEN** `sub` identifica al usuario autenticado y `client` es `angular-workshop`

### Requirement: El login valida el cliente solicitado

El sistema MUST aceptar unicamente el valor `angular-workshop` para `client` y MUST rechazar solicitudes sin ese valor o con un valor distinto antes de emitir un token.

#### Scenario: Cliente permitido
- **WHEN** la solicitud contiene `client` igual a `angular-workshop` y las credenciales son validas
- **THEN** el login puede completarse y el token refleja ese cliente

#### Scenario: Cliente ausente o no permitido
- **WHEN** la solicitud no contiene `client`, lo envia vacio o envia un valor distinto de `angular-workshop`
- **THEN** el servicio responde `400 Bad Request` y no emite un token

### Requirement: La duracion del token coincide con su expiracion

El sistema MUST informar `expiresIn` en segundos y MUST mantener la diferencia absoluta entre `expiresIn` y `exp - now` en no mas de 1 segundo.

#### Scenario: Duracion restante dentro de tolerancia
- **WHEN** se recibe una respuesta de login exitoso
- **THEN** `expiresIn` y `exp - now` difieren como maximo en 1 segundo

### Requirement: Los tokens expirados se consideran invalidos

El sistema MUST considerar expirado un token cuando `exp` es igual o anterior al instante actual y MUST rechazarlo cuando se valide su vigencia.

#### Scenario: Validacion de token expirado
- **WHEN** se valida un token cuyo `exp` es igual o anterior al instante actual
- **THEN** el token se rechaza como expirado

### Requirement: El contrato de login existente se preserva salvo cambios comprometidos

El sistema MUST conservar `POST /auth/login`, el formato JSON, los campos de usuario `username`, `displayName` y `role`, y los campos de respuesta existentes `token`, `expiresIn`, `user` y `debug`. Las credenciales invalidas MUST seguir respondiendo `401 Unauthorized`; los campos `username` o `password` ausentes o vacios MUST seguir respondiendo `400 Bad Request`.

#### Scenario: Login valido conserva la respuesta de usuario
- **WHEN** un usuario valido inicia sesion con `client` permitido
- **THEN** la respuesta `200 OK` incluye `token`, `expiresIn`, `user` y `debug`
- **THEN** `user` conserva `username`, `displayName` y `role`

#### Scenario: Credenciales invalidas conservan el estado HTTP
- **WHEN** se envia `client` permitido junto con credenciales invalidas
- **THEN** el servicio responde `401 Unauthorized` y no emite un token

#### Scenario: Credenciales obligatorias ausentes conservan el estado HTTP
- **WHEN** `username` o `password` falta o esta vacio
- **THEN** el servicio responde `400 Bad Request`

### Requirement: Los secretos y tokens no se exponen en logs

El sistema MUST NOT registrar el token emitido, el secreto de firma ni credenciales u otros valores sensibles durante el login.

#### Scenario: Login sin datos sensibles en logs
- **WHEN** se procesa un login exitoso o fallido
- **THEN** los logs no contienen el token, `AUTH_TOKEN_SECRET`, contrasenas ni otros valores sensibles de la solicitud

### Requirement: La configuracion del secreto se documenta para el entorno de laboratorio

La documentacion MUST identificar `AUTH_TOKEN_SECRET` como configuracion sensible y MUST indicar que cualquier valor predeterminado es exclusivo del sandbox de laboratorio y no debe usarse fuera de el.

#### Scenario: Documentacion del secreto y su alcance
- **WHEN** se consulta la documentacion de configuracion de autenticacion
- **THEN** se identifica `AUTH_TOKEN_SECRET` como secreto y cualquier valor predeterminado queda expresamente limitado al sandbox de laboratorio