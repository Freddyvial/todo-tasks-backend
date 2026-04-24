# Prompts utilizados con IA — TODO Tasks

## Herramienta utilizada

**Claude** (Anthropic) a través de Claude Code CLI, con modelo Claude Sonnet 4.6.

## Metodología de trabajo

El desarrollo se realizó con un enfoque de **especificación primero**. Antes de escribir cualquier
línea de código, se le solicitó a la IA que documentara la arquitectura, los contratos y las
decisiones técnicas en archivos `.plan/`. Solo después de tener esa base acordada se procedió
a la implementación.

Si durante la implementación era necesario modificar algo del diseño original, primero se
actualizaba la especificación en `.plan/` y luego se implementaba el cambio.

---

## Fase 1 — Planificación y arquitectura del backend

### Prompt inicial — plan de trabajo backend

```
Tengo una prueba técnica para construir una aplicación TODO Tasks con Java + Spring Boot
para el backend y Angular para el frontend.

Te voy a dar el documento de la prueba. Necesito que antes de escribir código, me ayudes
a crear un plan de trabajo completo para el backend. El plan debe cubrir:
- Arquitectura propuesta (capas, componentes)
- Modelo de dominio (entidades, estados)
- Reglas de negocio iniciales
- Persistencia
- Estrategia de pruebas con objetivo de cobertura del 80%
- Decisiones técnicas (versión de Java, Spring Boot, H2)
- Plan de ejecución por fases
- Criterios de terminado

Guarda todo en .plan/plan-trabajo-backend.md
```

**Resultado:** `.plan/plan-trabajo-backend.md`

---

### Prompt — contrato de la API REST

```
Con el plan de backend acordado, ahora necesito definir el contrato REST completo antes
de implementar. Documenta en .plan/contrato-api-backend.md:
- Todos los endpoints con sus URLs, métodos HTTP y descripciones
- Ejemplo de request y response para cada endpoint (JSON)
- Todos los DTOs propuestos: TaskCreateRequest, TaskUpdateRequest, TaskStatusUpdateRequest,
  TaskResponse, TaskItemResponse, PageResponse
- Reglas de validación por campo
- Reglas derivadas en la respuesta: pendingExecution, dueNowAlert, completedItems, totalItems
- Estrategia de manejo de errores con códigos HTTP y estructura uniforme
- Decisiones de diseño del contrato
```

**Resultado:** `.plan/contrato-api-backend.md`

---

### Prompt — estructura del proyecto y pom.xml

```
Ahora necesito definir la estructura exacta del proyecto Spring Boot antes de empezar
a escribir código. Documenta en .plan/estructura-proyecto-backend.md:
- Árbol de archivos completo del proyecto
- Responsabilidad de cada paquete (controller, service, repository, dto, mapper, exception)
- Modelo técnico inicial de las entidades Task y TaskItem con sus campos
- Contenido objetivo del pom.xml con todas las dependencias y plugins (Spring Boot 3.5.x,
  Java 21, JaCoCo, H2)
- Configuración inicial del application.yml
- Orden de construcción recomendado
```

**Resultado:** `.plan/estructura-proyecto-backend.md`

---

### Prompt — estrategia de manejo de errores y trazabilidad

```
Antes de implementar el backend necesito definir la estrategia de manejo de errores
y logging orientado a excepciones. Documenta en .plan/manejo-errores-trazabilidad-backend.md:
- Enfoque general (orientado a excepciones, no a flujo feliz)
- Rol de try/catch por capa (controller, service, repository, GlobalExceptionHandler)
- Cuándo usar y cuándo no usar try/catch
- Clasificación de excepciones por tipo (funcionales, validación, técnicas inesperadas)
- Qué debe incluir un log de excepción
- Niveles de logging (WARN, ERROR) y cuándo usar cada uno
- Contrato API de error uniforme
- Reglas para mensajes al cliente
- Datos que no deben registrarse
- Criterios de aceptación de la estrategia
```

**Resultado:** `.plan/manejo-errores-trazabilidad-backend.md`

---

## Fase 2 — Implementación del backend

### Prompt — implementación completa del backend

```
Con todos los planes acordados (plan-trabajo-backend.md, contrato-api-backend.md,
estructura-proyecto-backend.md, manejo-errores-trazabilidad-backend.md), implementa
el backend completo en el directorio todo-tasks-backend/.

Sigue exactamente la arquitectura y contratos definidos en los archivos .plan/.
El orden de implementación debe seguir las fases del plan de trabajo.
```

**Componentes implementados:**
- `Task.java`, `TaskItem.java`, `TaskStatus.java` (entidades JPA)
- `TaskCreateRequest`, `TaskUpdateRequest`, `TaskStatusUpdateRequest`, `TaskItemRequest` (DTOs request)
- `TaskResponse`, `TaskItemResponse`, `PageResponse`, `ApiErrorResponse` (DTOs response)
- `TaskMapper.java` (mapeo manual entidad ↔ DTO)
- `TaskRepository.java` (Spring Data JPA con consultas derivadas)
- `TaskService.java` (lógica de negocio, filtros, alertas, cambio de estado)
- `TaskController.java` (endpoints REST)
- `TaskNotFoundException`, `InvalidTaskStateException`, `InvalidSortParameterException` (excepciones)
- `GlobalExceptionHandler.java` (manejo centralizado de errores)
- `WebConfig.java` (CORS para Angular en `http://localhost:4200`)
- `application.yml` (configuración H2, puerto 8080)
- `data.sql` (datos de ejemplo para validación manual)

---

### Prompt — suite de pruebas del backend

```
Implementa la suite de pruebas del backend según la estrategia definida en
plan-trabajo-backend.md. El objetivo es cobertura mínima del 80%.

Cubre los escenarios definidos en el plan para:
- TaskServiceTest: pruebas unitarias de reglas de negocio
- TaskControllerTest: pruebas web con MockMvc para contratos HTTP
- TaskMapperTest: pruebas unitarias del mapper y campos derivados
- GlobalExceptionHandlerTest: validación de respuestas de error
- TaskServiceIntegrationTest: pruebas de integración con H2 real

Configura JaCoCo en el pom.xml con umbral mínimo del 80%.
```

**Resultado:** 5 clases de prueba con cobertura mayor al 80%.

---

## Fase 3 — Planificación y arquitectura del frontend

### Prompt — plan de trabajo frontend

```
El backend ya está terminado. Ahora necesito planificar el frontend Angular.
Documenta en .plan/plan-trabajo-frontend.md la arquitectura completa:
- Stack tecnológico (Angular 21, Angular Material, Signals, RxJS)
- Decisiones técnicas: standalone components, estado local con Signals, una sola ruta
- Estructura de paquetes objetivo con descripción de cada carpeta
- Descripción de cada componente: cuáles son smart, cuáles son dumb (OnPush)
- Estrategia de pruebas con Jasmine/Karma
- Configuración CORS en el backend
- Plan Docker (Dockerfile backend, Dockerfile frontend, docker-compose.yml)
- Plan de ejecución por fases
- Criterios de terminado del frontend
```

**Resultado:** `.plan/plan-trabajo-frontend.md`

---

### Prompt — contrato de consumo de API en el frontend

```
Documenta en .plan/contrato-api-frontend.md exactamente cómo el frontend Angular
va a consumir la API del backend:
- Configuración de conexión (base URL, environments)
- Para cada endpoint: petición exacta enviada desde Angular, conversión de datos,
  respuesta esperada y cómo se usa en la UI
- Tabla de campos calculados del backend y su uso en la interfaz
- Manejo de errores HTTP por código
- Implementación de buildHttpParams (cómo se construyen los query params)
- Configuración CORS del backend
```

**Resultado:** `.plan/contrato-api-frontend.md`

---

### Prompt — estructura de componentes y diseño de la UI

```
Documenta en .plan/estructura-proyecto-frontend.md la estructura completa del proyecto:
- Árbol de archivos completo
- Contenido de app.config.ts con todos los providers
- app.routes.ts
- Todos los modelos TypeScript con sus interfaces completas
- Diseño del TaskService con los 6 métodos y su firma
- Diseño detallado de cada componente con sus inputs, outputs, estado interno y template
- Tabla de importaciones de Angular Material por componente
```

**Resultado:** `.plan/estructura-proyecto-frontend.md`

---

### Prompt — diseño de calidad, errores y optimización

```
Documenta en .plan/diseno-calidad-frontend.md la estrategia de calidad del frontend:
- Arquitectura de manejo de errores en 4 capas (validators, componente, interceptor, GlobalErrorHandler)
- Código completo de cada capa con ejemplos
- Separación Smart vs Dumb (árbol de componentes)
- Diseño responsive con breakpoints del CDK
- Optimizaciones: OnPush, lazy loading de dialogs, switchMap para cancelar requests,
  takeUntilDestroyed, @for con track, provideAnimationsAsync, tree-shaking
```

**Resultado:** `.plan/diseno-calidad-frontend.md`

---

### Prompt — estrategia de pruebas y configuración Docker

```
Documenta en .plan/pruebas-docker-frontend.md:
- Estrategia de pruebas con Jasmine/Karma para el frontend
- Suites y escenarios concretos para TaskService, TaskStatusBadge, TaskFormDialog y TaskList
- Configuración completa de Docker:
  * Dockerfile multi-stage del backend (Maven + JRE alpine)
  * Dockerfile multi-stage del frontend (Node + nginx)
  * nginx.conf con proxy_pass a backend y soporte SPA
  * docker-compose.yml con ambos servicios en red compartida
  * Variables de entorno por perfil (desarrollo vs Docker)
```

**Resultado:** `.plan/pruebas-docker-frontend.md`

---

## Fase 4 — Implementación del frontend

### Prompt — implementación del frontend Angular

```
Con todos los planes acordados, implementa el frontend completo en todo-tasks.frontend/.
Sigue exactamente la estructura y contratos definidos en los archivos .plan/.
El frontend debe implementar todas las funcionalidades requeridas por la prueba técnica.
```

**Componentes y archivos implementados según los planes:**
- `app.config.ts`, `app.routes.ts`, `app.ts`, `app.html` (shell)
- `task.service.ts` (6 métodos HTTP)
- `error.interceptor.ts` y `global-error-handler.ts` (manejo de errores en capas)
- Modelos TypeScript: `task.model.ts`, `page-response.model.ts`, `task-filter.model.ts`
- `task-list` (smart — vista principal con Signals y paginación)
- `task-filter-bar` (dumb — filtros con debounce)
- `task-card` (dumb — tarjeta OnPush con indicadores)
- `task-status-badge` (dumb — chip de estado)
- `task-form-dialog` (smart — formulario reactivo crear/editar)
- `task-item-form` (dumb — FormArray de ítems)
- `status-change-dialog` (smart — cambio de estado con transiciones válidas)
- `task-items-quick-dialog` (gestión rápida de ítems desde la lista)
- `confirm-dialog`, `loading-spinner`, `empty-state` (componentes shared)
- `elapsed-time.pipe.ts` (pipe para mostrar tiempo transcurrido)
- `environments/` (development vs producción)
- `Dockerfile`, `nginx.conf` (Docker del frontend)

---

## Fase 5 — Docker y configuración final

### Prompt — Dockerfiles y docker-compose

```
Implementa la configuración Docker según lo definido en .plan/pruebas-docker-frontend.md:
- Dockerfile para el backend (multi-stage con Maven y JRE alpine)
- Dockerfile para el frontend (multi-stage con Node y nginx)
- nginx.conf para SPA con proxy_pass al backend
- docker-compose.yml en la raíz del proyecto
```

**Resultado:** `todo-tasks-backend/Dockerfile`, `todo-tasks.frontend/Dockerfile`,
`todo-tasks.frontend/nginx.conf`, `docker-compose.yml`

---

## Resumen del proceso

| Fase | Artefactos producidos | Metodología |
|------|-----------------------|-------------|
| Planificación backend | 4 archivos `.plan/` | Spec first — acordar antes de implementar |
| Implementación backend | ~20 clases Java | IA genera código según las specs |
| Planificación frontend | 5 archivos `.plan/` | Spec first — extender el plan existente |
| Implementación frontend | ~25 archivos TypeScript | IA genera código según las specs |
| Docker | 4 archivos de configuración | Según plan ya documentado |

> El evaluador puede abrir cualquier archivo `.plan/` para ver la especificación que precedió
> a cada parte del código. La carpeta `.plan/` es la trazabilidad del proceso de colaboración
> con la IA.
