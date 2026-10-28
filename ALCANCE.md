# ALCANCE — Sistema de Denuncias de Inseguridad Ciudadana (Serenazgo)

> Fuente de verdad funcional para **ambos** repositorios: `denunciaservice` (API) y `denunciasapp` (Android).
> Este archivo es idéntico en los dos repos. Si hay conflicto entre este archivo y el código, **gana este archivo**.
> Todas las decisiones técnicas ya están cerradas aquí para que el agente pueda trabajar en loop sin preguntar.

---

## 0. Variables del proyecto

| Variable | Valor | Nota |
|---|---|---|
| `{apellido}` | el que ya está en el proyecto generado | El agente **lee** el paquete real del código existente (`src/main/java/com/...` o `namespace` en `app/build.gradle.kts`). Nunca lo cambia. |
| Group ID API | `com.{apellido}` | |
| Artifact API | `denunciaservice` | |
| Paquete API | `com.{apellido}.denunciaservice` | |
| Paquete App | `com.{apellido}.denunciasapp` | |
| URL pública API | `https://denunciaservice-{apellido}.onrender.com/` | La define Render. El usuario la escribe en `gradle.properties` de la app. |
| URL local (emulador) | `http://10.0.2.2:8080/` | |
| URL local (celular físico) | `http://<IP-de-la-PC>:8080/` | |

---

## 1. Objetivo

Que un ciudadano registre denuncias de hechos de inseguridad ante el serenazgo de una municipalidad y que el serenazgo las marque como atendidas, mediante:

1. Una **API REST en Spring Boot** (capas entity / repository / service / controller), con **PostgreSQL**, empaquetada con **Dockerfile** y publicada como Web Service en **Render.com**.
2. Una **app Android en Kotlin** que consume esa API con **Retrofit**.

## 2. En alcance

- 5 endpoints exactos de la sección 4.
- Persistencia en PostgreSQL (Render en la nube; PostgreSQL local opcional).
- Validación de entrada y errores JSON consistentes (400 / 404).
- Configuración por **variables de entorno**; ninguna credencial en el repositorio.
- Dockerfile multi-stage funcional en Render (plan gratuito).
- Colección de Postman y script de smoke test con `curl`.
- App Android con 4 funcionalidades: lista paginada, registro, detalle con botón atender, búsqueda por DNI.
- Pruebas automatizadas (API: MockMvc + H2; App: unit tests + MockWebServer).
- `docs/EXPLICACION.md` en cada repo para preparar la presentación.

## 3. Fuera de alcance (NO implementar)

Autenticación / login / roles, editar o eliminar denuncias, fotos o archivos, mapas o GPS, notificaciones push, Jetpack Compose, Room / caché offline, Swagger UI, CI/CD fuera del auto-deploy de Render, i18n distinto a español.

---

## 4. Contrato de la API (cerrado — la app y la API deben coincidir exactamente)

Base: `/denuncias`. **Toda ruta debe responder con y sin slash final** (`/denuncias` y `/denuncias/`), porque Spring Boot 3 ya no hace "trailing slash matching" y el enunciado usa `denuncias/`.

Formato de fecha en JSON: ISO-8601 local sin zona, `yyyy-MM-dd'T'HH:mm:ss` (ej. `2026-10-28T09:15:00`), **truncada a segundos** (`.truncatedTo(ChronoUnit.SECONDS)` al asignar `fecha`, para que no salgan nanosegundos). Zona horaria de negocio: `America/Lima`.

### 4.1 Recurso `Denuncia` (tabla `denuncias`)

| Campo | Tipo BD | Java | Restricción |
|---|---|---|---|
| `id` | `integer` / serial | `Integer` | PK, autoincrement (`GenerationType.IDENTITY`) |
| `dni` | `varchar(8)` | `String` | not null, exactamente 8 dígitos (`^\d{8}$`) |
| `fecha` | `timestamp` | `LocalDateTime` | not null; si no viene en el POST, el servidor usa `LocalDateTime.now(ZoneId.of("America/Lima"))` |
| `tipo` | `varchar(30)` | `String` | not null; uno de: `ROBO`, `ASALTO`, `VANDALISMO`, `ACOSO`, `OTRO` (se normaliza a mayúsculas) |
| `direccion` | `varchar(200)` | `String` | not null, no vacío, máx. 200 |
| `descripcion` | `varchar(255)` | `String` | not null, no vacío, máx. 255 |
| `estado` | `integer` | `Integer` | not null, default 1. `1` = pendiente, `0` = atendida. El cliente **no** puede fijarlo en el POST |

### 4.2 Endpoints

| # | Tarea | Método | Ruta | Éxito | Errores |
|---|---|---|---|---|---|
| E1 | Registrar denuncia | POST | `/denuncias/` | `201` + denuncia creada (con `id`, `estado=1`, `fecha`) | `400` validación |
| E2 | Obtener denuncias paginadas | GET | `/denuncias/?offset=0&limit=5` | `200` + **arreglo JSON** (puede ser `[]`) | `400` si `offset<0` o `limit` fuera de 1..50 |
| E3 | Obtener denuncia por id | GET | `/denuncias/{id}` | `200` + denuncia | `404` si no existe |
| E4 | Denuncias de un ciudadano | GET | `/denuncias/ciudadano/{dni}` | `200` + arreglo (puede ser `[]`) | `400` si dni no tiene 8 dígitos |
| E5 | Atender denuncia | PUT | `/denuncias/{id}` | `200` + denuncia con `estado=0` | `404` si no existe |

Reglas:

- **E2 paginación por desplazamiento de registros** (no por número de página): `offset` = cuántos registros saltar; `limit` = cuántos traer. Defaults `offset=0`, `limit=5`. Orden: `id DESC` (más recientes primero). La app pide `offset=0,5,10,…`. Implementación recomendada: consulta nativa `SELECT * FROM denuncias ORDER BY id DESC LIMIT :limit OFFSET :offset` (funciona en PostgreSQL, MySQL y H2).
- **E4** orden `id DESC`.
- **E5** no requiere body (si llega uno, se ignora). Es idempotente: si ya estaba en `0`, responde `200` igual.
- **E1** ignora `id` y `estado` si vienen en el body.

### 4.3 Ejemplos

POST `/denuncias/` — request:
```json
{
  "dni": "45678912",
  "tipo": "ROBO",
  "direccion": "Av. Balta 123, Chiclayo",
  "descripcion": "Me arrebataron el celular cerca del mercado"
}
```
Respuesta `201`:
```json
{
  "id": 7,
  "dni": "45678912",
  "fecha": "2026-10-28T09:15:00",
  "tipo": "ROBO",
  "direccion": "Av. Balta 123, Chiclayo",
  "descripcion": "Me arrebataron el celular cerca del mercado",
  "estado": 1
}
```

Error estándar (400 / 404):
```json
{
  "timestamp": "2026-10-28T09:15:00",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Datos inválidos",
  "detalles": { "dni": "El DNI debe tener exactamente 8 dígitos" }
}
```
(`detalles` solo en errores de validación.)

---

## 5. API — decisiones técnicas cerradas

- **Java 17** (es el JDK instalado en la PC del alumno; no usar features de Java 21), **Maven** (con `mvnw`), **Spring Boot: la versión que ya trae el `pom.xml` (hoy 4.1.x; no cambiarla ni de 4.x a 3.x ni a otra mayor)**.
- Spring Boot 4 cambió nombres respecto a Boot 3; si algo no compila, corrige según esto: starters `spring-boot-starter-webmvc` (no `-web`) y `spring-boot-starter-webmvc-test`; `@AutoConfigureMockMvc` está en `org.springframework.boot.webmvc.test.autoconfigure`; Jackson es la versión 3 (paquete `tools.jackson`, las anotaciones siguen en `com.fasterxml.jackson.annotation`); la validación usa `jakarta.validation`.
- Dependencias (las que ya trae el `pom.xml`: webmvc, data-jpa, validation, postgresql runtime, y sus starters `-test`) más `com.h2database:h2` (**solo scope test**). No agregar Lombok ni nada más.
- Paquetes bajo `com.{apellido}.denunciaservice`: `entity`, `repository`, `service`, `controller`, `dto`, `exception`.
- DTOs como `record`: `DenunciaRequest`, `DenunciaResponse`. El controller nunca expone la entidad directamente.
- `service` = interfaz `DenunciaService` + `DenunciaServiceImpl`.
- `application.properties` (sin secretos):
  ```properties
  server.port=${PORT:8080}
  spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/denuncias}
  spring.datasource.username=${DB_USERNAME:postgres}
  spring.datasource.password=${DB_PASSWORD:}
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.open-in-view=false
  spring.jackson.time-zone=America/Lima
  ```
- Tests: `src/test/resources/application.properties` apunta a H2 en memoria con `MODE=PostgreSQL`.
- **Dockerfile** multi-stage: `maven:3.9-eclipse-temurin-17` (build, `-DskipTests`) → `eclipse-temurin:17-jre` (run), `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`. Más `.dockerignore` (`target/`, `.git/`, `.idea/`, `*.iml`).
- `.gitignore` debe excluir `target/`, `.idea/`, `.env`, `*.env`, `application-local.properties`.
- Entregables extra: `postman/denunciaservice.postman_collection.json` (variable `baseUrl`, los 5 requests), `scripts/smoke.sh <BASE_URL>` (curl a los 5 endpoints, falla con exit≠0 si algún status no es el esperado), `README.md`, `docs/EXPLICACION.md`.

## 6. App Android — decisiones técnicas cerradas

- **Kotlin**, **Views XML + ViewBinding** (no Compose; el enunciado exige **RecyclerView**), Material Components.
- El proyecto base viene generado con **Compose** (plantilla "Empty Activity"): la feature F0 lo convierte a Views (quita el plugin `kotlin.compose`, las dependencias `androidx.compose.*`, `buildFeatures.compose` y la carpeta `ui/theme`). AGP 9 ya incluye Kotlin integrado: **no** agregar el plugin `org.jetbrains.kotlin.android`.
- Respetar el sistema de build que generó Android Studio (Kotlin DSL + `gradle/libs.versions.toml`). Agregar dependencias **al version catalog**.
- Dependencias: Retrofit 2 + `converter-gson`, OkHttp + `logging-interceptor`, Kotlin Coroutines (android), `lifecycle-viewmodel-ktx`, `lifecycle-runtime-ktx`, `activity-ktx`, `recyclerview`, `material`. Test: `junit`, `kotlinx-coroutines-test`, `okhttp mockwebserver`.
- `minSdk 24`. `compileSdk`/`targetSdk`: los que generó el asistente.
- `BASE_URL` por `BuildConfig`, leída de `gradle.properties` (propiedad `API_BASE_URL`, debe terminar en `/`). Habilitar `buildFeatures { buildConfig = true; viewBinding = true }`.
- OkHttp con `connectTimeout`, `readTimeout`, `writeTimeout` = **90 s** (Render gratuito "despierta" en ~1 min).
- `AndroidManifest.xml`: `<uses-permission android:name="android.permission.INTERNET"/>` y `android:usesCleartextTraffic="true"` en `<application>`.
- Modelo en la app: `fecha` como `String` (no parsear con Gson a fecha).
- Arquitectura: `data/model`, `data/remote` (ApiService, RetrofitClient), `data/repository` (DenunciaRepository devuelve `Result<T>`), `ui/lista`, `ui/registro`, `ui/detalle`, `ui/busqueda` (cada una con Activity + ViewModel), `util` (validadores).
- Textos en `strings.xml` en español.

### 6.1 Pantallas

| Pantalla | Contenido | Comportamiento |
|---|---|---|
| **Lista** (launcher) | RecyclerView; botones "Cargar 5 siguientes", "Nueva denuncia", "Buscar por DNI" | Carga `offset=0&limit=5`. Botón agrega los siguientes 5 al final (`offset += 5`). Si llegan <5, deshabilita el botón y muestra "No hay más denuncias". Tap en ítem → Detalle. Al volver de Registro/Detalle con cambios, recarga desde `offset=0`. |
| Ítem de lista | `#id`, tipo, fecha, dirección, etiqueta de estado | "Pendiente" (estado 1) / "Atendida" (estado 0) con colores distintos |
| **Registro** | DNI (numérico, 8), Spinner tipo (ROBO, ASALTO, VANDALISMO, ACOSO, OTRO), dirección, descripción, botón "Registrar" | Valida en cliente igual que la API. POST. Éxito → Toast "Denuncia #id registrada" y vuelve a la lista con resultado OK. Error → muestra mensaje del servidor. |
| **Detalle** | Todos los campos; botón "Atender denuncia" | GET por id. Botón visible/habilitado solo si `estado == 1`. PUT → refresca y marca cambios para la lista. |
| **Búsqueda** | Campo DNI + botón "Buscar"; RecyclerView de resultados | GET `/denuncias/ciudadano/{dni}`. Vacío → "Sin denuncias para este DNI". Tap → Detalle. |

Todas las pantallas: indicador de carga, mensaje "El servidor puede tardar hasta 1 minuto en responder la primera vez" cuando la petición demora > 5 s, y error con opción de reintentar.

---

## 7. Criterios de aceptación ↔ Rúbrica (meta: 4 en todo)

| Criterio rúbrica | Se cumple cuando |
|---|---|
| Acceso a datos y API (4) | 4 capas presentes; los 5 endpoints pasan tests MockMvc y `scripts/smoke.sh` |
| Despliegue en Render (4) | `scripts/smoke.sh https://denunciaservice-{apellido}.onrender.com` termina con exit 0 |
| Aplicación móvil (4) | Lista paginada, registro, detalle+atender y búsqueda por DNI funcionan contra la API |
| Repositorio (4) | Ambos repos remotos con todo el código en `main` (sin secretos) |
| Presentación (4) | El alumno puede explicar arquitectura y flujo usando `docs/EXPLICACION.md` |

## 8. Entregables finales

1. Link repo API. 2. Link repo App. 3. URL pública Render. 4. Demo en emulador o celular.
