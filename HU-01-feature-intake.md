# Feature Intake -> Spec Checklist

## Summary
- Definir un token firmado con claims mínimos, expiración comprobable y una clave configurada externamente; conservar el flujo actual de login.
- Métrica de impacto: no especificada en la HU.

## Scope
- **In scope:** incluir `sub`, `client`, `iat` y `exp`; obtener y validar `client`; comprobar la coherencia de `expiresIn` con `exp`; cubrir login válido, credenciales inválidas y token expirado; evitar registrar valores sensibles.
- **Out of scope:** cambios a otros endpoints o definición de una tecnología concreta para firmar tokens.

## Assumptions
- Ninguna. El algoritmo, la tolerancia de expiración, las reglas de `client` y la política de configuración aún no están definidos.

## Technical decisions to make
- Resolver si se conserva HMAC o se adopta una librería JWT.
- Definir la política para el secreto fuera del sandbox: eliminar el valor por defecto o documentarlo como exclusivo del laboratorio.
- Especificar validación de `client`, tolerancia temporal y semántica de `expiresIn`.
- Determinar dónde se valida la expiración del token y cómo se manejan tokens expirados.

## Risks and mitigations
- **Claims o expiración inconsistentes:** comprobar los claims y contrastar `expiresIn` con `exp - now` mediante pruebas con la tolerancia acordada.
- **Exposición de valores sensibles:** verificar que los flujos de login y error no registren tokens, secretos ni otros valores sensibles.
- **Reglas ambiguas para `client` o configuración:** acordarlas antes de implementar y cubrir los casos válidos e inválidos con pruebas.

## Acceptance criteria
- Una prueba verifica que el token emitido contiene `sub`, `client`, `iat` y `exp`, y que `client` corresponde al valor de la solicitud tras su validación.
- Una prueba verifica que `expiresIn` coincide con `exp - now` dentro de la tolerancia documentada.
- Hay pruebas automatizadas para login válido, credenciales inválidas y token expirado; todas pasan.
- Las pruebas de firma verifican un token válido y detectan una alteración de su contenido o firma.
- La evidencia de ejecución confirma que no se registran tokens ni secretos.
- La decisión entre HMAC y librería JWT queda documentada.

## Validation plan
- **Unit tests:** claims, validación de `client`, firma, expiración y coherencia de `expiresIn`.
- **Functional tests:** login válido, credenciales inválidas y token expirado.
- **Security checks:** revisar la configuración para evitar un secreto fijo fuera de la política acordada; inspeccionar logs de prueba para detectar valores sensibles.
- **Evidence required:** salida de pruebas, decisión técnica documentada, configuración descrita sin valores sensibles y evidencia de logs redactados. Referenciar también el `SKILL.md` versionado, sus ejemplos de entrada/salida y su tag de versión.

## Suggested tasks by role
- **Chapter/PO:** confirmar el resultado esperado para `client` ausente o inválido; acordar el comportamiento esperado de login con credenciales inválidas y token expirado.
- **Tech Lead:** decidir HMAC o librería JWT; definir política del secreto; acordar tolerancia temporal y semántica de `expiresIn`.
- **Backend:** emitir los claims requeridos; validar `client` y expiración; configurar el secreto según la decisión y evitar registrar valores sensibles.
- **Frontend:** confirmar qué valor de `client` envía el flujo actual; verificar compatibilidad con la respuesta de login tras acordar las reglas.
- **QA:** preparar pruebas de claims y `client`; probar coherencia y expiración; verificar login válido, inválido y ausencia de valores sensibles en logs.
- **Cyber:** revisar la decisión de firma y manejo de expiración; revisar la política de configuración del secreto y la evidencia de logs redactados.

## Open questions
- ¿Se conservará HMAC o se usará una librería JWT?
- ¿Qué reglas determinan si `client` es válido y qué ocurre si falta?
- ¿Cuál es la tolerancia aceptada al comparar `expiresIn` con `exp - now`?
- ¿Qué política aplica al secreto fuera del sandbox?
- ¿Qué componente valida el token y rechaza los expirados?
- ¿Qué representa exactamente `sub` y cuál es el comportamiento esperado para credenciales inválidas y tokens expirados?
