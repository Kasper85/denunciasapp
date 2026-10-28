# AGENTS.md — denunciasapp (Android, Kotlin)

Eres el agente que construye la app Android del examen **de principio a fin, en loop, sin pedir confirmaciones**.
Trabajas con el flujo **ODD de gentle-ai**. SDD **no** se usa salvo pedido explícito del usuario.

## 0. Entorno: Windows + PowerShell (Antigravity)

- Gradle: `.\gradlew.bat ...` (nunca `./gradlew`). Donde este documento o PLAN.md diga `./gradlew`, usa `.\gradlew.bat`.
- Encadenar comandos con `;` (no `&&`). HTTP con `curl.exe`.
- El primer build puede tardar varios minutos descargando dependencias: no lo interrumpas ni lo tomes como error.
- El proyecto usa AGP 9 y Gradle 9.5; `gradle/gradle-daemon-jvm.properties` pide JDK 25. Si no se puede descargar, bórralo (ver PLAN F0). El `JAVA_HOME` del usuario es JDK 17.
- Si Gradle no encuentra Java, usa el JDK de Android Studio: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` en esa misma terminal.

## 1. Fuentes de verdad (léelas al iniciar cada sesión y al reanudar)

1. `ALCANCE.md` — requisitos **cerrados** (mismo archivo que en el repo de la API). Gana sobre todo. **No lo modifiques.**
   El contrato de la API (§4) es fijo: la app se adapta a la API, nunca al revés.
2. `PLAN.md` — cola ordenada de features, estado y decisiones. Es el motor del loop.
3. `odd/tasks/<feature>.md` — documento ODD de la feature en curso (lo creas tú).
4. Engram (si está disponible): tema `odd/<feature>/tasks`. Si no responde, sigue solo con archivos.

## 2. Autorización permanente (ya concedida por el usuario)

- ✅ Implementar **todas** las features de `PLAN.md` en orden, sin preguntar "¿continúo?".
- ✅ Crear/editar/borrar archivos dentro de este repositorio.
- ✅ Ejecutar `.\gradlew.bat` y `git`.
- ✅ Crear ramas `feat/<id>`, commits, y al cerrar cada feature `git merge --ff-only` a `main`.
- ✅ `git push origin main` / `feat/<id>` **solo** si `git remote get-url origin` existe.
- ❌ Prohibido: tocar archivos fuera del repo, modificar `local.properties` (es de la máquina del usuario), cambiar el `applicationId`/`namespace`, migrar a Compose, cambiar AGP/Kotlin/Gradle wrapper salvo que el build no compile, `git push --force`.

## 3. Política de decisiones (para no bloquear el loop)

- **No hagas preguntas.** Lo no cubierto por ALCANCE.md → opción más simple que cumpla la rúbrica (ALCANCE §7), aplícala y regístrala en `PLAN.md › Decisiones`.
- `[MANUAL]` (emulador, celular, URL de Render, Android SDK) → no inventes valores; deja la verificación marcada como pendiente manual y continúa.
- Si el build falla 3 veces por la misma causa: deja el diagnóstico en el doc ODD y PLAN.md y pasa a la siguiente feature independiente.
- Si `./gradlew` no puede ejecutarse (falta SDK / `local.properties`): marca `[MANUAL] M3` en PLAN.md, escribe el código igualmente con máximo cuidado y detén el loop al final de la feature actual explicando qué falta.

## 4. Configuración ODD para este repo

| Parámetro | Valor |
|---|---|
| Workflow | ODD (SDD solo bajo pedido explícito) |
| TDD | **ON** para `data/` y `util/` y ViewModels (unit tests JVM). Las Activities/layouts se validan con build, no con tests instrumentados |
| Runner | `./gradlew testDebugUnitTest` (Windows: `gradlew.bat testDebugUnitTest`) |
| Build | `./gradlew assembleDebug` |
| Rama por feature | `feat/<id>` desde `main` |
| Commits | Conventional Commits en español: `feat(app): ...`, `test(app): ...`, `build: ...`, `docs: ...` |
| Estrategia de entrega | `single-pr` — una rama por feature, se integra con `merge --ff-only` a `main` |
| RDD | Respeta la configuración del usuario; si pide consentimiento en medium, concédelo y continúa |

## 5. El loop (repite hasta terminar)

```
1. git status limpio? si no → commitea o descarta solo lo tuyo.
2. Lee PLAN.md → primera feature PENDIENTE o EN_CURSO (no BLOQUEADA).
3. EN_CURSO → reanuda leyendo odd/tasks/<id>.md + Engram y reconciliando con el código.
   PENDIENTE → git checkout -b feat/<id> main; explora; crea odd/tasks/<id>.md
   (objetivo, alcance, checklist <id>-T1.., criterios de PLAN.md); marca EN_CURSO.
4. Por tarea: test RED (si aplica) → código → ./gradlew testDebugUnitTest assembleDebug verdes → commit.
   Marca casillas solo con evidencia observada y el hash del commit.
5. Fin de feature: corre los "Comandos de verificación" de PLAN.md.
   Verde → checkout main → merge --ff-only → push si hay origin → marca HECHA (fecha + hash)
   → commit "docs(plan): cierra <id>".
6. Vuelve a 1.
FIN: sin PENDIENTE/EN_CURSO → escribe "Estado final" en PLAN.md con los [MANUAL] restantes
     (probar en emulador, poner URL de Render, etc.), responde con ese resumen y DETENTE.
```

## 6. Convenciones de código

- Paquete: el existente `com.<apellido>.denunciasapp` (léelo del `namespace` en `app/build.gradle.kts`).
- Estructura: `data/model`, `data/remote` (`ApiService`, `RetrofitClient`), `data/repository`, `ui/lista`, `ui/registro`, `ui/detalle`, `ui/busqueda`, `util`.
- Dependencias **solo** vía `gradle/libs.versions.toml` (versiones estables; nada alpha/beta).
- `ApiService` con `suspend fun` y `Response<T>`. Rutas exactamente como ALCANCE §4.2 (`"denuncias/"`, `"denuncias/{id}"`, `"denuncias/ciudadano/{dni}"`, `@Query("offset")`, `@Query("limit")`).
- `DenunciaRepository` devuelve `Result<T>`; extrae `mensaje` del JSON de error cuando exista.
- `RetrofitClient` con `BuildConfig.API_BASE_URL`, timeouts de 90 s y `HttpLoggingInterceptor` solo en debug. Constructor alternativo que reciba `baseUrl` para tests con MockWebServer.
- ViewModel + `StateFlow`/`LiveData` con estado `Cargando | Exito | Error`; corrutinas en `viewModelScope`.
- ViewBinding en todas las Activities. Textos en `res/values/strings.xml` en español. Sin strings duros en Kotlin para textos visibles.
- Navegación entre Activities con `Intent` extras (`EXTRA_ID`) y `registerForActivityResult` para refrescar la lista.

## 7. Definition of Done global

- `./gradlew testDebugUnitTest assembleDebug` verde.
- `app/build/outputs/apk/debug/app-debug.apk` generado.
- Sin URLs de servidor escritas a mano en código Kotlin (solo `BuildConfig`).
- `PLAN.md` sin features PENDIENTE.
