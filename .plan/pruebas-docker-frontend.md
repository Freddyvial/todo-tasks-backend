# Pruebas Angular y Docker — Frontend TODO Tasks

## 1. Estrategia de pruebas

### 1.1 Alcance

Las pruebas cubren la lógica del servicio HTTP y el comportamiento de los componentes
principales. No se implementan pruebas e2e (fuera de alcance para la prueba técnica).

### 1.2 Herramientas

- **Jasmine** — framework de aserciones (integrado en Angular CLI)
- **Karma** — runner de pruebas en navegador (integrado en Angular CLI)
- **`HttpClientTestingModule`** — mock de HttpClient para `TaskService`
- **`TestBed`** — utilidad de Angular para montar componentes en pruebas

### 1.3 Ejecutar pruebas

```bash
ng test
```

---

## 2. Suite `TaskService`

**Archivo:** `src/app/core/services/task.service.spec.ts`

Usa `HttpClientTestingModule` y `HttpTestingController` para interceptar peticiones HTTP sin
tocar la red real.

### Escenarios cubiertos

| Test | Descripción |
|------|-------------|
| `findAll sin filtros` | GET `/api/tasks?page=0&size=10&sort=...` → devuelve `PageResponse` |
| `findAll con query y status` | Verifica que los params se incluyen en la URL |
| `findAll con pendingOnly` | Verifica que `pendingOnly=true` se envía |
| `findAll con dueNowOnly` | Verifica que `dueNowOnly=true` se envía |
| `getById` | GET `/api/tasks/1` → devuelve `Task` |
| `create` | POST `/api/tasks` con body correcto → devuelve `Task` |
| `update` | PUT `/api/tasks/1` con body correcto → devuelve `Task` |
| `updateStatus` | PATCH `/api/tasks/1/status` con `{ status }` → devuelve `Task` |
| `delete` | DELETE `/api/tasks/1` → `204 No Content` |
| `error HTTP` | Petición con error `404` → el observable lanza error |

### Estructura de prueba

```typescript
describe('TaskService', () => {
  let service: TaskService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should call findAll with correct params', () => {
    service.findAll({ page: 0, size: 10, sort: 'executionDate,asc' }).subscribe();
    const req = httpTesting.expectOne(r => r.url.includes('/api/tasks'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ content: [], page: 0, size: 10, totalElements: 0, ... });
  });
});
```

---

## 3. Suite `TaskStatusBadge`

**Archivo:** `src/app/features/tasks/components/task-status-badge/task-status-badge.spec.ts`

| Test | Descripción |
|------|-------------|
| Renderiza `Programado` con clase `badge--programmed` | Input: `PROGRAMMED` |
| Renderiza `En progreso` con clase `badge--in-progress` | Input: `IN_PROGRESS` |
| Renderiza `Finalizado` con clase `badge--finished` | Input: `FINISHED` |
| Renderiza `Cancelado` con clase `badge--cancelled` | Input: `CANCELLED` |

---

## 4. Suite `TaskFormDialog`

**Archivo:** `src/app/features/tasks/components/task-form-dialog/task-form-dialog.spec.ts`

| Test | Descripción |
|------|-------------|
| Formulario inválido al inicio (modo create) | El botón "Guardar" está deshabilitado |
| Valida `title` requerido | Muestra error si se toca y deja vacío |
| Valida `title` minLength 3 | Muestra error con 2 caracteres |
| Valida `executionDate` requerido | Muestra error si no se selecciona fecha |
| Agregar ítem | El FormArray crece en 1 |
| Eliminar ítem | El FormArray decrece en 1 |
| Submit en modo create | Llama `TaskService.create` con los datos del formulario |
| Submit en modo edit | Llama `TaskService.update` con id y datos |
| Cierra dialog con `true` al éxito | `dialogRef.close(true)` se llama tras respuesta exitosa |

---

## 5. Suite `TaskList`

**Archivo:** `src/app/features/tasks/components/task-list/task-list.spec.ts`

| Test | Descripción |
|------|-------------|
| Carga inicial | Llama `TaskService.findAll` al inicializar |
| Muestra spinner mientras carga | `loading = true` → spinner visible |
| Muestra tarjetas al recibir datos | `tasks` → renderiza N `task-card` |
| Muestra mensaje vacío | `totalElements = 0` → texto "No se encontraron tareas" |
| Búsqueda con debounce | Input → espera 400ms → llama `findAll` con `query` |
| Cambio de página | `MatPaginator` emite evento → llama `findAll` con `page` nuevo |
| Toggle `pendingOnly` | Click → llama `findAll` con `pendingOnly=true` |
| Abrir dialog de creación | Click "Nueva tarea" → `MatDialog.open` llamado |

---

## 6. Docker

### 6.1 `todo-tasks-backend/Dockerfile`

Imagen multi-stage para el backend:

```dockerfile
# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Etapa 2: runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6.2 `todo-tasks.frontend/Dockerfile`

Imagen multi-stage para el frontend:

```dockerfile
# Etapa 1: build Angular
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: servir con nginx
FROM nginx:alpine
COPY --from=build /app/dist/todo-tasks-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 6.3 `todo-tasks.frontend/nginx.conf`

Configuración de nginx para SPA (redirige todo a `index.html`):

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://backend:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

> Con este `proxy_pass`, en Docker el frontend llama a `/api/tasks` (misma URL) y nginx
> lo reenvía al servicio `backend` en el puerto 8080. Esto evita CORS en producción Docker.
> Para desarrollo local (`ng serve`) se sigue usando CORS con `http://localhost:8080`.

### 6.4 `docker-compose.yml` (raíz del proyecto)

```yaml
version: '3.9'

services:
  backend:
    build:
      context: ./todo-tasks-backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    networks:
      - todo-net

  frontend:
    build:
      context: ./todo-tasks.frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - todo-net

networks:
  todo-net:
    driver: bridge
```

**Para levantar el stack completo:**

```bash
docker-compose up --build
```

- Frontend disponible en: `http://localhost`
- Backend disponible en: `http://localhost:8080`

> **Nota:** con Docker, el frontend usa `/api/tasks` (proxy nginx → backend). Para desarrollo
> local el frontend llama directamente a `http://localhost:8080/api/tasks` con CORS habilitado.

### 6.5 Variable de entorno por perfil

Para que el `apiUrl` funcione tanto en desarrollo como en Docker:

**`environment.development.ts`** → `apiUrl: 'http://localhost:8080'`  
**`environment.ts`** → `apiUrl: ''` (vacío → nginx hace el proxy con `/api/`)

El `angular.json` ya configura `fileReplacements` para usar el archivo correcto según el target.

---

## 7. Comandos de referencia

```bash
# Desarrollo
cd todo-tasks.frontend
ng serve                          # http://localhost:4200

# Tests
ng test

# Build de producción
ng build

# Docker
docker-compose up --build         # Stack completo
docker-compose down               # Detener
```
