# Pruebas Sprint 2 — Proyecto Paradiso

## Entorno

- Proyecto: Paradiso
- Sprint: 2
- Java: 21
- ActiveMQ: Apache ActiveMQ Classic 5.19.6
- Dependencia Maven del proyecto: `org.apache.activemq:activemq-client:5.15.12`
- Broker URL: `tcp://localhost:61616`
- Consola web ActiveMQ: `http://127.0.0.1:8161/admin`
- Topics utilizados:
  - `TicketmasterEvent`
  - `TflJourney`
- Event Store generado:
  - `eventstore/{topic}/{ss}/{YYYYMMDD}.events`

---

## 1. Arquitectura probada

El Sprint 2 implementa una arquitectura Publisher/Subscriber con Apache ActiveMQ.

```text
ticketmaster-feeder ──publish──> Topic TicketmasterEvent ──┐
                                                            │
                                                            ├──> ActiveMQ Broker
                                                            │
tfl-feeder          ──publish──> Topic TflJourney        ──┘
                                                              │
                                                              │ durable subscription
                                                              ▼
                                                   eventstore-builder
                                                              │
                                                              ▼
                                      eventstore/{topic}/{ss}/{YYYYMMDD}.events
```

Los módulos `ticketmaster-feeder` y `tfl-feeder` actúan como publishers.  
El módulo `eventstore-builder-feeder` actúa como subscriber durable y persiste los eventos en ficheros JSON Lines.

---

## 2. Arranque de ActiveMQ

ActiveMQ se arrancó desde Windows con:

```powershell
cd C:\apache-activemq-5.19.6
.\bin\win64\activemq.bat console
```

Salida relevante observada:

```text
Java Runtime: Microsoft 21.0.11 C:\Users\Usuario\.jdks\ms-21.0.11
Listening for connections at: tcp://DESKTOP-17I8RPJ:61616
Connector openwire started
Apache ActiveMQ 5.19.6 (...) started
ActiveMQ WebConsole available at http://127.0.0.1:8161/
```

Resultado: ActiveMQ arrancó correctamente y quedó escuchando conexiones JMS/OpenWire en el puerto `61616`.

---

## 3. Arranque del Event Store Builder

El Event Store Builder se ejecutó desde la raíz del proyecto con:

```powershell
java -jar .\eventstore-builder\target\eventstore-builder-1.0-SNAPSHOT.jar
```

Salida esperada/observada:

```text
[EventStoreBuilder] Broker:     tcp://localhost:61616
[EventStoreBuilder] Client ID:  paradiso-eventstore-builder
[EventStoreBuilder] Topics:     [TicketmasterEvent, TflJourney]
[EventStoreBuilder] Eventstore: eventstore
[EventStoreBuilder] Suscripción durable registrada: sub-TicketmasterEvent en topic TicketmasterEvent
[EventStoreBuilder] Suscripción durable registrada: sub-TflJourney en topic TflJourney
[EventStoreBuilder] Listo. Escuchando mensajes...
[EventStoreBuilder] Pulsa Ctrl+C para detener.
```

Resultado: el módulo se conectó correctamente al broker y registró suscripciones durables para ambos topics.

---

## 4. Prueba A — Publicación desde Ticketmaster

Comando ejecutado:

```powershell
java -jar .\ticketmaster-feeder\target\ticketmaster-feeder-1.0-SNAPSHOT.jar --once
```

Salida relevante observada:

```text
[Ticketmaster] Broker:  tcp://localhost:61616
[Ticketmaster] Topic:   TicketmasterEvent
[Ticketmaster] Source:  ticketmaster-feeder
[Ticketmaster] Modo: one-shot

[Ticketmaster] ======== Iniciando captura ========
[Ticketmaster] Lote: 5dfa4826-b7b7-4266-8ad2-dc170f227ff9
[Ticketmaster] Ventana: 2026-05-05T10:09:55Z -> 2026-05-19T10:09:55Z
  [GB/London/music] -> 50 eventos publicados
  [GB/London/festival] -> 22 eventos publicados
[Ticketmaster] Total publicado: 72 eventos en topic 'TicketmasterEvent'
[Ticketmaster] Finalizado.
```

Resultado: Ticketmaster publicó correctamente 72 eventos en el topic `TicketmasterEvent`.

En la consola web de ActiveMQ se verificó la existencia del topic:

```text
TicketmasterEvent
```

---

## 5. Prueba B — Publicación desde TfL

Comando ejecutado:

```powershell
java -jar .\tfl-feeder\target\tfl-feeder-1.0-SNAPSHOT.jar --once
```

Resultado observado:

- El módulo se conectó correctamente al broker.
- Publicó itinerarios en el topic `TflJourney`.
- En la consola web de ActiveMQ se verificó la existencia del topic `TflJourney`.

Resultado: TfL publicó correctamente mensajes en ActiveMQ.

---

## 6. Prueba C — Generación del Event Store

Tras ejecutar ambos publishers con el Event Store Builder activo, se verificó la estructura generada con:

```powershell
Get-ChildItem -Recurse .\eventstore
```

Salida relevante observada:

```text
Directorio: C:\Users\Usuario\IdeaProjects\paradiso\eventstore

Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----        05/05/2026     11:46                TflJourney
d-----        05/05/2026     11:45                TicketmasterEvent

Directorio: C:\Users\Usuario\IdeaProjects\paradiso\eventstore\TflJourney\tfl-feeder

Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----        05/05/2026     11:47          53612 20260505.events

Directorio: C:\Users\Usuario\IdeaProjects\paradiso\eventstore\TicketmasterEvent\ticketmaster-feeder

Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----        05/05/2026     11:45           2396 20260505.events
-a----        05/05/2026     11:45           2491 20260506.events
-a----        05/05/2026     11:45           8909 20260507.events
-a----        05/05/2026     11:45           2695 20260508.events
-a----        05/05/2026     11:45           1272 20260509.events
-a----        05/05/2026     11:45            668 20260510.events
-a----        05/05/2026     11:45            607 20260511.events
-a----        05/05/2026     11:45           1267 20260512.events
-a----        05/05/2026     11:45           3882 20260513.events
-a----        05/05/2026     11:45           3302 20260514.events
-a----        05/05/2026     11:45           7969 20260515.events
-a----        05/05/2026     11:45           5257 20260516.events
-a----        05/05/2026     11:45           3807 20260517.events
-a----        05/05/2026     11:45           1859 20260518.events
```

Resultado: la estructura generada cumple el formato requerido:

```text
eventstore/{topic}/{ss}/{YYYYMMDD}.events
```

Interpretación:

- `TflJourney` genera un fichero por día de captura porque su campo `ts` es `capturedAt`.
- `TicketmasterEvent` genera varios ficheros porque su campo `ts` usa `dateTimeIso` cuando existe, es decir, la fecha real del concierto. Si no existe `dateTimeIso`, usa `capturedAt` como fallback.

---

## 7. Prueba D — Verificación del formato JSON Lines / NDJSON

### 7.1 TfL

Comando ejecutado:

```powershell
Get-Content .\eventstore\TflJourney\tfl-feeder\20260505.events -TotalCount 2
```

Salida observada:

```jsonl
{"ts":"2026-05-05T10:46:09.910902100Z","ss":"tfl-feeder","payload":{"journeyHash":"6e95af24028b418b","originName":"King\u0027s Cross St. Pancras Underground Station","destinationName":"North Greenwich Underground Station","startDateTime":"2026-05-05T09:00:00","arrivalDateTime":"2026-05-05T09:27:00","durationMinutes":27,"numberOfLegs":2,"firstLegMode":"tube","captureDate":"2026-05-05","captureTime":"0900","sourceOrigin":"KingsCross","sourceDestination":"O2Arena","captureBatchId":"7e9acfa0-4ecb-47c8-9a30-5ce2ce60fe0a","capturedAt":"2026-05-05T10:46:09.910902100Z"}}
{"ts":"2026-05-05T10:46:09.910902100Z","ss":"tfl-feeder","payload":{"journeyHash":"3aad3a5f8c366d81","originName":"King\u0027s Cross St. Pancras Underground Station","destinationName":"North Greenwich Underground Station","startDateTime":"2026-05-05T09:03:00","arrivalDateTime":"2026-05-05T09:30:00","durationMinutes":27,"numberOfLegs":2,"firstLegMode":"tube","captureDate":"2026-05-05","captureTime":"0900","sourceOrigin":"KingsCross","sourceDestination":"O2Arena","captureBatchId":"7e9acfa0-4ecb-47c8-9a30-5ce2ce60fe0a","capturedAt":"2026-05-05T10:46:09.910902100Z"}}
```

Conclusión:

- Cada línea es un objeto JSON completo.
- Cada evento contiene los campos mínimos requeridos: `ts`, `ss` y `payload`.
- El valor de `ss` es `tfl-feeder`.
- El valor de `ts` es UTC y parseable con `Instant.parse()`.

### 7.2 Ticketmaster

Comando ejecutado:

```powershell
Get-Content .\eventstore\TicketmasterEvent\ticketmaster-feeder\20260505.events -TotalCount 2
```

Salida observada:

```jsonl
{"ts":"2026-05-05T17:30:00Z","ss":"ticketmaster-feeder","payload":{"externalEventId":"17uYvxG65mp7JRS","name":"ZAZ","classificationName":"music","segment":"Music","genre":"Rock","city":"London","countryCode":"GB","venueName":"The London Palladium","eventUrl":"https://www.ticketmaster.co.uk/zaz-london-05-05-2026/event/370062BB96126BF3","localDate":"2026-05-05","localTime":"18:30:00","dateTimeIso":"2026-05-05T17:30:00Z","sourceCountry":"GB","sourceCity":"London","sourceCategory":"music","captureBatchId":"39b48eea-0a5b-439f-add4-ab38bc33fde3","capturedAt":"2026-05-05T10:45:57.524031900Z"}}
{"ts":"2026-05-05T10:45:57.524031900Z","ss":"ticketmaster-feeder","payload":{"externalEventId":"G5vHZ_FD8nfdV","name":"Chase and Status: Section 63","classificationName":"music","segment":"Music","genre":"Pop","city":"London","countryCode":"GB","venueName":"Magazine","eventUrl":"https://www.ticketmaster.co.uk/chase-and-status-section-63-london-15-05-2026/event/1F00648ED0A940F4","localDate":"2026-05-15","sourceCountry":"GB","sourceCity":"London","sourceCategory":"music","captureBatchId":"39b48eea-0a5b-439f-add4-ab38bc33fde3","capturedAt":"2026-05-05T10:45:57.524031900Z"}}
```

Conclusión:

- Cada línea es un objeto JSON completo.
- Cada evento contiene `ts`, `ss` y `payload`.
- El valor de `ss` es `ticketmaster-feeder`.
- En eventos con `dateTimeIso`, `ts` usa la fecha real del evento.
- En eventos sin `dateTimeIso`, `ts` usa `capturedAt` como fallback.

---

## 8. Prueba E — Suscripción durable

Esta prueba valida el requisito más importante del Sprint 2: que el Event Store Builder recupere mensajes publicados mientras estaba detenido.

Procedimiento:

1. ActiveMQ quedó arrancado.
2. Event Store Builder se arrancó al menos una vez para registrar las suscripciones durables.
3. Se detuvo Event Store Builder con `Ctrl + C`.
4. Se ejecutó Ticketmaster con `--once` mientras el subscriber estaba detenido.
5. Se arrancó de nuevo Event Store Builder.
6. ActiveMQ entregó los mensajes pendientes.
7. Los ficheros `.events` aumentaron.

Medición realizada:

```text
Líneas antes en eventstore/TicketmasterEvent/ticketmaster-feeder/*.events: 72
Ticketmaster publicó: 72 eventos
Líneas después: 144
Aumento: 72 eventos
```

Resultado: **OK**.

Conclusión: la suscripción durable funciona correctamente. ActiveMQ conservó los mensajes publicados mientras Event Store Builder estaba parado y los entregó al reconectar.

---

## 9. Prueba F — Tests automáticos

Comando ejecutado:

```powershell
mvn test
```

Resultado esperado:

```text
BUILD SUCCESS
```

Tests añadidos para Sprint 2:

- `TicketmasterBrokerEventSerializerTest`
- `TflBrokerEventSerializerTest`
- `JsonLinesEventFileStoreTest`

Cobertura principal de los tests:

- Serialización de eventos con `ts`, `ss` y `payload`.
- Fallback de `TicketmasterEvent.dateTimeIso` a `capturedAt`.
- Uso de `capturedAt` como `ts` en TfL.
- Escritura de eventos en `eventstore/{topic}/{ss}/{YYYYMMDD}.events`.
- Escritura en append.
- Separación por día.
- Separación por topic.
- Validación de eventos sin `ts` o sin `ss`.
- Rechazo de timestamps no parseables por `Instant.parse()`.
- Sanitización de nombres de carpetas.

---

## 10. Conclusión general

Las pruebas realizadas validan que el Sprint 2 cumple los requisitos principales:

- Los feeders actúan como publishers.
- Los eventos se publican en formato JSON.
- Cada evento contiene `ts`, `ss` y `payload`.
- Se usa ActiveMQ como broker externo.
- Los topics son `TicketmasterEvent` y `TflJourney`.
- El Event Store Builder se suscribe de forma durable.
- Los eventos se almacenan en `eventstore/{topic}/{ss}/{YYYYMMDD}.events`.
- El formato de almacenamiento es JSON Lines / NDJSON.
- Los eventos se escriben en append.
- La suscripción durable recupera mensajes publicados mientras el subscriber estaba detenido.
