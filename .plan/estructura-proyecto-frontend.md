# Estructura del proyecto y diseño de componentes — Frontend Angular

## 1. Estructura de archivos completa

```text
todo-tasks.frontend/
├── Dockerfile
├── nginx.conf
├── README.md
├── angular.json
├── package.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.spec.json
└── src/
    ├── index.html
    ├── main.ts
    ├── styles.scss
    ├── environments/
    │   ├── environment.ts
    │   └── environment.development.ts
    └── app/
        ├── app.ts
        ├── app.html
        ├── app.scss
        ├── app.config.ts
        ├── app.routes.ts
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
        │   ├── pipes/
        │   │   └── elapsed-time.pipe.ts          ← pipe para mostrar tiempo transcurrido
        │   └── components/                      ← componentes reutilizables globales
        │       ├── confirm-dialog/               ← dialog de confirmación genérico
        │       │   └── confirm-dialog.ts
        │       ├── empty-state/                  ← mensaje de lista vacía
        │       │   └── empty-state.ts
        │       └── loading-spinner/              ← overlay de carga
        │           └── loading-spinner.ts
        └── features/
            └── tasks/
                └── components/
                    ├── task-list/                ← smart: orquesta la vista principal
                    │   ├── task-list.ts
                    │   ├── task-list.html
                    │   └── task-list.scss
                    ├── task-filter-bar/          ← dumb: barra de filtros
                    │   ├── task-filter-bar.ts
                    │   ├── task-filter-bar.html
                    │   └── task-filter-bar.scss
                    ├── task-card/                ← dumb: tarjeta individual OnPush
                    │   ├── task-card.ts
                    │   ├── task-card.html
                    │   └── task-card.scss
                    ├── task-form-dialog/         ← smart: crear/editar (lazy loaded)
                    │   ├── task-form-dialog.ts
                    │   ├── task-form-dialog.html
                    │   └── task-form-dialog.scss
                    ├── task-item-form/           ← dumb: FormArray de ítems
                    │   ├── task-item-form.ts
                    │   ├── task-item-form.html
                    │   └── task-item-form.scss
                    ├── task-status-badge/        ← dumb: chip de estado OnPush
                    │   ├── task-status-badge.ts
                    │   ├── task-status-badge.html
                    │   └── task-status-badge.scss
                    ├── task-items-quick-dialog/  ← gestión rápida de ítems desde la lista
                    │   └── task-items-quick-dialog.ts
                    └── status-change-dialog/     ← smart: cambio de estado (lazy loaded)
                        ├── status-change-dialog.ts
                        ├── status-change-dialog.html
                        └── status-change-dialog.scss
```

## 2. Archivos de configuración

### `app.config.ts`

```typescript
import { ApplicationConfig, ErrorHandler, provideZoneChangeDetection, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideNativeDateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { routes } from './app.routes';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { GlobalErrorHandler } from './core/handlers/global-error-handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor])),
    provideAnimationsAsync(),
    provideNativeDateAdapter(),
    { provide: LOCALE_ID, useValue: 'es' },
    { provide: MAT_DATE_LOCALE, useValue: 'es-CO' },
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
  ],
};
```

### `app.routes.ts`

```typescript
import { Routes } from '@angular/router';
import { TaskList } from './features/tasks/components/task-list/task-list';

export const routes: Routes = [
  { path: '', component: TaskList },
  { path: '**', redirectTo: '' },
];
```

### `environment.ts` (producción)

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080',
};
```

### `environment.development.ts` (desarrollo)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
};
```

### `app.html` (shell)

```html
<mat-toolbar color="primary">
  <span>TODO Tasks</span>
</mat-toolbar>
<main>
  <router-outlet />
</main>
```

### `app.ts`

```typescript
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatToolbarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
```

### `styles.scss` (global)

Mantener el theming de Angular Material generado por `ng add`. Agregar:

```scss
main {
  padding: 24px;
  max-width: 1100px;
  margin: 0 auto;
}

.mat-mdc-card {
  margin-bottom: 16px;
}
```

## 3. Modelos TypeScript

### `task.model.ts`

```typescript
export type TaskStatus = 'PROGRAMMED' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED';

export interface Task {
  id: number;
  title: string;
  description?: string;
  executionDate: string;
  status: TaskStatus;
  items: TaskItem[];
  pendingExecution: boolean;
  dueNowAlert: boolean;
  completedItems: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskItem {
  id?: number;
  description: string;
  completed: boolean;
  position?: number;
}

export interface TaskCreateRequest {
  title: string;
  description?: string;
  executionDate: string;
  status?: TaskStatus;
  items?: TaskItemRequest[];
}

export interface TaskUpdateRequest {
  title: string;
  description?: string;
  executionDate: string;
  status: TaskStatus;
  items?: TaskItemRequest[];
}

export interface TaskItemRequest {
  id?: number;
  description: string;
  completed: boolean;
  position?: number;
}

export interface TaskStatusUpdateRequest {
  status: TaskStatus;
}
```

### `page-response.model.ts`

```typescript
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

### `task-filter.model.ts`

```typescript
import { TaskStatus } from './task.model';

export interface TaskFilter {
  page?: number;
  size?: number;
  query?: string;
  status?: TaskStatus | null;
  pendingOnly?: boolean;
  dueNowOnly?: boolean;
  sort?: string;
}
```

## 4. Diseño del servicio HTTP

### `task.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly apiUrl = `${environment.apiUrl}/api/tasks`;
  private readonly http = inject(HttpClient);

  findAll(filter: TaskFilter): Observable<PageResponse<Task>> {
    const params = buildHttpParams(filter);
    return this.http.get<PageResponse<Task>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.apiUrl}/${id}`);
  }

  create(request: TaskCreateRequest): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, request);
  }

  update(id: number, request: TaskUpdateRequest): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}`, request);
  }

  updateStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.apiUrl}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

**Lógica de `buildHttpParams`:** convierte el objeto `TaskFilter` a `HttpParams` omitiendo
valores `null`, `undefined` y cadenas vacías.

### `error.interceptor.ts`

Interceptor funcional (no clase):

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = error.error?.message ?? 'Error inesperado. Intente de nuevo.';
      inject(MatSnackBar).open(message, 'Cerrar', { duration: 4000 });
      return throwError(() => error);
    })
  );
};
```

## 5. Diseño de componentes

### 5.1 `TaskStatusBadge`

**Responsabilidad:** chip reutilizable con color y texto según el estado.

**Input:** `status: TaskStatus`

**Tabla de estilos:**

| Estado | Texto visible | Color (CSS class) |
|--------|--------------|-------------------|
| `PROGRAMMED` | Programado | `badge--programmed` (azul) |
| `IN_PROGRESS` | En progreso | `badge--in-progress` (naranja) |
| `FINISHED` | Finalizado | `badge--finished` (verde) |
| `CANCELLED` | Cancelado | `badge--cancelled` (gris) |

**Template:**

```html
<span class="status-badge" [ngClass]="badgeClass()">{{ label() }}</span>
```

**Lógica:** `badgeClass()` y `label()` son signals computados a partir del input `status`.

---

### 5.2 `ConfirmDialog`

**Responsabilidad:** dialog de confirmación genérico para cualquier acción destructiva.

**Data inyectada (MAT_DIALOG_DATA):**

```typescript
export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;  // default: 'Confirmar'
  confirmColor?: 'primary' | 'warn' | 'accent';  // default: 'warn'
}
```

**Template:**

```html
<h2 mat-dialog-title>{{ data.title }}</h2>
<mat-dialog-content>{{ data.message }}</mat-dialog-content>
<mat-dialog-actions align="end">
  <button mat-button [mat-dialog-close]="false">Cancelar</button>
  <button mat-button [color]="data.confirmColor ?? 'warn'" [mat-dialog-close]="true">
    {{ data.confirmLabel ?? 'Confirmar' }}
  </button>
</mat-dialog-actions>
```

---

### 5.3 `StatusChangeDialog`

**Responsabilidad:** mostrar estado actual y opciones de transición válidas.

**Data inyectada:**

```typescript
{ task: Task }
```

**Transiciones permitidas (según regla de negocio del backend):**

```typescript
const TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  PROGRAMMED:  ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['FINISHED',    'CANCELLED'],
  FINISHED:    [],
  CANCELLED:   [],
};
```

**Flujo:**
1. Muestra estado actual con `TaskStatusBadge`.
2. Muestra botones para los estados destino permitidos.
3. Al seleccionar → llama `TaskService.updateStatus(id, newStatus)`.
4. Cierra el dialog pasando la tarea actualizada.
5. Si el backend responde `409` → el `ErrorInterceptor` muestra el snackbar y el dialog no cierra.

---

### 5.4 `TaskCard`

**Responsabilidad:** representar visualmente una tarea en la lista.

**Inputs:** `task: Task`  
**Outputs:** `edited`, `deleted`, `statusChanged` (para que `TaskList` recargue)

**Secciones del card:**

```
┌─────────────────────────────────────────────────────┐
│ [⚠ VENCIDA]  Título de la tarea       [badge estado] │
│ Fecha: 22/04/2026 10:00                          [⋮] │
│ Descripción breve...                               │
│ ████████░░ 2 / 3 ítems completados                │
└─────────────────────────────────────────────────────┘
```

**Indicadores:**
- `dueNowAlert === true` → chip rojo con texto "⚠ Vencida" (visible solo cuando aplica)
- `pendingExecution === true` y NO `dueNowAlert` → chip azul tenue "Pendiente"
- `mat-progress-bar` con `value = (completedItems / totalItems) * 100` (oculto si `totalItems === 0`)
- Texto `completedItems / totalItems ítems` (oculto si `totalItems === 0`)

**Menú de acciones (`mat-menu`):**
- Editar → emite `edited`
- Cambiar estado → emite `statusChanged`
- Eliminar → emite `deleted`

---

### 5.5 `TaskFormDialog`

**Responsabilidad:** formulario reactivo para crear y editar tareas.

**Modos:**
- `create`: título del dialog "Nueva tarea", sin campo de estado, submit llama `create()`
- `edit`: título del dialog "Editar tarea", muestra campo de estado, submit llama `update()`

**Data inyectada:**

```typescript
{ mode: 'create' | 'edit'; task?: Task }
```

**Estructura del formulario (Reactive Form):**

```typescript
FormGroup({
  title:         FormControl<string>([Validators.required, Validators.minLength(3), Validators.maxLength(120)]),
  description:   FormControl<string | null>([Validators.maxLength(1000)]),
  executionDate: FormControl<Date | null>([Validators.required]),
  status:        FormControl<TaskStatus>([Validators.required]),  // solo en modo edit
  items:         FormArray([
    FormGroup({
      id:          FormControl<number | null>(null),
      description: FormControl<string>([Validators.required, Validators.maxLength(250)]),
      completed:   FormControl<boolean>(false),
    })
  ])
})
```

**Layout del template:**

```
┌─────────────────────────────────────────────────────┐
│ Nueva tarea / Editar tarea                      [✕] │
├─────────────────────────────────────────────────────┤
│ Título *                                            │
│ [_______________________________________________]   │
│                                                     │
│ Descripción                                         │
│ [_______________________________________________]   │
│                                                     │
│ Fecha de ejecución *       Estado (solo edit)       │
│ [dd/mm/aaaa 📅]            [Programado         ▼]   │
│                                                     │
│ Ítems                                               │
│ ┌─────────────────────────────────────────────┐    │
│ │ [ ] Descripción del ítem               [✕]  │    │
│ │ [ ] Otro ítem                          [✕]  │    │
│ └─────────────────────────────────────────────┘    │
│ [+ Agregar ítem]                                    │
├─────────────────────────────────────────────────────┤
│                        [Cancelar]  [Guardar]        │
└─────────────────────────────────────────────────────┘
```

**Lógica de ítems al editar:**
- Los ítems existentes se cargan en el `FormArray` con su `id`.
- Al guardar: los ítems con `id` se envían para actualizar; los sin `id` son nuevos; los que
  no aparecen en el array son eliminados automáticamente por el backend (semantica `PUT`).
- La posición se asigna como el índice en el array (1-based).

**Conversión de fecha:**
- El backend espera `executionDate` como string ISO 8601.
- `NativeDateAdapter` convierte el `Date` del datepicker a string con:
  `date.toISOString().slice(0, 19)` para quitar la zona horaria.

**Resultado al cerrar:**
- Éxito: `dialogRef.close(true)` → `TaskList` recarga la lista.
- Cancelar: `dialogRef.close()` → sin acción.

---

### 5.6 `TaskList`

**Responsabilidad:** página principal. Orquesta filtros, lista y paginación.

**Estado interno (Signals):**

```typescript
tasks        = signal<Task[]>([]);
totalElements = signal(0);
loading      = signal(false);

// Filtros
searchQuery  = signal('');
statusFilter = signal<TaskStatus | null>(null);
pendingOnly  = signal(false);
dueNowOnly   = signal(false);
currentPage  = signal(0);
pageSize     = signal(10);
```

**Layout:**

```
┌──────────────────────────────────────────────────────────┐
│                                          [+ Nueva tarea] │
│                                                          │
│ [🔍 Buscar por título o descripción...]                  │
│                                                          │
│ [Estado ▼]  [Pendientes ▷]  [Vencidas ▷]                │
├──────────────────────────────────────────────────────────┤
│ <task-card *ngFor>                                       │
│ <task-card>                                              │
│ ...                                                      │
│                                                          │
│ (Sin resultados — texto si lista vacía)                  │
├──────────────────────────────────────────────────────────┤
│  < 1 2 3 >              [5] [10] [20] por página         │
└──────────────────────────────────────────────────────────┘
```

**Búsqueda con debounce:**
- `searchControl: FormControl` vinculado al input de búsqueda.
- `debounceTime(400)` con `distinctUntilChanged()` en el pipe del `valueChanges`.
- Al cambiar el valor → resetea `currentPage` a 0 → llama `loadTasks()`.

**Filtros de chips (toggle):**
- `pendingOnly` y `dueNowOnly` son toggles mutuamente excluyentes lógicamente
  (si se activa uno, se desactiva el otro).
- Al cambiar → resetea `currentPage` a 0 → llama `loadTasks()`.

**`loadTasks()` — flujo:**

```typescript
loadTasks(): void {
  this.loading.set(true);
  this.taskService.findAll({
    page: this.currentPage(),
    size: this.pageSize(),
    query: this.searchQuery() || undefined,
    status: this.statusFilter() || undefined,
    pendingOnly: this.pendingOnly() || undefined,
    dueNowOnly: this.dueNowOnly() || undefined,
    sort: 'executionDate,asc',
  }).subscribe({
    next: (page) => {
      this.tasks.set(page.content);
      this.totalElements.set(page.totalElements);
      this.loading.set(false);
    },
    error: () => this.loading.set(false),
  });
}
```

**Apertura de modales:**

```typescript
openCreateDialog(): void {
  const ref = this.dialog.open(TaskFormDialog, {
    width: '600px',
    data: { mode: 'create' },
  });
  ref.afterClosed().subscribe(result => {
    if (result) this.loadTasks();
  });
}

openEditDialog(task: Task): void {
  const ref = this.dialog.open(TaskFormDialog, {
    width: '600px',
    data: { mode: 'edit', task },
  });
  ref.afterClosed().subscribe(result => {
    if (result) this.loadTasks();
  });
}

openDeleteDialog(task: Task): void {
  const ref = this.dialog.open(ConfirmDialog, {
    data: {
      title: 'Eliminar tarea',
      message: `¿Eliminar "${task.title}"? Esta acción no se puede deshacer.`,
      confirmLabel: 'Eliminar',
    },
  });
  ref.afterClosed().subscribe(confirmed => {
    if (confirmed) {
      this.taskService.delete(task.id).subscribe(() => this.loadTasks());
    }
  });
}

openStatusDialog(task: Task): void {
  const ref = this.dialog.open(StatusChangeDialog, {
    data: { task },
  });
  ref.afterClosed().subscribe(result => {
    if (result) this.loadTasks();
  });
}
```

## 6. Importaciones de Angular Material por componente

| Componente | Módulos Material importados |
|------------|----------------------------|
| `App` | `MatToolbarModule` |
| `TaskList` | `MatButtonModule`, `MatInputModule`, `MatSelectModule`, `MatChipsModule`, `MatIconModule`, `MatProgressSpinnerModule`, `MatPaginatorModule`, `MatDialogModule`, `ReactiveFormsModule` |
| `TaskCard` | `MatCardModule`, `MatButtonModule`, `MatIconModule`, `MatMenuModule`, `MatProgressBarModule`, `MatChipsModule`, `DatePipe`, `NgClass` |
| `TaskFormDialog` | `MatDialogModule`, `MatFormFieldModule`, `MatInputModule`, `MatSelectModule`, `MatDatepickerModule`, `MatCheckboxModule`, `MatButtonModule`, `MatIconModule`, `ReactiveFormsModule` |
| `TaskStatusBadge` | `NgClass` |
| `StatusChangeDialog` | `MatDialogModule`, `MatButtonModule`, `MatProgressSpinnerModule` |
| `ConfirmDialog` | `MatDialogModule`, `MatButtonModule` |

## 7. Manejo de loading y estados vacíos

- `loading = signal(false)` en `TaskList`, muestra `mat-spinner` centrado mientras carga.
- Si `tasks().length === 0` y `!loading()`: mensaje "No se encontraron tareas".
- Los botones de submit en los dialogs se deshabilitan mientras la petición está en curso
  (`submitting = signal(false)`).

## 8. Responsividad

Angular Material usa flexbox internamente. Para el layout de las cards se usa CSS Grid:

```scss
.task-list-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
```

Las tarjetas son de ancho completo para máxima legibilidad del contenido.
