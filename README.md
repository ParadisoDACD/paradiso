# Paradiso

Proyecto de la asignatura **Desarrollo de Aplicaciones para Ciencia de Datos** de la Universidad de Las Palmas de Gran Canaria.

Paradiso es una aplicación Java multimódulo orientada a la incorporación de datos externos sobre eventos musicales en Londres y movilidad urbana mediante Transport for London. En el estado actual del repositorio, correspondiente al **Sprint 2**, el sistema implementa una arquitectura **Publisher/Subscriber** con **Apache ActiveMQ Classic** y un **Event Store** persistente en ficheros JSON Lines.

---

## Estado actual del proyecto

La rama `main` contiene la implementación del Sprint 2.

El Sprint 1 queda preservado como hito histórico mediante el tag Git:

```bash
git checkout sprint-1
```

Para volver al estado actual del proyecto:

```bash
git checkout main
```

---

## Objetivo funcional

El sistema captura datos desde dos fuentes externas:

- **Ticketmaster Discovery API**, para eventos musicales y festivales en Londres.
- **Transport for London Unified API**, para itinerarios de transporte público entre hubs de Londres y venues relevantes.

En Sprint 2, estos datos ya no se almacenan directamente en SQLite. Cada módulo capturador actúa como **publisher**, publica eventos JSON en un broker ActiveMQ, y un nuevo módulo **Event Store Builder** actúa como **subscriber durable**, consume esos eventos y los almacena de forma persistente y ordenada en disco.

---

## Arquitectura

```text
ticketmaster-module
    └── publica eventos JSON en topic TicketmasterEvent

ActiveMQ Classic Broker
    └── gestiona topics y suscripciones durables

tfl-module
    └── publica eventos JSON en topic TflJourney

Event Store Builder
    └── consume topics de forma durable
    └── escribe eventos en eventstore/{topic}/{ss}/{YYYYMMDD}.events
```

Flujo completo:

```text
API externa
    → Feeder OkHttp
    → Mapper de modelo interno
    → Serializer de evento broker {ts, ss, payload}
    → ActiveMQ Topic
    → Durable Subscriber
    → Event Store JSON Lines
```

---

## Módulos

| Módulo | Rol | Descripción | Topic |
|---|---|---|---|
| `ticketmaster-module` | Publisher | Captura eventos musicales de Ticketmaster y publica cada evento en ActiveMQ | `TicketmasterEvent` |
| `tfl-module` | Publisher | Captura itinerarios TfL y publica cada itinerario en ActiveMQ | `TflJourney` |
| `eventstore-builder-module` | Subscriber durable | Consume eventos desde ActiveMQ y los persiste en ficheros `.events` | `TicketmasterEvent`, `TflJourney` |

---

## Tecnologías principales

- **Java 21**
- **Maven multimódulo**
- **IntelliJ IDEA**
- **Apache ActiveMQ Classic 5.x** como broker externo
- **JMS / ActiveMQ Client 5.15.12** para comunicación con el broker
- **OkHttp** para consumo de APIs externas
- **Gson** para serialización y parseo JSON
- **JUnit 5** para pruebas automatizadas

---

## Broker de mensajería

El broker utilizado es **Apache ActiveMQ Classic**. Se ejecuta como una aplicación externa al proyecto Java.

Configuración local usada:

```text
Broker URL: tcp://localhost:61616
Web Console: http://127.0.0.1:8161/admin
```

Credenciales por defecto de la consola web:

```text
admin / admin
```

El proyecto usa la dependencia exigida por el enunciado del Sprint 2:

```xml
<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>activemq-client</artifactId>
    <version>5.15.12</version>
</dependency>
```

Aunque el broker instalado localmente puede ser una versión Classic 5.x más reciente, la dependencia del proyecto se mantiene en `5.15.12` para respetar la especificación del sprint y trabajar con `javax.jms`.

---

## Formato de evento

Todos los mensajes publicados en ActiveMQ son `TextMessage` con contenido JSON.

Estructura común:

```json
{
  "ts": "2026-05-05T10:46:09.910902100Z",
  "ss": "tfl-module",
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

El Event Store generado por el subscriber sigue la estructura requerida:

```text
eventstore/{topic}/{ss}/{YYYYMMDD}.events
```

Ejemplo real de estructura:

```text
eventstore/
├── TicketmasterEvent/
│   └── ticketmaster-module/
│       ├── 20260505.events
│       ├── 20260506.events
│       └── ...
└── TflJourney/
    └── tfl-module/
        └── 20260505.events
```

Cada fichero `.events` usa formato **JSON Lines / NDJSON**:

- un evento JSON completo por línea;
- sin array envolvente;
- sin comas entre eventos;
- escritura en modo append;
- el nombre del fichero se obtiene a partir del campo `ts` del evento.

Ejemplo:

```jsonl
{"ts":"2026-05-05T17:30:00Z","ss":"ticketmaster-module","payload":{"externalEventId":"17uYvxG65mp7JRS","name":"ZAZ"}}
{"ts":"2026-05-05T10:46:09.910902100Z","ss":"tfl-module","payload":{"journeyHash":"6e95af24028b418b"}}
```

---

## Configuración

Cada módulo utiliza un fichero `.properties.example` versionado como plantilla de configuración.

### Ticketmaster

Plantilla:

```text
ticketmaster-module/src/main/resources/ticketmaster.properties.example
```

Fichero local esperado:

```text
ticketmaster-module/src/main/resources/ticketmaster.properties
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
source.system=ticketmaster-module
```

### TfL

Plantilla:

```text
tfl-module/src/main/resources/tfl.properties.example
```

Fichero local esperado:

```text
tfl-module/src/main/resources/tfl.properties
```

Propiedades principales:

```properties
app.key=TU_CLAVE_TFL
routes=KingsCross>O2Arena;Victoria>WembleyPark;Waterloo>BrixtonAcademy;Paddington>RoyalAlbertHall;LondonBridge>AlexandraPalace
capture.times=0900,1400,1900
capture.period.minutes=60
broker.url=tcp://localhost:61616
topic.name=TflJourney
source.system=tfl-module
```

### Event Store Builder

Plantilla:

```text
eventstore-builder-module/src/main/resources/eventstore-builder.properties.example
```

Fichero local esperado:

```text
eventstore-builder-module/src/main/resources/eventstore-builder.properties
```

Contenido:

```properties
broker.url=tcp://localhost:61616
client.id=paradiso-eventstore-builder
topics=TicketmasterEvent,TflJourney
eventstore.path=eventstore
```

---

## Ejecución

### 1. Compilar y empaquetar

Desde la raíz del proyecto:

```bash
mvn clean package
```

El empaquetado genera tres JAR ejecutables:

```text
ticketmaster-module/target/ticketmaster-module-1.0-SNAPSHOT.jar
tfl-module/target/tfl-module-1.0-SNAPSHOT.jar
eventstore-builder-module/target/eventstore-builder-module-1.0-SNAPSHOT.jar
```

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
java -jar .\eventstore-builder-module\target\eventstore-builder-module-1.0-SNAPSHOT.jar
```

Este proceso queda escuchando indefinidamente los topics configurados.

### 4. Ejecutar publishers en modo one-shot

Ticketmaster:

```powershell
java -jar .\ticketmaster-module\target\ticketmaster-module-1.0-SNAPSHOT.jar --once
```

TfL:

```powershell
java -jar .\tfl-module\target\tfl-module-1.0-SNAPSHOT.jar --once
```

### 5. Ejecución periódica

Sin `--once`, cada publisher ejecuta capturas periódicas según `capture.period.minutes`:

```powershell
java -jar .\ticketmaster-module\target\ticketmaster-module-1.0-SNAPSHOT.jar
java -jar .\tfl-module\target\tfl-module-1.0-SNAPSHOT.jar
```

---

## Verificación básica

Listar ficheros generados:

```powershell
Get-ChildItem -Recurse .\eventstore
```

Ver primeras líneas de eventos:

```powershell
Get-Content .\eventstore\TicketmasterEvent\ticketmaster-module\*.events -TotalCount 2
Get-Content .\eventstore\TflJourney\tfl-module\*.events -TotalCount 2
```

Ejecutar tests:

```bash
mvn test
```

---

## Pruebas realizadas

La validación del Sprint 2 queda documentada en:

```text
docs/sprint2/pruebas-sprint2.md
```

Pruebas cubiertas:

- publicación de eventos desde `ticketmaster-module` a `TicketmasterEvent`;
- publicación de itinerarios desde `tfl-module` a `TflJourney`;
- consumo desde `eventstore-builder-module`;
- persistencia en `eventstore/{topic}/{ss}/{YYYYMMDD}.events`;
- validación del formato JSON Lines;
- recuperación de mensajes mediante suscripción durable;
- ejecución de tests automatizados con `mvn test`.

Resultado destacado de la prueba durable:

```text
Líneas antes: 72
Mensajes publicados con Event Store Builder detenido: 72
Líneas después: 144
Resultado: OK
```

---

## Tests automatizados

Se mantienen los tests del Sprint 1 y se añaden pruebas específicas del Sprint 2:

```text
TicketmasterBrokerEventSerializerTest
TflBrokerEventSerializerTest
JsonLinesEventFileStoreTest
```

Estas pruebas validan:

- estructura común `{ts, ss, payload}`;
- uso correcto de `dateTimeIso` y fallback `capturedAt` en Ticketmaster;
- uso de `capturedAt` como `ts` en TfL;
- parseabilidad de `ts` con `Instant.parse()`;
- creación de ficheros `.events`;
- escritura en append;
- separación por topic, source system y fecha;
- validación de errores ante eventos incompletos o timestamps inválidos.

---

## Estructura del repositorio

```text
paradiso/
├── pom.xml
├── README.md
├── ticketmaster-module/
│   └── src/main/java/org/ulpgc/paradiso/ticketmaster/
│       ├── config/
│       ├── controller/
│       ├── feeder/
│       ├── mapper/
│       ├── messaging/
│       ├── model/
│       └── Main.java
├── tfl-module/
│   └── src/main/java/org/ulpgc/paradiso/tfl/
│       ├── config/
│       ├── controller/
│       ├── feeder/
│       ├── mapper/
│       ├── messaging/
│       ├── model/
│       └── Main.java
├── eventstore-builder-module/
│   └── src/main/java/org/ulpgc/paradiso/eventstorebuilder/
│       ├── config/
│       ├── subscriber/
│       ├── store/
│       └── Main.java
└── docs/
    ├── sprint1/
    └── sprint2/
```

---

## Versionado y artefactos locales

El repositorio mantiene el código fuente, configuración de ejemplo, documentación y tests.

No deben subirse a GitHub:

- claves reales de API;
- ficheros `.properties` locales;
- bases de datos SQLite;
- contenido generado de `eventstore/`;
- artefactos `target/`.

El fichero `.gitignore` recoge estos artefactos locales y generados.

---

## Hitos Git

| Hito | Descripción |
|---|---|
| `sprint-1` | Captura directa y persistencia incremental en SQLite |
| `sprint-2` | Arquitectura Publisher/Subscriber con ActiveMQ y Event Store JSON Lines |

El tag `sprint-2` debe crearse al finalizar la verificación y documentación del sprint:

```bash
git tag -a sprint-2 -m "Cierre del Sprint 2: arquitectura Publisher/Subscriber con ActiveMQ y Event Store JSON Lines"
git push origin sprint-2
```
