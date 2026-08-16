# RAH Admin Server

Este repo es el servidor de administración/monitorización para el ecosistema RAH, construido con Java 21 y Spring Boot (WebFlux + Actuator + Security + Spring Boot Admin). Está pensado para ofrecer un panel reactivo y seguro que centralice métricas y administración de instancias.

## Stack
- Language(s): Java 21
- Framework / runtime: Spring Boot 4.x (WebFlux)
- Notable libraries (en uso): Spring Boot WebFlux, Spring Boot Actuator, Spring Security, Spring Boot Admin

## Qué hay en este repo (alto nivel)
- pom.xml — configuración Maven y dependencias.
- mvnw / mvnw.cmd — maven wrapper para reproducibilidad.
- src/main/java/com/rah/adminserver — código fuente Java (clase principal y config).

## Cómo ejecutar (rápido)
Desde un clon limpio:

```bash
# Build
./mvnw clean package

# Ejecutar con maven
./mvnw spring-boot:run

# O ejecutar el artefacto jar
java -jar target/rah-admin-server-0.0.1-SNAPSHOT.jar
```

Variables de entorno / configuración (ejemplo)
- SPRING_PROFILES_ACTIVE=local
- SERVER_PORT=8080
- MANAGEMENT_SERVER_PORT=8081

Ejemplo mínimo de application.yml (colocar en src/main/resources/application.yml o usar variables env):

```yaml
server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
  endpoint:
    health:
      show-details: when_authorized

spring:
  security:
    user:
      name: admin
      password: ${ADMIN_PASSWORD:changeme}
```

Nota: para producción, NO dejar credenciales en texto plano; usar Vault/KMS/secret manager.

## Qué revisar después (prioridades)
1. Documentación (alta)
   - Añadir README (hecho), CONTRIBUTING, y ejemplos de deployment (Dockerfile / docker-compose).
2. Tests & CI (alta)
   - Añadir pruebas unitarias y de integración: StepVerifier (reactor-test) y WebTestClient.
   - Configurar GitHub Actions para build + tests + análisis estático.
3. Seguridad (alta)
   - Revisar `AdminServerSecurityConfig` para asegurar endpoints (actuator, admin UI) y no usar credenciales en claro.
4. Revisión reactiva (alta)
   - Buscar y eliminar llamadas bloqueantes (.block(), uso de RestTemplate o repositorios JDBC sin adaptar).
   - Usar subscribeOn(Schedulers.boundedElastic()) para aislar tareas bloqueantes cuando no se pueda evitar.
5. Calidad de código
   - Añadir herramientas: SpotBugs, Checkstyle, PMD, formateo automático.

## Buenas prácticas y recomendaciones técnicas
- Programación funcional / reactiva
  - Mantener funciones puras en la lógica de negocio y limitar efectos (I/O, logging) a los bordes.
  - Componer operaciones con map / flatMap / filter / onErrorResume y evitar branching imperativo dentro de pipelines.
  - Usar `record` para DTOs inmutables cuando aplique (Java 21).
  - Pasar información por Reactor Context cuando sea necesario (correlation id, tracing).

- Evitar bloqueos en WebFlux
  - No usar `.block()` en el path de la petición.
  - Para librerías no-reactivas, envolver llamadas en Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic()).

- Seguridad
  - Proteger endpoints de actuator y UI del Admin con roles/authorities.
  - Exponer solo lo necesario en production: management.endpoints.web.exposure.include
  - Integrar OAuth2/OIDC/JWT para SSO en entornos productivos.

## Checklist para la siguiente PR (útil para tracking)
- [ ] README (esta PR)
- [ ] Añadir CONTRIBUTING.md con guía de estilo y how to run
- [ ] Añadir Dockerfile y docker-compose (opcional) para despliegue local
- [ ] Añadir pruebas unitarias (reactor-test) y pruebas de endpoints (WebTestClient)
- [ ] Pipeline CI (GitHub Actions): build, test, static analysis
- [ ] Integrar SpotBugs / Checkstyle y formateo automático
- [ ] Auditar y eliminar llamadas bloqueantes
- [ ] Revisar y endurecer AdminServerSecurityConfig (no credenciales en claro)

## Cómo contribuir
- Clona el repo, crea una rama feature/bugfix siguiendo la convención (feature/xyz), incluye pruebas para cambios lógicos y abre PR con descripción y checklist.

## Siguientes pasos que puedo hacer por ti
- Buscar en el código patrones problemáticos (por ejemplo `.block()`, `RestTemplate`, uso de JDBC) y listar ubicaciones.
- Proponer un workflow de GitHub Actions y crear el archivo.
- Crear Dockerfile y docker-compose.
- Revisar detalladamente AdminServerSecurityConfig y proponer cambios concretos.

Si quieres, comienzo buscando llamadas bloqueantes y un resumen de archivos sin test para priorizar donde añadir pruebas.
