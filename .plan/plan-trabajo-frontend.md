# Plan de trabajo y arquitectura — Frontend Angular

## 1. Contexto

La prueba solicita construir una aplicación tipo TODO Tasks con `Java + Spring Boot` para el
backend y `Angular` para el frontend. El backend está terminado. Este documento define la
arquitectura objetivo del frontend, las decisiones técnicas, el alcance funcional y el plan de
ejecución.

## 2. Objetivo del frontend

Proveer una SPA (Single Page Application) que permita al usuario:

- Listar tareas de forma paginada con búsqueda y filtros.
- Crear tareas con ítems checkeables.
- Editar tareas y sus ítems.
- Cambiar el estado de una tarea (flujo de estados controlado).
- Eliminar tareas con confirmación.
- Visualizar alertas por tareas vencidas (`dueNowAlert`).
- Visualizar indicador de tareas pendientes (`pendingExecution`).

## 3. Stack tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Framework | Angular | 21.x (última estable) |
| UI Components | Angular Material | 21.x |
| Formularios | Reactive Forms | integrado en Angular |
| HTTP | HttpClient | integrado en Angular |
| Reactividad | RxJS + Signals | integrado en Angular |
| Estilos | SCSS + Angular Material theming (M3) | - |
| Tests | Jasmine + Karma | integrado en Angular |
| Build | Angular CLI | 21.x |

## 4. Decisiones técnicas

### 4.1 Standalone components

Se usa el patrón moderno de Angular 21: **standalone components** sin NgModules. Cada componente
declara sus propias importaciones. La configuración de la aplicación se centraliza en
`app.config.ts`.

### 4.2 Patrón de estado local

No se usa NgRx ni ningún gestor de estado global. El estado de la lista de tareas (filtros,
paginación, resultados) vive en `TaskListComponent` usando `Signals` de Angular. Esto es
suficiente para el alcance de la prueba.

### 4.3 Una sola ruta

La aplicación tiene una sola vista (`/`). Las operaciones de crear, editar, cambiar estado y
eliminar se realizan desde **modales (MatDialog)** sin navegar a rutas distintas.

### 4.4 Comunicación con el backend

- URL base: `http://localhost:8080`
- Configurada en `environment.ts` como `apiUrl`
- Un único servicio `TaskService` encapsula todos los llamados HTTP
- Un interceptor de errores centralizado (`ErrorInterceptor`) captura errores HTTP y muestra un
  `MatSnackBar` con el mensaje de la respuesta o un mensaje genérico

### 4.5 Mapeo de respuestas derivadas

El backend ya calcula `pendingExecution`, `dueNowAlert`, `completedItems` y `totalItems`. El
frontend **no recalcula** nada — usa directamente los campos de la respuesta para mostrar
indicadores y estilos.

### 4.6 Formato de fechas

Las fechas llegan del backend en formato ISO 8601 (`2026-04-22T10:00:00`). Para mostrarlas al
usuario se usa el `DatePipe` de Angular con locale `es`. Para el date picker del formulario se
usa `MatDatepicker` con `NativeDateAdapter`.

## 5. Estructura de paquetes objetivo

```text
src/
└── app/
    ├── core/
    │   ├── handlers/
    │   │   └── global-error-handler.ts
    │   ├── interceptors/
    │   │   └── error.interceptor.ts
    │   └── services/
    │       └── task.service.ts
    ├── shared/
    │   ├── models/
    │   │   ├── task.model.ts
    │   │   ├── page-response.model.ts
    │   │   └── task-filter.model.ts
    │   └── components/
    │       ├── confirm-dialog/
    │       │   └── confirm-dialog.ts
    │       ├── empty-state/
    │       │   └── empty-state.ts
    │       └── loading-spinner/
    │           └── loading-spinner.ts
    └── features/
        └── tasks/
            └── components/
                ├── task-list/
                │   ├── task-list.ts
                │   ├── task-list.html
                │   └── task-list.scss
                ├── task-filter-bar/
                │   ├── task-filter-bar.ts
                │   ├── task-filter-bar.html
                │   └── task-filter-bar.scss
                ├── task-card/
                │   ├── task-card.ts
                │   ├── task-card.html
                │   └── task-card.scss
                ├── task-form-dialog/
                │   ├── task-form-dialog.ts
                │   ├── task-form-dialog.html
                │   └── task-form-dialog.scss
                ├── task-item-form/
                │   ├── task-item-form.ts
                │   ├── task-item-form.html
                │   └── task-item-form.scss
                ├── task-status-badge/
                │   ├── task-status-badge.ts
                │   ├── task-status-badge.html
                │   └── task-status-badge.scss
                └── status-change-dialog/
                    ├── status-change-dialog.ts
                    ├── status-change-dialog.html
                    └── status-change-dialog.scss
```

> **Nota sobre convención de nombres en Angular 21:** Los archivos de componentes no llevan el
> sufijo `.component`. Las clases se nombran sin el sufijo `Component` (ej: `TaskList` en lugar
> de `TaskListComponent`). Esta es la convención por defecto del CLI 21.

## 6. Componentes principales

### 6.1 `App` (shell — smart)
- Toolbar de Angular Material con el título de la app.
- `<router-outlet>` para renderizar `TaskList`.
- Expone signal `isMobile` a los componentes hijo.

### 6.2 `TaskList` (smart — orquestador)
- Vista principal: recibe eventos de `TaskFilterBar`, carga datos y pasa tareas a `TaskCard`.
- Gestiona paginación con `MatPaginator`.

### 6.3 `TaskFilterBar` (dumb)
- Barra de filtros: búsqueda con debounce, selector de estado, toggles pendientes/vencidas.
- Emite un `TaskFilter` completo cada vez que cambia algún control.
- Colapsa bajo un botón en móvil.

### 6.4 `TaskCard` (dumb — OnPush)
- Tarjeta individual con todos los indicadores de una tarea.
- Emite outputs `edit`, `delete` y `statusChange` hacia `TaskList`.

### 6.5 `TaskStatusBadge` (dumb — OnPush)
- Chip reutilizable con color y texto según el estado de la tarea.

### 6.6 `TaskFormDialog` (smart — lazy loaded)
- Modal de crear/editar con Reactive Form.
- Delega la UI del `FormArray` de ítems a `TaskItemForm`.

### 6.7 `TaskItemForm` (dumb)
- Gestiona el `FormArray` de ítems: agregar, eliminar y validar cada ítem.

### 6.8 `StatusChangeDialog` (smart — lazy loaded)
- Modal para cambiar el estado respetando las transiciones válidas del negocio.

### 6.9 `ConfirmDialog` (shared — dumb)
- Dialog de confirmación genérico: título, mensaje y etiqueta del botón configurables.
- Usado para borrado de tareas y otras acciones destructivas.

### 6.10 `LoadingSpinner` (shared — dumb)
- Overlay de carga centrado reutilizable.

### 6.11 `EmptyState` (shared — dumb)
- Mensaje de lista vacía con ícono y texto configurables.

## 7. Estrategia de pruebas

Objetivo: cubrir los escenarios funcionales críticos del frontend con pruebas unitarias.

| Suite | Tipo | Herramienta |
|-------|------|------------|
| `TaskService` | Unitaria | HttpClientTestingModule |
| `TaskList` | TestBed con mock de servicio | Jasmine |
| `TaskFormDialog` | TestBed | Jasmine |
| `TaskStatusBadge` | TestBed simple | Jasmine |

Escenarios mínimos a cubrir:

- `TaskService`: findAll con y sin filtros, create, update, updateStatus, delete, error HTTP
- `TaskList`: carga inicial, búsqueda con debounce, filtro por estado, paginación
- `TaskFormDialog`: formulario vacío inválido, submit create, submit update, agregar/quitar ítems
- `TaskStatusBadge`: renderizado correcto de cada estado

## 8. CORS en backend

Se agrega `WebConfig.java` al backend para permitir peticiones desde `http://localhost:4200`:

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:4200")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
```

## 9. Docker (bonus)

- `todo-tasks-backend/Dockerfile`: imagen `eclipse-temurin:21-jre-alpine`, copia el `.jar`
- `todo-tasks.frontend/Dockerfile`: multi-stage — Node para compilar, nginx para servir el `dist/`
- `docker-compose.yml` en raíz: servicios `backend` (8080) y `frontend` (80), red compartida

## 10. Plan de ejecución

### Fase 1 — Setup Angular
- `ng new` con Angular CLI 21, `--style=scss`, `--routing=false`, `--skip-git`
- `ng add @angular/material`

### Fase 2 — Documentación de diseño
- Crear archivos `.plan/` con definiciones de modelos, componentes y contratos

### Fase 3 — Modelos, servicio HTTP y configuración
- `shared/models/` con todas las interfaces TypeScript
- `task.service.ts` con los 6 métodos
- `error.interceptor.ts` y `global-error-handler.ts`
- `app.config.ts` configurado con todos los providers

### Fase 4 — CORS en backend

### Fase 5 — Componentes de soporte
- `TaskStatusBadge`, `ConfirmDialog`, `StatusChangeDialog`, `LoadingSpinner`, `EmptyState`

### Fase 6 — TaskCard, TaskFilterBar y TaskList
- `TaskFilterBar`: controles de búsqueda, estado y toggles (dumb, emite `TaskFilter`)
- `TaskCard`: tarjeta con indicadores, progreso y menú de acciones (dumb, OnPush)
- `TaskList`: orquesta filtros, lista paginada y apertura de dialogs (smart)

### Fase 7 — TaskFormDialog y TaskItemForm
- `TaskFormDialog`: formulario reactivo de crear/editar, lazy loaded
- `TaskItemForm`: gestión dinámica del `FormArray` de ítems (dumb)

### Fase 8 — Pruebas

### Fase 9 — Docker

### Fase 10 — README y documentación final

## 11. Criterios de terminado para frontend

El frontend se considerará listo cuando:

- La SPA permita ejecutar el CRUD completo de tareas.
- Los filtros (búsqueda, estado, pendientes, vencidas) funcionen correctamente.
- Los indicadores de alerta (`dueNowAlert`, `pendingExecution`) sean visibles en la UI.
- La gestión de ítems checkeables funcione en el formulario.
- Exista una suite de pruebas unitarias que cubra los escenarios definidos.
- La solución arranque con instrucciones claras (`ng serve`).
- Docker funcione con `docker-compose up`.
