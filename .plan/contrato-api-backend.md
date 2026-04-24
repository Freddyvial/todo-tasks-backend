# Contrato API backend

## 1. Objetivo

Definir el contrato REST del backend de TODO Tasks antes de iniciar la implementación, de forma que frontend y backend puedan trabajar sobre un acuerdo claro.

Base path propuesto:

- `/api/tasks`

Formato:

- `JSON`
- Fechas en formato ISO 8601
- `executionDate` con tipo `LocalDateTime`

## 2. Recurso principal

### 2.1 Task

```json
{
  "id": 1,
  "title": "Preparar informe",
  "description": "Consolidar informacion para el cierre",
  "executionDate": "2026-04-22T08:00:00",
  "status": "PROGRAMMED",
  "items": [
    {
      "id": 10,
      "description": "Revisar datos",
      "completed": true,
      "position": 1
    },
    {
      "id": 11,
      "description": "Enviar reporte",
      "completed": false,
      "position": 2
    }
  ],
  "pendingExecution": true,
  "dueNowAlert": true,
  "completedItems": 1,
  "totalItems": 2,
  "createdAt": "2026-04-22T10:15:00",
  "updatedAt": "2026-04-22T11:00:00"
}
```

### 2.2 Estados válidos

- `PROGRAMMED`
- `IN_PROGRESS`
- `FINISHED`
- `CANCELLED`

## 3. Endpoints

### 3.1 Crear tarea

`POST /api/tasks`

#### Request

```json
{
  "title": "Preparar informe",
  "description": "Consolidar informacion para gerencia",
  "executionDate": "2026-04-22T08:00:00",
  "status": "PROGRAMMED",
  "items": [
    {
      "description": "Revisar datos",
      "completed": false,
      "position": 1
    },
    {
      "description": "Consolidar cifras",
      "completed": false,
      "position": 2
    }
  ]
}
```

#### Reglas

- `title` obligatorio.
- `executionDate` obligatoria.
- `status` opcional en creación; si no llega, se asigna `PROGRAMMED`.
- `items` puede venir vacío.
- Cada item debe tener `description`.

#### Response

- `201 Created`
- Header `Location` opcional con la URL del recurso creado.

```json
{
  "id": 1,
  "title": "Preparar informe",
  "description": "Consolidar informacion para gerencia",
  "executionDate": "2026-04-22T08:00:00",
  "status": "PROGRAMMED",
  "items": [
    {
      "id": 10,
      "description": "Revisar datos",
      "completed": false,
      "position": 1
    },
    {
      "id": 11,
      "description": "Consolidar cifras",
      "completed": false,
      "position": 2
    }
  ],
  "pendingExecution": true,
  "dueNowAlert": false,
  "completedItems": 0,
  "totalItems": 2,
  "createdAt": "2026-04-22T10:15:00",
  "updatedAt": "2026-04-22T10:15:00"
}
```

### 3.2 Consultar tarea por id

`GET /api/tasks/{id}`

#### Response

- `200 OK`
- `404 Not Found` si no existe

### 3.3 Listar tareas paginadas

`GET /api/tasks`

#### Query params

- `page`: número de página, base 0. Default `0`.
- `size`: tamaño de página. Default `10`.
- `query`: búsqueda por título o descripción.
- `status`: filtro por estado.
- `pendingOnly`: `true|false`.
- `dueNowOnly`: `true|false`.
- `sort`: criterio opcional. Ejemplo `executionDate,asc`.

#### Criterios funcionales

- `query` buscará inicialmente por `title` y `description`.
- `pendingOnly=true` devolverá tareas en `PROGRAMMED`.
- `dueNowOnly=true` devolverá tareas en `PROGRAMMED` cuya fecha ya llegó.
- Los filtros deben poder combinarse.

#### Response

- `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "title": "Preparar informe",
      "description": "Consolidar informacion para gerencia",
      "executionDate": "2026-04-22T08:00:00",
      "status": "PROGRAMMED",
      "items": [
        {
          "id": 10,
          "description": "Revisar datos",
          "completed": true,
          "position": 1
        }
      ],
      "pendingExecution": true,
      "dueNowAlert": true,
      "completedItems": 1,
      "totalItems": 1,
      "createdAt": "2026-04-22T10:15:00",
      "updatedAt": "2026-04-22T11:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

### 3.4 Actualizar tarea completa

`PUT /api/tasks/{id}`

#### Request

```json
{
  "title": "Preparar informe final",
  "description": "Actualizar informe con comentarios",
  "executionDate": "2026-04-22T10:00:00",
  "status": "IN_PROGRESS",
  "items": [
    {
      "id": 10,
      "description": "Revisar datos",
      "completed": true,
      "position": 1
    },
    {
      "description": "Validar con gerencia",
      "completed": false,
      "position": 2
    }
  ]
}
```

#### Reglas

- El `PUT` representa actualización completa del recurso.
- Si un item llega con `id`, se intenta actualizar.
- Si un item no llega con `id`, se crea.
- Si un item existente no llega en la petición, se elimina.

#### Response

- `200 OK`
- `404 Not Found` si la tarea no existe

### 3.5 Eliminar tarea

`DELETE /api/tasks/{id}`

#### Response

- `204 No Content`
- `404 Not Found` si no existe

### 3.6 Cambiar estado de tarea

Dos opciones posibles:

- Opción A: usar solo `PUT /api/tasks/{id}` y cambiar `status` dentro del payload completo.
- Opción B: exponer endpoint específico.

Para esta prueba se propone además el endpoint específico:

`PATCH /api/tasks/{id}/status`

#### Request

```json
{
  "status": "FINISHED"
}
```

#### Response

- `200 OK`
- `404 Not Found`
- `409 Conflict` si se decide validar transiciones inválidas

## 4. DTOs propuestos

### 4.1 TaskCreateRequest

```json
{
  "title": "string",
  "description": "string",
  "executionDate": "2026-04-22T08:00:00",
  "status": "PROGRAMMED",
  "items": [
    {
      "description": "string",
      "completed": false,
      "position": 1
    }
  ]
}
```

### 4.2 TaskUpdateRequest

```json
{
  "title": "string",
  "description": "string",
  "executionDate": "2026-04-22T08:00:00",
  "status": "IN_PROGRESS",
  "items": [
    {
      "id": 1,
      "description": "string",
      "completed": true,
      "position": 1
    }
  ]
}
```

### 4.3 TaskStatusUpdateRequest

```json
{
  "status": "CANCELLED"
}
```

### 4.4 TaskResponse

```json
{
  "id": 1,
  "title": "string",
  "description": "string",
  "executionDate": "2026-04-22T08:00:00",
  "status": "PROGRAMMED",
  "items": [
    {
      "id": 10,
      "description": "string",
      "completed": false,
      "position": 1
    }
  ],
  "pendingExecution": true,
  "dueNowAlert": false,
  "completedItems": 0,
  "totalItems": 1,
  "createdAt": "2026-04-22T10:15:00",
  "updatedAt": "2026-04-22T10:15:00"
}
```

### 4.5 TaskPageResponse

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "empty": true
}
```

## 5. Validaciones funcionales

### 5.1 Task

- `title` obligatorio.
- `title` longitud mínima recomendada: 3.
- `title` longitud máxima recomendada: 120.
- `description` opcional.
- `description` longitud máxima recomendada: 1000.
- `executionDate` obligatoria.
- `status` debe pertenecer al enum definido.

### 5.2 TaskItem

- `description` obligatoria.
- `description` longitud máxima recomendada: 250.
- `completed` obligatorio si el campo está presente en request.
- `position` opcional, pero recomendable para conservar orden.

## 6. Reglas derivadas en la respuesta

### 6.1 `pendingExecution`

Será `true` cuando:

- `status = PROGRAMMED`

### 6.2 `dueNowAlert`

Será `true` cuando:

- `status = PROGRAMMED`
- `executionDate <= now`

### 6.3 `completedItems`

Cantidad de ítems con `completed = true`.

### 6.4 `totalItems`

Cantidad total de ítems asociados a la tarea.

## 7. Manejo de errores

Formato común propuesto:

```json
{
  "timestamp": "2026-04-22T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks"
}
```

### 7.1 Validación

- `400 Bad Request`

Ejemplo:

```json
{
  "timestamp": "2026-04-22T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "title must not be blank",
  "path": "/api/tasks"
}
```

### 7.2 Recurso no encontrado

- `404 Not Found`

Ejemplo:

```json
{
  "timestamp": "2026-04-22T12:31:00",
  "status": 404,
  "error": "Not Found",
  "message": "Task with id 99 was not found",
  "path": "/api/tasks/99"
}
```

### 7.3 Conflicto de negocio

- `409 Conflict`

Uso sugerido:

- transición de estado inválida
- intento de operación no permitida por regla de negocio

## 8. Decisiones de implementación derivadas del contrato

- El frontend consumirá un único recurso principal: `tasks`.
- La actualización completa de ítems se hará dentro del `PUT` de la tarea.
- El endpoint `PATCH /api/tasks/{id}/status` facilitará cambios rápidos de estado desde el listado.
- El backend devolverá campos derivados listos para pintar alertas y métricas sin recalcularlas en Angular.
- La respuesta paginada se expondrá en un formato plano y fácil de consumir, sin acoplar al frontend a clases internas de Spring.

## 9. Próximo paso propuesto

Documentar la estructura final del proyecto backend y el contenido objetivo del `pom.xml`, para después comenzar la implementación del esqueleto de Spring Boot.
