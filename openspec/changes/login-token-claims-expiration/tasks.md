# Tasks

## 1. Caracterizacion previa del contrato

- [ ] 1.1 Ampliar `AuthResourceTest` para caracterizar antes del cambio el login exitoso (`token`, `expiresIn`, `user`, `debug` y campos `username`, `displayName`, `role`), credenciales ausentes o vacias (`400`) y credenciales invalidas (`401`); verificar con `mvn test -Dtest=AuthResourceTest` antes de modificar comportamiento funcional.

## 2. Validacion del cliente

- [ ] 2.1 Agregar pruebas para `client=angular-workshop` y para cliente ausente, vacio o no permitido; comprobar que los no permitidos responden `400` y no emiten token, ejecutando `mvn test -Dtest=AuthResourceTest`.
- [ ] 2.2 Validar `client` contra la allowlist exacta antes de consultar credenciales o emitir token; actualizar `docs/ENDPOINTS.md` para reflejar el contrato final y verificar ambas rutas con `mvn test -Dtest=AuthResourceTest`.

## 3. Emision del token y configuracion del secreto

- [ ] 3.1 Seleccionar durante la revision de dependencias una biblioteca JWT/JWS mantenida y compatible con Quarkus, agregar la dependencia y configurar `AUTH_TOKEN_SECRET` externamente sin valor predeterminado embebido; documentar el secreto como sensible y exclusivo del sandbox cuando aplique, y verificar la resolucion con `mvn -DskipTests compile`.
- [ ] 3.2 Emitir tokens con `sub`, `client`, `iat` y `exp`, derivados de la identidad autenticada y del cliente validado; derivar `expiresIn` del mismo instante de expiracion con tolerancia maxima de un segundo; mantener la respuesta existente y agregar pruebas de claims/duracion. Actualizar `docs/ENDPOINTS.md` y verificar con `mvn test -Dtest=AuthResourceTest`.
- [ ] 3.3 Verificar mediante pruebas de configuracion que la emision en produccion no usa un secreto por defecto cuando falta `AUTH_TOKEN_SECRET` y que la configuracion del sandbox queda documentada; ejecutar las pruebas focalizadas de configuracion/autenticacion.

## 4. Expiracion y proteccion de datos sensibles

- [ ] 4.1 Agregar o completar la validacion interna para rechazar tokens con `exp` igual o anterior al instante actual, sin agregar una ruta publica; incluir la prueba de token expirado y verificarla con la prueba focalizada del validador.
- [ ] 4.2 Asegurar que el flujo de login no registre tokens, `AUTH_TOKEN_SECRET`, contrasenas ni otros valores sensibles; agregar o actualizar pruebas de logs para login exitoso y fallido y ejecutarlas junto con las pruebas focalizadas de autenticacion.

## 5. Verificacion integrada

- [ ] 5.1 Ejecutar `mvn verify` para comprobar en conjunto el contrato preservado, claims, allowlist, expiracion y proteccion de logs; ejecutar `openspec validate "login-token-claims-expiration" --strict` para confirmar que los artefactos OpenSpec siguen siendo validos.