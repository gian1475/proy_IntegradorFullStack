# Librerías del proyecto – Sistema de Préstamos, Biblioteca de Alejandría

Qué librerías usa el sistema, para qué sirve cada una, en qué archivo está y qué código nuestro reemplazó.
Sirve para la sección de librerías del informe (semana 10) y para la sustentación: cada integrante tiene que poder explicar por qué está cada una.

> **Librería:** código que otra persona ya escribió y probó, y que nosotros solo importamos.
> En el backend se agregan en [`backend/pom.xml`](../backend/pom.xml) (Maven las descarga solo). En el frontend, en [`frontend/package.json`](../frontend/package.json) (con `npm install`).

---

## Resumen

| Librería | Versión | Capa | Para qué la usamos | Dónde está |
|---|---|---|---|---|
| Spring Boot (Web MVC) | 4.1.2 | Backend | Arma la API REST (controllers, JSON, servidor Tomcat) | todos los `controller/` |
| Spring Data JPA + driver de PostgreSQL | 4.1.2 / 42.7.13 | Backend | Conexión a Neon y consultas sin escribir SQL a mano | `dao/`, `entity/` |
| **Spring Security** | 7.1.2 | Backend | Filtro del token, permisos por perfil (`@RolesAllowed`), CORS y BCrypt | `seguridad/SeguridadConfig.java`, `seguridad/FiltroToken.java` |
| **Bean Validation** (Hibernate Validator) | 9.1.3 | Backend | Reglas de los datos con anotaciones (`@NotBlank`, `@Pattern`...) | `dto/` |
| **Lombok** | 1.18.46 | Backend | Genera los getters, setters y el constructor vacío de las entidades | `entity/` |
| **Google Guava** ⭐ | 33.4.8 | Backend | Bloqueo de 15 min después de 3 intentos fallidos de login; sesiones que vencen a los 15 min sin actividad | `seguridad/LimitadorIntentosLogin.java`, `service/SesionService.java` |
| **Apache POI** ⭐ | 5.4.1 | Backend | Reportes en Excel (.xlsx) | `service/ReporteService.java` |
| **Apache Commons CSV** ⭐ | 1.14.0 | Backend | Reportes en CSV e importación de libros desde CSV | `service/ReporteService.java` |
| OpenPDF | 2.0.3 | Backend | Reportes en PDF | `service/ReporteService.java` |
| **Logback** ⭐ | 1.5.38 | Backend | Archivo de registro `backend/logs/biblioteca.log` | `logback-spring.xml` y cada `log.info/warn` |
| Spring Boot Actuator | 4.1.2 | Backend | Monitoreo: salud, memoria, CPU, peticiones, conexiones | `application.properties`, `service/MonitoreoService.java` |
| Angular | 22 | Frontend | Las pantallas (componentes, rutas, guards, signals) | `frontend/src/app/` |
| **Bootstrap** | 5.3.8 | Frontend | Diseño de los paneles del admin y del encargado | plantillas `.html` de los paneles, `styles/bootstrap-paneles.less` |
| LESS | 4 | Frontend | Estilos propios (inicio, login, registro, catálogo) | archivos `.less` |
| JUnit (Jupiter) + Mockito | 6.0.3 / 5.23 | Pruebas | 205 pruebas del backend | `backend/src/test/` |
| Vitest | 4 | Pruebas | 24 pruebas del frontend | archivos `.spec.ts` |

⭐ = librerías que la rúbrica del APF3 pide para el puntaje máximo (Guava, Apache POI, Apache Commons y Logback). **Las cuatro están.**

**Ojo con el informe:** Spring Boot 4 trae **JUnit 6** (Jupiter 6.0.3), no JUnit 5. Se escribe igual (`@Test`, `assertEquals`), pero el informe debería decir la versión correcta.

---

## Limpieza del 02/11: código propio que se cambió por librerías

Antes teníamos varias cosas escritas a mano que una librería ya resuelve. Se cambiaron para tener menos código y usar lo estándar. **El sistema hace exactamente lo mismo**: los mismos mensajes, los mismos códigos 401/403/400 y las mismas pantallas (solo cambia el diseño de los paneles).

| # | Antes (hecho a mano) | Ahora (librería) | Líneas |
|---|---|---|---|
| 1 | `ValidadorUsuario` + unos 10 `if (x == null) throw ...` repartidos en los services | **Bean Validation**: anotaciones en los DTOs y `@Valid` en el controller | se borró `ValidadorUsuario` (43) y los `if` de 5 services |
| 2 | DTOs con getters y setters escritos uno por uno | **`record` de Java** (no es librería: viene con Java 16+) | 11 DTOs: de 325 a 201 |
| 2 | Entidades con getters y setters escritos uno por uno | **Lombok** (`@Getter @Setter @NoArgsConstructor`) | 6 entidades: de 373 a 286 |
| 3 | `AutorizacionInterceptor` + `@RolesPermitidos` + `WebConfig` | **Spring Security** (`FiltroToken` + `@RolesAllowed` + `SeguridadConfig`) | de 123 a 106 |
| 4 | 337 líneas de LESS para tarjetas, tablas, botones, formularios, pestañas y modal | **Bootstrap** (ya estaba instalado, solo se usaba en el dashboard) | `panel.less`: de 337 a 51 |
| 5 | El error 401/403 se armaba pegando textos (`"{\"mensaje\":\"" + ...`) | Lo responde el `ManejadorErrores` como todos los demás errores | se borró junto con el interceptor |

En total: backend **−702 / +333** líneas en archivos que ya existían (más 106 de los 2 archivos nuevos de seguridad); frontend **−693 / +401** (las plantillas crecen un poco porque las clases de Bootstrap son más largas, pero desaparece casi todo el LESS de los paneles).

## Limpieza del 03/11 (L3): tres cosas más, con librerías que ya estaban

| Antes (hecho a mano) | Ahora (librería que ya teníamos) | Dónde |
|---|---|---|
| Sesiones en un `ConcurrentHashMap` con una fecha de vencimiento por sesión que se revisaba y renovaba a mano | **Caché de Guava** con `expireAfterAccess(15 min)`: cada lectura del token renueva los 15 minutos y Guava borra sola las vencidas | `SesionService` (de 91 a 85 líneas; `Sesion` ya no necesita `estaExpirada` ni `conVencimiento`) |
| `texto == null \|\| texto.isBlank()` en 3 lugares | **`StringUtils.hasText`** de Spring (`org.springframework.util`) | `LibroService`, `PrestamoService`, `IncidenciaService` |
| Fechas armadas con `padStart`, `toLocaleTimeString` y `toLocaleDateString` en 5 lugares | **`formatDate`** de `@angular/common`, con los datos del idioma `es-PE` que también trae Angular (`registerLocaleData`). Todo quedó en un solo archivo | [`utils/fechas.ts`](../frontend/src/app/utils/fechas.ts), usado por `sancion.ts`, `reportes-descarga`, `user-catalog`, `activos-tab` y `registrar-tab` |

No se instaló nada nuevo: Guava, Spring y Angular ya estaban en el proyecto.

Lo que **no** se cambió, porque ya usaba una librería o es lógica del negocio: el bloqueo por intentos (Guava), los reportes (POI, OpenPDF, Commons CSV), BCrypt, el monitoreo (Actuator), `ManejadorErrores` (es el patrón normal de Spring), la regla de la contraseña (`ValidadorContrasena`, porque también mide los 72 bytes de BCrypt) y las utilidades del front (`alerta-prestamo`, `estado-libro`, `regla-contrasena`).

---

## Backend

### Spring Security (seguridad y perfiles)

**Qué es:** la librería estándar de Spring para autenticación y autorización.

**Cómo la usamos:**
1. [`FiltroToken`](../backend/src/main/java/com/alejandria/biblioteca/seguridad/FiltroToken.java) corre en **cada petición**. Lee el header `Authorization: Bearer <token>`, le pregunta a `SesionService` si el token es válido y, si lo es, le dice a Spring Security quién es el usuario y su perfil (`ROLE_ENCARGADO`, por ejemplo).
2. Cada endpoint protegido dice qué perfiles lo pueden usar con `@RolesAllowed`:
   ```java
   @PostMapping
   @RolesAllowed(Rol.ENCARGADO)          // solo el encargado registra préstamos
   public ResponseEntity<PrestamoDTO> registrar(@Valid @RequestBody RegistroPrestamoDTO dto,
                                                @AuthenticationPrincipal Sesion sesion) { ... }
   ```
   `@AuthenticationPrincipal` entrega la sesión que guardó el filtro (así sabemos qué encargado registró el préstamo).
3. Si el perfil no está permitido, Spring Security lanza `AccessDeniedException` y el [`ManejadorErrores`](../backend/src/main/java/com/alejandria/biblioteca/exception/ManejadorErrores.java) responde **401** si no hay sesión o **403** si hay sesión pero el perfil no tiene permiso.
4. [`SeguridadConfig`](../backend/src/main/java/com/alejandria/biblioteca/seguridad/SeguridadConfig.java) arma todo: activa `@RolesAllowed`, pone el filtro, configura CORS (solo `http://localhost:4200`) y apaga lo que no usamos (la pantalla de login de Spring, CSRF y las sesiones con cookie).
5. **BCrypt** (`BCryptPasswordEncoder`) también es parte de Spring Security: guarda las contraseñas como hash y nunca en texto plano.

**Lo que sigue siendo nuestro:** el token y la sesión de 15 minutos (`SesionService`), el login (`AuthService`) y el bloqueo por intentos (Guava). Spring Security no decide quién entra: solo aplica los permisos.

**Si el profe pregunta "¿por qué Spring Security y no lo que tenían?":** hacía lo mismo, pero Spring Security es el estándar de la industria, está probado por miles de proyectos y trae protecciones extra (encabezados de seguridad en cada respuesta, CORS en el mismo lugar). Nuestro código ya no tiene que revisar los permisos a mano.

**Pruebas:** [`SeguridadEndpointsTest`](../backend/src/test/java/com/alejandria/biblioteca/seguridad/SeguridadEndpointsTest.java) (11): sin token → 401, token inventado → 401, prestatario en un endpoint del encargado → 403, el `@RolesAllowed` de la clase completa, CORS para el frontend y rechazo de otra página.

### Bean Validation (Hibernate Validator)

**Qué es:** la forma estándar de Java (Jakarta) de escribir las reglas de los datos como anotaciones.

**Cómo la usamos:** las reglas están en los DTOs y el controller las activa con `@Valid`:
```java
public record RegistroDTO(
        @NotBlank(message = "El nombre debe tener entre 1 y 50 caracteres")
        @Size(max = 50, message = "El nombre debe tener entre 1 y 50 caracteres")
        String nombre,

        @NotNull(message = "El DNI debe tener 8 dígitos")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,
        ...
```
Si un dato no cumple, Spring ni siquiera entra al método del controller: lanza `MethodArgumentNotValidException` y el `ManejadorErrores` responde **400** con el mensaje de la anotación (`{"mensaje": "El DNI debe tener 8 dígitos"}`).

| DTO | Reglas |
|---|---|
| `RegistroDTO`, `RegistroEncargadoDTO` | nombre y apellido de 1 a 50, DNI de 8 dígitos, correo `algo@algo.algo` de máximo 100 |
| `LoginDTO` | correo y contraseña obligatorios |
| `LibroFormDTO` | título, ISBN, editorial, año, stock y categoría obligatorios; stock ≥ 0; año entre 1000 y 2100 |
| `RegistroPrestamoDTO` | DNI del prestatario y libro obligatorios |
| `DevolucionDTO` | estado físico `bueno` o `dañado` |
| `ReservaDTO` | libro y prestatario obligatorios |

**Lo que se quedó en los services:** las reglas que necesitan la BD o la hora (ISBN repetido, stock 0 al editar, fecha de recojo, sanciones, turno del encargado) y la regla de la contraseña (`ValidadorContrasena`, que mide los 72 bytes).

**Importación de libros por CSV:** no pasa por un controller, así que `ReporteService` revisa cada fila con el mismo `Validator` (`validator.validate(form)`). Una fila con stock negativo sale como error de esa fila, igual que antes.

**Pruebas:** [`ValidacionDtoTest`](../backend/src/test/java/com/alejandria/biblioteca/dto/ValidacionDtoTest.java) (22) reemplaza a `ValidadorUsuarioTest` con los mismos casos (DNI de 7 o 9 dígitos, con letras, correos mal escritos, nombre de 51 caracteres...).

### `record` de Java (DTOs) y Lombok (entidades)

**`record` no es una librería**: viene con Java desde la versión 16. Un `record` declara los campos una sola vez y Java genera el constructor y los métodos para leerlos:
```java
public record RegistroPrestamoDTO(String dniPrestatario, Integer idLibro) {}
// se lee con dto.dniPrestatario() en vez de dto.getDniPrestatario()
```
Se usó en los 11 DTOs que todavía tenían getters y setters escritos a mano (otros 9 ya eran `record`). El JSON que recibe el frontend no cambió: los nombres de los campos son los mismos.

**Lombok** se usa en las **entidades**, porque JPA necesita clases normales (con constructor vacío y setters) y ahí no sirve `record`:
```java
@Entity
@Table(name = "prestamo")
@Getter               // genera getIdPrestamo(), getLibro(), ...
@Setter               // genera setIdPrestamo(...), setLibro(...), ...
@NoArgsConstructor    // genera el constructor vacío que pide JPA
public class Prestamo { ... }
```
Lombok trabaja **al compilar**: el `.class` tiene los métodos como si los hubiéramos escrito. Por eso en el `pom.xml` está como procesador de anotaciones y **no va dentro del `.jar`**.

**Si el profe pregunta "¿dónde está el getter?":** lo genera Lombok al compilar a partir de `@Getter`. En VS Code se ve en el "Outline" de la clase.

### Google Guava ⭐

**Para qué:** bloquear un correo 15 minutos después de 3 intentos fallidos de login (RF10).

**Dónde:** [`LimitadorIntentosLogin`](../backend/src/main/java/com/alejandria/biblioteca/seguridad/LimitadorIntentosLogin.java). Usa el `Cache` de Guava, que guarda "correo → intentos fallidos" y **borra la entrada solo** cuando pasan los 15 minutos (`expireAfterWrite`), sin que tengamos que limpiar nada a mano.

**También (desde el 03/11):** [`SesionService`](../backend/src/main/java/com/alejandria/biblioteca/service/SesionService.java) guarda "token → sesión" en otro `Cache` de Guava con `expireAfterAccess`: cada petición lee el token y eso le vuelve a contar los 15 minutos (RNF04). Si pasan 15 minutos sin leerlo, Guava lo borra.

### Apache POI ⭐, OpenPDF y Apache Commons CSV ⭐

**Para qué:** los 8 reportes del RF08 en Excel, PDF y CSV, y la importación de libros desde CSV.

**Dónde:** [`ReporteService`](../backend/src/main/java/com/alejandria/biblioteca/service/ReporteService.java). Cada reporte se arma una sola vez como `TablaReporte` (título, columnas, filas) y después:
- **Apache POI** (`XSSFWorkbook`) la convierte en `.xlsx`, con el encabezado en color.
- **OpenPDF** (`Document`, `PdfPTable`) la convierte en PDF.
- **Commons CSV** (`CSVPrinter`) la convierte en CSV; y `CSVParser` lee el CSV de libros (acepta `,` o `;`).

### Logback ⭐

**Para qué:** guardar en `backend/logs/biblioteca.log` lo que pasa: logins correctos y fallidos (sin la contraseña), préstamos, reservas, importaciones y errores.

**Dónde:** viene con Spring Boot; la configuración está en `backend/src/main/resources/logback-spring.xml`. En el código se usa con `log.info(...)`, `log.warn(...)` y `log.error(...)` (por ejemplo en `AuthService` y `ManejadorErrores`).

### Spring Boot Actuator

**Para qué:** monitoreo (lo pidió el profe junto con el `.jar`).

**Dónde:** corre en el puerto **8081** y solo responde desde la misma PC: `http://localhost:8081/actuator/health`, `/info` y `/metrics`. El admin ve un resumen en la pestaña Monitoreo (`MonitoreoService`).

### Spring Data JPA + driver de PostgreSQL

**Para qué:** conectarse a Neon y hacer las consultas. Los `dao/` son interfaces: con `existsByCorreo(...)` o `findByDni(...)` Spring escribe el SQL solo; las consultas más grandes (el JOIN de préstamos activos, el historial) están con `@Query`.

**`@EntityGraph` (02/11):** la lista y la búsqueda de libros (`LibroDao`) llevan `@EntityGraph(attributePaths = {"categoria", "autores"})`. Le dice a JPA que traiga la categoría y los autores **en la misma consulta** (con JOIN). Antes, Hibernate pedía los autores de cada libro con otra consulta (el problema "N+1": 25 libros = 26 consultas) y la lista tardaba unos 4 segundos contra Neon; ahora tarda 0,2 s.

---

## Frontend

### Bootstrap 5.3

**Antes:** solo se usaba en el dashboard del admin. Las demás pestañas de los paneles tenían 337 líneas de LESS propio para tarjetas, tablas, formularios, botones, pestañas y el modal.

**Ahora:** los **dos paneles completos** (admin y encargado) usan las clases de Bootstrap:

| Qué | Clase de Bootstrap |
|---|---|
| Pestañas | `nav nav-tabs` + `nav-link` |
| Tarjeta | `card card-body` |
| Formulario | `row g-3` + `col-sm-6 col-lg-4`, `form-label`, `form-control`, `form-select` |
| Botones | `btn btn-primary`, `btn btn-outline-secondary`, `btn btn-outline-danger` |
| Tabla | `table-responsive` + `table table-sm table-hover` |
| Mensajes | `alert alert-success`, `alert-danger`, `alert-warning` |
| Fila vencida / por vencer (RF05) | `table-danger` / `table-warning` |
| Modal de devolución | `modal`, `modal-dialog`, `modal-content` (se muestra con `d-block` porque lo abre Angular) |

**Está encerrado:** todo Bootstrap queda dentro de `.con-bootstrap` ([`styles/bootstrap-paneles.less`](../frontend/src/styles/bootstrap-paneles.less)), así **no cambia** el inicio, el login, el registro ni el catálogo, que tienen su propio diseño en LESS. En ese mismo archivo se cambia el azul de Bootstrap por el marrón del sistema (`#854d0e`).

**Lo que sigue en LESS** ([`styles/panel.less`](../frontend/src/styles/panel.less), 51 líneas): el color de fondo de la página, el fondo oscuro del modal y los **colores de cada estado** (`activo`, `vencido`, `en_espera`...), porque Bootstrap no conoce los estados de nuestra BD.

### Angular y LESS

**Angular 22** arma las pantallas: componentes standalone, rutas con guards por perfil (`/admin`, `/encargado`, `/catalogo`), un interceptor que agrega el token a cada petición y *signals* para el estado. Las fechas y horas se muestran con `formatDate` de `@angular/common` (`utils/fechas.ts`). **LESS** es CSS con variables y anidamiento; se usa en las pantallas que tienen un diseño propio.

---

## Pruebas

| Librería | Qué prueba | Cantidad |
|---|---|---|
| JUnit (Jupiter 6) + Mockito | Reglas del negocio con datos falsos (Mockito) y consultas contra la BD real de Neon (se deshacen al terminar) | 205 |
| Spring MockMvc (viene con Spring Boot) | Peticiones HTTP completas: Spring Security, `@Valid` y `ManejadorErrores` | 13 de las 205 |
| Vitest | Utilidades del frontend: alertas de 15 min, estado y búsqueda del catálogo, regla de la contraseña, "Cargando..." de la pestaña Libros, incidencias de cada préstamo (RF06) | 24 |

Se corren con `./mvnw test` (en `backend/`) y `npx ng test --watch=false` (en `frontend/`).
