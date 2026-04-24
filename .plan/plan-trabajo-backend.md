# Plan de trabajo y arquitectura backend

## 1. Contexto

La prueba solicita construir una aplicación tipo TODO Tasks con `Java + Spring Boot` para el backend y `Angular` para el frontend. Antes de implementar, este documento define la arquitectura objetivo del backend, las decisiones técnicas iniciales, el alcance funcional y el plan de ejecución.

## 2. Objetivo del backend

Exponer una API REST que permita:

- Crear tareas.
- Consultar tareas por id.
- Listar tareas de forma paginada.
- Buscar tareas por texto y estado.
- Editar tareas y sus ítems checkeables.
- Eliminar tareas.
- Gestionar el estado de una tarea.
- Identificar tareas pendientes por ejecutar.
- Alertar tareas programadas cuya fecha de ejecución ya llegó.

## 3. Arquitectura propuesta

### 3.1 Estilo arquitectónico

Se propone una arquitectura en capas, simple y mantenible, adecuada para una prueba técnica:

- `Controller`: expone endpoints REST y valida la entrada.
- `Service`: contiene reglas de negocio y orquestación.
- `Repository`: acceso a datos mediante Spring Data JPA.
- `Domain/Entity`: modelo persistente de tareas e ítems.
- `DTO/Mapper`: contrato de entrada/salida para no exponer directamente las entidades.

Esta estructura es suficiente para un alcance pequeño, permite crecer de forma ordenada y facilita pruebas unitarias e integración.

### 3.2 Componentes principales

- `TaskController`
  - CRUD de tareas.
  - Búsqueda y paginación.
  - Filtros de pendientes y alertas.
- `TaskService`
  - Crea y actualiza tareas.
  - Gestiona transición de estados.
  - Calcula indicadores derivados como alerta por fecha vencida.
- `TaskRepository`
  - Consultas paginadas y filtradas.
- `Task` y `TaskItem`
  - Modelo principal y subtareas checkeables.

### 3.3 Modelo de dominio preliminar

#### Entidad `Task`

- `id`
- `title`
- `description`
- `executionDate`
- `status`
- `items`
- `createdAt`
- `updatedAt`

#### Entidad `TaskItem`

- `id`
- `taskId`
- `description`
- `completed`
- `position`

#### Enum `TaskStatus`

- `PROGRAMMED`
- `IN_PROGRESS`
- `FINISHED`
- `CANCELLED`

### 3.4 Reglas de negocio iniciales

- Una tarea debe tener título, fecha de ejecución y al menos un estado válido.
- El estado por defecto al crear será `PROGRAMMED`.
- Una tarea en estado `PROGRAMMED` cuya `executionDate` sea menor o igual al momento actual deberá marcarse con alerta visual en la respuesta.
- Las tareas pendientes por ejecutar se interpretarán inicialmente como aquellas en estado `PROGRAMMED`.
- Los ítems checkeables podrán crearse, editarse, eliminarse y marcarse como completados.
- La eliminación de una tarea será física para simplificar la solución, salvo que durante la implementación aparezca una razón clara para usar borrado lógico.

### 3.5 Persistencia

Para la prueba se propone:

- Base de datos `H2` en archivo o memoria para facilitar ejecución local.
- Spring Data JPA para persistencia.
- Inicialización simple y reproducible.

Ventajas:

- No requiere infraestructura externa.
- Permite validar funcionalidad rápidamente.
- Hace simple la revisión por parte del evaluador.

### 3.6 API REST preliminar

#### Endpoints base

- `POST /api/tasks`
- `GET /api/tasks/{id}`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `GET /api/tasks`

#### Filtros en listado

- `page`
- `size`
- `query`
- `status`
- `pendingOnly`
- `dueNowOnly`

#### Contratos esperados

##### Crear/editar tarea

- `title`
- `description`
- `executionDate`
- `status`
- `items[]`
  - `id` opcional para actualización
  - `description`
  - `completed`
  - `position`

##### Respuesta de tarea

- Datos base de la tarea
- Lista de ítems
- `dueNowAlert`
- `pendingExecution`
- `completedItems`
- `totalItems`

### 3.7 Manejo de errores

Se implementará una estrategia homogénea con:

- `400 Bad Request` para validaciones.
- `404 Not Found` para recursos inexistentes.
- `409 Conflict` si se requiere controlar alguna transición inválida.
- Respuesta estándar con mensaje claro y campo de error.

### 3.8 Validaciones

- `title` obligatorio y con longitud mínima.
- `executionDate` obligatoria.
- `status` obligatorio.
- Descripciones de ítems no vacías.
- No permitir listas nulas si se envían ítems.

### 3.9 Observabilidad mínima

Para esta prueba bastará con:

- Logs básicos de arranque y errores.
- Mensajes claros en respuestas de validación.

## 4. Estructura de paquetes sugerida

```text
com.imaginamos.todo
├── config
├── controller
├── dto
├── entity
├── exception
├── mapper
├── repository
├── service
└── TodoTasksApplication
```

## 5. Decisiones técnicas iniciales

### 5.1 Framework

- `Spring Boot 3.5.x`
- `Spring Web`
- `Spring Data JPA`
- `Validation`
- `H2 Database`

### 5.2 Compatibilidad de Java

Se define `Java 21 LTS` como versión objetivo del proyecto.

Decisión tomada:

- `Java 21` será la versión mínima y objetivo de compilación.
- Se usará una línea estable de `Spring Boot 3.5.x`, compatible con Java 21 y adecuada para una prueba técnica moderna.
- El proyecto deberá compilar con `maven.compiler.release=21` o configuración equivalente.

Justificación:

- `Java 21` ofrece estabilidad, soporte LTS y buena compatibilidad con librerías actuales.
- Evita quedarse en una versión antigua como Java 11.
- Reduce el riesgo de incompatibilidades respecto a usar una combinación demasiado nueva para una prueba de tiempo limitado.

### 5.3 Estrategia de mapeo

Mapeo manual entre entidades y DTOs para mantener control y evitar sobreingeniería.

### 5.4 Estrategia de pruebas

Se define una estrategia de pruebas con objetivo mínimo de cobertura del `80%`.

Alcance mínimo:

- Pruebas unitarias sobre `TaskService` para reglas de negocio.
- Pruebas unitarias sobre `TaskMapper` para validar campos derivados.
- Pruebas web sobre `TaskController` para contratos HTTP, validaciones y códigos de respuesta.
- Validación del manejo de excepciones expuesto por `GlobalExceptionHandler`.

Objetivo de cobertura:

- Cobertura mínima global de líneas: `80%`.
- Cobertura mínima global de ramas: `80%` si la herramienta lo permite de forma simple.
- La prioridad será cubrir la capa `service`, `controller`, `mapper` y `exception`.

Escenarios mínimos a cubrir:

- Crear tarea con estado por defecto.
- Crear tarea con ítems checkeables.
- Consultar tarea existente.
- Error al consultar tarea inexistente.
- Actualizar tarea completa.
- Eliminar tarea.
- Buscar tareas por texto.
- Filtrar por estado.
- Filtrar tareas pendientes.
- Filtrar tareas cuya fecha ya llegó.
- Cambiar estado de tarea.
- Rechazar cambios de estado inválidos si la regla queda activa.
- Calcular `pendingExecution`.
- Calcular `dueNowAlert`.
- Validar requests inválidos con respuesta `400`.

Herramienta propuesta:

- `JaCoCo` integrado en Maven para generar reporte y validar la cobertura.

Criterio de aceptación:

- No se considerará terminado el backend si el reporte de cobertura queda por debajo de `80%`.

## 6. Riesgos y decisiones abiertas

### 6.1 Configuración local del entorno

Riesgo:

- El equipo local puede seguir apuntando a Java 11 aunque ya exista Java 21 instalado.

Acción:

- Ajustar `JAVA_HOME` y `PATH` para que Maven y el IDE usen Java 21.
- Verificar con `java -version` y `mvn -version` antes de iniciar la implementación.

### 6.2 Semántica de “pendientes por ejecutar”

Supuesto actual:

- Se consideran pendientes las tareas en `PROGRAMMED`.

Posible ajuste:

- Incluir también tareas vencidas que sigan programadas, diferenciándolas con la alerta.

### 6.3 Tipo de fecha

Decisión preliminar:

- Usar `LocalDateTime` para poder alertar con precisión horaria.

## 7. Plan de ejecución

### Fase 1. Preparación técnica

- Verificar que `JAVA_HOME` apunte a `Java 21`.
- Verificar versión de Java y herramientas.
- Crear proyecto Spring Boot.
- Definir dependencias base.
- Configurar persistencia H2 y propiedades iniciales.

### Fase 2. Modelo de dominio

- Crear entidades `Task` y `TaskItem`.
- Crear enum `TaskStatus`.
- Definir relaciones y restricciones.

### Fase 3. Contratos y reglas

- Diseñar DTOs de request y response.
- Implementar mapeadores.
- Implementar validaciones.

### Fase 4. Lógica de negocio

- Implementar servicio de creación.
- Implementar edición completa de tarea e ítems.
- Implementar borrado.
- Implementar búsqueda, filtros y paginación.
- Implementar cálculo de alertas y pendientes.

### Fase 5. API REST

- Exponer endpoints.
- Manejar excepciones.
- Documentar respuestas esperadas.

### Fase 6. Validación

- Configurar `JaCoCo` en Maven.
- Crear pruebas unitarias y web según la estrategia definida.
- Probar flujo CRUD completo.
- Probar filtros y alerta de vencimiento.
- Verificar que la cobertura mínima sea de `80%`.

### Fase 7. Documentación

- Actualizar README del backend.
- Documentar decisiones finales.
- Registrar prompts y evidencia de uso de IA en `.plan`.

## 8. Criterios de terminado para backend

El backend se considerará listo cuando:

- La API permita cubrir todas las historias funcionales del backend.
- Existan endpoints de CRUD, búsqueda y paginación.
- Las tareas devuelvan indicadores de pendientes y alerta por fecha.
- Exista una suite de pruebas automatizadas con cobertura mínima del `80%`.
- La solución arranque localmente con instrucciones claras.
- Haya documentación suficiente para revisión técnica.

## 9. Próximo paso propuesto

Con `Java 21` ya definido, el siguiente paso es documentar el contrato de la API REST, la estructura de paquetes definitiva y el `pom.xml` objetivo antes de comenzar la implementación.
