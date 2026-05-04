# Sprint 1 — Captura de datos: Ticketmaster + TfL

Asignatura: Desarrollo de Aplicaciones para Ciencia de Datos  
Universidad: Universidad de Las Palmas de Gran Canaria  
Sprint: 1 de 3

## Descripción

Proyecto multimódulo en Java 21 que captura datos de dos fuentes externas y los persiste de forma incremental en bases de datos SQLite independientes.

### Módulo 1 — Ticketmaster

Captura eventos culturales de música y festivales en Londres, Reino Unido, usando la Ticketmaster Discovery API v2.

Los datos se persisten en:

```text
data/ticketmaster.db
```

### Módulo 2 — TfL

Captura itinerarios de transporte público desde hubs principales de Londres hasta venues asociados a conciertos, usando la TfL Unified API.

Los datos se persisten en:

```text
data/tfl.db
```

### Propuesta de valor

En sprints posteriores, el proyecto permitirá cruzar eventos musicales de Ticketmaster con itinerarios reales de transporte público de TfL para identificar qué conciertos en Londres son accesibles desde distintos hubs urbanos.

En el Sprint 1 no se cruzan datos entre fuentes. Cada módulo consume, transforma y persiste sus propios datos de forma independiente.

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
│       │       ├── ticketmaster.properties.example
│       │       └── ticketmaster.properties
│       └── test/
│           └── java/org/ulpgc/paradiso/ticketmaster/mapper/
├── tfl-module/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/org/ulpgc/paradiso/tfl/
│       │   │   ├── Main.java
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── feeder/
│       │   │   ├── mapper/
│       │   │   ├── model/
│       │   │   └── persistence/
│       │   └── resources/
│       │       ├── tfl.properties.example
│       │       └── tfl.properties
│       └── test/
│           └── java/org/ulpgc/paradiso/tfl/mapper/
└── docs/
    └── sprint1/
        ├── diagrama-clases-ticketmaster.png
        ├── diagrama-clases-tfl.png
        ├── modelo-datos-ticketmaster.png
        └── modelo-datos-tfl.png
```

> Los archivos `.properties` reales no se suben al repositorio. Solo se versionan los `.properties.example`.

## Configuración

Antes de ejecutar cada módulo, hay que crear los archivos de configuración reales a partir de las plantillas.

### Ticketmaster

Crear:

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

### TfL

Crear:

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

## Ejecución desde IntelliJ

Cada módulo tiene su propio `Main.java`.

### Ticketmaster

Ejecutar:

```text
ticketmaster-module/src/main/java/org/ulpgc/paradiso/ticketmaster/Main.java
```

Para una ejecución única, añadir en **Program arguments**:

```text
--once
```

### TfL

Ejecutar:

```text
tfl-module/src/main/java/org/ulpgc/paradiso/tfl/Main.java
```

Para una ejecución única, añadir en **Program arguments**:

```text
--once
```

## Ejecución desde terminal

Compilar el proyecto completo:

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

## Persistencia

Cada módulo persiste en su propia base de datos SQLite.

### Ticketmaster

```text
data/ticketmaster.db
```

Tablas:

```text
ticketmaster_capture_run
ticketmaster_event_capture
```

### TfL

```text
data/tfl.db
```

Tablas:

```text
tfl_capture_run
tfl_journey_capture
```

Las tablas `capture_run` guardan metadatos de cada ejecución: identificador de lote, fecha de inicio, fecha de fin, estado, alcance y contadores.

Las tablas históricas guardan los datos capturados. La persistencia es incremental: las ejecuciones nuevas añaden filas, no borran las anteriores.

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

## Independencia de módulos

Durante el Sprint 1 los módulos son independientes:

- `ticketmaster-module` no importa clases de `tfl-module`.
- `tfl-module` no importa clases de `ticketmaster-module`.
- Cada módulo tiene su propia configuración, feeder, mapper, modelo, persistencia, controller y `Main`.
- Cada módulo persiste en su propia base de datos SQLite.

## APIs utilizadas

### Ticketmaster Discovery API

- Endpoint base: `https://app.ticketmaster.com/discovery/v2/events.json`
- Autenticación: parámetro `apikey`
- Ámbito usado en Sprint 1:
    - País: GB
    - Ciudad: London
    - Categorías: music, festival
    - Ventana temporal: próximos 14 días

### TfL Unified API

- Endpoint base: `https://api.tfl.gov.uk/Journey/JourneyResults`
- Autenticación: parámetro `app_key`
- Ámbito usado en Sprint 1:
    - Rutas desde hubs principales de Londres hacia venues musicales
    - Fechas: día actual y día siguiente
    - Franjas: 09:00, 14:00, 19:00

## Modelo de datos

### Ticketmaster

`ticketsmaster_capture_run` registra cada ejecución de captura.

`ticketmaster_event_capture` registra los eventos capturados y mantiene relación con la ejecución mediante `capture_batch_id`.

Relación:

```text
ticketmaster_capture_run 1 ─── N ticketmaster_event_capture
```

### TfL

`tfl_capture_run` registra cada ejecución de captura.

`tfl_journey_capture` registra los itinerarios capturados y mantiene relación con la ejecución mediante `capture_batch_id`.

Relación:

```text
tfl_capture_run 1 ─── N tfl_journey_capture
```

## Documentación gráfica

Los diagramas del Sprint 1 se encuentran en:

```text
docs/sprint1/
```

Archivos esperados:

```text
diagrama-clases-ticketmaster.png
diagrama-clases-tfl.png
modelo-datos-ticketmaster.png
modelo-datos-tfl.png
```

## Cierre del Sprint 1

Al finalizar el Sprint 1 se debe crear un tag anotado:

```bash
git tag -a sprint-1 -m "Cierre del Sprint 1: captura de datos Ticketmaster + TfL"
git push origin sprint-1
```

El desarrollo continuará en la misma rama `main` durante los siguientes sprints.
