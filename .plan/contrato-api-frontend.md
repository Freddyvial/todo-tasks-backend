# Contrato de consumo de API — Frontend Angular

## 1. Objetivo

Definir exactamente cómo el frontend Angular consume la API REST del backend. Incluye los
contratos de petición y respuesta, el mapeo de campos a la UI y los escenarios de error
esperados.

## 2. Configuración de conexión

| Parámetro | Valor desarrollo |
|-----------|-----------------|
| Base URL | `http://localhost:8080` |
| Prefijo de rutas | `/api/tasks` |
| Formato | JSON (`Content-Type: application/json`) |
| Formato de fechas | ISO 8601 (`YYYY-MM-DDTHH:mm:ss`) |

Configurado en `src/environments/environment.development.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
};
```

## 3. Endpoints y contratos

---

### 3.1 Listar tareas

**Petición:**

```
GET /api/tasks
```

**Query params enviados desde `TaskList`:**

| Parámetro | Tipo Angular | Enviado cuando |
|-----------|-------------|----------------|
| `page` | `number` | siempre (default 0) |
| `size` | `number` | siempre (default 10) |
| `query` | `string` | si el input tiene texto |
| `status` | `TaskStatus` | si el selector tiene valor |
| `pendingOnly` | `true` | si el toggle está activo |
| `dueNowOnly` | `true` | si el toggle está activo |
| `sort` | `string` | siempre: `executionDate,asc` |

> Los parámetros con valor `null`, `undefined` o `''` **no se incluyen** en la URL.

**Respuesta esperada (`200 OK`):**

```typescript
PageResponse<Task> = {
  content: Task[];
  page: number;      // página actual (base 0)
  size: number;      // tamaño de página
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

**Uso en UI:**
- `content` → listado de `TaskCard`
- `totalElements` → input `[length]` de `MatPaginator`
- `page` / `size` → inputs del paginator

---

### 3.2 Crear tarea

**Petición:**

```
POST /api/tasks
Content-Type: application/json
```

**Body enviado:**

```typescript
{
  title: string;           // requerido
  description?: string;   // omitido si vacío
  executionDate: string;   // "2026-04-22T10:00:00"
  status?: TaskStatus;     // omitido → backend asigna PROGRAMMED
  items?: TaskItemRequest[];
}
```

**Conversión de fecha en formulario:**

```typescript
// Date del MatDatepicker → string ISO sin zona horaria
const d: Date = form.value.executionDate;
const isoString = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
```

**Respuesta esperada (`201 Created`):**

```typescript
Task  // tarea creada con id, campos calculados y ítems
```

**UI post-respuesta:**
- Cierra `TaskFormDialog` con `dialogRef.close(true)`
- `TaskList` recarga la lista

---

### 3.3 Obtener tarea por id

**Petición:**

```
GET /api/tasks/{id}
```

**Respuesta esperada (`200 OK`):**

```typescript
Task  // tarea completa con ítems y campos calculados
```

**Uso:** se llama al abrir `TaskFormDialog` en modo `edit` para precargar datos frescos del backend antes de mostrar el formulario.

---

### 3.4 Actualizar tarea

**Petición:**

```
PUT /api/tasks/{id}
Content-Type: application/json
```

**Body enviado:**

```typescript
{
  title: string;
  description?: string;
  executionDate: string;
  status: TaskStatus;
  items?: TaskItemRequest[];  // incluye id de ítems existentes
}
```

**Semántica de ítems en PUT:**
- Ítem con `id` → se actualiza el ítem existente
- Ítem sin `id` → se crea como nuevo ítem
- Ítem existente no incluido en la lista → el backend lo elimina (orphan removal)

**Posición de ítems:**
- Se asigna como índice 1-based en el `FormArray`: `position = index + 1`

**Respuesta esperada (`200 OK`):**

```typescript
Task  // tarea actualizada
```

---

### 3.5 Cambiar estado de tarea

**Petición:**

```
PATCH /api/tasks/{id}/status
Content-Type: application/json
```

**Body enviado:**

```typescript
{ status: TaskStatus }
```

**Transiciones válidas (definidas en `StatusChangeDialog`):**

```typescript
const TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  PROGRAMMED:  ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['FINISHED',    'CANCELLED'],
  FINISHED:    [],
  CANCELLED:   [],
};
```

**Respuestas esperadas:**

| Código | Causa | Manejo UI |
|--------|-------|-----------|
| `200 OK` | Transición válida | Cierra dialog, recarga lista |
| `409 Conflict` | Transición inválida (estado final) | `ErrorInterceptor` muestra snackbar |

---

### 3.6 Eliminar tarea

**Petición:**

```
DELETE /api/tasks/{id}
```

**Respuesta esperada (`204 No Content`):**

Sin body. El frontend cierra `ConfirmDialog` y recarga la lista.

---

## 4. Campos calculados del backend — uso en UI

El backend calcula estos campos y el frontend los consume directamente:

| Campo | Tipo | Uso en UI |
|-------|------|-----------|
| `pendingExecution` | `boolean` | Chip "Pendiente" en `TaskCard` (solo si `!dueNowAlert`) |
| `dueNowAlert` | `boolean` | Chip rojo "⚠ Vencida" en `TaskCard` |
| `completedItems` | `number` | Numerador de la barra de progreso |
| `totalItems` | `number` | Denominador de la barra de progreso |

**Lógica del badge de alerta en `TaskCard`:**

```typescript
// No se calcula nada. Solo se lee el campo del backend.
if (task.dueNowAlert) { /* mostrar chip rojo */ }
else if (task.pendingExecution) { /* mostrar chip azul */ }
```

---

## 5. Manejo de errores HTTP

El `ErrorInterceptor` procesa todos los errores HTTP y muestra un `MatSnackBar`:

| Código HTTP | Mensaje mostrado |
|-------------|-----------------|
| `400` | `error.error.message` del backend (ej: "title: must not be blank") |
| `404` | `error.error.message` del backend (ej: "Task with id 99 was not found") |
| `409` | `error.error.message` del backend (ej: "Cannot change status from FINISHED...") |
| `500` | "Error inesperado. Intente de nuevo." |
| Sin conexión | "No se pudo conectar con el servidor." |

**El interceptor NO reintenta peticiones.** Simplemente notifica al usuario y propaga el error
para que los componentes puedan actualizar su estado de carga (`loading.set(false)`).

---

## 6. Construcción de HttpParams

```typescript
function buildHttpParams(filter: TaskFilter): HttpParams {
  let params = new HttpParams();
  if (filter.page !== undefined) params = params.set('page', filter.page);
  if (filter.size !== undefined) params = params.set('size', filter.size);
  if (filter.query)              params = params.set('query', filter.query);
  if (filter.status)             params = params.set('status', filter.status);
  if (filter.pendingOnly)        params = params.set('pendingOnly', 'true');
  if (filter.dueNowOnly)         params = params.set('dueNowOnly', 'true');
  if (filter.sort)               params = params.set('sort', filter.sort);
  return params;
}
```

---

## 7. CORS

El backend expone el header `Access-Control-Allow-Origin: http://localhost:4200` para todos los
endpoints bajo `/api/**`. Esto se configura en `WebConfig.java` del backend:

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:4200")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
```

Sin esta configuración, el navegador bloquea las peticiones por política de mismo origen.
