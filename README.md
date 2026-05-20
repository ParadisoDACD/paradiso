# Paradiso

**Paradiso** es una aplicación Java 21 multimódulo desarrollada para integrar eventos musicales de Londres con información de transporte público. El sistema captura datos desde fuentes externas, los publica como eventos, los almacena en un Event Store histórico y los explota desde una unidad de negocio que ofrece recomendaciones de rutas hacia conciertos mediante API REST y CLI interactiva.

El proyecto corresponde a la asignatura **Desarrollo de Aplicaciones para Ciencia de Datos** de la Universidad de Las Palmas de Gran Canaria.

---

## Índice

1. [Propuesta de valor](#propuesta-de-valor)
2. [Funcionalidades principales](#funcionalidades-principales)
3. [Arquitectura del sistema](#arquitectura-del-sistema)
4. [Arquitectura de la aplicación](#arquitectura-de-la-aplicación)
5. [Módulos del proyecto](#módulos-del-proyecto)
6. [Justificación de fuentes externas y datamart](#justificación-de-fuentes-externas-y-datamart)
7. [Formato de eventos y Event Store](#formato-de-eventos-y-event-store)
8. [Configuración](#configuración)
9. [Compilación](#compilación)
10. [Ejecución](#ejecución)
11. [CLI interactiva](#cli-interactiva)
12. [API REST](#api-rest)
13. [Datos generados de ejemplo](#datos-generados-de-ejemplo)
14. [Tests y validación](#tests-y-validación)
15. [Principios y patrones de diseño](#principios-y-patrones-de-diseño)
16. [Estructura del repositorio](#estructura-del-repositorio)
17. [Evolución por sprints](#evolución-por-sprints)

---

## Propuesta de valor

Paradiso permite planificar la asistencia a conciertos en Londres combinando dos dimensiones que normalmente se consultan por separado:

- eventos musicales disponibles;
- rutas de transporte público hacia los recintos donde se celebran.

La unidad de negocio construye un datamart de recomendaciones que relaciona conciertos, recintos, estaciones de origen y rutas TfL compatibles. A partir de esa información, el usuario puede consultar próximos conciertos, buscar eventos por texto, seleccionar un origen de salida y obtener rutas recomendadas con duración, modo de transporte, hora de salida, hora de llegada y puntuación.

El valor del sistema no está únicamente en capturar datos, sino en transformarlos en una vista de negocio preparada para responder preguntas útiles:

- qué conciertos están disponibles próximamente;
- desde qué orígenes TfL se puede planificar una salida;
- qué rutas existen hacia un recinto concreto;
- qué recomendaciones están disponibles para un concierto determinado;
- qué rutas son más convenientes según duración, número de tramos y proximidad al recinto.

---

## Funcionalidades principales

- Captura de eventos musicales en Londres mediante `ticketmaster-feeder`.
- Captura de rutas de transporte público mediante `tfl-feeder`.
- Publicación de eventos JSON en ActiveMQ mediante topics independientes.
- Persistencia histórica append-only mediante `eventstore-builder`.
- Organización del Event Store por topic, fuente y fecha.
- Reconstrucción del datamart desde eventos históricos.
- Consumo opcional en tiempo real desde ActiveMQ.
- Generación de recomendaciones precalculadas entre conciertos y rutas.
- Consulta mediante API REST.
- Consulta mediante CLI interactiva ejecutable desde IntelliJ.
- Configuración privada mediante variables de entorno, `.env` o ficheros `.properties` locales ignorados por Git.
- Pruebas automatizadas por módulo.

---

## Arquitectura del sistema

Paradiso sigue una arquitectura modular orientada a eventos. Los feeders capturan información externa y publican eventos en ActiveMQ. El módulo `eventstore-builder` consume esos eventos y los conserva en un Event Store histórico. El módulo `business-unit` carga eventos históricos, mantiene un datamart y expone la funcionalidad final al usuario.

![Arquitectura del sistema](docs/diagrams/system-architecture.png)

Flujo general:

```text
Fuentes externas
      │
      ▼
Feeders
      │
      ▼
ActiveMQ
      │
      ├──────────────► Event Store Builder ──────────────► Event Store
      │                                                        │
      ▼                                                        ▼
Business Unit ◄────────────────────────────────────── Carga histórica
      │
      ├──────────────► API REST
      └──────────────► CLI interactiva
```

La arquitectura final se aproxima a una **Kappa simplificada**: tanto los eventos históricos como los eventos recibidos en tiempo real son procesados por la misma lógica de negocio. El histórico inicializa el datamart y el consumo real-time permite mantenerlo actualizado durante la ejecución.

---

## Arquitectura de la aplicación

La aplicación se divide en módulos Maven independientes, cada uno con una responsabilidad clara. Dentro de cada módulo se separan las tareas de configuración, consumo externo, transformación, mensajería, persistencia, carga, consulta y exposición.

![Arquitectura de aplicación](docs/diagrams/application-architecture.png)

### Feeders

Los feeders aplican una estructura común:

```text
configuración ─► cliente externo ─► mapper ─► modelo interno ─► evento JSON ─► publisher JMS
```

### Event Store Builder

El constructor del Event Store se suscribe de forma durable a los topics configurados y escribe cada evento como una línea JSON independiente.

```text
subscriber JMS ─► validador de evento ─► resolución temporal ─► escritor append-only
```

### Business Unit

La unidad de negocio transforma los eventos en vistas optimizadas de consulta.

```text
EventStoreLoader ─┐
                  ├─► BusinessEventProcessor ─► BusinessIngestionService ─► Datamart
Subscriber JMS ───┘                                                     │
                                                                         ├─► REST API
                                                                         └─► CLI
```

---

## Módulos del proyecto

| Módulo | Responsabilidad | Patrón principal | Topic |
|---|---|---|---|
| `ticketmaster-feeder` | Captura eventos musicales, los transforma y los publica. | Adapter + Publisher | `TicketmasterEvent` |
| `tfl-feeder` | Captura rutas TfL para orígenes y destinos configurados. | Adapter + Publisher | `TflJourney` |
| `eventstore-builder` | Consume eventos y los almacena en ficheros históricos. | Durable Subscriber + Event Store | `TicketmasterEvent`, `TflJourney` |
| `business-unit` | Reconstruye el datamart y ofrece recomendaciones al usuario. | Datamart + Service Layer + REST/CLI | `TicketmasterEvent`, `TflJourney` |

---

## Justificación de fuentes externas y datamart

### Ticketmaster

Ticketmaster se utiliza como fuente de eventos porque proporciona datos adecuados para el caso de uso de Paradiso:

- identificador externo del evento;
- nombre del concierto o artista;
- fecha y hora;
- ciudad y país;
- recinto;
- género o clasificación;
- dirección pública del evento cuando está disponible en la respuesta externa.

Es una fuente dinámica porque el catálogo de eventos cambia con el tiempo. Cada captura se transforma en eventos propios del sistema, con timestamp y fuente de origen, para conservar su evolución en el Event Store.

### Transport for London

Transport for London se utiliza como fuente de rutas porque permite obtener itinerarios de transporte público dentro del mismo ámbito geográfico que los eventos: Londres.

El feeder de TfL trabaja con un catálogo controlado de orígenes y destinos. Esta decisión reduce capturas innecesarias, evita combinaciones masivas y centra la información en rutas útiles para los recintos musicales contemplados por el proyecto.

La fuente sigue siendo dinámica porque cada ejecución consulta la API externa para fechas y horas configuradas. Los resultados pueden variar por horarios, disponibilidad del servicio, incidencias o cambios operativos.

### Compatibilidad entre fuentes

La combinación de ambas fuentes es coherente porque Ticketmaster aporta el evento y su recinto, mientras que TfL aporta la movilidad hacia puntos cercanos a esos recintos. El sistema normaliza venues y destinos mediante catálogos internos de correspondencia para generar recomendaciones comparables.

### Diseño del datamart

El datamart de `business-unit` está implementado en memoria y se reconstruye desde el Event Store al arrancar. Esta decisión es adecuada porque el Event Store actúa como fuente histórica persistente y permite recomponer el estado de negocio cuando sea necesario.

Modelos principales:

| Modelo | Descripción |
|---|---|
| `ConcertRecord` | Vista de negocio de un evento musical. |
| `TransportRecord` | Vista de negocio de una ruta TfL. |
| `OriginRecord` | Origen de transporte disponible para consultas. |
| `ConcertRoutePlanRecord` | Recomendación precalculada entre concierto, origen y ruta. |

El datamart mantiene índices por evento, origen, artista, recinto y combinaciones relevantes para que las consultas REST y CLI no tengan que recalcular todas las relaciones en cada petición.

---

## Formato de eventos y Event Store

Los feeders publican mensajes JSON con una estructura común:

```json
{
  "ts": "2026-05-05T10:46:09.910902100Z",
  "ss": "ticketmaster-feeder",
  "payload": {
    "...": "..."
  }
}
```

| Campo | Descripción |
|---|---|
| `ts` | Timestamp UTC asociado al dato. |
| `ss` | Identificador del sistema productor. |
| `payload` | Datos propios del evento producido por cada feeder. |

El Event Store se organiza siguiendo la estructura:

```text
eventstore/{topic}/{ss}/{YYYYMMDD}.events
```

Cada fichero `.events` usa formato JSON Lines:

- un evento JSON completo por línea;
- escritura en modo append;
- sin sobrescribir capturas previas;
- separación temporal por fecha calculada a partir de `ts`.

Ejemplo:

```text
eventstore/TicketmasterEvent/ticketmaster-feeder/20260520.events
eventstore/TflJourney/tfl-feeder/20260520.events
```

---

## Configuración

La configuración privada no se versiona. El repositorio incluye plantillas seguras para documentar las variables y propiedades necesarias.

Prioridad de configuración:

```text
variables de entorno reales
        ↓
.env local
        ↓
ficheros .properties locales
        ↓
valores por defecto seguros
```

### Ficheros versionados

| Fichero | Propósito |
|---|---|
| `.env.example` | Plantilla general de variables de entorno. |
| `ticketmaster.properties.example` | Plantilla local para `ticketmaster-feeder`. |
| `tfl.properties.example` | Plantilla local para `tfl-feeder`. |
| `eventstore-builder.properties.example` | Plantilla local para `eventstore-builder`. |
| `business-unit.properties.example` | Plantilla local para `business-unit`. |

### Ficheros no versionados

| Fichero | Motivo |
|---|---|
| `.env` | Contiene valores locales de ejecución. |
| `ticketmaster.properties` | Puede contener clave o configuración privada. |
| `tfl.properties` | Puede contener clave o configuración privada. |
| `eventstore-builder.properties` | Configuración local del Event Store. |
| `business-unit.properties` | Configuración local del datamart, API y subscriber. |

### Variables principales

| Variable | Uso |
|---|---|
| `PARADISO_BROKER_URL` | Dirección del broker ActiveMQ. |
| `PARADISO_TOPICS` | Topics consumidos por módulos subscriber. |
| `PARADISO_EVENTSTORE_PATH` | Ruta del Event Store. |
| `PARADISO_API_PORT` | Puerto de la API REST. |
| `PARADISO_SUBSCRIBER_ENABLED` | Activa o desactiva consumo real-time en `business-unit`. |
| `PARADISO_TICKETMASTER_API_KEY` | Clave local de Ticketmaster. |
| `PARADISO_TICKETMASTER_BASE_URL` | URL base externa de Ticketmaster. |
| `PARADISO_TFL_APP_KEY` | Clave local de TfL. |
| `PARADISO_TFL_BASE_URL` | URL base externa de TfL. |

Los nombres de variables pueden convivir con propiedades locales equivalentes. Las claves reales y las URLs base externas no deben escribirse directamente en las clases Java.

### Ejemplo de `.env`

```env
PARADISO_BROKER_URL=tcp://localhost:61616
PARADISO_TOPICS=TicketmasterEvent,TflJourney
PARADISO_EVENTSTORE_PATH=eventstore
PARADISO_API_PORT=7000
PARADISO_SUBSCRIBER_ENABLED=false

PARADISO_TICKETMASTER_API_KEY=<ticketmaster-api-key>
PARADISO_TICKETMASTER_BASE_URL=<ticketmaster-base-url>

PARADISO_TFL_APP_KEY=<tfl-app-key>
PARADISO_TFL_BASE_URL=<tfl-base-url>
```

---

## Compilación

Desde la raíz del repositorio:

```bash
mvn clean package
```

Compilar un módulo concreto:

```bash
mvn -pl business-unit package
```

Ejecutar los tests:

```bash
mvn test
```

---

## Ejecución

### ActiveMQ

ActiveMQ debe estar disponible antes de ejecutar los módulos que publican o consumen eventos en tiempo real.

Configuración habitual:

```text
Broker URL: tcp://localhost:61616
```

### Event Store Builder

```bash
java -jar eventstore-builder/target/eventstore-builder-1.0-SNAPSHOT.jar
```

El módulo queda suscrito a los topics configurados y escribe eventos en la ruta definida para el Event Store.

### Ticketmaster Feeder

Ejecución única:

```bash
java -jar ticketmaster-feeder/target/ticketmaster-feeder-1.0-SNAPSHOT.jar --once
```

Ejecución periódica:

```bash
java -jar ticketmaster-feeder/target/ticketmaster-feeder-1.0-SNAPSHOT.jar
```

### TfL Feeder

Ejecución única:

```bash
java -jar tfl-feeder/target/tfl-feeder-1.0-SNAPSHOT.jar --once
```

Ejecución periódica:

```bash
java -jar tfl-feeder/target/tfl-feeder-1.0-SNAPSHOT.jar
```

### Business Unit

Modo API REST:

```bash
java -jar business-unit/target/business-unit-1.0-SNAPSHOT.jar
```

Modo CLI interactiva:

```bash
java -jar business-unit/target/business-unit-1.0-SNAPSHOT.jar --cli
```

En modo CLI, la API REST también queda disponible mientras la consola interactiva está activa.

---

## CLI interactiva

La CLI permite consultar el datamart desde consola sin utilizar herramientas externas.

Funcionalidades disponibles:

1. Ver próximos conciertos en Londres.
2. Buscar conciertos por artista, recinto o nombre.
3. Seleccionar un concierto.
4. Seleccionar un origen TfL.
5. Consultar rutas recomendadas cuando existan planes precalculados.

Ejemplo de flujo:

```text
Paradiso — Planificador de conciertos

[1] Ver próximos conciertos en Londres
[2] Buscar conciertos por artista, recinto o nombre
[3] Salir

Elige una opción: 1
```

La CLI utiliza el mismo datamart que la API REST. Por tanto, los resultados dependen de los eventos históricos cargados y de los eventos recibidos en tiempo real si el subscriber está activado.

---

## API REST

La API REST expone el datamart y las recomendaciones de la unidad de negocio.

### Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/` | Información general de la API. |
| GET | `/status` | Estado del datamart. |
| GET | `/concerts` | Conciertos disponibles. |
| GET | `/concerts?query={texto}` | Búsqueda de conciertos por texto. |
| GET | `/concerts/upcoming` | Próximos conciertos. |
| GET | `/concerts/{id}` | Detalle de un concierto. |
| GET | `/concerts/{id}/routes` | Rutas recomendadas para un concierto. |
| GET | `/concerts/{id}/routes?origin={origin}` | Rutas para un concierto desde un origen. |
| GET | `/recommendations` | Recomendaciones disponibles. |
| GET | `/recommendations?page={page}&size={size}` | Recomendaciones paginadas. |
| GET | `/recommendations?artist={artist}&origin={origin}&venue={venue}` | Recomendaciones filtradas. |
| GET | `/artists/{artist}/recommendations` | Recomendaciones por artista. |
| GET | `/origins` | Orígenes TfL cargados. |
| GET | `/venues` | Recintos normalizados y destinos asociados. |
| GET | `/transport` | Rutas TfL cargadas. |

### Ejemplo de estado del datamart

```json
{
  "concerts": 167,
  "transports": 192,
  "origins": 5,
  "routePlans": 21,
  "lastProcessedAt": "2026-05-20T09:34:00Z"
}
```

### Ejemplo de recomendación

```json
{
  "eventId": "G5vHZbSELNbiT",
  "artistName": "Joe Bonamassa",
  "eventName": "Joe Bonamassa",
  "venueName": "Royal Albert Hall",
  "originName": "Paddington Underground Station",
  "destinationStopName": "High Street Kensington",
  "departureTime": "2026-05-06T19:07:00",
  "arrivalTime": "2026-05-06T19:13:00",
  "durationMinutes": 6,
  "numberOfLegs": 1,
  "firstLegMode": "tube",
  "score": 1.0,
  "matchType": "EXACT_VENUE_STOP"
}
```

---

## Datos generados de ejemplo

El repositorio incluye muestras reducidas en `docs/samples/`. Estas muestras documentan el formato de los datos generados sin versionar capturas completas ni información privada.

Estructura prevista:

```text
docs/samples/
├── eventstore/
│   ├── TicketmasterEvent/
│   │   └── ticketmaster-feeder/
│   │       └── sample.events
│   └── TflJourney/
│       └── tfl-feeder/
│           └── sample.events
└── datamart/
    ├── status.json
    ├── concerts.json
    ├── transport.json
    ├── origins.json
    ├── venues.json
    └── recommendations.json
```

Las muestras del Event Store mantienen el formato JSON Lines. Las muestras del datamart representan respuestas de la API REST tras procesar eventos históricos.

---

## Tests y validación

Ejecutar todos los tests:

```bash
mvn test
```

Ejecutar tests por módulo:

```bash
mvn -pl ticketmaster-feeder test
mvn -pl tfl-feeder test
mvn -pl eventstore-builder test
mvn -pl business-unit test
```

Aspectos cubiertos:

- serialización de eventos con `ts`, `ss` y `payload`;
- mapeo desde respuestas externas a modelos internos;
- escritura append-only del Event Store;
- lectura histórica desde ficheros `.events`;
- reconstrucción del datamart;
- normalización de recintos;
- generación de recomendaciones;
- scoring de rutas;
- endpoints REST principales;
- formato de salida de la CLI.

---

## Principios y patrones de diseño

### Separación de responsabilidades

Cada módulo separa consumo, transformación, publicación, persistencia, carga histórica y consulta. Esta división facilita pruebas, mantenimiento y evolución.

### Publisher/Subscriber

Los feeders publican eventos en topics independientes y los consumidores se suscriben a ellos. Esta estrategia desacopla productores y consumidores.

### Durable Subscriber

`eventstore-builder` utiliza suscripción durable para conservar eventos no consumidos durante interrupciones temporales.

### Event Store

Los eventos se almacenan como fuente histórica append-only. El sistema conserva capturas pasadas y permite reconstruir vistas de negocio.

### Datamart

`business-unit` mantiene una vista optimizada de consulta para evitar recalcular recomendaciones en cada petición.

### Materialized View

Las recomendaciones `ConcertRoutePlanRecord` actúan como vista materializada entre conciertos, recintos, orígenes y rutas.

### Service Layer

La lógica de negocio queda encapsulada en servicios. La API REST y la CLI consumen esos servicios sin acceder directamente a detalles de ingestión o procesamiento.

### Adapter y Mapper

Los feeders aíslan las fuentes externas mediante clientes y transforman las respuestas a modelos internos antes de publicarlas.

### Configuración externa

Las claves, URLs base y parámetros de ejecución se mantienen fuera del código fuente.

---

## Estructura del repositorio

```text
paradiso/
├── pom.xml
├── README.md
├── .env.example
├── ticketmaster-feeder/
│   └── src/main/java/org/ulpgc/paradiso/ticketmaster/
├── tfl-feeder/
│   └── src/main/java/org/ulpgc/paradiso/tfl/
├── eventstore-builder/
│   └── src/main/java/org/ulpgc/paradiso/eventstorebuilder/
├── business-unit/
│   └── src/main/java/org/ulpgc/paradiso/businessunit/
│       ├── api/
│       ├── cli/
│       ├── config/
│       ├── datamart/
│       ├── event/
│       ├── loader/
│       ├── messaging/
│       ├── recommendation/
│       ├── service/
│       ├── utils/
│       ├── venue/
│       └── Main.java
└── docs/
    ├── diagrams/
    └── samples/
```

Los directorios generados durante la ejecución, los artefactos Maven, las claves reales, los `.env` locales y los `.properties` privados quedan fuera del versionado.

---

## Evolución por sprints

| Sprint | Resultado |
|---|---|
| Sprint 1 | Captura de dos fuentes externas dinámicas y persistencia incremental independiente. |
| Sprint 2 | Arquitectura Publisher/Subscriber con ActiveMQ y Event Store JSON Lines. |
| Sprint 3 | Business Unit con carga histórica, consumo real-time, datamart, API REST y CLI interactiva. |
