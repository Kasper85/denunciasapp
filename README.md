# DenunciasApp — Sistema Móvil de Denuncias Ciudadanas (Serenazgo)

Aplicación Android nativa en **Kotlin** para el registro, consulta paginada, búsqueda y atención de denuncias ciudadanas ante el serenazgo municipal, consumiendo la API REST `denunciaservice`.

- **Alumno**: RodriguezRuiz
- **Paquete**: `com.RodriguezRuiz.denunciasapp`
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 37

---

## 1. Arquitectura y Tecnologías

- **Lenguaje**: Kotlin
- **Diseño de Interfaz**: Views en XML + **ViewBinding** (conforme a los requerimientos de evaluación que exigen `RecyclerView`, sin Compose) y **Material Components 3**.
- **Arquitectura**: MVVM (Model-View-ViewModel) por capas:
  - `data/model`: DTOs (`Denuncia`, `DenunciaRequest`, `ErrorResponse`).
  - `data/remote`: Interfaz Retrofit `ApiService` y `RetrofitClient` con OkHttp.
  - `data/repository`: `DenunciaRepository` con encapsulación en `Result<T>` y extracción de errores JSON.
  - `ui/lista`: `MainActivity` (Launcher), `ListaViewModel`, `DenunciaAdapter` (con `DiffUtil`).
  - `ui/registro`: `RegistroActivity`, `RegistroViewModel` con validación previa.
  - `ui/detalle`: `DetalleActivity`, `DetalleViewModel` con operación idempotente PUT.
  - `ui/busqueda`: `BusquedaActivity`, `BusquedaViewModel` por DNI.
  - `util`: `Validadores` cliente idénticos a los del backend.
- **Concurrencia**: Kotlin Coroutines (`viewModelScope`, `Dispatchers`) y `StateFlow`.
- **Pruebas**: Unit tests en JVM con **JUnit 4**, `kotlinx-coroutines-test` y **MockWebServer** de OkHttp.

---

## 2. Configuración de la API (`API_BASE_URL`)

La URL base de la API **no está quemada en código Kotlin**; se inyecta mediante `BuildConfig.API_BASE_URL` configurado en `gradle.properties`. Debe terminar siempre con una barra diagonal `/`.

Edita el archivo `gradle.properties`:

### Opción A: Emulador Oficial de Android Studio
```properties
API_BASE_URL=http://10.0.2.2:8080/
```
*(10.0.2.2 es el alias del host localhost en el emulador estándar de Android).*

### Opción B: Dispositivo Físico (vía Wi-Fi local)
Asegúrate de que tu PC y el celular estén conectados a la misma red Wi-Fi y usa la IP local de tu PC:
```properties
API_BASE_URL=http://192.168.1.50:8080/
```

### Opción C: API Desplegada en la Nube (Render.com)
```properties
API_BASE_URL=https://denunciaservice-rodriguezruiz.onrender.com/
```

> **Nota sobre Render**: En el plan gratuito de Render, las instancias inactivas se suspenden ("duermen") y demoran entre 50 y 60 segundos en responder la primera petición. La app está configurada con timeouts de 90 segundos y muestra automáticamente un aviso explicativo si la petición toma más de 5 segundos.

---

## 3. Compilación y Generación del APK

### En Windows (PowerShell / CMD)
```powershell
.\gradlew.bat assembleDebug
```

El archivo APK ejecutable se genera en:
```
app/build/outputs/apk/debug/app-debug.apk
```

Para instalarlo directamente en un dispositivo conectado mediante ADB:
```powershell
.\gradlew.bat installDebug
```

---

## 4. Ejecución de Pruebas Automatizadas

Para correr toda la suite de pruebas unitarias en la máquina local (sin necesidad de emulador):
```powershell
.\gradlew.bat testDebugUnitTest
```

Las pruebas cubren:
- Repositorio con MockWebServer (rutas exactas, queries `offset`/`limit`, respuestas 201, 404 y parseo de JSON de error).
- Validaciones completas de DNI (8 dígitos), longitudes y tipos permitidos en cliente.
- ViewModels de Lista, Registro, Detalle y Búsqueda verificando emisión de estados (`StateFlow`).

---

## 5. Pantallas de la Aplicación

1. **Lista de Denuncias (Launcher)**:
   - Carga inicial de 5 elementos (`offset=0&limit=5`).
   - Botón *"Cargar 5 siguientes"* que incrementa el desplazamiento en +5 y desactiva el botón al llegar al final ("No hay más denuncias").
   - Botones superiores para *"Nueva denuncia"* y *"Buscar por DNI"*.
   - Tap en cualquier tarjeta para abrir el detalle.
   - Refresco automático al regresar si se registraron o atendieron denuncias.
2. **Registro de Denuncia**:
   - Campos: DNI (8 dígitos), Spinner de Tipo (`ROBO`, `ASALTO`, `VANDALISMO`, `ACOSO`, `OTRO`), Dirección y Descripción.
   - Validación reactiva en cliente; al confirmar muestra un Toast *"Denuncia #ID registrada con éxito"* y vuelve a la lista.
3. **Detalle y Atender**:
   - Muestra todos los atributos con tarjeta Material y badge de estado con colores diferenciados.
   - Botón *"Atender denuncia"* visible solo si el estado es 1 (Pendiente). Al atender, pasa a estado 0 y se deshabilita la acción.
4. **Búsqueda por DNI**:
   - Búsqueda por número de documento de identidad ciudadano (endpoint `/denuncias/ciudadano/{dni}`).
   - Si no existen denuncias muestra mensaje *"Sin denuncias para este DNI"*.
