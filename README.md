# Paradiso

Proyecto de la asignatura **Desarrollo de Aplicaciones para Ciencia de Datos** de la Universidad de Las Palmas de Gran Canaria.

**Paradiso** es una aplicación Java 21 multimódulo que integra eventos musicales en Londres con rutas de transporte público de Transport for London. El sistema captura datos externos, los publica en un broker ActiveMQ, los persiste como eventos históricos en un Event Store y los explota desde una unidad de negocio mediante una API REST.

La versión actual del repositorio corresponde al **Sprint 3**, centrado en la explotación de datos en tiempo real y en diferido mediante el módulo `business-unit`.

---

## Estado actual del proyecto

La rama `main` contiene la implementación actual del sistema con los módulos:

- `ticketmaster-feeder`
- `tfl-feeder`
- `eventstore-builder`
- `business-unit`

Los sprints anteriores quedan preservados como hitos históricos del repositorio mediante tags Git cuando están disponibles.

```bash
git checkout sprint-1
git checkout sprint-2
git checkout main
```

---

## Objetivo funcional

Paradiso permite combinar dos tipos de información:

- **Eventos de Ticketmaster**, principalmente conciertos y eventos musicales en Londres.
- **Rutas de Transport for London**, calculadas desde orígenes relevantes de Londres hacia paradas cercanas a venues.

A partir de esos datos, el módulo `business-unit` genera recomendaciones de transporte para responder preguntas como:

- qué rutas hay disponibles para llegar a un concierto concreto;
- qué conciertos de un artista tienen rutas calculadas;
- cómo llegar desde un origen concreto a un evento;
- qué eventos son accesibles desde una estación determinada;
- qué ruta tiene mejor puntuación según duración, número de tramos y hora de llegada.

---

## Arquitectura general

```text
Ticketmaster API
    ↓
ticketmaster-feeder
    ↓ publica eventos en ActiveMQ
Topic: TicketmasterEvent
    ↓
eventstore-builder
    ↓ persiste JSON Lines en eventstore

TfL Unified API
    ↓
tfl-feeder
    ↓ publica eventos en ActiveMQ
Topic: TflJourney
    ↓
eventstore-builder
    ↓ persiste JSON Lines en eventstore

Event Store + ActiveMQ
    ↓
business-unit
    ↓ carga histórico, consume tiempo real y mantiene datamart
API REST
```

Flujo conceptual completo:

```text
API externa
    → Feeder OkHttp
    → Mapper de modelo interno
    → Serializer de evento broker {ts, ss, payload}
    → ActiveMQ Topic
    → Event Store JSON Lines
    → Business Unit
    → Datamart
    → API REST
```

---

## Módulos

| Módulo | Rol | Descripción | Topics |
|---|---|---|---|
| `ticketmaster-feeder` | Publisher | Captura eventos de Ticketmaster y publica eventos JSON en ActiveMQ | `TicketmasterEvent` |
| `tfl-feeder` | Publisher | Captura itinerarios TfL entre orígenes y destinos configurados | `TflJourney` |
| `eventstore-builder` | Subscriber durable | Consume eventos desde ActiveMQ y los persiste como `.events` | `TicketmasterEvent`, `TflJourney` |
| `business-unit` | Unidad de negocio | Carga histórico, consume tiempo real, mantiene datamart y expone API REST | `TicketmasterEvent`, `TflJourney` |

---

## Tecnologías principales

- **Java 21**
- **Maven multimódulo**
- **IntelliJ IDEA**
- **Apache ActiveMQ Classic** como broker externo
- **JMS / ActiveMQ Client 5.15.12**
- **OkHttp** para consumo de APIs externas
- **Gson** para serialización y parseo JSON
- **Javalin** para la API REST de `business-unit`
- **JUnit 5** para pruebas automatizadas

---

## Broker de mensajería

El broker utilizado es **Apache ActiveMQ Classic**. Se ejecuta como una aplicación externa al proyecto Java.

Configuración local habitual:

```text
Broker URL: tcp://localhost:61616
Web Console: http://127.0.0.1:8161/admin
```

Credenciales por defecto de la consola web:

```text
admin / admin
```

El proyecto usa la dependencia `activemq-client` con versión `5.15.12`, manteniendo compatibilidad con `javax.jms`.

---

## Formato de evento

Todos los mensajes publicados en ActiveMQ son `TextMessage` con contenido JSON.

Estructura común:

```json
{
  "ts": "2026-05-05T10:46:09.910902100Z",
  "ss": "tfl-feeder",
  "payload": {
    "...": "..."
  }
}
```

Campos:

| Campo | Descripción |
|---|---|
| `ts` | Timestamp UTC asociado al dato. Se usa para organizar temporalmente el Event Store. |
| `ss` | Source system. Identifica el módulo productor del evento. |
| `payload` | Objeto completo del modelo interno serializado a JSON. |

Criterio de `ts` por fuente:

| Fuente | Valor de `ts` |
|---|---|
| Ticketmaster | `dateTimeIso` cuando existe; `capturedAt` como fallback |
| TfL | `capturedAt`, porque `startDateTime` no incluye zona horaria |

---

## Event Store

El Event Store generado por `eventstore-builder` sigue la estructura:

```text
eventstore/{topic}/{ss}/{YYYYMMDD}.events
```

Ejemplo:

```text
eventstore/
├── TicketmasterEvent/
│   └── ticketmaster-feeder/
│       ├── 20260505.events
│       └── ...
└── TflJourney/
    └── tfl-feeder/
        ├── 20260505.events
        └── ...
```

Cada fichero `.events` usa formato **JSON Lines / NDJSON**:

- un evento JSON completo por línea;
- sin array envolvente;
- sin comas entre eventos;
- escritura en modo append;
- el nombre del fichero se obtiene a partir del campo `ts`.

Ejemplo:

```jsonl
{"ts":"2026-05-05T17:30:00Z","ss":"ticketmaster-feeder","payload":{"externalEventId":"17uYvxG65mp7JRS","name":"ZAZ"}}
{"ts":"2026-05-05T10:46:09.910902100Z","ss":"tfl-feeder","payload":{"journeyHash":"6e95af24028b418b"}}
```

---

## Configuración

Cada módulo utiliza un fichero `.properties.example` versionado como plantilla. Los ficheros `.properties` reales no deben subirse al repositorio.

### Ticketmaster Feeder

Plantilla:

```text
ticketmaster-feeder/src/main/resources/ticketmaster.properties.example
```

Fichero local esperado:

```text
ticketmaster-feeder/src/main/resources/ticketmaster.properties
```

Propiedades principales:

```properties
api.key=TU_CLAVE_TICKETMASTER
countries=GB
cities=London
categories=music,festival
lookahead.days=14
capture.period.minutes=60
broker.url=tcp://localhost:61616
topic.name=TicketmasterEvent
source.system=ticketmaster-feeder
```

### TfL Feeder

Plantilla:

```text
tfl-feeder/src/main/resources/tfl.properties.example
```

Fichero local esperado:

```text
tfl-feeder/src/main/resources/tfl.properties
```

El feeder de TfL trabaja con catálogos de orígenes y destinos. Los orígenes representan estaciones relevantes de Londres desde las que un usuario podría iniciar su viaje. Los destinos se mantienen limitados a paradas cercanas a venues musicales usados por el proyecto, para que las rutas capturadas estén alineadas con las recomendaciones generadas por la unidad de negocio.
Ejemplo de configuración:

```properties
app.key=TU_CLAVE_TFL
origins=KingsCross,Victoria,Waterloo,Paddington,LondonBridge,LiverpoolStreet,Euston,Marylebone,CharingCross,Stratford,CanaryWharf,BakerStreet,OxfordCircus,PiccadillyCircus,LeicesterSquare,TottenhamCourtRoad,Farringdon,Blackfriars,Westminster,Bank,Moorgate,GreenPark,Holborn,SouthKensington,Hammersmith,ShepherdsBush,Whitechapel,CamdenTown,NottingHillGate,Heathrow
destinations=O2Arena,WembleyPark,BrixtonAcademy,RoyalAlbertHall,AlexandraPalace
capture.times=1530,1600,1630,1700,1730,1800,1815,1830,1845,1900,1915,1930,1945,2000,2030
capture.start.day.offset=0
capture.days.ahead=10
request.sleep.ms=150
http.connect.timeout.seconds=10
http.read.timeout.seconds=45
http.call.timeout.seconds=60
request.max.retries=2
request.retry.backoff.ms=1000
capture.period.minutes=90
broker.url=tcp://localhost:61616
topic.name=TflJourney
source.system=tfl-feeder
```

También se mantiene compatibilidad con la configuración legacy basada en `routes=origen>destino` cuando no se definen `origins` y `destinations`.

### Event Store Builder

Plantilla:

```text
eventstore-builder/src/main/resources/eventstore-builder.properties.example
```

Fichero local esperado:

```text
eventstore-builder/src/main/resources/eventstore-builder.properties
```

Contenido típico:

```properties
broker.url=tcp://localhost:61616
client.id=paradiso-eventstore-builder
topics=TicketmasterEvent,TflJourney
eventstore.path=eventstore
```

### Business Unit

Plantilla:

```text
business-unit/src/main/resources/business-unit.properties.example
```

Fichero local esperado:

```text
business-unit/src/main/resources/business-unit.properties
```

Propiedades habituales:

```properties
broker.url=tcp://localhost:61616
client.id=paradiso-business-unit
topics=TicketmasterEvent,TflJourney
eventstore.path=eventstore
api.port=7000
subscriber.enabled=true
```

---

## Business Unit y Sprint 3

El módulo `business-unit` explota los eventos históricos y en tiempo real para construir un datamart local en memoria.

### Flujo de arranque

```text
1. Crea el datamart en memoria.
2. Crea VenueNormalizer.
3. Crea RouteScoringService.
4. Crea RecommendationBuilder.
5. Crea BusinessIngestionService.
6. Carga eventos históricos desde eventstore.
7. Reconstruye recomendaciones precalculadas.
8. Arranca el subscriber en tiempo real.
9. Arranca la API REST.
```

### Datamart

El datamart almacena:

| Modelo | Descripción |
|---|---|
| `ConcertRecord` | Evento procedente de Ticketmaster |
| `TransportRecord` | Ruta procedente de TfL |
| `OriginRecord` | Origen disponible para recomendaciones |
| `VenueStopMapping` | Relación entre venue y parada TfL cercana |
| `ConcertRoutePlanRecord` | Recomendación precalculada para un concierto y una ruta |

Además, mantiene índices para consultar rápidamente por:

- evento;
- artista;
- origen;
- artista + origen;
- origen + destino.

### Componentes principales

| Clase | Responsabilidad |
|---|---|
| `BusinessEventProcessor` | Convierte eventos JSON en modelos internos |
| `BusinessIngestionService` | Centraliza la ingestión de conciertos y rutas |
| `VenueNormalizer` | Normaliza venues de Ticketmaster hacia paradas TfL |
| `RecommendationBuilder` | Genera recomendaciones precalculadas |
| `RouteScoringService` | Calcula la puntuación de una ruta |
| `Datamart` | Guarda datos base, orígenes, planes e índices |
| `RestApi` | Expone las consultas al usuario final |

### Vista materializada de recomendaciones

Las recomendaciones se guardan como una vista materializada:

```text
concierto + venue normalizado + origen + ruta TfL compatible
```

Esto evita recalcular rutas en cada consulta REST y permite responder rápidamente por evento, artista, origen o combinación de filtros.

---

## Ejecución

### 1. Compilar y empaquetar

Desde la raíz del proyecto:

```bash
mvn clean package
```

También puede compilarse un único módulo:

```bash
mvn -pl business-unit package
```

El empaquetado genera los JAR ejecutables de los módulos.

### 2. Arrancar ActiveMQ Classic

Desde la carpeta de instalación de ActiveMQ:

```powershell
cd C:\apache-activemq-5.19.6
.\bin\win64\activemq.bat console
```

El broker queda escuchando en:

```text
tcp://localhost:61616
```

### 3. Arrancar Event Store Builder

Desde la raíz del proyecto:

```powershell
java -jar .\eventstore-builder\target\eventstore-builder-1.0-SNAPSHOT.jar
```

Este proceso queda escuchando indefinidamente los topics configurados.

### 4. Ejecutar feeders en modo one-shot

Ticketmaster:

```powershell
java -jar .\ticketmaster-feeder\target\ticketmaster-feeder-1.0-SNAPSHOT.jar --once
```

TfL:

```powershell
java -jar .\tfl-feeder\target\tfl-feeder-1.0-SNAPSHOT.jar --once
```

### 5. Ejecutar feeders en modo periódico

Sin `--once`, cada feeder ejecuta capturas periódicas según `capture.period.minutes`:

```powershell
java -jar .\ticketmaster-feeder\target\ticketmaster-feeder-1.0-SNAPSHOT.jar
java -jar .\tfl-feeder\target\tfl-feeder-1.0-SNAPSHOT.jar
```

### 6. Arrancar Business Unit

```powershell
java -jar .\business-unit\target\business-unit-1.0-SNAPSHOT.jar
```

La API REST queda disponible en:

```text
http://localhost:7000
```

---

## API REST

### Estado del datamart

```powershell
Invoke-RestMethod http://localhost:7000/status | ConvertTo-Json -Depth 5
```

Ejemplo orientativo de respuesta:

```json
{
  "concerts": 167,
  "transports": 1589,
  "origins": 30,
  "routePlans": 339,
  "lastProcessedAt": "2026-07-09T17:30:00Z"
}
```

### Endpoints principales

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/status` | Estado general del datamart y de la unidad de negocio |
| GET | `/concerts` | Lista de conciertos disponibles en el datamart |
| GET | `/concerts?query={texto}` | Búsqueda de conciertos por texto |
| GET | `/concerts/upcoming` | Próximos conciertos |
| GET | `/concerts/upcoming?query={text}&limit={n}` | Próximos conciertos filtrados por texto y límite |
| GET | `/concerts/{id}` | Detalle de un concierto |
| GET | `/concerts/{id}/routes` | Rutas recomendadas precalculadas para un concierto |
| GET | `/concerts/{id}/routes?origin={origin}` | Rutas recomendadas para un concierto desde un origen concreto |
| GET | `/artists/{artist}/recommendations` | Recomendaciones por artista |
| GET | `/artists/{artist}/recommendations?origin={origin}` | Recomendaciones por artista desde un origen concreto |
| GET | `/recommendations` | Consulta general de recomendaciones precalculadas |
| GET | `/recommendations?artist={artist}&origin={origin}&venue={venue}&fromDate={yyyy-mm-dd}&untilDate={yyyy-mm-dd}` | Recomendaciones filtradas |
| GET | `/recommendations?page={page}&size={size}` | Recomendaciones paginadas |
| GET | `/origins` | Orígenes TfL disponibles |
| GET | `/venues` | Venues conocidos y paradas TfL asociadas |

### Endpoint de diagnóstico

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/transport` | Inspección de rutas TfL cargadas en el datamart |

### Endpoints legacy

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/concerts/{id}/transport` | Endpoint mantenido por compatibilidad |
| GET | `/recommendations/{id}` | Endpoint mantenido por compatibilidad |

Los endpoints legacy se conservan para no romper compatibilidad, pero el flujo principal del Sprint 3 debe probarse mediante `/concerts/{id}/routes`, `/artists/{artist}/recommendations` y `/recommendations`.

### Ejemplos de consultas

Consultar orígenes:

```powershell
Invoke-RestMethod http://localhost:7000/origins | ConvertTo-Json -Depth 5
```

Consultar venues mapeados:

```powershell
Invoke-RestMethod http://localhost:7000/venues | ConvertTo-Json -Depth 8
```

Consultar recomendaciones paginadas:

```powershell
Invoke-RestMethod "http://localhost:7000/recommendations?page=0&size=5" | ConvertTo-Json -Depth 10
```

Consultar rutas para un concierto concreto:

```powershell
Invoke-RestMethod "http://localhost:7000/concerts/G5vHZbSELNbiT/routes?page=0&size=5" | ConvertTo-Json -Depth 10
```

Consultar recomendaciones por artista:

```powershell
Invoke-RestMethod "http://localhost:7000/artists/Joe%20Bonamassa/recommendations?page=0&size=5" | ConvertTo-Json -Depth 10
```

Consultar recomendaciones por origen:

```powershell
Invoke-RestMethod "http://localhost:7000/recommendations?origin=Paddington&page=0&size=5" | ConvertTo-Json -Depth 10
```

### Ejemplo de recomendación

```json
{
  "eventId": "G5vHZbSELNbiT",
  "artistName": "Joe Bonamassa",
  "venueName": "Royal Albert Hall",
  "originKey": "Paddington",
  "originName": "Paddington Underground Station",
  "destinationStopKey": "RoyalAlbertHall",
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

Consultar recomendaciones filtradas por artista, origen y venue:

```powershell
Invoke-RestMethod "http://localhost:7000/recommendations?artist=Joe%20Bonamassa&origin=Paddington&venue=Royal%20Albert%20Hall&page=0&size=5" | ConvertTo-Json -Depth 10
```

---

## Verificación básica

Listar ficheros generados:

```powershell
Get-ChildItem -Recurse .\eventstore
```

Ver primeras líneas de eventos:

```powershell
Get-Content .\eventstore\TicketmasterEvent\*\*.events -TotalCount 2
Get-Content .\eventstore\TflJourney\*\*.events -TotalCount 2
```

Ejecutar tests:

```bash
mvn test
```

Ejecutar tests de un módulo concreto:

```bash
mvn -pl business-unit test
```

---

## Tests automatizados

El proyecto incluye pruebas unitarias e integración ligera para los módulos principales.

En `business-unit` se validan, entre otros aspectos:

- carga histórica desde el Event Store;
- procesamiento de eventos Ticketmaster y TfL;
- actualización del datamart;
- normalización de venues;
- generación de recomendaciones precalculadas;
- scoring de rutas;
- consulta de recomendaciones por evento, artista y origen.

La suite debe ejecutarse sin fallos antes de cada entrega:

```text
mvn test
```

El número exacto de tests puede variar conforme se añadan pruebas de validación, pero la entrega final debe mantenerse en verde.

---

## Estructura del repositorio

```text
paradiso/
├── pom.xml
├── README.md
├── ticketmaster-feeder/
│   └── src/main/java/org/ulpgc/paradiso/ticketmaster/
├── tfl-feeder/
│   └── src/main/java/org/ulpgc/paradiso/tfl/
├── eventstore-builder/
│   └── src/main/java/org/ulpgc/paradiso/eventstorebuilder/
├── business-unit/
│   └── src/main/java/org/ulpgc/paradiso/businessunit/
│       ├── api/
│       ├── config/
│       ├── datamart/
│       ├── event/
│       ├── loader/
│       ├── messaging/
│       ├── recommendation/
│       ├── service/
│       ├── venue/
│       └── Main.java
└── docs/
    ├── sprint1/
    ├── sprint2/
    └── sprint3/
```

---

## Versionado y artefactos locales

El repositorio mantiene el código fuente, configuración de ejemplo, documentación y tests.

No deben subirse a GitHub:

- claves reales de API;
- ficheros `.properties` locales;
- contenido generado de `eventstore/`;
- artefactos `target/`;
- ficheros temporales del IDE.

El fichero `.gitignore` recoge estos artefactos locales y generados.

---

## Hitos del proyecto

| Hito | Descripción |
|---|---|
| Sprint 1 | Captura directa y persistencia incremental en SQLite |
| Sprint 2 | Arquitectura Publisher/Subscriber con ActiveMQ y Event Store JSON Lines |
| Sprint 3 | Business Unit con datamart, consumo histórico/tiempo real y API REST de recomendaciones |
