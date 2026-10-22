# Pruebas — workshop-auth-backend

Matriz basada únicamente en el contrato implementado y documentado en [ENDPOINTS.md](ENDPOINTS.md), incluidos los cambios aprobados para el Issue #11.

## Matriz de pruebas

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `POST /auth/login` | `student` con credenciales válidas y `client=angular-workshop` | Feliz | `200`; JWT opaco con `sub`, `client`, `iat`, `exp`; `expiresIn` coherente dentro de 1 segundo; campos existentes de `user` y `debug` |
| `POST /auth/login` | Cliente omitido, vacío o no permitido | Borde/Error | `400`; no emite token |
| `POST /auth/login` | Campo JSON desconocido | Borde | `200`; el campo se ignora |
| `POST /auth/login` | Solicitud JSON `null` | Error | `400`; `message` es `Request is null` |
| `POST /auth/login` | `username` ausente o vacío | Error | `400`; `message` es `Username is required` |
| `POST /auth/login` | `password` ausente o vacío | Error | `400`; `message` es `Password is required` |
| `POST /auth/login` | Contraseña incorrecta | Error | `401`; incluye `message` y no emite `token` |
| `POST /auth/login` | Tautología SQL en contraseña | Error/Regresión | `401`, mensaje de credenciales inválidas y sin token; pasa con `PreparedStatement` |
| `POST /auth/login` | Token con firma alterada | Error | El validador interno rechaza la firma |
| Validación interna del token | `exp` igual o anterior a `now` | Error | El token se rechaza como expirado; no se agrega endpoint |
| `POST /auth/login` | Excepción durante la consulta de credenciales (doble test-only) | Error | `500`; incluye un campo `message` |
| `GET /q/health` | Servicio disponible | Feliz | `200`; `status=UP` |
| `GET /q/health` | Sin checks de salud propios de la aplicación | Borde | `200`; `checks` está vacío |
| `GET /q/health` | Respuestas de error | Error | ENDPOINTS no documenta respuestas de error para este endpoint; no se inventa un caso |

## Código de los tests

Fuentes: [AuthResourceTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceTest.java), [TokenServiceTest.java](../src/test/java/com/bcp/workshop/auth/TokenServiceTest.java), [AuthResourceLogTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceLogTest.java), [SqlTautologyRegressionTest.java](../src/test/java/com/bcp/workshop/auth/SqlTautologyRegressionTest.java), [HealthEndpointTest.java](../src/test/java/com/bcp/workshop/auth/HealthEndpointTest.java), [AuthResourceFailureTest.java](../src/test/java/com/bcp/workshop/auth/AuthResourceFailureTest.java) y [LoginFailureTestProfile.java](../src/test/java/com/bcp/workshop/auth/LoginFailureTestProfile.java).

### AuthResourceTest

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
				.body("expiresIn", anyOf(is(3599), is(3600)))
				.body("user.username", is("student"))
				.body("user.displayName", is("Workshop Student"))
				.body("user.role", is("STUDENT"))
				.body("debug", is("authenticated-with-legacy-flow"));
	}

	@Test
	void loginTokenContainsIdentityClaimsAndMatchingExpiration() {
		io.restassured.response.Response response = given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(200)
				.extract()
				.response();

		DecodedJWT token = JWT.decode(response.jsonPath().getString("token"));
		long now = Instant.now().getEpochSecond();
		long remainingSeconds = token.getExpiresAt().toInstant().getEpochSecond() - now;

		assertEquals("student", token.getSubject());
		assertEquals("angular-workshop", token.getClaim("client").asString());
		assertNotNull(token.getIssuedAt());
		assertNotNull(token.getExpiresAt());
		assertTrue(Math.abs(response.jsonPath().getInt("expiresIn") - remainingSeconds) <= 1);
	}

	@Test
	void rejectsMissingClient() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("token", nullValue());
	}

	@Test
	void rejectsEmptyClient() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("token", nullValue());
	}

	@Test
	void rejectsUnlistedClient() {
		given()
				.contentType("application/json")
				.body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"unknown-client\"}")
				.when().post("/auth/login")
				.then()
				.statusCode(400)
				.body("token", nullValue());
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
}
```

### SqlTautologyRegressionTest

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
class SqlTautologyRegressionTest {
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
				.body(Map.of("username", "student", "password", "Workshop123!", "client", "angular-workshop"))
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
| `mvn test -Dtest=SqlTautologyRegressionTest` | 0 | 1 test, H-03 devuelve `401` y no emite token |
| `mvn test "-Dtest=AuthResourceTest,TokenServiceTest,AuthResourceLogTest,AuthResourceFailureTest,HealthEndpointTest"` | 0 | 22 tests, 0 fallos, 0 errores |
| `mvn verify` | 0 | 23 tests, 0 fallos, 0 errores; `BUILD SUCCESS` |
| `mvn jacoco:report` | 0 | reporte generado en `target/site/jacoco/index.html` |

Medición JaCoCo 0.8.12: `AuthResource` 144/146 instrucciones (98.6%), `TokenService` 96/111 (86.5%), `LegacyDatabase` 97/100 (97.0%), bundle 364/384 (94.8%). La cobertura de instrucciones de las clases modificadas supera la meta de 85% de `REGLAS.md`. El entorno usa Temurin JDK 21; JaCoCo imprime avisos de instrumentación de algunas clases del JDK, pero `mvn verify` termina en `BUILD SUCCESS`.

## Fallos

- **H-03, inyección SQL (issue #22):** corregida mediante parámetros enlazados; `SqlTautologyRegressionTest` confirma `401` y ausencia de token.
- **H-07, detalle en respuesta 500:** el contrato actual documenta el campo `exception`, pero se trata como exposición heredada no recomendada. El test verifica `500` y que exista `message`, sin fijar la clase de excepción como contrato.

## Pendientes

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