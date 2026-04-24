# Estructura del proyecto backend y pom objetivo

## 1. Objetivo

Definir la estructura concreta del proyecto `todo-tasks-backend` y el contenido objetivo del `pom.xml` antes de comenzar la implementación.

Este documento aterriza:

- organización de carpetas
- paquetes Java
- archivos base del proyecto
- dependencias Maven
- plugins de build
- configuración inicial esperada

## 2. Stack definido

- `Java 21`
- `Spring Boot 3.5.x`
- `Maven 3.9+`
- `Spring Web`
- `Spring Data JPA`
- `Spring Validation`
- `H2 Database`
- `Spring Boot Test`
- `JaCoCo`

## 3. Estructura objetivo del proyecto

Ruta base:

- `todo-tasks-backend`

### 3.1 Estructura de archivos

```text
todo-tasks-backend
├── pom.xml
├── README.md
├── Dockerfile
├── .gitignore
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── imaginamos
    │   │           └── todo
    │   │               ├── TodoTasksApplication.java
    │   │               ├── config
    │   │               │   └── WebConfig.java           ← CORS para Angular (localhost:4200)
    │   │               ├── controller
    │   │               │   └── TaskController.java
    │   │               ├── dto
    │   │               │   ├── request
    │   │               │   │   ├── TaskCreateRequest.java
    │   │               │   │   ├── TaskItemRequest.java
    │   │               │   │   ├── TaskStatusUpdateRequest.java
    │   │               │   │   └── TaskUpdateRequest.java
    │   │               │   └── response
    │   │               │       ├── ApiErrorResponse.java
    │   │               │       ├── PageResponse.java
    │   │               │       ├── TaskItemResponse.java
    │   │               │       └── TaskResponse.java
    │   │               ├── entity
    │   │               │   ├── Task.java
    │   │               │   ├── TaskItem.java
    │   │               │   └── TaskStatus.java
    │   │               ├── exception
    │   │               │   ├── GlobalExceptionHandler.java
    │   │               │   ├── InvalidSortParameterException.java  ← validación de sort param
    │   │               │   ├── InvalidTaskStateException.java
    │   │               │   └── TaskNotFoundException.java
    │   │               ├── mapper
    │   │               │   └── TaskMapper.java
    │   │               ├── repository
    │   │               │   └── TaskRepository.java
    │   │               └── service
    │   │                   └── TaskService.java
    │   └── resources
    │       ├── application.yml
    │       └── data.sql                                 ← datos de ejemplo para validación manual
    └── test
        └── java
            └── com
                └── imaginamos
                    └── todo
                        ├── controller
                        │   └── TaskControllerTest.java
                        ├── exception
                        │   └── GlobalExceptionHandlerTest.java  ← pruebas del handler de errores
                        ├── mapper
                        │   └── TaskMapperTest.java              ← pruebas del mapper y campos derivados
                        └── service
                            ├── TaskServiceTest.java             ← pruebas unitarias de negocio
                            └── TaskServiceIntegrationTest.java  ← pruebas de integración con H2 real
```

## 4. Responsabilidad por paquete

### 4.1 `controller`

Responsabilidad:

- exponer endpoints REST
- recibir query params y payloads
- delegar en `service`
- retornar DTOs de respuesta

Clase inicial:

- `TaskController`

### 4.2 `dto.request`

Responsabilidad:

- representar payloads de entrada
- contener anotaciones de validación

Clases iniciales:

- `TaskCreateRequest`
- `TaskUpdateRequest`
- `TaskItemRequest`
- `TaskStatusUpdateRequest`

### 4.3 `dto.response`

Responsabilidad:

- representar salidas del API
- evitar exponer entidades JPA directamente

Clases iniciales:

- `TaskResponse`
- `TaskItemResponse`
- `PageResponse<T>`
- `ApiErrorResponse`

### 4.4 `entity`

Responsabilidad:

- mapear el dominio persistente
- definir relaciones JPA

Clases iniciales:

- `Task`
- `TaskItem`
- `TaskStatus`

### 4.5 `repository`

Responsabilidad:

- acceso a base de datos
- consultas paginadas y filtradas

Clase inicial:

- `TaskRepository`

### 4.6 `service`

Responsabilidad:

- implementar reglas de negocio
- orquestar CRUD, filtros, alertas y cambios de estado

Clase inicial:

- `TaskService`

### 4.7 `mapper`

Responsabilidad:

- transformar entre entidades y DTOs
- centralizar el cálculo de campos derivados en la respuesta cuando aplique

Clase inicial:

- `TaskMapper`

### 4.8 `exception`

Responsabilidad:

- modelar errores de negocio
- traducir excepciones a respuestas HTTP consistentes

Clases iniciales:

- `TaskNotFoundException`
- `InvalidTaskStateException`
- `GlobalExceptionHandler`

## 5. Modelo técnico inicial

### 5.1 Entidad `Task`

Campos previstos:

- `Long id`
- `String title`
- `String description`
- `LocalDateTime executionDate`
- `TaskStatus status`
- `List<TaskItem> items`
- `LocalDateTime createdAt`
- `LocalDateTime updatedAt`

Notas:

- relación `@OneToMany` con `TaskItem`
- cascada controlada para facilitar actualización completa de ítems
- orden por `position`

### 5.2 Entidad `TaskItem`

Campos previstos:

- `Long id`
- `String description`
- `Boolean completed`
- `Integer position`
- `Task task`

Notas:

- relación `@ManyToOne` hacia `Task`

## 6. `pom.xml` objetivo

## 6.1 Dependencias

Se propone un `pom.xml` con estas dependencias base:

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `com.h2database:h2`
- `spring-boot-starter-test`

## 6.2 Plugins

- `spring-boot-maven-plugin`
- `maven-compiler-plugin` configurado a `release 21` si se requiere explícitamente
- `jacoco-maven-plugin` para reporte y umbral de cobertura

## 6.3 Parent

- `org.springframework.boot:spring-boot-starter-parent:3.5.x`

## 6.4 Propuesta de contenido objetivo

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.x</version>
        <relativePath/>
    </parent>

    <groupId>com.imaginamos</groupId>
    <artifactId>todo-tasks-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>todo-tasks-backend</name>
    <description>Backend para la prueba tecnica TODO Tasks</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

## 7. Configuración inicial esperada

### 7.1 `application.yml`

Objetivo inicial:

- puerto por defecto `8080`
- configuración H2
- mostrar SQL solo si aporta a depuración
- crear esquema automáticamente en entorno local de prueba

Propuesta:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:tododb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
server:
  port: 8080
```

Nota:

- En una iteración posterior se puede cambiar `mem` por archivo si queremos persistencia entre reinicios.

### 7.2 `data.sql`

Uso previsto:

- opcional
- solo si queremos cargar datos demo para facilitar validación manual

## 8. Estrategia de implementación derivada

Orden recomendado de construcción:

1. Crear `pom.xml` y clase principal.
2. Configurar `application.yml`.
3. Crear entidades y enum.
4. Crear repositorio.
5. Crear DTOs.
6. Implementar mapper.
7. Implementar servicio.
8. Implementar controlador.
9. Implementar manejo de errores.
10. Agregar pruebas mínimas.
11. Verificar el reporte de cobertura y ajustar hasta llegar al `80%`.

## 9. Criterios de aceptación previos a implementar

Antes de iniciar código deberíamos tener acordado:

- `Java 21` como versión final
- línea `Spring Boot 3.5.x`
- estructura de paquetes mostrada arriba
- formato de respuesta paginada propio
- actualización completa de ítems mediante `PUT`
- endpoint específico para cambio de estado

## 10. Próximo paso propuesto

Con el plan, el contrato API y la estructura del proyecto ya definidos, el siguiente paso es comenzar la implementación del esqueleto inicial del backend en `todo-tasks-backend`.
