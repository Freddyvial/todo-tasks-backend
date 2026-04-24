# todo-tasks-backend

API REST para gestionar tareas tipo TODO construida con **Java 21**, **Spring Boot 3.5**, **Spring Data JPA** y **H2**.

## Requisitos

| Herramienta | Versión mínima |
|-------------|----------------|
| Java | 21 LTS |
| Maven | 3.9+ |

## Ejecutar localmente

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

## Arquitectura

El proyecto sigue una arquitectura en capas:

```
com.imaginamos.todo
├── controller      → endpoints REST y validación de entrada
├── service         → reglas de negocio y orquestación
├── repository      → acceso a datos con Spring Data JPA
├── entity          → modelo persistente (Task, TaskItem, TaskStatus)
├── dto             → contratos de entrada y salida
├── mapper          → conversión entre entidades y DTOs
└── exception       → manejo centralizado de errores
```

## Modelo de dominio

### Estados de tarea (`TaskStatus`)

| Estado | Descripción |
|--------|-------------|
| `PROGRAMMED` | Estado inicial. La tarea está pendiente de ejecución. |
| `IN_PROGRESS` | La tarea está siendo ejecutada. |
| `FINISHED` | La tarea fue completada. Estado final. |
| `CANCELLED` | La tarea fue cancelada. Estado final. |

Una vez que una tarea alcanza `FINISHED` o `CANCELLED`, su estado no puede modificarse.

### Indicadores calculados en la respuesta

- `pendingExecution` — `true` cuando la tarea está en estado `PROGRAMMED`.
- `dueNowAlert` — `true` cuando la tarea está en `PROGRAMMED` y su `executionDate` ya llegó.
- `completedItems` — cantidad de ítems checkeados como completados.
- `totalItems` — total de ítems de la tarea.

## Endpoints

### Crear tarea

```
POST /api/tasks
```

**Body:**
```json
{
  "title": "Preparar demo",
  "description": "Validar ambiente de pruebas",
  "executionDate": "2026-04-25T10:00:00",
  "status": "PROGRAMMED",
  "items": [
    { "description": "Revisar slides", "completed": false, "position": 1 },
    { "description": "Probar conexión", "completed": false, "position": 2 }
  ]
}
```

Respuesta: `201 Created` con cabecera `Location: /api/tasks/{id}`.

---

### Consultar tarea por id

```
GET /api/tasks/{id}
```

Respuesta: `200 OK` con el objeto de tarea completo.

---

### Listar tareas con filtros y paginación

```
GET /api/tasks
```

**Parámetros de consulta:**

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `page` | int | Número de página (defecto: 0) |
| `size` | int | Tamaño de página (defecto: 10) |
| `query` | string | Búsqueda por título o descripción |
| `status` | string | Filtro por estado exacto (`PROGRAMMED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`) |
| `pendingOnly` | boolean | Solo tareas en estado `PROGRAMMED` |
| `dueNowOnly` | boolean | Solo tareas `PROGRAMMED` cuya fecha de ejecución ya llegó |
| `sort` | string | Campo y dirección: `campo,asc` o `campo,desc` |

**Campos válidos para `sort`:** `id`, `title`, `executionDate`, `status`, `createdAt`, `updatedAt`.

**Ejemplos:**

```bash
# Listar todas, ordenadas por fecha de ejecución ascendente
GET /api/tasks?page=0&size=10&sort=executionDate,asc

# Buscar por texto en estado PROGRAMMED
GET /api/tasks?query=demo&status=PROGRAMMED

# Solo tareas vencidas (dueNowAlert)
GET /api/tasks?dueNowOnly=true

# Solo tareas pendientes de ejecución
GET /api/tasks?pendingOnly=true
```

---

### Actualizar tarea

```
PUT /api/tasks/{id}
```

Reemplaza todos los campos y los ítems de la tarea. Para preservar un ítem existente incluir su `id` en la lista.

---

### Cambiar estado

```
PATCH /api/tasks/{id}/status
```

**Body:**
```json
{ "status": "IN_PROGRESS" }
```

Devuelve `409 Conflict` si se intenta cambiar el estado de una tarea ya finalizada o cancelada.

---

### Eliminar tarea

```
DELETE /api/tasks/{id}
```

Respuesta: `204 No Content`.

---

## Respuesta de error estándar

Todos los errores siguen el mismo contrato:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: must not be blank"
}
```

| Código | Caso |
|--------|------|
| `400` | Validación de entrada o parámetro de ordenamiento inválido |
| `404` | Tarea no encontrada |
| `409` | Transición de estado inválida |
| `500` | Error interno no anticipado |

## Base de datos

Se usa **H2 en memoria**. La consola de administración queda disponible en:

```
http://localhost:8080/h2-console
```

Credenciales:

| Campo | Valor |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:tododb` |
| Usuario | `sa` |
| Password | *(vacío)* |

Al iniciar la aplicación se cargan datos de ejemplo que cubren los distintos estados y escenarios de alerta.

## Pruebas y cobertura

```bash
# Ejecutar suite completa y validar cobertura mínima
mvn verify
```

La suite incluye **pruebas unitarias** (servicio, mapper, controlador y excepciones) y **pruebas de integración** contra H2 real para validar los filtros de búsqueda.

| Métrica | Resultado | Umbral |
|---------|-----------|--------|
| Cobertura de líneas | ~97% | 80% (verificado por JaCoCo) |
| Cobertura de ramas | ~90% | — |

El reporte HTML detallado se genera en:

```
target/site/jacoco/index.html
```

### Suites de prueba

| Suite | Tipo | Pruebas |
|-------|------|---------|
| `TaskControllerTest` | Web (MockMvc) | 9 |
| `TaskServiceTest` | Unitaria (Mockito) | 23 |
| `TaskServiceIntegrationTest` | Integración (H2 real) | 5 |
| `GlobalExceptionHandlerTest` | Unitaria | 6 |
| `TaskMapperTest` | Unitaria | 3 |

## Escenarios cubiertos por las pruebas

- Crear tarea con estado por defecto y con estado explícito
- Crear tarea con ítems checkeables
- Consultar tarea existente e inexistente
- Actualizar tarea completa reemplazando ítems
- Actualizar solo el estado (transición válida e inválida)
- Eliminar tarea
- Búsqueda por texto en título y descripción
- Filtro por estado, `pendingOnly` y `dueNowOnly`
- Paginación y ordenamiento con y sin dirección
- Parámetro de sort inválido
- Campos derivados `pendingExecution` y `dueNowAlert`
- Manejo de errores inesperados con log y relanzamiento
- Validaciones de entrada con respuesta `400`

## Evidencia de uso de IA

Este proyecto fue desarrollado con apoyo de **Claude** (Anthropic) usando una metodología
de **especificación primero**: antes de escribir código, se documentó la arquitectura,
los contratos y las decisiones de diseño en la carpeta `.plan/`.

La carpeta `.plan/` contiene:

| Archivo | Contenido |
|---------|-----------|
| `plan-trabajo-backend.md` | Arquitectura, fases, estrategia de pruebas y decisiones técnicas del backend |
| `plan-trabajo-frontend.md` | Arquitectura Angular, componentes, decisiones y Docker |
| `contrato-api-backend.md` | Todos los endpoints, DTOs, validaciones y manejo de errores |
| `contrato-api-frontend.md` | Cómo Angular consume la API y manejo de errores HTTP |
| `estructura-proyecto-backend.md` | Árbol de archivos, responsabilidad de paquetes, `pom.xml` objetivo |
| `estructura-proyecto-frontend.md` | Árbol de archivos, modelos, diseño de componentes |
| `manejo-errores-trazabilidad-backend.md` | Estrategia de errores, logging y uso de try/catch por capa |
| `diseno-calidad-frontend.md` | Errores en capas, diseño responsive, OnPush, lazy loading y optimizaciones |
| `pruebas-docker-frontend.md` | Specs de pruebas, Dockerfiles y docker-compose |
| `prompts-ia.md` | **Prompts utilizados con Claude** en cada fase del desarrollo |

El evaluador puede abrir `prompts-ia.md` para ver qué se le pidió a la IA en cada etapa
y cómo esa ayuda se integró durante la construcción de la solución.
