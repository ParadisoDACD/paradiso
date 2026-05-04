# Sprint 1 — Captura de datos: Ticketmaster + TfL

Asignatura: Desarrollo de Aplicaciones para Ciencia de Datos  
Universidad de Las Palmas de Gran Canaria  
Proyecto: Paradiso  
Sprint: 1 de 3

## Descripción

Proyecto Java 21 multimódulo orientado a la captura de datos desde dos fuentes externas dinámicas. Cada módulo consume una API distinta, transforma la respuesta JSON a un modelo interno propio y persiste los datos de forma incremental en una base de datos SQLite independiente.

El objetivo del Sprint 1 es dejar preparada una base sólida de ingesta y persistencia, sin cruzar todavía los datos entre fuentes.

## Módulos

### Ticketmaster Module

El módulo `ticketmaster-module` captura eventos musicales y festivales en Londres, Reino Unido, usando la Ticketmaster Discovery API v2.

Los datos se almacenan en:

```text
data/ticketmaster.db
```

Tablas principales:

```text
ticketmaster_capture_run
ticketmaster_event_capture
```

### TfL Module

El módulo `tfl-module` captura itinerarios de transporte público desde hubs principales de Londres hasta venues asociados a conciertos, usando la TfL Unified API Journey Planner.

Los datos se almacenan en:

```text
data/tfl.db
```

Tablas principales:

```text
tfl_capture_run
tfl_journey_capture
```

## Propuesta de valor

En sprints posteriores, el proyecto permitirá cruzar eventos musicales disponibles en Ticketmaster con itinerarios reales de transporte público de TfL para identificar qué conciertos en Londres son accesibles desde distintos hubs urbanos.

Durante el Sprint 1, los módulos se mantienen completamente independientes:

- Ticketmaster captura y persiste únicamente eventos.
- TfL captura y persiste únicamente itinerarios.
- No existe cruce de datos entre ambas fuentes.
- Cada módulo tiene su propia configuración, modelo, feeder, mapper, persistencia, controller y punto de entrada.

## Tecnologías

- Java 21
- Maven multimódulo
- IntelliJ IDEA
- OkHttp 4.12.0
- Gson 2.10.1
- SQLite JDBC 3.45.1.0
- JUnit Jupiter 5.10.2

## Estructura del proyecto

```text
paradiso/
├── pom.xml
├── README.md
├── docs/
│   └── sprint1/
│       ├── diagrama-clases-ticketmaster.png
│       ├── diagrama-clases-tfl.png
│       ├── modelo-datos-ticketmaster.png
│       └── modelo-datos-tfl.png
├── ticketmaster-module/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/org/ulpgc/paradiso/ticketmaster/
│       │   │   ├── Main.java
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── feeder/
│       │   │   ├── mapper/
│       │   │   ├── model/
│       │   │   └── persistence/
│       │   └── resources/
│       │       └── ticketmaster.properties.example
│       └── test/
│           └── java/org/ulpgc/paradiso/ticketmaster/mapper/
└── tfl-module/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/org/ulpgc/paradiso/tfl/
        │   │   ├── Main.java
        │   │   ├── config/
        │   │   ├── controller/
        │   │   ├── feeder/
        │   │   ├── mapper/
        │   │   ├── model/
        │   │   └── persistence/
        │   └── resources/
        │       └── tfl.properties.example
        └── test/
            └── java/org/ulpgc/paradiso/tfl/mapper/
```

Los archivos `.properties` reales no se versionan porque contienen claves de API. El repositorio incluye únicamente las plantillas `.properties.example`.

## Configuración

Antes de ejecutar los módulos, hay que crear los archivos de configuración reales a partir de las plantillas incluidas en el repositorio.

### Configuración de Ticketmaster

Crear el archivo:

```text
ticketmaster-module/src/main/resources/ticketmaster.properties
```

Contenido esperado:

```properties
api.key=TU_CLAVE_TICKETMASTER
countries=GB
cities=London
categories=music,festival
lookahead.days=14
capture.period.minutes=60
sqlite.path=data/ticketmaster.db
```

### Configuración de TfL

Crear el archivo:

```text
tfl-module/src/main/resources/tfl.properties
```

Contenido esperado:

```properties
app.key=TU_CLAVE_TFL
routes=KingsCross>O2Arena;Victoria>WembleyPark;Waterloo>BrixtonAcademy;Paddington>RoyalAlbertHall;LondonBridge>AlexandraPalace
capture.times=0900,1400,1900
capture.period.minutes=60
sqlite.path=data/tfl.db
```

## Ejecución

Cada módulo tiene su propio `Main.java` y puede ejecutarse de forma independiente.

### Ejecución desde IntelliJ

#### Ticketmaster

Clase principal:

```text
ticketmaster-module/src/main/java/org/ulpgc/paradiso/ticketmaster/Main.java
```

Para una ejecución única, añadir en **Program arguments**:

```text
--once
```

#### TfL

Clase principal:

```text
tfl-module/src/main/java/org/ulpgc/paradiso/tfl/Main.java
```

Para una ejecución única, añadir en **Program arguments**:

```text
--once
```

Sin el argumento `--once`, cada módulo arranca en modo periódico y ejecuta capturas usando `ScheduledExecutorService`.

### Ejecución desde terminal

Compilar y empaquetar el proyecto completo:

```bash
mvn clean package
```

Ejecutar Ticketmaster:

```bash
java -jar ticketmaster-module/target/ticketmaster-module-1.0-SNAPSHOT.jar --once
```

Ejecutar TfL:

```bash
java -jar tfl-module/target/tfl-module-1.0-SNAPSHOT.jar --once
```

## Persistencia incremental

Cada ejecución genera un identificador de lote (`capture_batch_id`) y registra metadatos en una tabla de control. Los datos capturados se insertan en tablas históricas. Las ejecuciones nuevas añaden filas; no se borran ni sobrescriben capturas anteriores.

### Ticketmaster

`tickermaster_capture_run` no existe en el proyecto. La tabla correcta es:

```text
ticketmaster_capture_run
```

Registra cada ejecución de captura: identificador de lote, inicio, fin, estado, alcance, registros obtenidos, registros insertados y error en caso de fallo.

```text
ticketmaster_event_capture
```

Registra los eventos capturados y mantiene relación con la ejecución mediante `capture_batch_id`.

Relación:

```text
ticketmaster_capture_run 1 ─── N ticketmaster_event_capture
```

### TfL

```text
tfl_capture_run
```

Registra cada ejecución de captura: identificador de lote, inicio, fin, estado, alcance, registros obtenidos, registros insertados y error en caso de fallo.

```text
tfl_journey_capture
```

Registra los itinerarios capturados y mantiene relación con la ejecución mediante `capture_batch_id`.

Relación:

```text
tfl_capture_run 1 ─── N tfl_journey_capture
```

## APIs utilizadas

### Ticketmaster Discovery API

- Endpoint base: `https://app.ticketmaster.com/discovery/v2/events.json`
- Autenticación: parámetro `apikey`
- Ámbito usado en Sprint 1:
  - País: GB
  - Ciudad: London
  - Categorías: music, festival
  - Ventana temporal: próximos 14 días

### TfL Unified API Journey Planner

- Endpoint base: `https://api.tfl.gov.uk/Journey/JourneyResults`
- Autenticación: parámetro `app_key`
- Ámbito usado en Sprint 1:
  - Rutas desde hubs principales de Londres hacia venues musicales
  - Fechas: día actual y día siguiente
  - Franjas: 09:00, 14:00 y 19:00
  - Modos: tube, bus, overground, elizabeth-line, dlr, tram y national-rail

## Tests

Ejecutar todos los tests:

```bash
mvn test
```

Tests incluidos:

```text
TicketmasterEventMapperTest
TflJourneyMapperTest
```

Los tests cubren el mapeo de JSON completo, respuestas sin datos y casos parciales en los que faltan campos opcionales.

## Independencia de módulos

Durante el Sprint 1, los módulos se mantienen desacoplados:

- `ticketmaster-module` no importa clases de `tfl-module`.
- `tfl-module` no importa clases de `ticketmaster-module`.
- Cada módulo se ejecuta por separado.
- Cada módulo persiste en su propia base de datos SQLite.
- No hay lógica de cruce de datos en este sprint.

## Documentación gráfica

Los diagramas del Sprint 1 se encuentran en:

```text
docs/sprint1/
```

Archivos incluidos:

```text
diagrama-clases-ticketmaster.png
diagrama-clases-tfl.png
modelo-datos-ticketmaster.png
modelo-datos-tfl.png
```

## Versionado del Sprint 1

La entrega del Sprint 1 queda identificada mediante el tag anotado:

```text
sprint-1
```

Este tag permite recuperar el estado exacto del repositorio correspondiente a la entrega del Sprint 1.

El desarrollo de los siguientes sprints continuará sobre la misma rama `main`, evolucionando los módulos existentes sin duplicar el proyecto en carpetas separadas por sprint.
