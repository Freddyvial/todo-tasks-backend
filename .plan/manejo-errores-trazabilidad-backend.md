# Definicion de manejo de errores y logging orientado a excepciones

## 1. Objetivo

Definir una estrategia consistente para:

- responder errores HTTP de forma clara y segura
- garantizar trazabilidad tecnica cuando ocurra una excepcion
- registrar errores con suficiente contexto funcional y tecnico
- evitar exposicion de detalles internos al cliente

Esta definicion aplica al backend `todo-tasks-backend` y debe validarse antes de implementar cambios en codigo.

## 2. Enfoque general

El manejo de errores del backend debe estar orientado a excepciones.

Esto significa que:

- la trazabilidad principal se activa cuando ocurre una excepcion
- el objetivo del logging no es registrar todo el flujo feliz
- el objetivo del logging es dejar evidencia clara del fallo, su contexto y su severidad

La estrategia se compone de dos niveles obligatorios:

- nivel API
  - traduce excepciones a respuestas HTTP consistentes
- nivel tecnico
  - registra en logs cada excepcion relevante

Regla base:

- ninguna excepcion relevante debe salir de una capa sin dejar trazabilidad en logs

## 3. Principios

- Toda respuesta de error debe tener una estructura uniforme.
- Los errores de cliente deben responder `4xx`.
- Los errores inesperados del servidor deben responder `500`.
- No se deben exponer stack traces ni detalles internos en la respuesta HTTP.
- Los logs deben centrarse en excepciones, no en ruido operativo innecesario.
- Capturar excepciones solo cuando agregue contexto, transforme la excepcion o permita registrar mejor el error.
- No usar `try/catch` vacios ni capturas genericas que oculten el problema.

## 4. Rol de try/catch por capa

### 4.1 Controller

Regla:

- evitar `try/catch` en controller salvo que se necesite agregar contexto muy puntual
- la mayoria de excepciones deben propagarse hacia `GlobalExceptionHandler`

Uso recomendado:

- capturar solo si se necesita enriquecer el log con contexto de la request antes de relanzar

No recomendado:

- capturar excepciones solo para hacer `return 500`
- duplicar log del mismo error sin aportar contexto

### 4.2 Service

Regla:

- esta es la capa principal para usar `try/catch` cuando una operacion pueda fallar y se requiera contexto de negocio
- si se captura una excepcion, debe registrarse y luego relanzarse la misma o una excepcion de negocio mas clara

Uso recomendado:

- envolver operaciones de persistencia o bloques de negocio donde se conozca el contexto funcional
- registrar ids, operacion, estado previo o parametros relevantes antes de relanzar

Ejemplo conceptual:

```java
try {
    // operacion de negocio
} catch (RuntimeException ex) {
    log.error("Error updating task id={}", id, ex);
    throw ex;
}
```

### 4.3 Repository o integraciones

Regla:

- normalmente no agregar `try/catch` si solo se va a relanzar sin contexto
- capturar solo cuando se traduzca una excepcion tecnica a una mas util para la capa superior

### 4.4 GlobalExceptionHandler

Regla:

- toda excepcion traducida a respuesta HTTP debe quedar registrada aqui si no fue ya registrada con suficiente contexto
- esta capa garantiza el ultimo punto de trazabilidad antes de responder al cliente

## 5. Cuando usar try/catch

Usar `try/catch` cuando ocurra alguno de estos casos:

- se necesita agregar contexto funcional al log
- se necesita transformar una excepcion tecnica en una excepcion de negocio
- se necesita diferenciar severidad o tipo de error
- se necesita asegurar que una excepcion quede registrada antes de propagarse

No usar `try/catch` cuando:

- solo se va a capturar y relanzar sin agregar informacion
- el `GlobalExceptionHandler` ya cubre adecuadamente el caso y la capa actual no aporta contexto extra
- la captura esconderia el origen real del problema

## 6. Clasificacion de excepciones

### 6.1 Excepciones funcionales controladas

Ejemplos:

- `TaskNotFoundException`
- `InvalidTaskStateException`
- `InvalidSortParameterException`

Tratamiento:

- responder con `4xx`
- registrar en `WARN`
- incluir contexto funcional

### 6.2 Excepciones de validacion

Ejemplos:

- `MethodArgumentNotValidException`
- `ConstraintViolationException`

Tratamiento:

- responder con `400`
- registrar en `WARN`
- incluir resumen de validaciones fallidas

### 6.3 Excepciones tecnicas inesperadas

Ejemplos:

- `RuntimeException` no mapeada
- fallas de persistencia
- errores de integracion

Tratamiento:

- responder con `500`
- registrar en `ERROR`
- incluir stack trace en logs internos

## 7. Que debe incluir un log de excepcion

Todo log generado por una excepcion debe incluir, cuando aplique:

- capa donde ocurrio
- operacion ejecutada
- identificador del recurso afectado
- tipo de excepcion
- mensaje tecnico de la excepcion
- path HTTP si ya esta en capa web

Contexto recomendado por capa:

- controller
  - path
  - metodo HTTP
  - identificador del recurso
- service
  - operacion de negocio
  - id de la tarea
  - estado o parametros funcionales relevantes
- exception handler
  - tipo de excepcion
  - status HTTP devuelto
  - path

## 8. Niveles de logging

- `WARN`
  - excepciones funcionales esperadas
  - validaciones rechazadas
  - recursos no encontrados
  - conflictos de negocio
- `ERROR`
  - excepciones inesperadas
  - errores `500`
  - fallas tecnicas no controladas

Reglas:

- en `ERROR` se debe conservar stack trace
- en `WARN` el stack trace es opcional y debe usarse solo si aporta diagnostico

## 9. Contrato API de error

Toda respuesta de error debe seguir esta estructura:

```json
{
  "timestamp": "2026-04-22T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported sort field: items",
  "path": "/api/tasks"
}
```

Campos:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

## 10. Reglas para mensajes al cliente

- El mensaje debe describir el problema, no la causa tecnica interna.
- Para `404`, el mensaje debe identificar el recurso no encontrado.
- Para `409`, el mensaje debe describir la regla de negocio violada.
- Para `500`, usar un mensaje generico y estable.

Ejemplos aceptables:

- `Task with id 99 was not found`
- `Cannot change status from FINISHED to IN_PROGRESS once task is finalized`
- `An unexpected error occurred`

## 11. Datos que no deben registrarse

- contraseñas
- tokens
- cookies sensibles
- payloads completos con datos sensibles
- datos personales no necesarios para diagnostico

## 12. Estrategia de implementacion propuesta

### 12.1 Service

Implementar `try/catch` en operaciones donde:

- haya acceso a repositorio
- haya transiciones de negocio relevantes
- se pueda agregar contexto util antes de relanzar

### 12.2 GlobalExceptionHandler

Mantener logging centralizado para:

- excepciones funcionales que lleguen a capa web
- validaciones
- errores inesperados

### 12.3 Controller

Mantenerlo liviano.

- no usar `try/catch` por defecto
- solo usarlo si se necesita contexto adicional muy especifico

## 13. Criterios de aceptacion de esta estrategia

La implementacion se considerara correcta cuando:

- toda excepcion relevante deje log
- el log tenga contexto suficiente para diagnostico
- no se duplique logging innecesariamente entre capas
- el cliente reciba respuestas limpias y seguras
- los `try/catch` agreguen valor y no solo ruido

## 14. Pruebas minimas requeridas

- validar `404` y verificar log `WARN`
- validar `409` y verificar log `WARN`
- validar `500` y verificar log `ERROR`
- validar que una excepcion inesperada registrada desde service conserve contexto funcional
- validar que `GlobalExceptionHandler` siga traduciendo correctamente las respuestas
