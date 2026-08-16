# RAH Admin Server

Este repo es el servidor de administración/monitorización para el ecosistema RAH, construido con Java 21 y Spring Boot (WebFlux + Actuator + Security + Spring Boot Admin). Está pensado para ofrecer un panel reactivo y seguro que centralice métricas y administración de instancias.

## Stack
- Language(s): Java 21
- Framework / runtime: Spring Boot 4.x (WebFlux)
- Notable libraries (en uso): Spring Boot WebFlux, Spring Boot Actuator, Spring Security, Spring Boot Admin

## Lo bueno

Stack moderno: Java 21 + Spring Boot 4 + WebFlux. Esto permite escribir aplicaciones reactivas y aprovechar features recientes del JDK.
Uso de WebFlux + Actuator + Spring Boot Admin sugiere que el proyecto está pensado para ser no‑bloqueante y observable.
Estructura de proyecto estándar Maven (mvn wrapper incluido) — facilita ejecución/CI.
Existe una clase de configuración de seguridad (AdminServerSecurityConfig), lo que indica que se ha pensado en asegurar el acceso al panel/actuadores.
Lo malo / áreas claras de mejora

## Documentación insuficiente

Falta README, instrucciones de arranque, variables de entorno y ejemplos de uso. Riesgo: otra persona no puede poner el servicio en marcha rápido.
Falta Dockerfile / compose para despliegue reproducible.
Visibilidad del código y pruebas

No vi tests en el árbol (o no están ubicados/nombrados). Necesarias pruebas unitarias e integradas (especialmente para código reactivo).
Falta configuración de CI (GitHub Actions) para builds, tests y análisis estático.
Prácticas reactivas / bloqueo accidental

Al usar WebFlux, hay riesgo común de introducir llamadas bloqueantes (JDBC, reclamos a librerías sin drivers reactivos). Hay que auditar el código y asegurar que I/O es reactivo o se delega explícitamente a un Scheduler.
Seguridad: ajustes a revisar

Hay un archivo de configuración de seguridad; conviene revisar que:
Actuator endpoints estén protegidos según roles.
No haya credenciales en texto plano o in‑memory para producción.
CSRF/CORS y cabeceras de seguridad estén correctamente configuradas si hay UI.
Preferir JWT/OAuth2/OIDC para integración con SSO si es necesario.
Calidad del código y limpieza

No pude revisar implementaciones concretas, pero recomiendo seguir principios SOLID, nomenclatura clara, separación de responsabilidades y límites de tamaño de método/clase.
Falta ver si se usan DTOs/records (Java 21 permite records) para inmutabilidad.
Recomendaciones concretas (priorizadas)

## Documentación mínima (alta prioridad)

* Añadir README con:
Comandos para run/build/test: mvnw clean package; mvnw spring-boot:run
Variables de entorno (application.yml / application.properties ejemplos).
Endpoints principales y puertos.
Añadir CONTRIBUTING y un ejemplo de deployment (Dockerfile + docker-compose).
Tests & CI (alta prioridad)

* Añadir tests unitarios para servicios con StepVerifier (reactor-test) y WebTestClient para endpoints WebFlux.
Configurar GitHub Actions con pasos: mvn -B -DskipTests=false test, maven‑checkstyle/spotbugs, build.
Añadir cobertura mínima y gating en la pipeline.
Seguridad (alta)

* Revisar AdminServerSecurityConfig:
Asegurar uso de PasswordEncoder si hay usuarios locales.
Restringir actuator endpoints (management.endpoints.web.exposure) y asegurar /actuator/, /admin/ con roles.
Evitar exponer información sensible en health/info en producción.
Integrar OAuth2/OIDC/JWT para producción si procede.
Revisión reactiva y programación funcional (alta → técnica)

* Evitar bloqueos: revisar que no haya llamadas a métodos bloqueantes dentro de pipelines Reactor (no call to .block(), no usar repositorios JDBC sin adaptar).
Preferir tipos inmutables:
Usar record para DTOs cuando corresponda.
Encapsular efectos: mantener funciones puras donde sea posible y empujar efectos (I/O, logging) a los bordes.

* Aprovechar operadores Reactor y composition:
map/flatMap/filter/transform/retryWhen/onErrorResume en vez de flujo imperativo.
Evitar long if/else en pipelines: componer pequeñas funciones puras.

* Propagación de context y tracing:
Usar Reactor Context para información por petición (correlation id), y compatibilidad con MDC para logs reactivos.
Considerar RouterFunctions (en WebFlux) para un estilo más funcional de rutas/handlers si se quiere un enfoque FP más evidente.
Arquitectura y limpieza del código

* Paquetes sugeridos: controller (handlers), router (si usa functional routing), service, repository, model/dto, config, util.
Mantener responsabilidades: controllers/handlers solo validación/transformación y delegar lógica al service.
Validación: usar validaciones reactivas (@Validated) y composición de errores claros (errores tipados, HTTP status adecuados).
Centralizar manejo de errores: handler global para mapear excepciones a responses (WebExceptionHandler / @RestControllerAdvice).
Herramientas de calidad

* Añadir: SpotBugs/FindBugs, PMD, Checkstyle o google-java-format.
Formateo automático y pre-commit hook (git hooks).
Dependabot/renovate para mantener dependencias actualizadas (especialmente Spring Boot y librerías de seguridad).
Dependencias / pom

* Confirmar que versiones de spring-boot-admin y parent están alineadas. Añadir maven-surefire/failsafe con configuraciones claras.
Usar dependencyManagement/BOM si integra varias dependencias.
Ejemplos de patrones funcionales y Reactor (conceptual)

* Evitar:
Llamadas bloqueantes dentro de flatMap: flatMap(x -> blockingCall()); // malo

* Preferir:
flatMap(x -> Mono.fromCallable(() -> blockingCall()).subscribeOn(Schedulers.boundedElastic()))
Componer funciones puras: service.findUser(id).map(this::toDto).flatMap(repo::save)
Pruebas reactivas básicas

* Servicios: usar reactor-test StepVerifier:
StepVerifier.create(service.doSomething(...)).expectNextMatches(...).verifyComplete();
Endpoints: usar WebTestClient.


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
