# PLAN — denunciasapp

> Cola de trabajo del loop ODD. El agente actualiza **Estado**, **Evidencia** y **Decisiones**.
> Estados: `PENDIENTE` · `EN_CURSO` · `HECHA` · `BLOQUEADA` (solo por `[MANUAL]`).
> Requisitos completos en `ALCANCE.md` (§4 contrato API, §6 app).

## Prerrequisitos [MANUAL] (los hace el usuario antes de arrancar)

- [x] M1. Proyecto creado en Android Studio en esta carpeta (paquete `com.RodriguezRuiz.denunciasapp`, minSdk 24, AGP 9, Kotlin DSL). Vino con plantilla **Compose** → F0 lo convierte a Views.
- [ ] M2. Repo remoto `denunciasapp` en GitHub. No bloquea: sin `origin` el agente trabaja local.
- [ ] M3. `API_BASE_URL` en `gradle.properties` — si falta, el agente agrega `API_BASE_URL=http://10.0.2.2:8080/`.

---

## F0 · app-git — Repositorio inicial (lo hace el agente)
**Estado:** HECHA (2026-10-28 · d91e556)
**Tareas:**
- Verificar que existen `settings.gradle.kts` y `app/build.gradle.kts`.
- **Convertir de Compose a Views** (el enunciado exige RecyclerView): en `app/build.gradle.kts` quitar `alias(libs.plugins.kotlin.compose)`, `buildFeatures { compose = true }` y todas las `androidx.compose.*` / `activity.compose`; en `libs.versions.toml` quitar esas entradas y el plugin `kotlin-compose`; borrar `ui/theme/` y reemplazar `MainActivity.kt` por una Activity mínima con ViewBinding (luego F2 la convierte en la lista); el tema `Theme.DenunciasApp` en `themes.xml` pasa a heredar de `Theme.Material3.DayNight.NoActionBar` (requiere la dependencia `com.google.android.material:material`); borrar `ExampleInstrumentedTest` si referencia Compose. Dejar `ExampleUnitTest` y que `testDebugUnitTest` pase. No agregar el plugin `org.jetbrains.kotlin.android` (AGP 9 trae Kotlin integrado).
- Si Gradle falla por `gradle/gradle-daemon-jvm.properties` (pide JDK 25 y no lo puede descargar), bórralo: Gradle usará el JDK 17 de `JAVA_HOME`. Anótalo en Decisiones.
- Si no hay `.git` → `git init -b main`. Asegurar que `.gitignore` excluye `local.properties`, `.gradle/`, `build/`, `.idea/`, `*.iml`. Agregar `API_BASE_URL` a `gradle.properties` si falta. Commit `chore: proyecto inicial de Android Studio`.
**Criterios de aceptación:** el proyecto ya no usa Compose; `.\gradlew.bat assembleDebug` compila; hay un commit en `main`; `local.properties` no está versionado.
**Comandos de verificación:** `.\gradlew.bat assembleDebug`; `git ls-files local.properties` (debe salir vacío)
**Evidencia:** `.\gradlew.bat testDebugUnitTest assembleDebug` BUILD SUCCESSFUL in 46s; `git ls-files local.properties` vacío. Commit inicial d91e556.

## F1 · app-data — Capa de datos y configuración
**Estado:** HECHA (2026-10-28 · 37257c1)
**Objetivo:** la app sabe hablar con la API y está probado sin servidor real.
**Tareas sugeridas:**
- T1 Version catalog + `app/build.gradle.kts`: dependencias de ALCANCE §6, `buildFeatures { viewBinding = true; buildConfig = true }`, `buildConfigField("String","API_BASE_URL", "\"${project.findProperty("API_BASE_URL") ?: "http://10.0.2.2:8080/"}\"")`.
- T2 `AndroidManifest.xml`: permiso INTERNET y `usesCleartextTraffic="true"`.
- T3 `data/model/Denuncia`, `DenunciaRequest`, `ErrorResponse`.
- T4 `data/remote/ApiService` (E1–E5) + `RetrofitClient`.
- T5 `data/repository/DenunciaRepository` + tests con MockWebServer: lista paginada (verifica query `offset`/`limit` y path), POST 201, GET 404 → `Result.failure` con mensaje, búsqueda por DNI `[]`, PUT 200 `estado=0`.
- T6 `util/Validadores` (DNI 8 dígitos, campos no vacíos, máx. longitudes) + tests.
**Criterios de aceptación:** tests T5 y T6 verdes; `assembleDebug` compila.
**Comandos de verificación:** `./gradlew testDebugUnitTest assembleDebug`
**Evidencia:** `.\gradlew.bat testDebugUnitTest assembleDebug` BUILD SUCCESSFUL in 17s (tests de repositorio y validadores verdes, app-debug.apk generado). Commit 37257c1.

## F2 · app-lista — Lista paginada (pantalla principal)
**Estado:** HECHA (2026-10-28 · fa10f3e)
**Objetivo:** ver denuncias de 5 en 5 (ALCANCE §6.1 Lista).
**Tareas sugeridas:** `ListaViewModel` (offset acumulado, `hayMas`, estado) con test usando repositorio falso; `DenunciaAdapter` + `item_denuncia.xml` (etiqueta Pendiente/Atendida con colores); `ListaActivity` como launcher (reemplaza la `MainActivity` generada o la reutiliza); botones Cargar 5 siguientes / Nueva denuncia / Buscar por DNI; mensaje "servidor puede tardar" si >5 s; reintentar.
**Criterios de aceptación:** test del ViewModel: primera carga offset 0, segunda offset 5, `hayMas=false` cuando llegan <5; build verde.
**Comandos de verificación:** `./gradlew testDebugUnitTest assembleDebug`
**Evidencia:** `.\gradlew.bat testDebugUnitTest assembleDebug` BUILD SUCCESSFUL in 20s (tests de ListaViewModelTest verdes, app-debug.apk generado de 10.3 MB). Commit fa10f3e.

## F3 · app-registro — Registrar denuncia
**Estado:** HECHA (2026-10-28 · eb2cbf5)
**Objetivo:** formulario de ALCANCE §6.1 Registro.
**Tareas sugeridas:** `RegistroViewModel` (valida con `Validadores`, llama POST) + test; `activity_registro.xml` (DNI numérico maxLength 8, Spinner con los 5 tipos desde `strings.xml` string-array, dirección, descripción); Toast con id; `setResult(RESULT_OK)` y la lista recarga desde offset 0.
**Criterios de aceptación:** test: DNI inválido no llama al repositorio y expone error de campo; datos válidos → estado Exito con id; build verde.
**Comandos de verificación:** `./gradlew testDebugUnitTest assembleDebug`
**Evidencia:** `.\gradlew.bat testDebugUnitTest assembleDebug` BUILD SUCCESSFUL in 17s (tests de RegistroViewModelTest verdes, app-debug.apk generado). Commit eb2cbf5.

## F4 · app-detalle — Detalle y atender
**Estado:** HECHA (2026-10-28 · 991ea9a)
**Objetivo:** ver una denuncia y marcarla atendida (PUT).
**Tareas sugeridas:** `DetalleViewModel` (cargar por id, atender) + test; `DetalleActivity` recibe `EXTRA_ID`; botón "Atender denuncia" solo si `estado==1`; tras atender muestra "Atendida", deshabilita botón y devuelve `RESULT_OK`.
**Criterios de aceptación:** test: atender cambia estado a 0 y oculta acción; 404 muestra error; build verde.
**Comandos de verificación:** `./gradlew testDebugUnitTest assembleDebug`
**Evidencia:** `.\gradlew.bat testDebugUnitTest assembleDebug` BUILD SUCCESSFUL in 18s (tests de DetalleViewModelTest verdes, app-debug.apk generado). Commit 991ea9a.

## F5 · app-busqueda — Denuncias de un ciudadano
**Estado:** PENDIENTE
**Objetivo:** buscar por DNI (E4).
**Tareas sugeridas:** `BusquedaViewModel` + test; `BusquedaActivity` (campo DNI, botón Buscar, RecyclerView reutilizando `DenunciaAdapter`, mensaje "Sin denuncias para este DNI"); tap → Detalle.
**Criterios de aceptación:** test: DNI inválido no consulta; lista vacía produce estado vacío; build verde.
**Comandos de verificación:** `./gradlew testDebugUnitTest assembleDebug`
**Evidencia:** —

## F6 · app-docs — Pulido y documentación
**Estado:** PENDIENTE
**Objetivo:** dejar la app presentable y explicable.
**Tareas sugeridas:** revisar que todas las pantallas tengan carga/error/reintento; títulos en la ActionBar; `README.md` (cómo cambiar `API_BASE_URL`, emulador vs celular, generar APK); `docs/EXPLICACION.md` (arquitectura en capas con diagrama ASCII, flujo de "Cargar 5 siguientes" de botón a SQL, Retrofit/corrutinas/ViewModel explicado simple, por qué timeouts de 90 s y cleartext, 8–10 preguntas probables del profesor con respuesta corta).
**Criterios de aceptación:** build y tests verdes; ambos documentos existen.
**Comandos de verificación:** `./gradlew testDebugUnitTest assembleDebug`
**Evidencia:** —

## F7 · app-e2e-check — Prueba contra la API real
**Estado:** BLOQUEADA — `[MANUAL]` requiere la URL de Render en `gradle.properties` y un emulador/celular.
**Objetivo:** confirmar el flujo completo contra Render.
**Checklist para el usuario (el agente solo la deja escrita y verifica que `API_BASE_URL` no sea la de 10.0.2.2):**
- [ ] La lista carga (primera vez puede tardar ~1 min).
- [ ] "Cargar 5 siguientes" agrega registros.
- [ ] Registrar una denuncia y verla arriba en la lista.
- [ ] Abrir detalle y atenderla → aparece "Atendida".
- [ ] Buscar por el DNI usado y verla.
**Evidencia:** —

---

## Decisiones
<!-- El agente agrega aquí: fecha · feature · decisión · porqué (una línea) -->
- 2026-10-28 · F0 · Eliminación de gradle/gradle-daemon-jvm.properties · Forzaba descarga de JDK 25 no disponible; se usa JDK 17 local configurado en JAVA_HOME.

## Estado final
<!-- El agente escribe aquí el resumen al terminar el loop y los [MANUAL] pendientes -->
