# Diseño

## Contexto

Consulta `proposal.md` para la motivacion y `specs/auth-token/spec.md` para el comportamiento normativo.

El flujo actual de `POST /auth/login` acepta `client` pero lo ignora, devuelve un valor fijo de `expiresIn` de 3600 segundos y delega la creacion del token a un formato heredado que codifica en Base64 datos del usuario, un valor de expiracion y un secreto constante. La configuracion de la aplicacion no declara `AUTH_TOKEN_SECRET`, y la prueba actual del endpoint solo cubre un login exitoso. Esto difiere del comportamiento con HMAC y secreto externo descrito en el Issue #11; el codigo del repositorio es la linea base del estado actual, mientras que el issue y la spec definen el objetivo.

## Objetivos / No objetivos

**Objetivos:**
- Cumplir los requisitos observables de `specs/auth-token/spec.md` y preservar el contrato de login documentado, excepto por el cambio intencional de validacion de `client`.
- Mantener coherentes los datos de identidad y expiracion del token, y evitar que el material de firma y las credenciales aparezcan en logs.
- Limitar el cambio a autenticacion, manejo de tokens, configuracion, pruebas relacionadas y documentacion de endpoints.

**No objetivos:**
- Agregar un endpoint HTTP o cambiar la estructura de respuesta de `user`, el almacenamiento de autenticacion, las credenciales u otros endpoints.
- Redisenar la arquitectura de la aplicacion, CORS o persistencia.
- Implementar el cambio como parte de este artefacto de planificacion.

## Contratos protegidos

- Mantener `POST /auth/login`, las solicitudes y respuestas JSON, y las claves de respuesta `token`, `expiresIn`, `user` y `debug`.
- Preservar `user.username`, `user.displayName` y `user.role`.
- Preservar `401 Unauthorized` para credenciales invalidas y `400 Bad Request` para `username` o `password` ausentes o vacios.
- Cambiar intencionalmente el comportamiento actual que ignora `client`: solo se acepta `angular-workshop`; si falta o no esta permitido, se responde `400 Bad Request` antes de emitir el token.
- Tratar el token como opaco para los clientes; su codificacion interna heredada no es un contrato publico que deba preservarse o exponerse.

## Componentes afectados

- Validacion y coordinacion de la solicitud de login.
- Emision del token y limite de validacion necesario para rechazar tokens expirados. No se agrega una ruta publica.
- Configuracion del secreto y comportamiento de logs en el flujo de autenticacion.
- Pruebas de caracterizacion y aceptacion, ademas de `docs/ENDPOINTS.md`.

## Decisiones

1. **Usar una biblioteca JWT/JWS mantenida y compatible con Quarkus en lugar del formato Base64 personalizado.** Los claims y la semantica de expiracion requeridos son responsabilidades estandar de los tokens; una biblioteca evita implementar manualmente la firma y validacion criptograficas. Se considero un formato HMAC propio, pero se descarto por el riesgo de errores en la firma, verificacion o manejo de claims. La biblioteca y version compatibles concretas pueden seleccionarse durante la revision de dependencias sin cambiar esta direccion tecnica.

	La revision de dependencias selecciono `com.auth0:java-jwt:4.5.0`. El repositorio usa JWS con HS256 (HMAC-SHA256): se conserva HMAC como algoritmo de firma, pero se sustituye el formato Base64 personalizado por un formato JWT estandar.

2. **Usar un unico instante de expiracion como origen de `exp` y `expiresIn`.** Representar `iat` y `exp` como segundos desde epoch. Derivar `expiresIn` del mismo instante de expiracion y del tiempo actual al construir la respuesta; permitir una diferencia maxima de un segundo, como establece la spec. Esto evita que duraciones configuradas por separado diverjan.

3. **Validar el cliente antes de consultar credenciales y emitir el token.** Aceptar exactamente `angular-workshop`; responder `400 Bad Request` si falta, esta vacio o tiene otro valor. Este es el cambio de compatibilidad explicito en la spec; las credenciales invalidas con un cliente valido siguen respondiendo `401 Unauthorized`.

4. **Eliminar los valores predeterminados de secretos de firma incorporados en el codigo.** Leer `AUTH_TOKEN_SECRET` desde configuracion externa, no registrar su valor y exigir que este configurado en produccion en lugar de recurrir silenciosamente a un valor incluido en el codigo. La configuracion documentada del sandbox debe identificar esta variable como sensible. Esto resuelve la discrepancia entre el codigo fuente y el issue sin conservar la constante actual en el comportamiento objetivo.

5. **Probar la expiracion en el limite de validacion del token sin agregar una ruta.** El backend actual no expone una ruta que consuma tokens. Se probara el comportamiento de expiracion mediante el mismo componente de validacion usado por los tokens, mientras las pruebas del contrato de login verifican `exp` y `expiresIn`. Agregar una ruta protegida solo para hacer accesible esta prueba queda fuera del alcance.

## Estrategia incremental y pruebas de caracterizacion

1. Antes de cambiar el comportamiento, ampliar la caracterizacion de las claves actuales de respuesta exitosa y los campos de `user`, credenciales ausentes o vacias (`400`) y credenciales invalidas (`401`). Tratar `token` como opaco; no guardar su valor ni el secreto actual en snapshots. Registrar que hoy se ignora `client` para que su rechazo posterior quede identificado como cambio intencional.
2. Agregar pruebas de aceptacion focalizadas para claims requeridos, clientes permitidos y rechazados, tolerancia de un segundo entre `expiresIn` y `exp`, rechazo de tokens expirados y ausencia de tokens, secretos, contrasenas u otros datos sensibles en logs.
3. Cambiar autenticacion y configuracion de forma incremental, manteniendo cubiertos por las pruebas de caracterizacion los campos de respuesta y estados HTTP que se preservan.
4. Actualizar `docs/ENDPOINTS.md` para describir el contrato observable final y la configuracion del secreto. Ejecutar las pruebas focalizadas de autenticacion antes de una validacion mas amplia del proyecto.

## Riesgos / Compensaciones

- [Los consumidores pueden depender del formato interno no documentado del token heredado] -> Mantener estable y opaco el campo de respuesta y verificar la compatibilidad de consumidores antes del despliegue.
- [La falta del secreto de produccion puede impedir el inicio o la emision de tokens] -> Documentar la configuracion obligatoria y validarla antes del despliegue; no restaurar silenciosamente un valor predeterminado hardcodeado.
- [La allowlist solo incluye `angular-workshop`] -> Incorporar futuros IDs de cliente mediante un cambio explicito del contrato, en vez de aceptar valores arbitrarios.
- [Este backend no tiene una ruta publica que consuma tokens] -> Mantener la validacion de expiracion interna y probarla en ese limite; no agregar un endpoint fuera del alcance aprobado.
- [Los timestamps en segundos pueden generar diferencias en los limites] -> Derivar ambos valores de un mismo instante de expiracion y aplicar la tolerancia especificada de un segundo.

## Plan de migracion y reversa

No se espera una migracion de base de datos. Desplegar la configuracion del secreto junto con el cambio del emisor y validador; verificar el cliente aceptado y el contrato de respuesta antes de exponer la version a los consumidores. Si el despliegue causa una regresion, revertir el emisor y el validador como una misma unidad versionada en el laboratorio aislado. No reintroducir el secreto constante ni desplegar el formato heredado como alternativa para produccion.

## Preguntas abiertas

- La biblioteca JWT/JWS y su version compatibles concretas quedan pendientes de la revision de dependencias durante la implementacion; esto no cambia la direccion elegida de usar una biblioteca ni la spec.