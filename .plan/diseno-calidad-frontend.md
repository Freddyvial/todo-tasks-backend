# Diseño de calidad — Manejo de errores, componentes reutilizables, responsive y optimización

## 1. Manejo de errores — Arquitectura en capas

Angular recomienda manejar errores en múltiples capas para no acoplar la presentación con el
transporte ni la lógica de negocio con la UI. Se definen cuatro capas:

```
┌──────────────────────────────────────────────────────────┐
│  Capa 4 — GlobalErrorHandler                             │
│  Captura errores JS no atrapados (null refs, etc.)       │
├──────────────────────────────────────────────────────────┤
│  Capa 3 — ErrorInterceptor (HTTP)                        │
│  Traduce HttpErrorResponse → mensaje de usuario          │
├──────────────────────────────────────────────────────────┤
│  Capa 2 — Componente (subscribe error callback)          │
│  Gestiona estado de carga y habilita el formulario       │
├──────────────────────────────────────────────────────────┤
│  Capa 1 — Reactive Forms (validators)                    │
│  Valida entrada antes de enviar al servidor              │
└──────────────────────────────────────────────────────────┘
```

---

### 1.1 Capa 1 — Validación de formularios (Reactive Forms)

Primera defensa: no llegar al backend con datos inválidos.

**Validadores por campo:**

```typescript
const form = new FormGroup({
  title: new FormControl('', [
    Validators.required,
    Validators.minLength(3),
    Validators.maxLength(120),
  ]),
  description: new FormControl('', [
    Validators.maxLength(1000),
  ]),
  executionDate: new FormControl<Date | null>(null, [
    Validators.required,
  ]),
  status: new FormControl<TaskStatus>('PROGRAMMED', [
    Validators.required,
  ]),
});
```

**Mensajes de error en template — función helper reutilizable:**

```typescript
// task-form-dialog.ts
getFieldError(field: string): string | null {
  const control = this.form.get(field);
  if (!control?.touched || !control.errors) return null;
  if (control.errors['required'])   return 'Este campo es obligatorio.';
  if (control.errors['minlength'])  return `Mínimo ${control.errors['minlength'].requiredLength} caracteres.`;
  if (control.errors['maxlength'])  return `Máximo ${control.errors['maxlength'].requiredLength} caracteres.`;
  return 'Valor inválido.';
}
```

**En template:**

```html
<mat-form-field>
  <mat-label>Título</mat-label>
  <input matInput formControlName="title" />
  @if (getFieldError('title'); as error) {
    <mat-error>{{ error }}</mat-error>
  }
</mat-form-field>
```

**Regla:** el botón "Guardar" solo se habilita cuando `form.valid && !submitting()`.

---

### 1.2 Capa 2 — Manejo de error en componentes

Cada componente que llama al servicio controla su propio estado de carga y recuperación.

**Patrón estándar:**

```typescript
// En cualquier componente que hace una llamada HTTP
readonly loading  = signal(false);
readonly submitting = signal(false);

// Para listados (loadTasks)
loadTasks(): void {
  this.loading.set(true);
  this.taskService.findAll(this.buildFilter()).subscribe({
    next: page => {
      this.tasks.set(page.content);
      this.totalElements.set(page.totalElements);
      this.loading.set(false);
    },
    error: () => {
      // El interceptor ya mostró el snackbar.
      // Solo limpiamos el estado de carga.
      this.loading.set(false);
    },
  });
}

// Para formularios (submit)
submit(): void {
  if (this.form.invalid || this.submitting()) return;
  this.submitting.set(true);
  const call$ = this.isEdit
    ? this.taskService.update(this.taskId, this.buildRequest())
    : this.taskService.create(this.buildRequest());

  call$.subscribe({
    next: () => this.dialogRef.close(true),
    error: () => this.submitting.set(false),  // rehabilita el formulario
  });
}
```

**Lo que NO se hace:**
- No se rethrow ni se re-muestra el error (ya lo hizo el interceptor).
- No se vacía la lista en caso de error de paginación (se conserva el último estado).
- No se usa `console.error` en producción.

---

### 1.3 Capa 3 — ErrorInterceptor (HTTP)

Interceptor funcional de Angular 21 (`HttpInterceptorFn`) que centraliza la traducción de
errores HTTP a mensajes de usuario.

**Archivo:** `src/app/core/interceptors/error.interceptor.ts`

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = resolveMessage(error);
      inject(MatSnackBar).open(message, 'Cerrar', {
        duration: 5000,
        panelClass: ['snack-error'],
      });
      return throwError(() => error);
    }),
  );
};

function resolveMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique su conexión.';
  }
  // El backend siempre devuelve { message: string } en errores controlados
  const backendMessage: string | undefined = error.error?.message;
  if (backendMessage) return backendMessage;

  switch (error.status) {
    case 400: return 'Los datos enviados no son válidos.';
    case 404: return 'El recurso solicitado no existe.';
    case 409: return 'La operación no está permitida en el estado actual.';
    case 500: return 'Error interno del servidor. Intente más tarde.';
    default:  return 'Error inesperado. Intente de nuevo.';
  }
}
```

**Registro en `app.config.ts`:**

```typescript
provideHttpClient(withInterceptors([errorInterceptor]))
```

**Tabla de comportamiento por código HTTP:**

| Código | Causa típica | Mensaje al usuario |
|--------|--------------|--------------------|
| `0` | Sin conexión / CORS | "No se pudo conectar con el servidor." |
| `400` | Validación backend | Mensaje específico del backend |
| `404` | Tarea no encontrada | "Task with id N was not found" |
| `409` | Transición inválida | "Cannot change status from FINISHED..." |
| `500` | Error interno | "Error interno del servidor." |

---

### 1.4 Capa 4 — GlobalErrorHandler

Captura errores JavaScript no tratados (excepciones en código síncrono, errores de referencia
nula, etc.) que Angular no gestiona automáticamente.

**Archivo:** `src/app/core/handlers/global-error-handler.ts`

```typescript
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly snackBar = inject(MatSnackBar);

  handleError(error: unknown): void {
    // En desarrollo: mostrar en consola con detalle
    console.error('[GlobalErrorHandler]', error);
    // En producción: notificar sin exponer detalles técnicos
    this.snackBar.open(
      'Ocurrió un error inesperado en la aplicación.',
      'Cerrar',
      { duration: 5000, panelClass: ['snack-error'] }
    );
  }
}
```

**Registro en `app.config.ts`:**

```typescript
{ provide: ErrorHandler, useClass: GlobalErrorHandler }
```

**Cuándo aplica:**
- Null pointer exceptions en templates
- Errores en pipes o directivas
- Errores en callbacks fuera de Zones
- Promesas rechazadas no manejadas

---

### 1.5 Estilo visual del snackbar de error

**`styles.scss` global:**

```scss
.snack-error .mdc-snackbar__surface {
  background-color: var(--mat-sys-error) !important;
  color: var(--mat-sys-on-error) !important;
}
```

---

## 2. Componentes reutilizables — Separación de responsabilidades

### 2.1 Principio Smart vs Dumb (Container vs Presentational)

| Categoría | Conoce servicios | Maneja estado | Emite eventos | Ejemplos |
|-----------|-----------------|--------------|--------------|---------|
| **Smart** (container) | Sí | Sí | No necesariamente | `TaskList`, `TaskFormDialog`, `StatusChangeDialog` |
| **Dumb** (presentational) | No | No | Sí (outputs) | `TaskCard`, `TaskStatusBadge`, `TaskFilterBar`, `TaskItemForm`, `EmptyState`, `LoadingSpinner`, `ConfirmDialog` |

**Regla:** los componentes dumb solo se comunican mediante `input()` y `output()`. No inyectan
servicios. Son completamente predecibles y fáciles de testear de forma aislada.

---

### 2.2 Árbol de componentes

```
App (shell)
└── TaskList (smart — orquesta todo)
    ├── TaskFilterBar (dumb — emite cambios de filtro)
    ├── LoadingSpinner (dumb — input: visible)
    ├── EmptyState (dumb — input: message)
    ├── TaskCard x N (dumb — input: task, outputs: edit/delete/statusChange)
    │   └── TaskStatusBadge (dumb — input: status)
    ├── TaskFormDialog (smart — abre modal, llama servicio)
    │   └── TaskItemForm (dumb — input: FormArray, maneja ítems)
    ├── StatusChangeDialog (smart — llama servicio de estado)
    └── ConfirmDialog (dumb — genérico, input: title/message)
```

---

### 2.3 Componentes shared reutilizables

#### `ConfirmDialog`

Dialog de confirmación configurable para cualquier contexto.

**Ubicación:** `src/app/shared/components/confirm-dialog/`

**Data inyectada:**

```typescript
export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;  // default: 'Confirmar'
  confirmColor?: 'primary' | 'warn' | 'accent';  // default: 'warn'
}
```

**Uso:**

```typescript
// Para borrar una tarea
this.dialog.open(ConfirmDialog, {
  data: {
    title: 'Eliminar tarea',
    message: `¿Eliminar "${task.title}"? Esta acción no se puede deshacer.`,
    confirmLabel: 'Eliminar',
    confirmColor: 'warn',
  }
});

// Para cancelar una tarea (distinto contexto, mismo componente)
this.dialog.open(ConfirmDialog, {
  data: {
    title: 'Cancelar tarea',
    message: '¿Estás seguro de que deseas cancelar esta tarea?',
    confirmLabel: 'Sí, cancelar',
    confirmColor: 'accent',
  }
});
```

---

#### `LoadingSpinner`

Overlay de carga centrado reutilizable.

**Ubicación:** `src/app/shared/components/loading-spinner/`

```typescript
@Component({
  selector: 'app-loading-spinner',
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="spinner-overlay">
      <mat-spinner [diameter]="diameter()" />
    </div>
  `,
  styles: [`
    .spinner-overlay {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 40px 0;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingSpinner {
  readonly diameter = input(48);
}
```

**Uso:**

```html
@if (loading()) {
  <app-loading-spinner />
}
```

---

#### `EmptyState`

Mensaje de lista vacía configurable.

**Ubicación:** `src/app/shared/components/empty-state/`

```typescript
@Component({
  selector: 'app-empty-state',
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon>{{ icon() }}</mat-icon>
      <p>{{ message() }}</p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly message = input('No se encontraron resultados.');
  readonly icon    = input('inbox');
}
```

**Uso:**

```html
@if (!loading() && tasks().length === 0) {
  <app-empty-state message="No hay tareas que coincidan con los filtros aplicados." />
}
```

---

#### `TaskFilterBar`

Barra de filtros independiente. Emite un objeto `TaskFilter` cada vez que cambia algún control.

**Ubicación:** `src/app/features/tasks/components/task-filter-bar/`

**Outputs:**

```typescript
readonly filterChange = output<TaskFilter>();
```

**Controles internos:**

```typescript
searchControl  = new FormControl('');
statusControl  = new FormControl<TaskStatus | null>(null);
pendingOnly    = signal(false);
dueNowOnly     = signal(false);
```

**Lógica de emisión:**

```typescript
// Con debounce para búsqueda
searchControl.valueChanges.pipe(
  debounceTime(400),
  distinctUntilChanged(),
  takeUntilDestroyed(),
).subscribe(() => this.emitFilter());

// Inmediato para estado y toggles
statusControl.valueChanges.pipe(takeUntilDestroyed())
  .subscribe(() => this.emitFilter());
```

**Ventaja:** `TaskList` no contiene lógica de filtros. Solo recibe el `TaskFilter` emitido y
llama `loadTasks()`.

---

#### `TaskItemForm`

Maneja el `FormArray` de ítems de forma aislada.

**Ubicación:** `src/app/features/tasks/components/task-item-form/`

**Input:**

```typescript
readonly itemsArray = input.required<FormArray>();
```

**Responsabilidades:**
- Renderizar cada control del FormArray
- Botón "+ Agregar ítem"
- Botón "✕" para eliminar un ítem
- Mensajes de validación por ítem

**Beneficio:** `TaskFormDialog` queda limpio. Solo construye el form y delega la UI de ítems.

---

## 3. Diseño responsive

### 3.1 Breakpoints definidos

Se usan los breakpoints del Angular CDK:

| Breakpoint | Rango | Nombre CDK |
|------------|-------|-----------|
| Mobile | `< 600px` | `Breakpoints.XSmall` |
| Tablet | `600px – 959px` | `Breakpoints.Small` |
| Desktop | `≥ 960px` | `Breakpoints.Medium` / `Large` |

### 3.2 Signal de breakpoint en `App`

Se expone un signal global de `isMobile` que los componentes pueden inyectar o heredar via
`input()`:

```typescript
// app.ts
readonly isMobile = toSignal(
  inject(BreakpointObserver)
    .observe(Breakpoints.XSmall)
    .pipe(map(r => r.matches)),
  { initialValue: false }
);
```

Se pasa a los componentes que necesiten adaptar su layout:

```html
<!-- app.html -->
<app-task-list [isMobile]="isMobile()" />
```

### 3.3 Adaptaciones por componente

#### `TaskFilterBar` — móvil

En móvil, los filtros se colapsan bajo un botón "Filtros ▾":

```
Desktop:
[🔍 Buscar...]  [Estado ▼]  [Pendientes]  [Vencidas]  [+ Nueva tarea]

Móvil:
[🔍 Buscar...]  [Filtros ▾]  [+ Nueva]
    ↓ (expandido)
    [Estado ▼]
    [Pendientes]  [Vencidas]
```

Implementación: `filtersExpanded = signal(false)` + `@if (isMobile() ? filtersExpanded() : true)`.

#### `TaskFormDialog` — móvil

```typescript
// TaskList al abrir el dialog
openDialog(task?: Task): void {
  this.dialog.open(TaskFormDialog, {
    width: this.isMobile() ? '100vw' : '600px',
    maxWidth: '100vw',
    maxHeight: '100vh',
    data: { mode: task ? 'edit' : 'create', task },
  });
}
```

En móvil el dialog ocupa toda la pantalla (`100vw / 100vh`).

#### `TaskCard` — móvil

```
Desktop:
┌────────────────────────────────────────────────────────┐
│ [⚠ Vencida]  Título de la tarea        [PROGRAMMED] [⋮]│
│ 📅 22/04/2026 10:00 · Descripción breve                │
│ ████████░░░  2 / 3 ítems completados                   │
└────────────────────────────────────────────────────────┘

Móvil:
┌──────────────────────────────┐
│ [⚠ Vencida]      [PROGR.][⋮]│
│ Título de la tarea           │
│ 📅 22/04/2026                │
│ ██████░░  2/3 ítems          │
└──────────────────────────────┘
```

La descripción se oculta en móvil (`@if (!isMobile())`) para ahorrar espacio.

#### `MatPaginator` — móvil

```typescript
@ViewChild(MatPaginator) paginator!: MatPaginator;

// En ngAfterViewInit o con signal
// Ocultar selector de tamaño en móvil
```

```html
<mat-paginator
  [hidePageSize]="isMobile()"
  [pageSizeOptions]="[5, 10, 20]"
  [pageSize]="pageSize()"
  [length]="totalElements()"
  (page)="onPageChange($event)" />
```

### 3.4 CSS responsive

Estrategia CSS-first con variables y media queries:

```scss
// styles.scss — variables globales
:root {
  --app-padding: 24px;
  --card-gap: 12px;
}

@media (max-width: 599px) {
  :root {
    --app-padding: 12px;
    --card-gap: 8px;
  }
  main {
    padding: var(--app-padding);
  }
}
```

Regla: **no** se usan `px` fijos en componentes. Se usan las variables globales y los tokens
de Angular Material M3 (`--mat-sys-*`).

---

## 4. Optimización en la carga de recursos y componentes

### 4.1 Change Detection: `OnPush` en todos los componentes dumb

Todos los componentes presentacionales usan `ChangeDetectionStrategy.OnPush`. Angular solo
re-renderiza el componente cuando:
- Un `input()` cambia (por referencia)
- Un `signal()` interno cambia
- Se dispara manualmente con `ChangeDetectorRef.markForCheck()`

```typescript
@Component({
  // ...
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCard {
  readonly task = input.required<Task>();
}
```

**Componentes que usan OnPush:**
- `TaskCard`
- `TaskStatusBadge`
- `TaskFilterBar`
- `TaskItemForm`
- `EmptyState`
- `LoadingSpinner`
- `ConfirmDialog`

**Componentes smart** (`TaskList`, `TaskFormDialog`, `StatusChangeDialog`) también usan
`OnPush` + signals. Angular 21 detecta cambios de signals automáticamente dentro de `OnPush`.

---

### 4.2 Lazy loading de dialogs

Los dialogs se importan de forma dinámica (`import()`) para que no formen parte del bundle
inicial. El código del dialog solo se descarga cuando el usuario lo abre por primera vez.

```typescript
// task-list.ts
async openCreateDialog(): Promise<void> {
  const { TaskFormDialog } = await import(
    '../task-form-dialog/task-form-dialog'
  );
  const ref = this.dialog.open(TaskFormDialog, {
    width: this.isMobile() ? '100vw' : '600px',
    data: { mode: 'create' },
  });
  ref.afterClosed().subscribe(result => {
    if (result) this.loadTasks();
  });
}
```

**Efecto:** el bundle inicial (~50KB menos) carga más rápido. Los dialogs se descargan on-demand.

---

### 4.3 `switchMap` para cancelar peticiones de búsqueda

Cuando el usuario escribe en el buscador y cambia el texto rápidamente, se cancela la petición
anterior antes de enviar la nueva. Evita condiciones de carrera en los resultados.

```typescript
// task-list.ts
ngOnInit(): void {
  this.filterBar.filterChange.pipe(
    switchMap(filter => {
      this.loading.set(true);
      this.currentPage.set(0);
      return this.taskService.findAll({ ...filter, page: 0, size: this.pageSize() }).pipe(
        catchError(() => of(null))   // absorber error ya manejado por interceptor
      );
    }),
    takeUntilDestroyed(this.destroyRef),
  ).subscribe(page => {
    if (page) {
      this.tasks.set(page.content);
      this.totalElements.set(page.totalElements);
    }
    this.loading.set(false);
  });
}
```

**Por qué `switchMap` y no `mergeMap`:** `switchMap` cancela la suscripción anterior
(HttpClient cancela el request HTTP con `AbortController`) cuando llega un nuevo valor.
`mergeMap` dejaría correr todas las peticiones en paralelo, causando race conditions.

---

### 4.4 `takeUntilDestroyed` — gestión de suscripciones

Angular 16+ provee `takeUntilDestroyed()` que desuscribe automáticamente cuando el componente
se destruye. Evita fugas de memoria sin necesidad de `Subject` + `ngOnDestroy`.

```typescript
// En componentes Angular 21
private readonly destroyRef = inject(DestroyRef);

ngOnInit(): void {
  someObservable$.pipe(
    takeUntilDestroyed(this.destroyRef),
  ).subscribe(...);
}
```

**Regla:** toda suscripción en un componente debe tener `takeUntilDestroyed`.

---

### 4.5 `@for` con `track` — renderizado eficiente de listas

Angular 21 usa el nuevo control flow syntax. El `track` es obligatorio y permite a Angular
reutilizar nodos DOM existentes en lugar de destruirlos y recrearlos.

```html
<!-- task-list.html -->
@for (task of tasks(); track task.id) {
  <app-task-card
    [task]="task"
    (edit)="openEditDialog(task)"
    (delete)="openDeleteDialog(task)"
    (statusChange)="openStatusDialog(task)" />
}
```

**Sin `track`:** Angular destruye y recrea todos los elementos al cambiar la lista.  
**Con `track task.id`:** Angular solo actualiza los elementos que cambiaron.

---

### 4.6 `provideAnimationsAsync()` — animaciones diferidas

Angular Material con `provideAnimationsAsync()` (ya en el plan) carga el módulo de animaciones
de forma asíncrona, fuera del bundle principal.

```typescript
// app.config.ts
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

providers: [
  provideAnimationsAsync(),  // asíncrono, no bloquea la carga inicial
]
```

---

### 4.7 Tree-shaking de Angular Material

Al importar solo los módulos necesarios por componente (ya definido en la tabla de
`estructura-proyecto-frontend.md`), el compilador de Angular elimina del bundle todos los
componentes Material no usados.

**Ejemplo:** `TaskStatusBadge` solo importa lo que necesita:

```typescript
@Component({
  imports: [NgClass],   // ← solo NgClass, nada más de Material
  // ...
})
```

**Evitar:**

```typescript
// ❌ importar módulos barrel → incluye TODO Material en el bundle
import { MaterialModule } from './material.module';
```

---

### 4.8 Resumen de técnicas de optimización aplicadas

| Técnica | Componentes | Impacto |
|---------|-------------|---------|
| `OnPush` en todos los dumb | TaskCard, Badge, FilterBar, Spinner, EmptyState | Elimina re-renders innecesarios |
| Lazy loading de dialogs | TaskFormDialog, StatusChangeDialog, ConfirmDialog | Reduce bundle inicial |
| `switchMap` + debounce | TaskList (búsqueda) | Cancela requests duplicados |
| `takeUntilDestroyed` | Todos los componentes con suscripciones | Previene memory leaks |
| `@for track task.id` | TaskList | Reutiliza nodos DOM |
| `provideAnimationsAsync` | App global | Carga animaciones asíncrona |
| Import granular de Material | Todos los componentes | Bundle mínimo |
| Señales (signals) para estado | TaskList, dialogs | Re-renders granulares y precisos |

