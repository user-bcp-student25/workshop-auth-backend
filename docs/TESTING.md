# Pruebas — workshop-auth-backend

Matriz basada únicamente en el contrato actual de [ENDPOINTS.md](ENDPOINTS.md). Los cambios futuros descritos allí para el Issue #11 no se tratan como comportamiento ya implementado.

## Matriz de pruebas

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `POST /auth/login` | `student` con credenciales válidas y cliente informado | Feliz | `200`; token opaco no nulo, `expiresIn=3600`, `user.username/displayName/role` y `debug` conservados |
| `POST /auth/login` | Cliente omitido | Borde | `200`; el cliente es opcional en la línea base actual |
| `POST /auth/login` | Campo JSON desconocido | Borde | `200`; el campo se ignora |
| `POST /auth/login` | Solicitud JSON `null` | Error | `400`; `message` es `Request is null` |
| `POST /auth/login` | `username` ausente o vacío | Error | `400`; `message` es `Username is required` |
| `POST /auth/login` | `password` ausente o vacío | Error | `400`; `message` es `Password is required` |
| `POST /auth/login` | Contraseña incorrecta | Error | `401`; incluye `message` y no emite `token` |
| `POST /auth/login` | Tautología SQL en contraseña | Error | Contrato esperado `401`, sin token; **actualmente falla**: responde `200` |
| `POST /auth/login` | Excepción durante la consulta de credenciales (doble test-only) | Error | `500`; incluye un campo `message` |
| `GET /q/health` | Servicio disponible | Feliz | `200`; `status=UP` |
| `GET /q/health` | Sin checks de salud propios de la aplicación | Borde | `200`; `checks` está vacío |
| `GET /q/health` | Respuestas de error | Error | ENDPOINTS no documenta respuestas de error para este endpoint; no se inventa un caso |

## Código de los tests

Fuentes: [AuthResourceTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceTest.java), [HealthEndpointTest.java](../src/test/java/com/bcp/workshop/auth/HealthEndpointTest.java), [AuthResourceFailureTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceFailureTest.java) y [LoginFailureTestProfile.java](../src/test/java/com/bcp/workshop/auth/LoginFailureTestProfile.java).

### AuthResourceTest

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {
	@Test
	void logsInWithWorkshopUser() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(200)
				.body("token", notNullValue())
				.body("expiresIn", is(3600))
				.body("user.username", is("student"))
				.body("user.displayName", is("Workshop Student"))
				.body("user.role", is("STUDENT"))
				.body("debug", is("authenticated-with-legacy-flow"));
	}

	@Test
	void acceptsLoginWithoutClientAsCurrentBehavior() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(200)
				.body("token", notNullValue());
	}

	@Test
	void ignoresUnknownRequestFields() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\",\"extra\":\"ignored\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(200)
				.body("token", notNullValue());
	}

	@Test
	void rejectsNullRequest() {
		given()
				.contentType("application/json")
				.body("null")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("message", is("Request is null"));
	}

	@Test
	void rejectsMissingUsername() {
		given()
				.contentType("application/json")
				.body("{\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("message", is("Username is required"));
	}

	@Test
	void rejectsEmptyUsername() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("message", is("Username is required"));
	}

	@Test
	void rejectsMissingPassword() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("message", is("Password is required"));
	}

	@Test
	void rejectsEmptyPassword() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("message", is("Password is required"));
	}

	@Test
	void rejectsInvalidCredentialsWithoutIssuingToken() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"wrong-password\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(401)
				.body("message", is("Invalid user student or password"))
				.body("token", nullValue());
	}

	@Test
	void rejectsSqlTautologyAsInvalidCredentials() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"x' OR '1'='1\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(401)
				.body("message", is("Invalid user student or password"))
				.body("token", nullValue());
	}
}
```

### HealthEndpointTest

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
class HealthEndpointTest {
	@Test
	void reportsServiceUp() {
		given()
				.when().get("/q/health")
				.then()
				.statusCode(200)
				.body("status", is("UP"));
	}

	@Test
	void reportsNoApplicationHealthChecks() {
		given()
				.when().get("/q/health")
				.then()
				.statusCode(200)
				.body("checks", empty());
	}
}
```

### AuthResourceFailureTest y soporte de perfil

```java
package com.bcp.workshop.auth;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(LoginFailureTestProfile.class)
class AuthResourceFailureTest {
	@Test
	void returnsDocumented500WhenLoginFlowThrows() {
		given()
				.contentType("application/json")
				.body(Map.of("username", "student", "password", "Workshop123!"))
				.when().post("/auth/login")
				.then()
				.statusCode(500)
				.body("message", notNullValue());
	}
}

@Alternative
@Priority(1)
@ApplicationScoped
@IfBuildProfile("login-failure-test")
class FailingLegacyDatabase extends LegacyDatabase {
	@Override
	public UserRecord findByCredentials(String username, String password) {
		throw new IllegalStateException("simulated login failure");
	}
}
```

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTestProfile;

public class LoginFailureTestProfile implements QuarkusTestProfile {
	@Override
	public String getConfigProfile() {
		return "login-failure-test";
	}
}
```

## Ejecuciones y cobertura

Directorio de ejecución: raíz del repositorio (`workshop-auth-backend/`). El informe de JaCoCo se genera en `target/site/jacoco/index.html` y el CSV en `target/site/jacoco/jacoco.csv`.

| comando | código de salida | resultado |
|---|---:|---|
| `mvn test "-Dtest=AuthResourceTest#ignoresUnknownRequestFields"` | 0 | 1 test, 0 fallos, 0 errores |
| `mvn test -Dtest=AuthResourceFailureTest` | 0 | 1 test, 0 fallos, 0 errores |
| `mvn test -Dtest=AuthResourceTest` | 1 | 10 tests; falla el caso SQL-tautology: esperaba `401`, recibió `200` |
| `mvn verify` | 1 | 13 tests; 1 fallo (SQL-tautology), 0 errores; los demás pasan |
| `mvn jacoco:report` | 0 | reporte generado a partir de `target/jacoco.exec` |

Medición JaCoCo 0.8.12, cobertura de instrucciones sobre clases de producción: `AuthResource` 127/129 (98.4%); `LegacyDatabase` 91/94 (96.8%); bundle 252/257 (98.1%). Ramas del bundle: 17/20 (85%). No se modificó código de producción; la meta de `REGLAS.md` (85% del código modificado) queda superada para las clases ejercitadas. El entorno usa JDK 25 y JaCoCo 0.8.12 imprime avisos de instrumentación de clases del JDK; esto no altera los resultados de pruebas reportados.

## Fallos

- **H-03, inyección SQL (issue #22):** `rejectsSqlTautologyAsInvalidCredentials` recibe `200` y un token en lugar de `401`. No se deshabilitó ni adaptó la expectativa; no se modificó producción.
- **H-07, detalle en respuesta 500:** el contrato actual documenta el campo `exception`, pero se trata como exposición heredada no recomendada. El test verifica `500` y que exista `message`, sin fijar la clase de excepción como contrato.

## Pendientes

- Corregir H-03 en un cambio de implementación aprobado y volver a ejecutar `mvn verify`; hasta entonces el comando integral termina con código 1.
- ENDPOINTS no define respuestas de error para `GET /q/health`; no se agregó una prueba de error inventada.

## Pruebas propuestas

- Ninguna adicional fuera del contrato actual. Los cambios futuros de claims JWT, allowlist de `client`, expiración y logs sensibles descritos bajo el Issue #11 requieren sus pruebas de aceptación cuando se implemente ese contrato.

## Documentos consultados

- `docs/ENDPOINTS.md`
- `REGLAS.md`
- `docs/TESTING.md` del repositorio archivado (solo como antecedente; los resultados anteriores se descartaron y reemplazaron por resultados de esta rama)
- `pom.xml` y `src/main/resources/application.properties`
- `src/main/java/com/bcp/workshop/auth/AuthResource.java`
- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`
- `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`