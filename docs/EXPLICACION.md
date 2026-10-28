# Guía de Explicación y Sustentación — DenunciasApp

Este documento sirve como material de preparación para la sustentación oral del proyecto ante el docente o evaluador. Explica en detalle las decisiones de arquitectura, el flujo de datos de extremo a extremo, las tecnologías utilizadas y las respuestas a las preguntas más frecuentes.

---

## 1. Arquitectura General del Sistema

La aplicación sigue el patrón arquitectural **MVVM (Model-View-ViewModel)** recomendado por Google, estructurado en capas bien desacopladas:

```
+-----------------------------------------------------------------------+
|                              CAPA DE UI                                |
|  Activities: MainActivity, RegistroActivity, DetalleActivity, ...      |
|  ViewBinding (XML Layouts) | Material Components 3 | RecyclerView     |
+-----------------------------------------------------------------------+
                                   ▲ | Observa StateFlow
                                   | ▼ Invoca métodos de negocio
+-----------------------------------------------------------------------+
|                           CAPA DE VIEWMODEL                           |
|  ListaViewModel, RegistroViewModel, DetalleViewModel, BusquedaViewModel|
|  Gestión de estado inmutable (StateFlow), Corrutinas en viewModelScope|
+-----------------------------------------------------------------------+
                                   ▲ | Devuelve Result<T>
                                   | ▼ Llama métodos suspend
+-----------------------------------------------------------------------+
|                          CAPA DE REPOSITORIO                          |
|  DenunciaRepository: Encapsula la lógica de acceso a datos,           |
|  manejo de excepciones de red y extracción de errores JSON            |
+-----------------------------------------------------------------------+
                                   ▲ | Response<T>
                                   | ▼ HTTP Call
+-----------------------------------------------------------------------+
|                            CAPA DE RED                                |
|  Retrofit (ApiService) + OkHttp (Timeouts 90s, LoggingInterceptor)   |
|  GsonConverterFactory (Serialización / Deserialización JSON)          |
+-----------------------------------------------------------------------+
                                   ▲ | Internet / Red Local (HTTP/HTTPS)
                                   ▼ |
+-----------------------------------------------------------------------+
|                    BACKEND REST API (Spring Boot)                     |
|  Controller -> Service -> Repository -> Base de Datos PostgreSQL      |
+-----------------------------------------------------------------------+
```

### Componentes Principales:
1. **Views (Activities y XML Layouts)**: Únicamente responsables de pintar la interfaz y capturar eventos de usuario. No contienen lógica de negocio ni llamadas a red.
2. **ViewModels**: Sobreviven a cambios de configuración (rotación de pantalla). Exponen el estado de la UI mediante `StateFlow` reactivo con estados tipados (`Cargando`, `Exito`, `Error`, etc.).
3. **Repository**: Fuente única de verdad para el ViewModel. Abstrae la fuente de datos y mapea las respuestas HTTP en tipos seguros `Result<T>`.
4. **Retrofit & OkHttp**: Cliente HTTP fuertemente tipado que mapea las llamadas de red a funciones suspendidas de Kotlin.

---

## 2. Flujo Completo: "Cargar 5 Siguientes" (Paso a Paso)

A continuación se detalla el ciclo de vida completo de la acción más importante de la lista paginada:

```
[Usuario] 
   │ 
   ▼ (1. Presiona botón "Cargar 5 siguientes")
[MainActivity] 
   │ 
   ▼ (2. Invoca viewModel.cargarSiguientes())
[ListaViewModel]
   │ 
   ▼ (3. Activa bandera cargandoMas = true en StateFlow; lanza corrutina con viewModelScope)
[DenunciaRepository]
   │ 
   ▼ (4. Invoca apiService.listarDenuncias(offset = 5, limit = 5))
[Retrofit / OkHttp]
   │ 
   ▼ (5. Ejecuta petición HTTP: GET http://<host>/denuncias/?offset=5&limit=5)
[Spring Boot Controller]
   │ 
   ▼ (6. Recibe parámetros @RequestParam offset=5, limit=5)
[Spring Boot Service / JPA]
   │ 
   ▼ (7. Ejecuta consulta nativa SQL en PostgreSQL:
          SELECT * FROM denuncias ORDER BY id DESC LIMIT 5 OFFSET 5;)
[PostgreSQL]
   │ 
   ▼ (8. Retorna conjunto de registros en orden descendente)
[Spring Boot]
   │ 
   ▼ (9. Serializa DTOs a JSON array y responde HTTP 200 OK)
[Retrofit / Gson]
   │ 
   ▼ (10. Deserializa JSON a List<Denuncia>)
[DenunciaRepository]
   │ 
   ▼ (11. Retorna Result.success(lista))
[ListaViewModel]
   │ 
   ▼ (12. Concatena los nuevos 5 elementos a la lista existente;
          actualiza offsetActual += lista.size;
          si lista.size < 5, marca hayMas = false;
          emite nuevo ListaUiState.Exito al StateFlow)
[MainActivity]
   │ 
   ▼ (13. Recibe la emisión en repeatOnLifecycle(STARTED);
          envía la lista unificada a adapter.submitList;
          si !hayMas, oculta botón y muestra "No hay más denuncias")
[RecyclerView / DiffUtil]
   │ 
   ▼ (14. Calcula diferencias de elementos eficientemente y anima la inserción de las filas)
[Usuario] Visualiza los nuevos registros agregados al final de la pantalla.
```

---

## 3. Conceptos Clave Explicados de Forma Simple

### ¿Qué es Retrofit y cómo funciona?
Es una biblioteca de Square para Android y Java que convierte una interfaz HTTP en una interfaz de código Kotlin. Anotaciones como `@GET("denuncias/")`, `@POST("denuncias/")` o `@Query("offset")` le indican a Retrofit la URL, el verbo HTTP y los parámetros. Retrofit se apoya en un conversor (en nuestro caso Gson) para transformar automáticamente el texto JSON en clases de datos (`data class`).

### ¿Por qué usamos Corrutinas y `viewModelScope`?
Las operaciones de red no pueden ejecutarse en el hilo principal (`Main Thread`), de lo contrario la aplicación se congelaría y Android lanzaría una excepción `NetworkOnMainThreadException`.
Las corrutinas permiten ejecutar código asíncrono de forma secuencial y legible (`suspend fun`), sin necesidad de callbacks anidados. `viewModelScope` vincula el ciclo de vida de la corrutina al del `ViewModel`: si el usuario cierra la pantalla, la petición se cancela automáticamente, evitando fugas de memoria.

### ¿Por qué usamos `StateFlow` en lugar de `LiveData`?
`StateFlow` es un flujo reactivo caliente de la librería oficial de Kotlin Coroutines. Es seguro contra tipos nulos, siempre tiene un valor inicial, expone un estado inmutable hacia la vista y se integra de manera limpia y moderna con el ciclo de vida de Android mediante `lifecycle.repeatOnLifecycle`.

---

## 4. Justificación de Decisiones Técnicas

### 1. ¿Por qué Timeouts de 90 segundos?
El backend está diseñado para alojarse en el plan gratuito de **Render.com**. Render suspende los contenedores inactivos para ahorrar recursos; cuando llega una nueva petición, el servidor "despierta" (arranque en frío de Linux, Java y Spring Boot), lo cual puede demorar entre **50 y 60 segundos**. Con los timeouts estándar de 10 o 15 segundos, la app arrojaría `SocketTimeoutException` antes de que el servidor termine de iniciar. Fijar 90 segundos garantiza que la primera llamada no falle prematuramente.

### 2. ¿Por qué `android:usesCleartextTraffic="true"` en el AndroidManifest?
Desde Android 9 (API 28), el sistema operativo bloquea por defecto todas las conexiones HTTP sin cifrar (texto plano). Para desarrollo y pruebas locales con el emulador (`http://10.0.2.2:8080/`) o con la IP de la red Wi-Fi (`http://192.168.x.x:8080/`), el tráfico viaja sobre HTTP. Habilitar `usesCleartextTraffic="true"` permite la comunicación durante el desarrollo sin requerir certificados SSL/TLS autofirmados.

### 3. ¿Por qué Views (XML + RecyclerView) y no Jetpack Compose?
La rúbrica y las especificaciones cerradas del examen en `ALCANCE.md` exigen explícitamente el uso de **RecyclerView** y Views tradicionales. El proyecto base generado con la plantilla Empty Activity traía Compose por defecto, por lo que en la feature F0 se realizó la conversión técnica a Views, ViewBinding y Material Components 3 para cumplir al 100% con los criterios de evaluación.

---

## 5. Preguntas Frecuentes del Evaluador / Profesor

### P1: ¿Cómo está estructurada la arquitectura de la aplicación?
> **Respuesta:** Está basada en MVVM (Model-View-ViewModel). Las Activities se encargan exclusivamente de la interfaz de usuario con ViewBinding; los ViewModels gestionan el estado reactivo con `StateFlow` y ejecutan casos de uso mediante corrutinas; y la capa de datos (`DenunciaRepository` y `ApiService`) abstrae las llamadas HTTP usando Retrofit y OkHttp.

### P2: ¿Cómo funciona la paginación de denuncias?
> **Respuesta:** Se basa en desplazamiento de registros mediante los parámetros `offset` y `limit`. La primera carga solicita `offset=0&limit=5`. Al presionar "Cargar 5 siguientes", el ViewModel solicita los siguientes 5 (`offset=5&limit=5`), acumulándolos en la lista existente. Si la respuesta contiene menos de 5 elementos, el sistema detecta que se alcanzó el final de los registros y desactiva el botón mostrando "No hay más denuncias".

### P3: ¿Por qué la fecha se modela como String en la app y no como Date/LocalDateTime?
> **Respuesta:** Según las reglas del ALCANCE §6, la fecha viaja en formato ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`) truncada a segundos. Modelarla como String evita desfases por zonas horarias causados por el parseo automático de Gson y preserva la fecha tal cual fue generada en Lima por el servidor.

### P4: ¿Dónde y cómo se valida la información que ingresa el usuario?
> **Respuesta:** Se valida en dos niveles. En el cliente, la clase `Validadores` comprueba que el DNI tenga exactamente 8 dígitos numéricos, que el tipo pertenezca a los 5 permitidos (`ROBO`, `ASALTO`, `VANDALISMO`, `ACOSO`, `OTRO`), y que la dirección y descripción no estén vacías ni excedan los límites (200 y 255 caracteres). Si falla, se muestra el error en el campo sin consumir recursos de red. Si es válida, el backend realiza la misma validación y devuelve 400 con un JSON estándar si algo no cumple.

### P5: ¿Cómo se maneja la actualización de la lista cuando se registra una denuncia o se atiende una existente?
> **Respuesta:** Se utiliza el contrato moderno `registerForActivityResult`. Cuando se abre la pantalla de Registro o Detalle, se espera un resultado `Activity.RESULT_OK`. Si el usuario creó una denuncia o la atendió, la pantalla secundaria finaliza con `setResult(RESULT_OK)` y la actividad principal recarga automáticamente la lista desde `offset=0`.

### P6: ¿Qué ocurre si la API demora en responder o arroja un error 404/500?
> **Respuesta:** Si la petición demora más de 5 segundos, la app despliega un mensaje informativo avisando que el servidor en la nube puede tardar hasta 1 minuto en despertar. Si la petición falla, `DenunciaRepository` captura el flujo de error, parsea el objeto `ErrorResponse` del JSON y expone el mensaje al ViewModel, el cual muestra una pantalla de error amigable con botón de "Reintentar".

### P7: ¿Cómo se probó el código sin depender de la API real o de internet?
> **Respuesta:** En la capa de datos, se usó **MockWebServer** de OkHttp para simular respuestas HTTP reales (200, 201, 404, cuerpos JSON y errores), comprobando rutas y verbos. En la capa de presentación, se implementaron pruebas unitarias para cada ViewModel (`ListaViewModelTest`, `RegistroViewModelTest`, `DetalleViewModelTest`, `BusquedaViewModelTest`) inyectando repositorios simulados (*Fake Repository*) y usando `MainDispatcherRule` para controlar las corrutinas.

### P8: ¿Cómo se evita exponer credenciales o URLs estáticas en el código fuente?
> **Respuesta:** La URL base se configura como propiedad `API_BASE_URL` en `gradle.properties` y el plugin de Android la genera en `BuildConfig.API_BASE_URL`. De esta forma, el código Kotlin jamás contiene URLs quemadas y es sencillo alternar entre el emulador local (`10.0.2.2`), la IP local o la nube (Render) sin modificar clases fuente.
