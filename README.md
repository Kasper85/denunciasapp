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
  - `util`: `Validadores` cliente, `FormatoFecha` amigable y `TipoDenuncia` temático.
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
- Formato amigable de fechas (`FormatoFecha`) y mapeo visual de tipos de denuncia (`TipoDenuncia`).

---

## 5. Pantallas de la Aplicación y Sistema de Diseño

La aplicación cuenta con una interfaz moderna basada en **Material 3 DayNight**, con soporte nativo para **modo claro y oscuro**, diseño **edge-to-edge** respetando los `WindowInsets` de la barra de estado y de navegación gestual, e iconografía vectorial en todas las pantallas.

1. **Lista de Denuncias (Principal)**:
   - **Encabezado institucional**: Escudo de serenazgo, título y subtítulo, junto con botón de acceso a búsqueda por DNI.
   - **Filtros rápidos (ChipGroup)**: Chips para alternar en tiempo real entre *Todas*, *Pendientes* y *Atendidas*, acompañados de un indicador de conteo (`Mostrando X de Y cargadas`).
   - **Deslizar para actualizar (`SwipeRefreshLayout`)**: Permite refrescar la lista completa desde el servidor.
   - **Tarjetas rediseñadas (`item_denuncia.xml`)**: Muestran el ícono y color temático según el tipo de hecho (`ROBO`, `ASALTO`, `VANDALISMO`, `ACOSO`, `OTRO`), barra vertical indicadora si la denuncia sigue pendiente, fecha relativa amigable (*Hoy, 09:15*, *Ayer, 18:30* o fecha completa), dirección y chip de estado.
   - **Paginación visible**: Botón *"Cargar 5 siguientes"* al pie (`offset += 5`, `limit = 5`), con indicador de carga y mensaje cuando se alcanzan todas las denuncias.
   - **Extended FAB**: Botón flotante para registrar una nueva denuncia, el cual se contrae al ícono al desplazarse hacia abajo y se expande al subir.
   - **Estados ilustrados**: Pantalla de carga con aviso tras 5 segundos de demora del servidor, estado vacío con acción de registro, y estado de error con botón de reintento.

2. **Registro de Denuncia (`RegistroActivity`)**:
   - Barra superior con navegación hacia atrás y título *"Nueva denuncia"*.
   - **Sección "¿Quién denuncia?"**: Campo de DNI con ícono de persona, contador de 8 dígitos y teclado numérico.
   - **Sección "¿Qué pasó?"**: Selección de tipo mediante `ChipGroup` con íconos vectoriales para cada delito, campo de dirección con ícono de ubicación y contador /200, y descripción detallada multilínea con contador /255.
   - **Validación en tiempo real**: Mensajes de error en los `TextInputLayout` al perder el foco y al intentar enviar datos inválidos.
   - **Botón inferior fijo (56 dp)**: Con indicador de progreso y bloqueo de campos durante el envío. Notificación de éxito con `Toast`/`Snackbar` y regreso automático a la lista.

3. **Detalle y Atención (`DetalleActivity`)**:
   - **Cabecera destacada**: Contenedor con el ícono grande del tipo de hecho y su color identificador, y un **banner de estado** a ancho completo (*"Pendiente de atención por serenazgo"* en ámbar con ícono de alerta, o *"Atendida por serenazgo"* en verde con ícono de confirmación).
   - **Filas de información estructurada**: Ciudadano (DNI), fecha amigable y fecha exacta original, dirección del suceso y tarjeta independiente para la descripción completa.
   - **Acción "Atender denuncia"**: Botón inferior fijo visible únicamente si la denuncia está pendiente. Al pulsarlo, solicita confirmación mediante un diálogo Material (`MaterialAlertDialogBuilder`) y tras confirmar actualiza el estado, muestra un `Snackbar` de confirmación y refresca la lista al volver.

4. **Búsqueda por DNI (`BusquedaActivity`)**:
   - Campo de búsqueda amplio con ícono, contador de 8 dígitos, acción `actionSearch` en el teclado virtual y botón *"Buscar"*.
   - **Encabezado de resultados**: Indica la cantidad de denuncias halladas para el documento consultado.
   - **Resultados**: Reutiliza la misma tarjeta visual de la lista principal, permitiendo pulsar cualquier elemento para inspeccionar su detalle.
   - **Estados ilustrados**: Estado inicial con guía de búsqueda, estado vacío si el DNI no registra denuncias, y manejo de errores de conexión.
