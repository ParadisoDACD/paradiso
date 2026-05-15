# Pruebas y validación — Paradiso Sprint 3

## Objetivo

Este documento describe las comprobaciones recomendadas para validar el funcionamiento del Sprint 3.

La validación cubre:

- compilación y tests automatizados;
- publicación de eventos;
- persistencia en event store;
- carga histórica;
- consumo en tiempo real;
- reconstrucción del datamart;
- consultas mediante API REST.

## Tests automatizados

Ejecutar todos los tests desde la raíz:

```powershell
mvn test
```

Resultado esperado:

```text
BUILD SUCCESS
```

Ejecución por módulos:

```powershell
mvn -pl ticketmaster-feeder test
mvn -pl tfl-feeder test
mvn -pl eventstore-builder test
mvn -pl business-unit test
```

## Validación del broker

1. Arrancar ActiveMQ Classic.
2. Verificar que el broker queda disponible en:

```text
tcp://localhost:61616
```

3. Acceder a la consola web si está habilitada:

```text
http://localhost:8161/admin
```

## Validación del event store

1. Arrancar `eventstore-builder`.
2. Ejecutar los feeders.
3. Comprobar que aparecen ficheros `.events`.

Estructura esperada:

```text
eventstore/
├── TicketmasterEvent/
│   └── ...
└── TflJourney/
    └── ...
```

Cada fichero `.events` debe contener un evento JSON por línea.

## Validación de carga histórica

1. Detener `business-unit` si estaba arrancado.
2. Confirmar que existen ficheros `.events`.
3. Arrancar `business-unit`.
4. Verificar en logs que `EventStoreLoader` procesa líneas históricas.
5. Consultar `/status`.

PowerShell:

```powershell
Invoke-RestMethod http://localhost:7000/status | ConvertTo-Json -Depth 5
```

curl:

```bash
curl -s http://localhost:7000/status | python3 -m json.tool
```

Resultado esperado:

- `concerts` mayor que 0 si existen eventos de Ticketmaster;
- `transports` mayor que 0 si existen eventos de TfL;
- `routePlans` mayor que 0 si hay conciertos y rutas compatibles.

## Validación de consumo en tiempo real

1. Arrancar ActiveMQ.
2. Arrancar `eventstore-builder`.
3. Arrancar `business-unit`.
4. Consultar `/status` y anotar contadores.
5. Ejecutar un feeder en modo puntual.
6. Volver a consultar `/status`.

Resultado esperado:

- el datamart se actualiza sin reiniciar `business-unit`;
- los nuevos eventos quedan reflejados en los contadores;
- las recomendaciones se actualizan cuando existen datos compatibles.

## Validación de endpoints principales

### `/status`

```bash
curl -s http://localhost:7000/status | python3 -m json.tool
```

Debe devolver contadores del datamart.

### `/concerts/upcoming`

```bash
curl -s "http://localhost:7000/concerts/upcoming?limit=5" | python3 -m json.tool
```

Debe devolver conciertos futuros disponibles en el datamart.

### `/origins`

```bash
curl -s http://localhost:7000/origins | python3 -m json.tool
```

Debe devolver orígenes TfL capturados.

### `/venues`

```bash
curl -s http://localhost:7000/venues | python3 -m json.tool
```

Debe devolver venues conocidos y normalizados.

### `/recommendations`

```bash
curl -s "http://localhost:7000/recommendations?page=0&size=5" | python3 -m json.tool
```

Debe devolver recomendaciones precalculadas.

### `/concerts/{id}/routes`

```bash
curl -s "http://localhost:7000/concerts/{id}/routes" | python3 -m json.tool
```

Debe devolver rutas para el concierto si:

- el concierto existe;
- el venue está mapeado;
- hay rutas TfL compatibles.

### `/artists/{artist}/recommendations`

```bash
curl -s "http://localhost:7000/artists/{artist}/recommendations" | python3 -m json.tool
```

Ejemplo con espacios codificados:

```bash
curl -s "http://localhost:7000/artists/Arctic%20Monkeys/recommendations" | python3 -m json.tool
```

Debe devolver recomendaciones asociadas al artista cuando existan datos compatibles.

## Validación de errores esperados

### Concierto inexistente

```bash
curl -i "http://localhost:7000/concerts/UNKNOWN/routes"
```

Resultado esperado:

```text
HTTP/1.1 404
```

### Página sin resultados

```bash
curl -s "http://localhost:7000/recommendations?page=999&size=5" | python3 -m json.tool
```

Resultado esperado:

- `count` conserva el total real;
- `results` puede aparecer vacío.

## Checklist final

- [ ] El proyecto compila con Java 21.
- [ ] `mvn test` termina correctamente.
- [ ] ActiveMQ está disponible en local.
- [ ] Los feeders publican eventos en sus topics.
- [ ] `eventstore-builder` genera ficheros `.events`.
- [ ] `business-unit` carga históricos desde el event store.
- [ ] `business-unit` consume eventos nuevos desde ActiveMQ.
- [ ] El datamart muestra contadores coherentes en `/status`.
- [ ] La API REST responde en `http://localhost:7000`.
- [ ] `/recommendations` devuelve recomendaciones precalculadas cuando existen datos compatibles.
- [ ] `/concerts/{id}/routes` funciona para conciertos con venue mapeado.
- [ ] `/artists/{artist}/recommendations` funciona para artistas presentes en el datamart.
