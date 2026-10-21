# Hallazgos técnicos

**Fecha:** 2026-10-21  
**Alcance:** revisión documental de los hallazgos sembrados para este backend Quarkus.  
**Resultado:** se documentan observaciones confirmadas en los archivos locales; no se modificó código.

## Resumen

| ID | Área | Hallazgo | Estado |
|---|---|---|---|
| H-01 | Dependencias (SCA) | El proyecto declara Quarkus `3.27.4` y Commons Text `1.9`; falta validar advisories aplicables. | Versiones confirmadas; CVEs pendientes de verificación |
| H-02 | Persistencia | Las contraseñas de laboratorio se almacenan y comparan en texto plano. | Confirmado |
| H-03 | SQL | La consulta de autenticación concatena directamente los valores de usuario y contraseña. | Confirmado |
| H-04 | Token | El token se codifica en Base64 y contiene un secreto embebido, sin firma criptográfica. | Confirmado |
| H-05 | Configuración | CORS acepta cualquier origen mediante una expresión permisiva. | Confirmado |
| H-06 | Diseño | `AuthResource.login` concentra validación, acceso a datos, emisión de token y construcción de respuesta. | Confirmado |
| H-07 | Errores | Las respuestas y la salida de diagnóstico pueden exponer detalles internos de excepciones. | Confirmado |

## Detalle

### H-01: Dependencias pendientes de validación SCA

El manifiesto declara Quarkus `3.27.4` y Apache Commons Text `1.9`. Esto confirma las versiones indicadas por el reto, pero no demuestra por sí mismo qué CVEs les aplican en este proyecto ni si son explotables en su contexto.

- **Impacto potencial:** uso de componentes con vulnerabilidades conocidas, sujeto a confirmar mediante análisis de dependencias y advisories vigentes.
- **Evidencia:** [`pom.xml`](pom.xml).
- **Seguimiento:** ejecutar una evaluación SCA sobre el árbol efectivo de dependencias y registrar advisory, versión afectada, alcance y versión corregida. No se atribuye aquí ningún CVE específico.

### H-02: Contraseñas almacenadas en texto plano

La tabla `legacy_users` almacena la contraseña como texto y las cuentas de laboratorio se inicializan con contraseñas legibles. La autenticación compara directamente el valor recibido con la columna.

- **Impacto:** una exposición de la base de datos también revelaría las contraseñas almacenadas.
- **Evidencia:** [`LegacyDatabase.java`](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java).
- **Seguimiento:** definir una migración a hashes de contraseña con salt y algoritmo lento, preservando el contrato de login aprobado.

### H-03: Consulta SQL construida por concatenación

`findByCredentials` incorpora `username` y `password` en una cadena SQL y la ejecuta mediante `Statement`.

- **Impacto:** la consulta no separa datos de la estructura SQL y queda expuesta a manipulación de la consulta a través de entradas.
- **Evidencia:** [`LegacyDatabase.java`](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java).
- **Seguimiento:** reemplazar la concatenación por parámetros enlazados con `PreparedStatement` y añadir pruebas de caracterización.

### H-04: Token reversible sin firma

`LegacyTokenUtil` forma el contenido del token como texto delimitado, incluye el secreto constante `WORKSHOP_SECRET_2026` y codifica el resultado en Base64. Base64 no proporciona firma ni integridad criptográfica.

- **Impacto:** el token no ofrece garantías criptográficas de autenticidad o integridad y revela su estructura al decodificarse.
- **Evidencia:** [`LegacyTokenUtil.java`](src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java).
- **Seguimiento:** acordar el formato y la estrategia de firma, externalizar el secreto y probar expiración y validación de claims antes de cambiar el contrato.

### H-05: Política CORS permisiva

La configuración de CORS utiliza `/.*/` como expresión de orígenes permitidos.

- **Impacto:** la política permite solicitudes desde cualquier origen que coincida con la expresión, en lugar de restringirlas a los clientes autorizados.
- **Evidencia:** [`application.properties`](src/main/resources/application.properties).
- **Seguimiento:** definir y externalizar una lista explícita de orígenes permitidos por entorno.

### H-06: Responsabilidades concentradas en el resource

`AuthResource.login` realiza validaciones, consulta la base de datos, genera el token, transforma datos de respuesta y maneja excepciones en un método con condiciones anidadas.

- **Impacto:** aumenta el acoplamiento y hace más difícil probar y modificar cada responsabilidad de forma aislada.
- **Evidencia:** [`AuthResource.java`](src/main/java/com/bcp/workshop/auth/AuthResource.java).
- **Seguimiento:** separar responsabilidades en cambios pequeños y revisables, conservando el contrato observable documentado en [`ENDPOINTS.md`](docs/ENDPOINTS.md).

### H-07: Exposición de detalles internos en errores

El endpoint devuelve el mensaje y la clase de la excepción en respuestas `500`. La inicialización y consulta de base de datos también imprimen trazas de excepción directamente.

- **Impacto:** los consumidores pueden recibir detalles internos y los registros pueden contener información de diagnóstico no controlada.
- **Evidencia:** [`AuthResource.java`](src/main/java/com/bcp/workshop/auth/AuthResource.java) y [`LegacyDatabase.java`](src/main/java/com/bcp/workshop/auth/LegacyDatabase.java).
- **Seguimiento:** responder con mensajes genéricos, registrar errores mediante logging controlado y evitar incluir secretos o credenciales en logs.

## Limitaciones

- Los hallazgos reflejan el estado inspeccionado de los archivos locales el 2026-10-21; no representan una nueva consulta a GitHub.
- La revisión no ejecutó un escáner SCA ni verificó CVEs específicos.
- Este documento registra observaciones y seguimiento recomendado; no implementa remediaciones ni sustituye la aprobación de un issue o una decisión técnica.
