# Tasks

## 1. Caracterizacion previa del contrato

- [x] 1.1 Ampliar `AuthResourceTest` para caracterizar antes del cambio el login exitoso (`token`, `expiresIn`, `user`, `debug` y campos `username`, `displayName`, `role`), credenciales ausentes o vacias (`400`) y credenciales invalidas (`401`); verificar con `mvn test -Dtest=AuthResourceTest` antes de modificar comportamiento funcional. Resultado: `mvn test -Dtest=AuthResourceTest` — `BUILD SUCCESS`, 7 pruebas, 0 fallos, 0 errores. JaCoCo 0.8.12 mostró avisos de instrumentación con el JDK 25 del entorno.

## 2. Validacion del cliente

- [x] 2.1 Agregar pruebas para `client=angular-workshop` y para cliente ausente, vacio o no permitido; comprobar que los no permitidos responden `400` y no emiten token, ejecutando `mvn test -Dtest=AuthResourceTest`. Resultado: 11 pruebas, 0 fallos, 0 errores.
- [x] 2.2 Validar `client` contra la allowlist exacta antes de consultar credenciales o emitir token; actualizar `docs/ENDPOINTS.md` para reflejar el contrato final y verificar ambas rutas con `mvn test -Dtest=AuthResourceTest`. Resultado: 11 pruebas, 0 fallos, 0 errores.

## 3. Emision del token y configuracion del secreto

- [x] 3.1 Seleccionar durante la revision de dependencias una biblioteca JWT/JWS mantenida y compatible con Quarkus, agregar la dependencia y configurar `AUTH_TOKEN_SECRET` externamente sin valor predeterminado embebido; documentar el secreto como sensible y exclusivo del sandbox cuando aplique, y verificar la resolucion con `mvn -DskipTests compile`. Resultado: `com.auth0:java-jwt:4.5.0` resuelto; `mvn -DskipTests compile` — `BUILD SUCCESS`.
- [x] 3.2 Emitir tokens con `sub`, `client`, `iat` y `exp`, derivados de la identidad autenticada y del cliente validado; derivar `expiresIn` del mismo instante de expiracion con tolerancia maxima de un segundo; mantener la respuesta existente y agregar pruebas de claims/duracion. Actualizar `docs/ENDPOINTS.md` y verificar con `mvn test -Dtest=AuthResourceTest`. Resultado: `AuthResourceTest` 12 pruebas y `TokenServiceTest` 5 pruebas, ambas con 0 fallos y 0 errores.
- [x] 3.3 Verificar mediante pruebas de configuracion que la emision en produccion no usa un secreto por defecto cuando falta `AUTH_TOKEN_SECRET` y que la configuracion del sandbox queda documentada; ejecutar las pruebas focalizadas de configuracion/autenticacion. Resultado: `TokenServiceTest.doesNotIssueTokenWithoutConfiguredSigningSecret` pasa; `AUTH_TOKEN_SECRET` no tiene valor predeterminado productivo y `%test.auth.token.secret` está documentado como test-only.

## 4. Expiracion y proteccion de datos sensibles

- [x] 4.1 Agregar o completar la validacion interna para rechazar tokens con `exp` igual o anterior al instante actual, sin agregar una ruta publica; incluir la prueba de token expirado y verificarla con la prueba focalizada del validador. Resultado: `mvn test -Dtest=TokenServiceTest` — 5 pruebas, 0 fallos, 0 errores; cubre `exp == now` y `exp < now` sin ruta HTTP nueva.
- [x] 4.2 Asegurar que el flujo de login no registre tokens, `AUTH_TOKEN_SECRET`, contrasenas ni otros valores sensibles; agregar o actualizar pruebas de logs para login exitoso y fallido y ejecutarlas junto con las pruebas focalizadas de autenticacion. Resultado: `mvn test -Dtest=AuthResourceLogTest` — 2 pruebas, 0 fallos, 0 errores.

## 5. Verificacion integrada

- [x] 5.1 Ejecutar `mvn verify` para comprobar en conjunto el contrato preservado, claims, allowlist, expiracion y proteccion de logs; ejecutar `openspec validate "login-token-claims-expiration" --strict` para confirmar que los artefactos OpenSpec siguen siendo validos. Resultado: `mvn verify` — código 0, 23 pruebas, 0 fallos, 0 errores; `openspec validate "login-token-claims-expiration" --strict` — válido; reporte JaCoCo generado, bundle 364/384 instrucciones (94.8%), `TokenService` 86.5%, `AuthResource` 98.6% y `LegacyDatabase` 97%.