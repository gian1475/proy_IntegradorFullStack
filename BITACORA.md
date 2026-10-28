# Bitácora de cambios – Sistema de Préstamos, Biblioteca de Alejandría

Registro de todo lo que se ha modificado en el código y en la base de datos, con el motivo de cada cambio.
Sirve para preparar la sustentación: cada integrante debe poder explicar cualquier parte.

---

## Cómo levantar el proyecto

1. **Credenciales de la BD**: crear `backend/.env` (copiar de [backend/.env.example](backend/.env.example)) y completar URL, usuario y contraseña de Neon. Este archivo **no se sube a GitHub**.
2. **Base de datos**: si la BD de Neon se creó con una versión anterior del script, ejecutar en el SQL Editor de Neon las migraciones que falten (carpeta `database/migraciones/`), en orden (`migracion_01_prestamo.sql`, `migracion_02_sancion_monto.sql`, `migracion_03_reserva_cola.sql`, `migracion_04_quitar_fk_hibernate.sql`). Para tener los libros y los movimientos de la demo, ejecutar `database/datos_demo.sql`. Todos se pueden ejecutar más de una vez. (En Neon ya están las migraciones 01 y 02 y los libros, al 21/10, y la 03, la 04 y los movimientos desde el 27/10.)
3. **Backend** (desde la carpeta `backend/`, porque ahí busca el `.env`):
   ```
   cd backend
   ./mvnw spring-boot:run
   ```
4. **Frontend** (en otra terminal):
   ```
   cd frontend
   npm start
   ```
   Abrir `http://localhost:4200`.

El frontend no se conecta a la BD ni necesita credenciales: solo le habla al backend en `localhost:8080`.

---

## Decisiones tomadas

| Fecha | Decisión | Motivo |
|---|---|---|
| 21/10 | Autenticación con **token propio** (UUID en memoria); el logout lo invalida en el backend. **Cambió el 21/10:** ya no dura 8 h fijas, se cierra tras **15 min sin actividad** (RNF04) | No agrega librerías (sin Spring Security ni JWT) y el equipo lo puede explicar completo |
| 21/10 | La regla de contraseña se exige al **registrarse**, no al iniciar sesión | Para no bloquear usuarios que ya existían |
| 21/10 | Contraseña con máximo **64** caracteres | BCrypt solo procesa hasta 72 bytes; con más, falla |
| 21/10 | Tabla `prestamo`: se mantienen los nombres del **script** (`fecha_prestamo`, `fecha_devolucion_prevista`, `fecha_devolucion_real`) | Decisión del equipo |
| 21/10 | Eliminación lógica: préstamo → estado `anulado`; libro → estado `baja` | No se borran filas |
| 21/10 | La ampliación de un préstamo suma **1 hora** y solo si el libro no tiene reservas pendientes (constante `HORAS_AMPLIACION`) | Lo dice el RF05 del informe (antes sumaba 3 h como supuesto) |
| 21/10 | Una devolución con estado `dañado` **no** crea una incidencia automáticamente | El encargado registra incidencias aparte |
| 21/10 | **Reservar aparta un ejemplar**: el stock baja 1 (con 0 el libro queda `reservado`). Cancelar o dejar vencer la reserva devuelve el ejemplar | Regla "al reservar, el stock se actualiza solo" |
| 21/10 | Si el encargado presta un libro que el prestatario tenía reservado, la reserva pasa a `atendida` y el stock **no** baja otra vez | El ejemplar ya estaba apartado |
| 21/10 | El límite de recojo de una reserva es el **final del turno** elegido; se puede reservar hasta **7 días** antes | Supuesto. Una reserva no recogida a tiempo pasa a `vencida` |
| 21/10 | El stock se maneja en **un solo lugar** (`LibroService`); préstamos y reservas lo reutilizan | No repetir código (SOLID) |
| 21/10 | El `.jar` se llama `biblioteca-alejandria.jar` (`<finalName>` en el `pom.xml`) | Antes salía `demo-0.0.1-SNAPSHOT.jar`, nombre de plantilla |
| 21/10 | El informe del Avance III y las notas `.txt` de `docs/` no se suben a GitHub. **Cambió el 27/10:** el informe (`docs/Avance_III_IntegradorV8_corregido.docx`) sí se puede subir; las notas `.txt` siguen fuera | Las notas tienen el chat de la revisión con el profe |
| 21/10 | Ampliación, intentos de login y sesión: **manda el informe** (1 h solo sin reservas, 3 intentos, 15 min de inactividad) | Se cambia el código para que coincida con RF05, RF10 y RNF04 |
| 21/10 | Diccionario de datos: **manda la BD**. Solo se agrega `sancion.monto` | Cambiar la BD a una semana es más riesgoso; el informe se reescribe |
| 21/10 | CRUD de usuarios (RF09, RF12, RF13): **no entra en el Avance III**, queda opcional para el final | Decisión del equipo. La eliminación física se hace con las reservas canceladas o vencidas (RF17). El informe debe pasar "usuarios" del Sprint 3 al Sprint 4 |
| 21/10 | Herencia de `Usuario`: se corrige el informe | La herencia está en la BD (tablas de subtipo), no en clases Java |
| 21/10 | Se comprobó que la BD de Neon trabaja en hora GMT y Java en hora de Lima | Causa del error B1 (préstamos vencidos al instante) |
| 27/10 | Horario de atención **08:00–20:00**: fuera de ese horario no se presta y la hora límite nunca pasa de las 20:00 (`limitarAlCierre`). Se valida la **hora**, no el día de la semana | RF01 y RF05 piden no pasar del cierre. Las horas están en `application.properties` para poder probar de noche sin tocar el código |
| 27/10 | La ampliación es **"hasta" 1 hora**: si la hora límite es 19:30, se amplía hasta las 20:00. Si ya termina a las 20:00, no se amplía | RF05: "una sola ampliación de hasta una hora", sin superar el cierre |
| 27/10 | Cola de espera: estado nuevo **`en_espera`** (migración 03). Sin stock, la reserva entra a la cola sin fecha; cuando vuelve un ejemplar, **no sube el stock**: pasa al primero de la cola, que tiene hasta las **20:00 del día siguiente** para recogerlo | RF04 y RF16. No hay correo ni SMS (limitación del informe): el prestatario lo ve en "Mis Reservas" |
| 27/10 | Si alguien está en la cola de un libro, tampoco se amplía su préstamo ni se da de baja el libro | Ampliar haría esperar más a la cola (RF05); dar de baja dejaría la cola sin libro (RF14) |
| 27/10 | El historial del prestatario ("Mis Préstamos") se filtra **en pantalla** | Para que la consulta siga siendo de **una sola tabla** (la que pidió el profe) |
| 27/10 | Reportes: un solo generador de Excel, PDF y CSV para los 8 reportes (`TablaReporte`). El encargado también puede descargarlos; importar libros sigue siendo solo del admin | RF08 y CU11 (actor: Administrador y Encargado) |
| 27/10 | Sin fechas, los reportes y el historial no filtran: se usa del año 1900 al 3000 | PostgreSQL no acepta una fecha en `null` dentro de la consulta (`could not determine data type`) |
| 27/10 | El admin registra **encargados** desde su panel (turno `mañana` o `tarde`). Actualizar y eliminar usuarios (RF12, RF13) sigue pendiente | RF09. Los demás requisitos de usuarios son del Sprint 4 |
| 27/10 | Al iniciar sesión, el usuario tiene que estar en **un solo** perfil; si no, 403. Antes, sin perfil entraba como prestatario | "Perfil único", pendiente del informe (3.5) |
| 27/10 | La contraseña además no puede pasar de **72 bytes** | BCrypt solo usa 72 bytes; 64 caracteres con tildes o ñ podían pasarse |
| 27/10 | Un libro nuevo entra con **al menos 1 ejemplar** y siempre `disponible`. Al editar, el stock no se puede bajar a 0 a mano | RF03: sin ejemplares, el estado depende del movimiento que se llevó el último (préstamo, reserva o daño). Antes, un libro registrado con stock 0 quedaba `prestado` sin ningún préstamo. No se agregó un estado "agotado" porque la BD y el RF03 solo tienen 4 estados |

---

## Prioridad 1 – Seguridad ✅

| Cambio | Archivo(s) | Por qué |
|---|---|---|
| Se quitaron URL, usuario y contraseña de Neon escritos en el código; ahora solo se leen de variables de entorno o de `backend/.env` (`spring.config.import=optional:file:.env[.properties]`) | [application.properties](backend/src/main/resources/application.properties) | Las credenciales estaban en GitHub. **La contraseña de Neon se cambió** porque sigue en el historial de Git |
| Plantilla de variables sin valores | [backend/.env.example](backend/.env.example) | Para que cada integrante sepa qué configurar |
| Se quitó el botón ADMIN y `loginAdmin()` (tenía `admin@... / 123456` escrito) | [login.component.ts](frontend/src/app/pages/login/login.component.ts), [.html](frontend/src/app/pages/login/login.component.html), [.less](frontend/src/app/pages/login/login.component.less) | Credenciales visibles en el frontend |
| El login solo valida con **BCrypt** (se quitó la comparación en texto plano) | [AuthService.java](backend/src/main/java/com/alejandria/biblioteca/service/AuthService.java) | Una contraseña sin hash nunca debe aceptarse |
| CORS: `"*"` → `http://localhost:4200` | Todos los controllers | Solo el frontend propio puede llamar a la API |
| `.gitignore` de la raíz: ignora `.env`, `CLAUDE.md` y `.claude/` en todo el repo; permite `.env.example` | [.gitignore](.gitignore), [backend/.gitignore](backend/.gitignore) | Que las reglas apliquen a todas las carpetas |

---

## Prioridad 2 – Login y autorización ✅

### Backend

| Archivo | Qué hace |
|---|---|
| [seguridad/ValidadorContrasena.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/ValidadorContrasena.java) | Regex: 8–64 caracteres, una minúscula, una mayúscula, un número y un símbolo especial |
| [service/SesionService.java](backend/src/main/java/com/alejandria/biblioteca/service/SesionService.java) | Crea el token (UUID), lo guarda en memoria con usuario, rol y vencimiento; cada petición renueva los 15 min; lo valida y lo invalida (logout) |
| [seguridad/RolesPermitidos.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/RolesPermitidos.java) | Anotación para marcar qué roles pueden usar un endpoint. Sin anotación, el endpoint es público |
| [seguridad/AutorizacionInterceptor.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/AutorizacionInterceptor.java) | Se ejecuta antes de cada endpoint: sin token o con token vencido responde **401**; con un rol no permitido responde **403** |
| [seguridad/WebConfig.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/WebConfig.java) | Registra el interceptor para `/api/**` |
| [seguridad/Rol.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/Rol.java), [Sesion.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/Sesion.java) | Constantes de roles y datos de una sesión |
| [service/AuthService.java](backend/src/main/java/com/alejandria/biblioteca/service/AuthService.java) | Lógica de login, registro y logout (antes estaba en el controller). Detecta el rol buscando al usuario en `administrador` → `encargado` → si no, `PRESTATARIO`. Rechaza a los usuarios `inactivo` |
| [controller/AuthController.java](backend/src/main/java/com/alejandria/biblioteca/controller/AuthController.java) | Solo recibe y responde. Nuevo endpoint `POST /api/auth/logout` |
| [exception/NegocioException.java](backend/src/main/java/com/alejandria/biblioteca/exception/NegocioException.java), [ManejadorErrores.java](backend/src/main/java/com/alejandria/biblioteca/exception/ManejadorErrores.java) | Los errores del negocio llegan al frontend como `{"mensaje": "..."}` con su código HTTP |
| [dao/UsuarioDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/UsuarioDao.java) | Consultas `esAdministrador`, `esEncargado`, `esPrestatario`, `registrarComoPrestatario` (reemplazan el `JdbcTemplate` del controller) |
| [dto/LoginRespuestaDTO.java](backend/src/main/java/com/alejandria/biblioteca/dto/auth/LoginRespuestaDTO.java) | La respuesta del login: token, fecha de vencimiento y datos del usuario |

**Permisos de los endpoints existentes:**
- `POST /api/libros`: solo Administrador.
- `GET /api/reservas`: Administrador y Encargado.
- Crear y cancelar reservas: Encargado y Prestatario; **un prestatario solo puede tocar sus propias reservas**.
- Los `GET` de libros son públicos.

### Frontend

| Archivo | Qué hace |
|---|---|
| [services/auth.service.ts](frontend/src/app/services/auth.service.ts) | Guarda token, vencimiento y usuario en `localStorage` (clave `sesion_alejandria`, antes `usuario_luz_del_saber`). `rutaInicio(rol)` indica la pantalla de cada perfil |
| [interceptors/auth.interceptor.ts](frontend/src/app/interceptors/auth.interceptor.ts) | Agrega `Authorization: Bearer <token>` a cada petición. Si el backend responde 401, cierra la sesión y manda al login |
| [guards/rol.guard.ts](frontend/src/app/guards/rol.guard.ts) | Bloquea rutas por rol. Sin sesión → `/login`. Con otro rol → la pantalla de su perfil |
| [app.routes.ts](frontend/src/app/app.routes.ts) | `/catalogo` (Prestatario), `/admin` (Administrador), `/encargado` (Encargado) |
| [pages/admin-panel/](frontend/src/app/pages/admin-panel/), [pages/encargado-panel/](frontend/src/app/pages/encargado-panel/) | Pantallas base de cada perfil (se completan en la prioridad 3) |
| [pages/register/](frontend/src/app/pages/register/) | Muestra los requisitos de la contraseña con ✓ / ✗ mientras se escribe y no deja enviar si falta alguno |
| [components/header/](frontend/src/app/components/header/) | "Mi panel" lleva a la pantalla de cada rol. "Cerrar sesión" invalida el token y manda al login |

### Base de datos
- Los usuarios de prueba (`admin@`, `encargado@`, `alumno@alejandria.edu.pe`) tienen una contraseña antigua que **no cumple la regla**. Se pensaba que se había cambiado con un `UPDATE` en Neon, pero el 21/10 se comprobó que **no se cambió**. Login funciona igual, porque la regla solo se exige al registrarse. **Pendiente B7** en `PLAN_CODIGO.md`.
- Herramienta para arreglarlo: [backend/herramientas/cambiar-contrasena.cmd](backend/herramientas/cambiar-contrasena.cmd) (el código está en [CambiarContrasenaPrueba.java](backend/herramientas/CambiarContrasenaPrueba.java); no es parte del sistema). Pide la contraseña nueva sin mostrarla, revisa que cumpla la regla y pone el **mismo hash BCrypt** a los 3 usuarios de prueba, en Neon y en `schema_postgresql.sql`. No toca a los prestatarios que se registraron. Con `--probar` solo revisa la conexión y cuenta usuarios y hashes, sin cambiar nada. 21/10: el comentario del script ahora se reemplaza aunque git lo baje con saltos `\r\n` (Windows).
- Primera ejecución (21/10): cambió el hash en Neon, pero falló al comprobarlo (`NoClassDefFoundError: org/springframework/util/StringUtils`): `matches` de BCrypt necesita `spring-core` y el `.cmd` no lo incluía, así que el script **no** se actualizó. Arreglo: se agregó `spring-core` a la lista de jars del `.cmd` (comprobado: con esa librería `matches` funciona y sin ella sale el mismo error) y ahora el cambio en Neon va en una **transacción**: si la comprobación falla, se deshace y Neon queda como estaba. Se volvió a ejecutar y salió bien: **B7 terminado** (ver "Sustentación – guion y contraseña de prueba").

---

## Prioridad 3 – CRUD de préstamos ✅ (falta probarlo completo en la web)

### 3.1 Base de datos y backend de préstamos ✅

**Migración** [database/migraciones/migracion_01_prestamo.sql](database/migraciones/migracion_01_prestamo.sql), ejecutada en Neon el 21/10 (también aplicada en [schema_postgresql.sql](database/schema_postgresql.sql)):
- `prestamo.ampliado BOOLEAN NOT NULL DEFAULT FALSE`
- `prestamo.estado_fisico_devolucion` (`bueno` / `dañado`)
- `estado_prestamo` acepta además `anulado`
- `estado_libro` acepta además `baja`

**Archivos nuevos:**

| Archivo | Qué hace |
|---|---|
| [entity/Prestamo.java](backend/src/main/java/com/alejandria/biblioteca/entity/Prestamo.java) | Entidad JPA de la tabla `prestamo` |
| [dao/PrestamoDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/PrestamoDao.java) | Consulta de **una tabla** (`buscarPorPrestatario`), consulta con **JOIN** (`listarActivosConDetalle`: prestamo + usuario + libro), `marcarVencidos` y `tieneSancionActiva` (sancion → incidencia → prestamo) |
| [service/PrestamoService.java](backend/src/main/java/com/alejandria/biblioteca/service/PrestamoService.java) | Todas las reglas del negocio (ver abajo) |
| [controller/PrestamoController.java](backend/src/main/java/com/alejandria/biblioteca/controller/PrestamoController.java) | Endpoints REST con sus permisos |
| `dto/RegistroPrestamoDTO`, `DevolucionDTO`, `PrestamoDTO`, `PrestamoDetalleDTO` | Lo que entra y sale de la API |
| [dao/LibroDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/LibroDao.java) | `buscarParaActualizar`: lee el libro bloqueando su fila para que dos préstamos simultáneos no descuadren el stock |

**Endpoints `/api/prestamos`:**

| CRUD | Endpoint | Quién |
|---|---|---|
| Adicionar | `POST /` con `{dniPrestatario, idLibro}` | Encargado (se toma de la sesión) |
| Consultar (JOIN) | `GET /activos` | Encargado, Admin |
| Consultar (1 tabla) | `GET /prestatario/{id}` | Encargado, Admin, el propio prestatario |
| Consultar | `GET /{id}` | Encargado, Admin |
| Actualizar | `PUT /{id}/ampliar` | Encargado |
| Actualizar | `PUT /{id}/devolucion` con `{estadoFisico}` | Encargado |
| Eliminar (lógico) | `DELETE /{id}` | Encargado, Admin |

**Reglas del negocio implementadas:**
- Hora límite = inicio + 3 h (`calcularHoraLimite`, método aparte para la prueba JUnit).
- Se puede ampliar **una sola vez** y solo si el préstamo no venció.
- No se presta si el DNI no existe o no es de un prestatario, si el prestatario está suspendido o inactivo, si tiene una **sanción activa**, ni si el libro está de baja o en mantenimiento, o tiene **stock 0**.
- **Stock y estado automáticos:** al prestar, el stock baja 1 y con 0 el libro pasa a `prestado`. Al devolver o anular, el stock sube 1 y el libro vuelve a `disponible`.
- Al consultar los activos, primero se marcan como `vencido` los préstamos cuya hora límite ya pasó.
- La consulta "de dos tablas" en realidad une tres (`prestamo`, `usuario`, `libro`).

> Después se refactorizó: el manejo del stock pasó a `LibroService` y la validación del prestatario a `PrestatarioService` (ver 3.3 y 3.4). `PrestamoService` ahora también atiende la reserva pendiente del prestatario.

### 🐞 Error encontrado: el catálogo no venía de la BD
`GET /api/libros` respondía **500** (`Cannot lazily initialize collection ... Libro.autores`). Los autores de un libro se cargan en modo LAZY (solo cuando se piden). Con `open-in-view=false`, la conexión ya estaba cerrada cuando Spring convertía el libro a JSON. El frontend escondía el error mostrando una **lista de 5 libros escrita a mano**: en la BD hay 4, y "Don Quijote" no existe. Lo mismo pasaba con las reservas.
**Solución:** los services convierten las entidades a DTO (`LibroDTO`, `ReservaRespuestaDTO`) **dentro de la transacción**, y se quitó la lista de respaldo del frontend. Si el backend falla, ahora se muestra el error.

### 3.2 Pantallas del Encargado ✅
[pages/encargado-panel/](frontend/src/app/pages/encargado-panel/), con estilos compartidos en [pages/panel-base.less](frontend/src/styles/panel.less) y el servicio [services/prestamo.service.ts](frontend/src/app/services/prestamo.service.ts).

| Pestaña | Qué hace | Endpoint |
|---|---|---|
| Préstamos activos | Tabla con prestatario, DNI, libro, inicio, hora límite y estado (`activo` / `vencido`). Botones **Ampliar** (se desactiva si ya se amplió), **Devolver** (modal: bueno / dañado) y **Anular** | `GET /activos` (JOIN), `PUT /{id}/ampliar`, `PUT /{id}/devolucion`, `DELETE /{id}` |
| Registrar préstamo | DNI del prestatario + libro (muestra el stock) | `POST /api/prestamos` |
| Reservas pendientes | Lista de reservas por recoger. El botón **Entregar** precarga el libro en "Registrar préstamo" | `GET /api/reservas` |

### 3.3 CRUD de libros ✅
**Backend:**

| Archivo | Qué hace |
|---|---|
| [service/LibroService.java](backend/src/main/java/com/alejandria/biblioteca/service/LibroService.java) | Listar (sin los dados de baja), buscar, crear, actualizar y **dar de baja** (estado `baja`, no se borra la fila). No permite dar de baja un libro con préstamos sin devolver ni repetir un ISBN. También tiene `bloquearParaSalida`, `descontarStock` y `reponerStock`, que usan préstamos y reservas |
| [dto/LibroDTO.java](backend/src/main/java/com/alejandria/biblioteca/dto/libro/LibroDTO.java), [LibroFormDTO.java](backend/src/main/java/com/alejandria/biblioteca/dto/libro/LibroFormDTO.java) | Respuesta (con categoría y autores) y formulario |
| [controller/LibroController.java](backend/src/main/java/com/alejandria/biblioteca/controller/LibroController.java) | `GET` públicos; `POST`, `PUT /{id}` y `DELETE /{id}` solo para el Administrador |
| [controller/CatalogoController.java](backend/src/main/java/com/alejandria/biblioteca/controller/CatalogoController.java) | `GET /api/categorias` y `GET /api/autores` para el formulario |

**Frontend:** [pages/admin-panel/](frontend/src/app/pages/admin-panel/)
- Pestaña **Libros**: formulario para registrar o editar (título, ISBN, editorial, año, stock, categoría y autores) y una tabla con **Editar** y **Dar de baja**.
- Pestaña **Préstamos activos**: la misma consulta con JOIN, solo para ver.

### 3.4 Reservas conectadas al backend ✅
**Backend:**

| Archivo | Qué hace |
|---|---|
| [service/ReservaService.java](backend/src/main/java/com/alejandria/biblioteca/service/ReservaService.java) | Crear (valida el prestatario, que no tenga otra reserva pendiente del mismo libro y la fecha de recojo; descuenta stock), cancelar (repone stock), `liberarVencidas` y `atenderSiExiste` (lo usa el préstamo) |
| [service/PrestatarioService.java](backend/src/main/java/com/alejandria/biblioteca/service/PrestatarioService.java) | `validarHabilitado`: es prestatario, está activo y no tiene sanción activa (la usan préstamos y reservas) |
| [dto/ReservaRespuestaDTO.java](backend/src/main/java/com/alejandria/biblioteca/dto/reserva/ReservaRespuestaDTO.java) | Reserva con título, autor, categoría y nombre del prestatario |
| [controller/ReservaController.java](backend/src/main/java/com/alejandria/biblioteca/controller/ReservaController.java) | Ahora solo llama al service (antes tenía la lógica adentro) |

**Frontend:** [pages/user-catalog/](frontend/src/app/pages/user-catalog/)
- Se quitó la reserva escrita a mano ("La Ciudad y los Perros") y la lista de libros de respaldo.
- "Mis Reservas" se carga del backend (solo las pendientes).
- Reservar envía la fecha límite de recojo (fecha + fin del turno). Si el backend rechaza la reserva, el error se muestra en el modal; antes siempre decía "Reserva realizada".
- Cancelar llama al backend.
- Los libros con stock 0 muestran **Agotado** y no se pueden reservar.
- La fecha de recojo por defecto es hoy (antes estaba fija en 24/09/2026).

---

## Prioridad 4 – Librerías y pruebas ✅

**Dependencias nuevas** en [pom.xml](backend/pom.xml) (versión fija para que todo el grupo use la misma):

| Librería | Versión | Para qué |
|---|---|---|
| Google Guava | 33.4.8-jre | Bloquear el login tras varios intentos fallidos |
| Apache POI (`poi-ooxml`) | 5.4.1 | Reporte de préstamos en Excel |
| Apache Commons CSV | 1.14.0 | Exportar préstamos e importar libros en CSV |
| OpenPDF | 2.0.3 | Reporte de préstamos en PDF |
| Spring Boot Actuator | (versión de Spring Boot) | Monitoreo: salud, memoria, CPU, peticiones, BD |
| Logback | (viene con Spring Boot) | Logs en consola y archivo |
| JUnit 5 + Mockito | (vienen con `spring-boot-starter-webmvc-test`) | Pruebas unitarias |

### Logback ✅
- [logback-spring.xml](backend/src/main/resources/logback-spring.xml): consola + archivo `backend/logs/biblioteca.log`, un archivo por día, se guardan 30 días. La carpeta `logs/` está en el `.gitignore`.
- Qué se registra:
  - `AuthService`: login correcto, **login fallido (n/3)**, login bloqueado, usuario inactivo y nuevo registro. **Nunca se registra la contraseña.**
  - `PrestamoService`: préstamo registrado (avisa si venía de una reserva), ampliado, devuelto (avisa **CON RETRASO** si pasó la hora límite) y anulado.
  - `ReservaService`: creada, cancelada y vencida.
  - `ReporteService`: reportes generados e importaciones.

### Guava ✅
- [seguridad/LimitadorIntentosLogin.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/LimitadorIntentosLogin.java): un `Cache` de Guava guarda `correo → intentos fallidos` y **borra la entrada solo a los 15 minutos** (`expireAfterWrite`). Con **3 fallos**, el login responde **429** "Demasiados intentos fallidos". Un login correcto reinicia el contador.
- Probado en vivo: 3 intentos → 401, el 4° → 429, y todo queda en el log (antes eran 5; se cambió al 3 del RF10).

### Apache POI y Commons CSV ✅
- [service/ReporteService.java](backend/src/main/java/com/alejandria/biblioteca/service/ReporteService.java):
  - `generarExcelPrestamos()`: hoja "Préstamos" con título, fecha de generación, encabezado con color, columnas ajustadas y el encabezado fijo al bajar.
  - `generarCsvPrestamos()`: CSV en UTF-8 con BOM, para que Excel muestre bien las tildes.
  - `importarLibrosCsv()`: lee un CSV con columnas `titulo,isbn,anio_publicacion,editorial,stock,categoria` (acepta `,` o `;`). Cada fila se guarda por separado: si una falla, las demás igual se registran, y se informa el número de fila y el motivo.
- [dao/PrestamoDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/PrestamoDao.java): nueva consulta `listarParaReporte` (JOIN prestamo + prestatario + encargado + libro). Su resultado es [dto/ReportePrestamoDTO.java](backend/src/main/java/com/alejandria/biblioteca/dto/reporte/ReportePrestamoDTO.java).
- [controller/ReporteController.java](backend/src/main/java/com/alejandria/biblioteca/controller/ReporteController.java), solo Administrador: `GET /api/reportes/prestamos/excel`, `GET /api/reportes/prestamos/csv` y `POST /api/reportes/libros/csv` (archivo).
- Frontend: pestaña **Reportes** en [admin-panel](frontend/src/app/pages/admin-panel/) y [services/reporte.service.ts](frontend/src/app/services/reporte.service.ts). La descarga se pide con HttpClient para que viaje el token.
- Archivo de ejemplo para la demo: [database/ejemplo_importar_libros.csv](database/ejemplo_importar_libros.csv).

### JUnit ✅ (34 pruebas en la prioridad 4; ahora son 65, ver "Depuración – parte 1")

| Clase de prueba | Qué prueba |
|---|---|
| [ValidadorContrasenaTest](backend/src/test/java/com/alejandria/biblioteca/seguridad/ValidadorContrasenaTest.java) | 13 casos de la regla de contraseña: válidas, cada requisito que falta, 64/65 caracteres, nulo |
| [LimitadorIntentosLoginTest](backend/src/test/java/com/alejandria/biblioteca/seguridad/LimitadorIntentosLoginTest.java) | Bloqueo al 5° intento, desbloqueo a los 15 min (con un reloj falso), reinicio al acertar, contador por correo |
| [PrestamoServiceTest](backend/src/test/java/com/alejandria/biblioteca/service/PrestamoServiceTest.java) | **Cálculo de 3 horas** (incluso pasando la medianoche), ampliar solo una vez y no vencido, devolución (estado físico, stock, no devolver dos veces), anular sin borrar la fila |
| [ReporteServiceTest](backend/src/test/java/com/alejandria/biblioteca/service/ReporteServiceTest.java) | El Excel se puede abrir y tiene los datos; el PDF se genera (empieza con `%PDF`); el CSV tiene encabezado y filas; la importación crea las filas válidas y reporta las inválidas; acepta `;`; rechaza columnas incorrectas |
| `BibliotecaApplicationTests` (ya existía) | Spring arranca y se conecta a la BD |

Las pruebas nuevas usan **Mockito** (DAOs simulados), así que no tocan la base de datos.

### OpenPDF ✅ (`com.github.librepdf:openpdf:2.0.3`, elegida por el equipo el 21/10)
- `ReporteService.generarPdfPrestamos()`: A4 horizontal, con título, fecha y total, y una tabla de 11 columnas con encabezado color marrón (el del sistema) que se repite en cada página.
- `GET /api/reportes/prestamos/pdf` (Admin) y el botón **Descargar PDF** en la pestaña Reportes.

### Monitoreo con Spring Boot Actuator ✅ (elegido por el equipo el 21/10)
- Dependencia `spring-boot-starter-actuator`. Configuración en [application.properties](backend/src/main/resources/application.properties):
  - Actuator corre en el **puerto 8081**, solo accesible desde la misma PC (`127.0.0.1`), separado de la API.
  - Endpoints expuestos: `/actuator/health` (estado de la app, la **BD** y el disco), `/actuator/info` (nombre y versión) y `/actuator/metrics` (todas las métricas).
- [service/MonitoreoService.java](backend/src/main/java/com/alejandria/biblioteca/service/MonitoreoService.java) junta las métricas de Micrometer en un resumen:
  - **Técnicas:** estado (UP/DOWN), tiempo activo, memoria heap usada y máxima, CPU, hilos, peticiones HTTP (total, con error, tiempo promedio) y conexiones a la BD (HikariCP).
  - **Del negocio:** sesiones activas, préstamos activos y vencidos, reservas pendientes.
- `GET /api/monitoreo` (solo Admin) y la pestaña **Monitoreo** en el panel del Administrador, con una tabla que explica cada métrica.
- Probado en vivo: health `UP` con PostgreSQL `UP`; `/api/monitoreo` y el PDF sin token → 401.
- Detalle técnico: `info.app.nombre` va **sin tildes**. Spring lee `application.properties` en ISO-8859-1, y Maven convierte `é` otra vez en `é` al compilar, así que la tilde se vería mal.

---

## Prioridad 5 – Limpieza y coherencia ✅

| Cambio | Archivo(s) | Por qué |
|---|---|---|
| Clave `usuario_luz_del_saber` → `sesion_alejandria` | [auth.service.ts](frontend/src/app/services/auth.service.ts) | Ya se había hecho en la prioridad 2; se verificó que no quede en ningún archivo |
| README nuevo en la raíz: estructura real (`controller → service → dao`), perfiles, cómo levantar, `.jar`, pruebas, endpoints, librerías y monitoreo | [README.md](README.md) | No había README del proyecto (solo el de plantilla de Angular) |
| Comentario del script: ya no dice `'123456'` ni muestra contraseñas; aclara que en Neon el hash se cambió con un `UPDATE` | [schema_postgresql.sql](database/schema_postgresql.sql) | No escribir contraseñas en el repositorio |
| El `.jar` se genera como `target/biblioteca-alejandria.jar` | [pom.xml](backend/pom.xml) | Nombre del proyecto en vez de `demo` |
| `.gitignore`: se ignora `.angular/` de la raíz, el PDF del Avance III y los `.txt` de `docs/` | [.gitignore](.gitignore) | La caché `.angular/` se subió por error en el commit del CRUD |

**Prueba del `.jar` (21/10):** `./mvnw clean package` → 34 pruebas OK, BUILD SUCCESS. Se ejecutó `java -jar target/biblioteca-alejandria.jar` desde `backend/`: arrancó en 9 s, `/actuator/health` → `UP` (BD PostgreSQL `UP`), `GET /api/libros` → 200 y el PDF sin token → 401. El frontend compila con `ng build`.

**Para el próximo commit:** la caché `.angular/` sigue en el repositorio aunque ya está en el `.gitignore`. Hay que sacarla con `git rm -r --cached .angular` (no borra la carpeta de la PC).

---

## Depuración – parte 1 (21/10) ✅

### Orden de carpetas
Lo que pide Maven/Spring Boot (`src/main/java`, `src/main/resources`, `src/test`, `pom.xml`) y la estructura de Angular (`src/app`, `angular.json`) **no se tocaron**. Solo se ordenó lo de adentro.

| Cambio | Antes | Ahora | Por qué |
|---|---|---|---|
| Paquete raíz | `com.backend.demo` | `com.alejandria.biblioteca` | "demo" era el nombre de la plantilla de Spring |
| Clase principal | `DemoApplication` | `BibliotecaApplication` | Mismo motivo. También `groupId` y `artifactId` del `pom.xml` y `spring.application.name` |
| DTO | 15 archivos sueltos en `dto/` | `dto/auth`, `usuario`, `libro`, `prestamo`, `reserva`, `reporte` | Encontrar rápido los de cada módulo |
| Panel del admin | 1 componente con 4 pestañas (~430 líneas) | `admin-panel/tabs/` libros, prestamos, reportes, monitoreo | Cada pestaña en su archivo; ahí van a ir Usuarios y Dashboard |
| Panel del encargado | 1 componente con 3 pestañas | `encargado-panel/tabs/` activos, registrar, reservas | Igual |
| Estilos de los paneles | `pages/panel-base.less` | [styles/panel.less](frontend/src/styles/panel.less) (global) | Los estilos de un componente no llegan a sus hijos (las pestañas) |
| Mensaje de error | Copiado en 3 archivos | [utils/mensaje-error.ts](frontend/src/app/utils/mensaje-error.ts) | No repetir código |

> Si VS Code marca errores en rojo después de mover los paquetes: `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace**. El código compila bien con `./mvnw`.

### Errores arreglados (primero la prueba, después el arreglo)

| ID | Error | Arreglo | Prueba |
|---|---|---|---|
| B1 | **Todo préstamo nuevo salía vencido.** La BD de Neon está en hora GMT (5 h adelante) y `marcarVencidos` usaba `NOW()` | La hora la manda Java: `marcarVencidos(LocalDateTime.now())` | [PrestamoDaoTest](backend/src/test/java/com/alejandria/biblioteca/dao/PrestamoDaoTest.java): contra la BD real, falló con *expected activo but was vencido* y después pasó |
| B2 | Una sanción que terminaba hoy dejaba de valer desde las 19:00 (`CURRENT_DATE` de la BD ya era mañana) | `tieneSancionActiva(id, LocalDate.now())` | Misma clase |
| B3 | Se podía dar de baja un libro con reservas pendientes (el RF14 no lo permite) | `LibroService.darDeBaja` revisa `ReservaDao` | [LibroServiceTest](backend/src/test/java/com/alejandria/biblioteca/service/LibroServiceTest.java) |
| B4 | El registro no validaba los datos: un DNI de 9 dígitos daba error 500 | [ValidadorUsuario](backend/src/main/java/com/alejandria/biblioteca/seguridad/ValidadorUsuario.java): DNI de 8 dígitos, correo bien escrito, largos de la BD. También en el frontend; la pantalla dice "DNI" (antes "DNI / CDE") | [ValidadorUsuarioTest](backend/src/test/java/com/alejandria/biblioteca/seguridad/ValidadorUsuarioTest.java) |
| B5 | Los errores que no eran del negocio llegaban sin mensaje | [ManejadorErrores](backend/src/main/java/com/alejandria/biblioteca/exception/ManejadorErrores.java) atrapa todos: JSON mal escrito (400), dato inválido en la ruta (400), rechazo de la BD (409), ruta inexistente (404), cualquier otro (500, el detalle técnico solo va al log) | [ManejadorErroresTest](backend/src/test/java/com/alejandria/biblioteca/exception/ManejadorErroresTest.java) |

Las pruebas que usan la BD real corren dentro de una transacción que se deshace al terminar: se verificó que la BD siguió con 0 préstamos, 0 incidencias y 0 sanciones.

### Reglas alineadas con el informe (manda el informe)

| ID | Antes | Ahora | Dónde |
|---|---|---|---|
| C8 | La ampliación sumaba 3 h | Suma **1 h** y no se permite si el libro tiene **reservas pendientes** (RF05) | `PrestamoService.ampliar`, `ReservaService.hayReservasPendientes` |
| C10 | Bloqueo a los 5 intentos | Bloqueo a los **3 intentos** (RF10) | `LimitadorIntentosLogin.MAX_INTENTOS` |
| C11 | El token duraba 8 h fijas | Se cierra tras **15 min sin actividad**; cada petición renueva el plazo (RNF04). El frontend lleva la misma cuenta | [SesionService](backend/src/main/java/com/alejandria/biblioteca/service/SesionService.java), `app.sesion.inactividad-minutos=15`, [auth.interceptor.ts](frontend/src/app/interceptors/auth.interceptor.ts) |

### Limpieza

| ID | Cambio |
|---|---|
| D1 | La sección "Libros de Mario Vargas Llosa" del inicio ya no tiene libros escritos a mano (3 no existían en la BD); muestra los reales o un mensaje |
| D2 | Login: dice "Ingresa tu correo" (antes "Usuario o Correo") y "¿Olvidaste tu contraseña?" ya no es un enlace que no hacía nada |
| D3 | `CatalogoController` ahora pasa por `LibroService` y devuelve DTO (antes llamaba al DAO y devolvía entidades) |
| D4 | Se borraron 6 métodos de DAO que nadie usaba |
| D5 | Se quitaron los `@Autowired` y `@Repository` innecesarios |
| D6 | CORS en un solo lugar: [WebConfig](backend/src/main/java/com/alejandria/biblioteca/seguridad/WebConfig.java) (antes `@CrossOrigin` repetido en 7 controllers) |
| D7 | `spring.jpa.hibernate.ddl-auto=validate`: Hibernate solo revisa que las entidades coincidan con las tablas; ya no cambia la BD |
| D12 | En el script, `dni VARCHAR(8)` (igual que en Neon) |

### Pruebas: de 34 a 65, todas pasan
Nuevas: `PrestamoDaoTest` (3, contra la BD real), `LibroServiceTest` (3), `ValidadorUsuarioTest` (14), `ManejadorErroresTest` (5), `SesionServiceTest` (4), +1 en `PrestamoServiceTest` (reservas al ampliar) y +1 en `LimitadorIntentosLoginTest` (tercer intento).

### Prueba completa con los 3 perfiles (B6, 21/10)
Se hizo por la API, con las mismas peticiones que hace la web:

| Paso | Resultado |
|---|---|
| Login de admin, encargado y prestatario | ✅ cada uno con su rol; la sesión vence 15 min después |
| Encargado registra un préstamo | ✅ hora límite = inicio + 3 h |
| Préstamos activos | ✅ sale **activo** (antes del arreglo B1 salía vencido) |
| Ampliar | ✅ +1 h; la segunda vez → 409 "ya fue ampliado" |
| Ampliar con una reserva pendiente del libro | ✅ 409 "el libro tiene reservas pendientes" |
| Admin da de baja un libro con reserva | ✅ 409 "el libro tiene reservas pendientes" |
| Stock automático | ✅ al prestar o reservar baja; con 0 el libro pasa a `prestado` / `reservado`; al devolver, anular o cancelar vuelve |
| Anular préstamo | ✅ queda `anulado` (no se borra) y repone el stock |
| Prestatario: ver todas las reservas / préstamos de otro / registrar préstamo | ✅ 403 en los tres |
| Prestatario: ver sus préstamos (consulta de una tabla), reservar y cancelar | ✅ |
| Devolución "bueno" y "dañado" | ✅ (ver nota) |
| Reportes Excel, PDF y CSV (admin) | ✅; el encargado → 403 |
| Monitoreo | ✅ estado UP |
| Token después del logout | ✅ 401 |
| Stock al final | ✅ igual al inicial (3, 2, 1, 2) |

Al terminar se borraron los préstamos y reservas de prueba: la BD quedó con 0 préstamos y 0 reservas, como estaba.

> **Nota sobre la ñ:** si se prueba con `curl` desde la terminal de Windows, `"dañado"` llega mal codificado y el backend responde 400 "Los datos enviados tienen un formato incorrecto". **No es un error del sistema**: el navegador manda UTF-8 y con UTF-8 funciona.

### Prueba en vivo con el `.jar` (21/10)
Categorías como DTO ✅ · CORS: el frontend pasa y otro sitio recibe 403 ✅ · ruta inexistente → 404 con mensaje ✅ · `/api/libros/abc` → 400 ✅ · registro con DNI de 9 dígitos → 400 "El DNI debe tener 8 dígitos" ✅ · JSON mal escrito → 400 ✅ · 3 logins fallidos → 401 y el 4° → 429 ✅ · sin token → 401 ✅.

---

## Depuración – parte 2 (21/10): eliminación física ✅

El profe preguntó "¿dónde está la eliminación física?". Se hizo con las **reservas canceladas o vencidas** (RF17, actor: Administrador).

| | Lógica | Física |
|---|---|---|
| Qué hace | Cambia el estado (`UPDATE`) | Borra la fila (`DELETE`) |
| Dónde | Libros → `baja`, préstamos → `anulado`, reservas → `cancelada` | Reservas canceladas o vencidas |
| Endpoint | `DELETE /api/libros/{id}`, `DELETE /api/prestamos/{id}`, `PUT /api/reservas/{id}/cancelar` | `DELETE /api/reservas/{id}` (solo admin) |
| Por qué | Son historial: los préstamos apuntan al libro y las incidencias al préstamo; los reportes los necesitan | Una reserva cancelada o vencida ya no sirve: su ejemplar volvió al stock y ningún préstamo la necesita |

**Reglas:** solo se borran las `cancelada` o `vencida`. Una `pendiente` no (alguien va a recoger el libro) y una `atendida` tampoco (es parte de un préstamo). Si una pendiente ya pasó su hora de recojo, primero pasa a `vencida`.

| Archivo | Qué se agregó |
|---|---|
| [ReservaService.java](backend/src/main/java/com/alejandria/biblioteca/service/ReservaService.java) | `eliminar(id)`: revisa el estado y llama a `reservaDao.delete(...)`, que hace el `DELETE FROM reserva` |
| [ReservaController.java](backend/src/main/java/com/alejandria/biblioteca/controller/ReservaController.java) | `DELETE /api/reservas/{id}` con `@RolesPermitidos(Rol.ADMINISTRADOR)` |
| [reserva.service.ts](frontend/src/app/services/reserva.service.ts) | `eliminar(id)` |
| [admin-panel/tabs/reservas/](frontend/src/app/pages/admin-panel/tabs/reservas/) | Pestaña **Reservas** del admin: todas las reservas, filtro "Canceladas y vencidas" y botón **Eliminar** (con confirmación) solo en esas |

**Pruebas (primero la prueba, después el código):**
- [ReservaServiceTest](backend/src/test/java/com/alejandria/biblioteca/service/ReservaServiceTest.java) (5): borra canceladas y vencidas; no borra pendientes ni atendidas; 404 si no existe.
- [ReservaEliminacionFisicaTest](backend/src/test/java/com/alejandria/biblioteca/service/ReservaEliminacionFisicaTest.java) (1, contra la BD real): la fila **desaparece de la tabla**: `COUNT(*)` pasa de 1 a 0. Se deshace al terminar.
- Total: **71 pruebas**, todas pasan.

**Prueba en vivo (21/10):** pendiente → 409 · cancelar (lógica) → 200 · el encargado intenta borrar → 403 · el admin borra → 200 · borrar otra vez → 404 "Reserva no encontrada". En la BD la fila ya no existe y el log muestra *"Reserva #5 (cancelada) eliminada físicamente de la BD"*. La BD quedó como estaba.

---

## Depuración – parte 3 (21/10): "Mis préstamos" (consulta de una tabla) ✅

El profe pidió ver dónde se consulta **una tabla** y qué campos se muestran. La consulta ya existía (`PrestamoDao.buscarPorPrestatario`), pero no tenía pantalla.

**La consulta:** `SELECT * FROM prestamo WHERE id_prestatario = ? ORDER BY fecha_prestamo DESC`. Solo usa la tabla `prestamo`.

**Qué se muestra (una parte de la tabla):** N°, inicio, hora límite, fecha de devolución, ampliado, estado físico y estado del préstamo.
**Qué no se muestra:** `id_prestatario` (es el mismo usuario) e `id_encargado`.
**El título del libro** no viene en la consulta: el frontend lo busca por `id_libro` en el catálogo que ya tiene cargado. Así la consulta sigue siendo de una sola tabla.

| Archivo | Qué cambió |
|---|---|
| [user-catalog](frontend/src/app/pages/user-catalog/) | Opción **Mis Préstamos** en el menú del prestatario, con una tabla y el número de préstamos en curso. Los anulados no se muestran (eliminación lógica) |
| [PrestamoService.java](backend/src/main/java/com/alejandria/biblioteca/service/PrestamoService.java) | `listarPorPrestatario` primero marca los vencidos; antes, un préstamo pasado de hora aparecía como "activo" en esta consulta |

**Pruebas:** `elHistorialDelPrestatarioPrimeroMarcaLosVencidos` (falló antes del cambio) y `laConsultaDeUnaTablaSoloTraeLosPrestamosDeEsePrestatario` (contra la BD real). Total: **73 pruebas**, todas pasan.

**Prueba en vivo (21/10):** el encargado registra un préstamo → el prestatario lo ve "activo" → se devuelve → lo ve "devuelto", con fecha y estado físico. El préstamo de prueba se borró al final.

---

## Depuración – parte 4 (21/10): alertas de 15 min y devolución dañada ✅

### C9 – Alertas en el panel del encargado (RF05)
"El sistema mostrará alertas visuales en el panel del encargado cuando falten 15 minutos para cumplirse las tres horas o cuando el préstamo esté vencido."

- [utils/alerta-prestamo.ts](frontend/src/app/utils/alerta-prestamo.ts): una función que, con la hora límite y la hora actual, dice si el préstamo está **a tiempo**, **por vencer** (15 min o menos) o **vencido**, y arma el texto ("Faltan 12 min", "Venció hace 5 min").
- [Pestaña Préstamos activos](frontend/src/app/pages/encargado-panel/tabs/activos/):
  - arriba, un aviso rojo con cuántos están vencidos y uno amarillo con cuántos vencen en 15 min o menos;
  - cada fila se pinta de rojo o amarillo y muestra cuánto falta;
  - la pantalla recalcula las alertas sola cada 30 segundos y tiene un botón **Actualizar**;
  - el botón Ampliar se desactiva si ya venció.
- **Primera prueba del frontend (TDD):** [alerta-prestamo.spec.ts](frontend/src/app/utils/alerta-prestamo.spec.ts), 7 casos (2 h, 16 min, 15 min, 1 min, vencido, recién vencido, fecha del backend). Falló porque la función no existía y después pasó. Se corren con `npx ng test --watch=false` (desde `frontend/`).

### C12 – Devolución dañada (RF02)
"Al cerrar, el ejemplar pasa a Disponible o a En mantenimiento si está dañado."

El sistema guarda **títulos con stock**, no cada ejemplar por separado:
- **bueno** → el ejemplar vuelve al stock;
- **dañado** → el ejemplar **no** vuelve al stock (se va a reparar). Si era el último, el libro pasa a `mantenimiento` y no se puede prestar;
- cuando está reparado, el admin **le sube el stock** al editar el libro y vuelve a `disponible`. El formulario lo indica.

| Archivo | Qué cambió |
|---|---|
| [LibroService.java](backend/src/main/java/com/alejandria/biblioteca/service/LibroService.java) | Nuevo `registrarEjemplarDanado`. `reponerStock` y `actualizar` ahora también sacan al libro de `mantenimiento` (antes se quedaba atascado aunque volviera a tener stock: **lo encontró la prueba**) |
| [PrestamoService.java](backend/src/main/java/com/alejandria/biblioteca/service/PrestamoService.java) | `devolver`: si es "dañado" llama a `registrarEjemplarDanado`; si es "bueno", a `reponerStock` |
| Modal de devolución | Si se elige "Dañado", avisa que el ejemplar no vuelve al stock. El mensaje final también avisa si **se devolvió con retraso** |

**Pruebas:** 6 nuevas en `PrestamoServiceTest` y `LibroServiceTest`. Total backend: **78**; frontend: **8**.

**Prueba en vivo (21/10, *Cien Años de Soledad*, 1 ejemplar):** prestado → stock 0 `prestado` · devuelto dañado → stock 0 `mantenimiento` · prestarlo otra vez → 409 "El libro no está disponible" · el admin le sube el stock a 1 → `disponible`, con su autor intacto. El préstamo de prueba se borró.

> La incidencia por daño o retraso (RF06) todavía no se registra: es del Sprint 4 (C13).

---

## Depuración – parte 5 (21/10): monto de multas, libros reales y dashboard ✅

### C20 – Columna `sancion.monto` (RF06)
- [database/migraciones/migracion_02_sancion_monto.sql](database/migraciones/migracion_02_sancion_monto.sql): `monto DECIMAL(8,2) NULL` + `CHECK (monto IS NULL OR monto >= 0)`. **Ejecutada en Neon el 21/10** y agregada a [schema_postgresql.sql](database/schema_postgresql.sql).
- Pruebas contra la BD real: una multa guarda su monto (15.50) y un monto negativo es rechazado por la BD. Antes de la migración fallaron con *column "monto" does not exist*.

### D11 – 21 libros reales con su portada
- [database/datos_demo.sql](database/datos_demo.sql): 21 libros y 26 autores. Se puede ejecutar varias veces sin duplicar. **Ejecutado en Neon el 21/10**: ahora hay **25 libros, 30 autores y 48 ejemplares**.
- Los datos (ISBN, editorial y año **de esa edición**) se verificaron en **Open Library** (openlibrary.org). Ejemplos: *7 ensayos de interpretación de la realidad peruana* (Mariátegui), *Los ríos profundos* (Arguedas), *Pedro Páramo*, *Rayuela*, *Sapiens*, *Breve historia del tiempo*, *Design Patterns*, *Introduction to Algorithms*, *English Grammar in Use*. Hay de las 5 categorías.
- **Portadas:** el frontend las pide a Open Library con el ISBN ([utils/portada.ts](frontend/src/app/utils/portada.ts)). No se guarda nada nuevo en la BD; si un libro no tiene portada, sale una por defecto. Se ven en el inicio, el catálogo, Mis Reservas y la tabla de libros del admin. Necesitan internet, igual que la BD.
- **Errores en los datos originales** (los encontraron las portadas):
  - *La Ciudad y los Perros* tenía el ISBN `9788420471839`, que es de *Cien años de soledad*. Ahora tiene `9788432221026` (Seix Barral, 1984).
  - *Cien años de soledad* decía "Sudamericana 1967", pero su ISBN es de Vintage Español 2009.
  - *Core Java* decía 2020 y su edición es de 2018.

  Se corrigieron en Neon y en el script.
- [ejemplo_importar_libros.csv](database/ejemplo_importar_libros.csv) también tenía datos mal puestos (un ISBN inglés con título en español). Ahora trae 3 libros verificados que no están en la carga inicial (*Clean Architecture*, *El Principito*, *Una muy breve historia de casi todo*), para que la demo de importar muestre libros nuevos.
- Los autores de cada libro salen siempre en el mismo orden (el real: Gamma, Helm, Johnson, Vlissides). Los de varios autores se ven en el catálogo como "Erich Gamma y otros".

### C18 – Dashboard del administrador (Bootstrap)
Es la **primera pestaña** del panel del admin. Patrón Dashboard del informe (2.1.3.6).

| Parte | Qué muestra |
|---|---|
| Tarjetas | Títulos y ejemplares, préstamos en sala, vencidos, reservas pendientes, préstamos del mes, prestatarios |
| **Indicadores de gestión (1.2.2)** | **PDF** (devoluciones fuera de plazo) y **TIM** (incidencias): se calculan con los préstamos del mes y dicen si cumplen la meta. **TAP** e **IPI** muestran "Sin dato" y explican qué falta: la hora de inicio de la atención y la auditoría física. No se inventan valores |
| Barras | Libros por estado, préstamos del mes por categoría, préstamos de los últimos 7 días |

- Backend: [DashboardService](backend/src/main/java/com/alejandria/biblioteca/service/DashboardService.java) + [IndicadoresGestion](backend/src/main/java/com/alejandria/biblioteca/service/IndicadoresGestion.java) (las fórmulas) + `GET /api/dashboard` (solo admin). Las fechas del mes las manda Java (hora de Lima).
- **Bootstrap 5.3.8** (versión fija en `package.json`). Está **encerrado** en [styles/bootstrap-dashboard.less](frontend/src/styles/bootstrap-dashboard.less): todas sus reglas quedan dentro de `.dashboard-bootstrap`, así que **no cambia el diseño de las otras pantallas**, que siguen hechas con LESS. Usa el marrón del sistema en vez del azul de Bootstrap. Se adapta al celular.
- Avisos conocidos del build:
  - `"@charset" must be the first rule`: la regla de Bootstrap queda dentro del bloque y el navegador la ignora. Es inofensivo.
  - Se subió el límite de aviso del tamaño inicial de 500 kB a 800 kB en `angular.json`. Comprimido, Bootstrap suma unos 25 kB.
- **Pruebas:** `IndicadoresGestionTest` (4, con el ejemplo del informe: 20 de 400 = 5 %), `DashboardServiceTest` (5) y una prueba de las consultas del dashboard contra la BD real.

### Verificación visual (21/10)
Se manejó Chrome sin ventana con los 3 perfiles y se sacaron capturas. Están en [docs/capturas/](docs/capturas/) y sirven para los anexos:
- inicio con portadas;
- catálogo;
- Mis Préstamos;
- alertas del encargado (fila roja y amarilla);
- dashboard (en computadora y celular);
- tabla de libros con portadas.

Para las capturas se insertaron 4 préstamos de prueba, que se borraron al final.

**Total de pruebas: 90 del backend y 8 del frontend, todas pasan.**

---

## Sustentación – guion y contraseña de prueba (21/10) ✅

### B7 – Contraseña de prueba que cumple la regla
- El equipo ejecutó [cambiar-contrasena.cmd](backend/herramientas/cambiar-contrasena.cmd). Los 3 usuarios de prueba tienen ahora una contraseña que **sí cumple la regla**, en Neon y en [schema_postgresql.sql](database/schema_postgresql.sql) (hash nuevo y comentario actualizado). La contraseña no está en ningún archivo: la maneja el equipo.
- La primera ejecución falló a la mitad (ver "Base de datos" en la Prioridad 2). Se arregló y la segunda salió bien.

### S1, S2, S3 – Guion de sustentación
- `GUION_SUSTENTACION.md` en la raíz (**local**, está en el `.gitignore` como los planes). Tiene:
  - lo que hay que preparar antes de entrar;
  - el orden de la exposición (15 min);
  - cada pregunta que hizo el profe el 21/10, con el marcador a buscar, el archivo y qué decir;
  - las demos: el profe pidió ver que la lógica usa UPDATE y la física usa DELETE, y eso se muestra en Neon;
  - las preguntas posibles y el reparto entre los 5.
- Ojo, sale en el guion: la consulta "de dos tablas" en realidad une **tres** (`prestamo` + `usuario` + `libro`).

### Comentario corregido
- [AuthService.java](backend/src/main/java/com/alejandria/biblioteca/service/AuthService.java): el comentario del bloqueo decía "si ya fallo 5 veces"; son **3** desde C10. Si el profe lo leía, no coincidía con el informe.

### S4 – Capturas de los 3 perfiles (21/10)
- **16 capturas nuevas** en [docs/capturas/](docs/capturas/), sacadas con Chrome sin ventana y la contraseña de prueba nueva (pasada por variable de entorno, no escrita en ningún archivo). Ahora hay **23** en total; el detalle está en la tabla "Capturas para el anexo".
  - Público: login, registro (contraseña débil y válida), login bloqueado.
  - Prestatario: Mis Reservas, Mis Préstamos (uno activo y uno devuelto).
  - Encargado: reservas pendientes, préstamo registrado con su hora límite, préstamos activos (JOIN), ampliado y devolución con "Dañado".
  - Admin: préstamos activos, reservas antes y después de la eliminación física, reportes, monitoreo, editar libro.
- Datos de prueba: 2 préstamos y 2 reservas creados para las capturas. Al final se borraron y el stock de los 25 libros quedó **igual que antes** (comparado libro por libro). En Neon quedan 0 préstamos y 0 reservas.
- También se comprobó que un prestatario que escribe `/admin` termina en `/catalogo`.

### Commits preparados
- `PASOS_COMMITS.md` (local, en el `.gitignore`): **5 commits** para subir las correcciones (caché de Angular, backend, base de datos, frontend, documentación), con los mensajes listos para copiar.
- **No se borran los commits anteriores**: los dos que pidió el profe (`44597ad` login, `263c935` CRUD) se quedan como están.
- Antes de prepararlos se corrieron las pruebas: **90 del backend y 8 del frontend, todas pasan**. Con una prueba en seco (`git add --dry-run`) se revisó que no se suba nada sensible (`.env`, logs, `target`).
- **Ojo:** en esta PC están borrados `docs/diccionario de datos.docx`, `docs/diccionario_de_datos.md` y `docs/prototipos-alonso.pdf` (siguen en GitHub). El paso 1 de `PASOS_COMMITS.md` explica cómo recuperarlos.

---

## Requisitos que funcionaban en parte (27/10) ✅

Se completaron los RF que estaban a medias: **RF01, RF03, RF04, RF05, RF07, RF08 y RF09**, además de RF16 (avance de la cola) y las validaciones pendientes de 3.5 (horario, perfil único y contraseña en bytes).

### RF01 y RF05 – Horario de atención (08:00 a 20:00)

| Archivo | Qué cambió |
|---|---|
| [service/HorarioAtencion.java](backend/src/main/java/com/alejandria/biblioteca/service/HorarioAtencion.java) (nuevo) | Lee `app.horario.apertura` y `app.horario.cierre`. `validarAbierto` (no se presta antes de abrir ni desde el cierre), `limitarAlCierre` (la hora límite no pasa de las 20:00), `esHoraDeRecojo` y `cierreDeManana`. También da la hora actual con un reloj que las pruebas pueden fijar |
| [PrestamoService.java](backend/src/main/java/com/alejandria/biblioteca/service/PrestamoService.java) | `registrar`: rechaza fuera de horario (409) y la hora límite es `limitarAlCierre(inicio + 3 h)`. `ampliar`: suma **hasta** 1 hora sin pasar del cierre; si ya termina a las 20:00, responde 409 |
| [ReservaService.java](backend/src/main/java/com/alejandria/biblioteca/service/ReservaService.java) | La hora de recojo tiene que estar dentro del horario (400) |
| [application.properties](backend/src/main/resources/application.properties) | `app.horario.apertura=08:00`, `app.horario.cierre=20:00` |
| Pestaña Registrar préstamo | Muestra el horario y avisa si la hora límite se ajustó al cierre |

> **Para probar de noche:** poner `app.horario.cierre=23:59` en `application.properties` (o la variable de entorno `APP_HORARIO_CIERRE=23:59`) y volverlo a `20:00` antes de la demo.

### RF03 – Consulta de disponibilidad

| Archivo | Qué cambió |
|---|---|
| [LibroDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/LibroDao.java) | `buscarEnCatalogo`: un solo texto busca en título, ISBN (con o sin guiones), autor y categoría. Reemplaza a `findByTituloContainingIgnoreCase` (solo título), que ya no se usa |
| [LibroService.java](backend/src/main/java/com/alejandria/biblioteca/service/LibroService.java), [LibroController.java](backend/src/main/java/com/alejandria/biblioteca/controller/LibroController.java) | `buscarPorTitulo` → `buscar` (`GET /api/libros/buscar?q=`) |
| [utils/estado-libro.ts](frontend/src/app/utils/estado-libro.ts) (nuevo) | `etiquetaEstado`: `prestado` → "Prestado en sala", `mantenimiento` → "En mantenimiento", etc. `coincideBusqueda`: la búsqueda del catálogo, sin importar tildes |
| Catálogo del prestatario | Cada libro muestra su **estado** y su **stock** (antes no se veían). La búsqueda encuentra por ISBN y por cualquier autor (antes solo título y primer autor). También la usa la sección de libros del inicio |
| Pestaña Registrar préstamo | Buscador de libro (título, autor, categoría o ISBN) y el estado de cada libro en la lista |

### RF04 y RF16 – Cola de espera de reservas

**Migración** [database/migraciones/migracion_03_reserva_cola.sql](database/migraciones/migracion_03_reserva_cola.sql): `estado_reserva` acepta además `en_espera`. También está en [schema_postgresql.sql](database/schema_postgresql.sql). **Ejecutada en Neon el 27/10** (ver "Migración 03 en Neon" más abajo).

| Paso | Qué pasa |
|---|---|
| Reservar con stock | Igual que antes: `pendiente`, aparta un ejemplar (el stock baja) |
| Reservar **sin** stock | Entra a la cola: `en_espera`, sin fecha de recojo, con su puesto en `posicion_cola` (1, 2, 3...) |
| Vuelve un ejemplar (devolución en buen estado, anulación, cancelación o vencimiento de una reserva, o el admin sube el stock) | Si hay cola, **el stock no sube**: la reserva del primero pasa a `pendiente` con recojo hasta las 20:00 del día siguiente, y los demás avanzan un puesto. Sin cola, el ejemplar vuelve al stock como antes |
| Salir de la cola | La reserva queda `cancelada` y los de atrás avanzan un puesto (RF16). El stock no cambia |

| Archivo | Qué cambió |
|---|---|
| [service/ColaEsperaService.java](backend/src/main/java/com/alejandria/biblioteca/service/ColaEsperaService.java) (nuevo) | `siguientePosicion`, `asignarAlSiguiente` y `reordenar` |
| [LibroService.java](backend/src/main/java/com/alejandria/biblioteca/service/LibroService.java) | `reponerStock` primero ofrece el ejemplar a la cola; `actualizar` reparte los ejemplares nuevos a la cola; `darDeBaja` no deja dar de baja un libro con gente en la cola |
| [ReservaService.java](backend/src/main/java/com/alejandria/biblioteca/service/ReservaService.java) | `crear` decide entre reserva normal y cola; `cancelar` también saca de la cola; `hayReservasPendientes` cuenta la cola (no se amplía el préstamo) |
| [ReservaDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/ReservaDao.java), [ReservaRespuestaDTO.java](backend/src/main/java/com/alejandria/biblioteca/dto/reserva/ReservaRespuestaDTO.java) | Consultas de la cola; la respuesta trae `posicionCola` |
| Catálogo del prestatario | Sin stock, el botón dice **Unirme a la cola** (antes "Agotado", desactivado). "Mis Reservas" muestra el puesto y el botón **Salir de la cola** |
| Pestaña Reservas pendientes (encargado) | Tabla nueva **En cola de espera** (libro, puesto, prestatario) |
| Pestaña Reservas (admin) | Las reservas en cola muestran su puesto. No se pueden eliminar físicamente |

### RF07 – Historial de préstamos con filtros

| Archivo | Qué cambió |
|---|---|
| [PrestamoDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/PrestamoDao.java) | `buscarHistorial(dni, libro, desde, hasta)`: JOIN de cuatro tablas (prestamo, prestatario, encargado y libro) con filtros opcionales. Trae todos los estados, también los anulados. El reporte de préstamos usa la misma consulta (`listarParaReporte`) |
| [PrestamoService.java](backend/src/main/java/com/alejandria/biblioteca/service/PrestamoService.java), [PrestamoController.java](backend/src/main/java/com/alejandria/biblioteca/controller/PrestamoController.java) | `GET /api/prestamos/historial?dni=&libro=&desde=&hasta=` (encargado y admin). "Hasta el 27/10" incluye todo el 27 |
| [service/RangoFechas.java](backend/src/main/java/com/alejandria/biblioteca/service/RangoFechas.java) (nuevo) | El rango de fechas de los filtros (lo usan el historial y los reportes). Rechaza "desde" posterior a "hasta" (400) |
| [components/historial-prestamos/](frontend/src/app/components/historial-prestamos/) (nuevo) | Pestaña **Historial** en los paneles del encargado y del admin: filtros por DNI, libro (título o ISBN) y fechas |
| "Mis Préstamos" (prestatario) | Filtros por libro y fechas, en pantalla: la consulta sigue siendo de una sola tabla |

### RF08 – Reportes por fechas

Los 8 reportes del RF08, en **Excel, PDF y CSV**, con rango de fechas. Ahora el **encargado** también puede descargarlos.

| Reporte | Qué trae |
|---|---|
| Préstamos | Todos los préstamos del periodo (como antes, ahora con fechas) |
| Devoluciones | Libros devueltos en el periodo, "A tiempo" o "Con retraso (N min)", y su estado físico |
| Reservas | Reservas del periodo con su estado y su puesto si están en la cola. Aclara que las eliminadas físicamente (RF17) ya no aparecen |
| Incidencias y sanciones | Cada incidencia con su sanción (tipo, monto, fechas). Las incidencias se registran con RF06 (pendiente): el reporte muestra las que estén en la BD |
| Bajas lógicas | Libros en `baja`, préstamos `anulado` y usuarios `inactivo`. La BD no guarda la fecha de baja de libros ni usuarios: el rango solo filtra los préstamos anulados |
| Stock por categoría | Títulos, ejemplares disponibles, títulos sin stock y en mantenimiento, con una fila de total. Es la foto de ahora (no usa fechas) |
| Indicadores de gestión | TAP, PDF, IPI y TIM del periodo. Sin datos suficientes dicen "Sin dato" |
| Tiempos de préstamo | Mínimo, promedio y máximo que estuvo el libro en sala (de la entrega a la devolución), por categoría y en total |

| Archivo | Qué cambió |
|---|---|
| [ReporteService.java](backend/src/main/java/com/alejandria/biblioteca/service/ReporteService.java) | Cada reporte se arma como una [TablaReporte](backend/src/main/java/com/alejandria/biblioteca/dto/reporte/TablaReporte.java) y la misma tabla se convierte con `generarExcel` (POI), `generarPdf` (OpenPDF) o `generarCsv` (Commons CSV). Reemplazan a `generarExcelPrestamos`, `generarPdfPrestamos` y `generarCsvPrestamos` |
| [ReporteController.java](backend/src/main/java/com/alejandria/biblioteca/controller/ReporteController.java) | `GET /api/reportes/{tipo}/{formato}?desde=&hasta=` (admin y encargado). Los enlaces viejos (`/prestamos/excel`...) siguen funcionando. Importar libros sigue siendo solo del admin |
| [IndicadoresGestion.java](backend/src/main/java/com/alejandria/biblioteca/service/IndicadoresGestion.java) | `calcular`: los 4 indicadores, antes dentro de `DashboardService`. Los usan el dashboard (mes actual) y el reporte (periodo elegido) |
| Consultas nuevas | `listarDevoluciones`, `listarIncidenciasConSancion`, `tiemposPorCategoria` (PrestamoDao), `listarParaReporte` (ReservaDao), `stockPorCategoria` y `findByEstadoLibroOrderByTitulo` (LibroDao), `findByEstadoOrderByApellidoAscNombreAsc` (UsuarioDao) |
| [components/reportes-descarga/](frontend/src/app/components/reportes-descarga/) (nuevo) | Elegir reporte, fechas (por defecto, del 1 del mes a hoy) y formato. Está en la pestaña Reportes del admin (junto con importar libros) y en una pestaña **Reportes** nueva del encargado |

### 🐞 Error encontrado: el BOM del CSV quedaba después del encabezado
El CSV escribe un BOM para que Excel lea bien las tildes, pero el `CSVPrinter` escribe el encabezado **apenas se crea**, así que el BOM quedaba detrás del encabezado y Excel podía mostrar mal las tildes. Se vio al probar en vivo (el BOM aparecía al inicio de la segunda línea). **Arreglo:** el BOM se escribe antes de crear el `CSVPrinter`. Prueba: `elCsvDePrestamosTieneEncabezadoYDatos` ahora revisa que el archivo empiece con el BOM.

### RF09 – El admin registra encargados

| Archivo | Qué cambió |
|---|---|
| [service/UsuarioService.java](backend/src/main/java/com/alejandria/biblioteca/service/UsuarioService.java) (nuevo) | `crearCuenta`: valida datos, regla de contraseña, que no se repita DNI ni correo, y guarda con BCrypt. La usan el registro del prestatario (`AuthService.registro`, que antes tenía todo esto adentro) y el registro de encargados. `registrarEncargado` (turno `mañana` o `tarde`) y `listarEncargados` |
| [controller/UsuarioController.java](backend/src/main/java/com/alejandria/biblioteca/controller/UsuarioController.java) (nuevo) | `GET` y `POST /api/usuarios/encargados`, solo admin |
| [UsuarioDao.java](backend/src/main/java/com/alejandria/biblioteca/dao/UsuarioDao.java) | `registrarComoEncargado`, `listarEncargados` y `contarPerfiles` |
| [AuthService.java](backend/src/main/java/com/alejandria/biblioteca/service/AuthService.java) | **Perfil único:** si el usuario no está en ninguna tabla de perfil, o está en dos, el login responde 403 |
| [ValidadorContrasena.java](backend/src/main/java/com/alejandria/biblioteca/seguridad/ValidadorContrasena.java), [utils/regla-contrasena.ts](frontend/src/app/utils/regla-contrasena.ts) (nuevo) | La contraseña tampoco puede pasar de 72 bytes. La regla del frontend está en un solo archivo y la usan el registro y el formulario de encargados |
| [tabs/encargados/](frontend/src/app/pages/admin-panel/tabs/encargados/) (nuevo) | Pestaña **Encargados** del admin: formulario (con los ✓ / ✗ de la contraseña) y tabla de encargados |

### Pruebas: de 90 a 154 en el backend y de 8 a 16 en el frontend, todas pasan

| Clase de prueba | Qué prueba |
|---|---|
| `HorarioAtencionTest` (6) | Dentro y fuera del horario, la hora límite queda en las 20:00, hora de recojo, cierre de mañana |
| `PrestamoServiceTest` (+7) | Préstamo a las 10:00 → 13:00; a las 18:30 → 20:00 (no 21:30); a las 21:00 → 409; ampliación hasta las 20:00; no se amplía si ya termina a las 20:00; historial con filtros y fechas al revés. Ahora usa un **reloj fijo**: las pruebas dan lo mismo aunque se corran de noche |
| `ColaEsperaServiceTest` (4), `ReservaColaEsperaTest` (8) | El primero recibe el ejemplar y los demás avanzan; sin cola el ejemplar vuelve al stock; sin stock se entra a la cola; no se reserva dos veces; fecha obligatoria con stock; recojo en horario; salir de la cola no toca el stock |
| `ColaEsperaBdTest` (1, BD real) | Flujo completo con 3 prestatarios: reservar, cola, cancelar, avanzar y volver al stock. Necesita la migración 03 |
| `LibroServiceTest` (+3) | No se da de baja con cola; el ejemplar devuelto es para la cola; el admin agrega ejemplares y primero se reparten a la cola |
| `LibroDaoTest` (5, BD real) | Búsqueda por título, ISBN con y sin guiones, autor y categoría, sin bajas ni repetidos |
| `PrestamoDaoTest` (+2, BD real) | Historial por DNI, libro y fechas; consultas de devoluciones, incidencias y tiempos |
| `ReporteServiceTest` (+8) | Rango de días completos; fechas al revés, tipo y formato inválidos; puntualidad; puesto en la cola; bajas lógicas; total del stock; "Sin dato"; tiempos con promedio ponderado; BOM del CSV |
| `ReporteBdTest` (11, BD real) | Los 8 reportes con y sin fechas y los 3 formatos |
| `UsuarioServiceTest` (4), `UsuarioBdTest` (1, BD real), `AuthServicePerfilTest` (3) | Registro del encargado (BCrypt, turno, correo repetido, contraseña débil); queda en la tabla `encargado` con un solo perfil; login con 0, 1 o 2 perfiles |
| `ValidadorContrasenaTest` (+1) | 64 caracteres con ñ que pasan los 72 bytes |
| Frontend: `estado-libro.spec.ts` (5), `regla-contrasena.spec.ts` (3) | Nombres del estado y búsqueda; regla de contraseña en bytes |

Las pruebas con BD real se corrieron contra una copia local de PostgreSQL 16 creada con `schema_postgresql.sql`, las 3 migraciones y `datos_demo.sql` (con la BD en hora GMT y Java en hora de Lima, igual que con Neon). **Conviene correrlas también contra Neon después de ejecutar la migración 03.** *(Hecho el 27/10: 158 pruebas pasan contra Neon, ver "Migración 03 en Neon".)*

### Prueba en vivo (27/10, BD local, a las 18:20)
Por la API, con las mismas peticiones de la web: el admin registra una encargada → 201 y ella entra con rol ENCARGADO · el encargado intenta registrar encargados → 403 · turno "noche" → 400 · contraseña de 64 caracteres y 124 bytes → 400 · búsqueda `978-0307474728` → *Cien Años de Soledad* · recojo a las 21:00 → 400 · reserva con stock → `pendiente` · otra prestataria sin stock → `en_espera`, puesto 1 · entrar dos veces → 409 · el primero cancela → la reserva de la cola pasa a `pendiente` con recojo hasta el 28/10 20:00 y el stock sigue en 0 · el préstamo de las 18:21 queda con hora límite **20:00** · ampliarlo → 409 "ya termina a la hora de cierre" · historial por DNI, libro y fechas → correcto; fechas al revés → 400; el prestatario → 403 · los 8 reportes en los 3 formatos → 200 para el encargado; tipo inválido → 400; el prestatario → 403.

En el navegador (Chrome sin ventana, con los 3 perfiles) se recorrieron las mismas pantallas sin errores de consola. Las portadas no cargaron porque ese entorno no tenía acceso a Open Library; en la PC del equipo sí cargan.

### Qué cambiar en el informe
- **Tabla 2 y fichas de RF01, RF03, RF04, RF05, RF07, RF08, RF09 y RF16:** quitar "pendiente", "entrega final" y "no se acredita en este avance" donde ya se cumple. RF08: el encargado ya tiene acceso.
- **Pendientes para la entrega final (3.5):** quedan RF06 (incidencias y sanciones), RF12, RF13 y RF15. Sacar de la lista la cola de espera, los reportes, la creación de personal y las validaciones de horario, perfil único y bytes.
- **Diccionario de datos:** `estado_reserva` acepta `en_espera`; `posicion_cola` es el puesto en la cola de espera; `fecha_recojo_limite` es `NULL` mientras la reserva está en la cola; `fecha_devolucion_prevista` ya no dice "validación automática del cierre pendiente".
- **Librerías (2.x):** los métodos ahora son `generarExcel`, `generarPdf` y `generarCsv` de `ReporteService` (antes `generarExcelPrestamos`, `generarPdfPrestamos` y `generarCsvPrestamos`) y generan los 8 reportes.
- **Pantallas (3.5):** nuevas pestañas Historial, Encargados y Reportes del encargado; botón "Unirme a la cola"; estado y stock en el catálogo. Las capturas están en `docs/capturas/` (ver la tabla de abajo).
- **Pruebas:** 158 del backend y 16 del frontend (154 con esta sección y 4 más con la corrección del stock 0, abajo).

El detalle de cada cambio del informe (qué dice la V8 y qué debe decir, el Gantt, la Tabla 34, la Tabla 52 y el texto del RF01) está en la **sección 7 de `PLAN_INFORME.md`**.

---

## Corrección (27/10): libro nuevo con stock 0 quedaba "prestado" ✅

Lo observó un integrante: en `LibroService.crear`, un libro registrado con stock 0 quedaba con estado `prestado`. ¿Prestado a quién? No había ningún préstamo. Lo mismo pasaba al editar un libro y bajarle el stock a 0.

**Qué dice el informe (RF03):** "Con unidades se muestra Disponible; sin ellas, el estado agregado depende del movimiento que agotó el stock". Es decir, un libro sin ejemplares está `prestado`, `reservado` o en `mantenimiento` **porque algo se llevó el último ejemplar**. Un libro nuevo no tuvo ningún movimiento, así que no puede quedar sin ejemplares.

**Por qué no "agotado":** la BD (`chk_libro_estado`) y el RF03 solo tienen los estados disponible, prestado, reservado y mantenimiento (más `baja`). Agregar otro estado obligaba a cambiar la BD, el diccionario de datos y el RF03.

| Antes | Ahora |
|---|---|
| Registrar con stock 0 → `prestado` | Registrar con stock 0 → **400** "Un libro nuevo debe tener al menos 1 ejemplar". Siempre entra `disponible` |
| Editar un libro `disponible` y bajarle el stock a 0 → `prestado` | → **400**: el stock solo llega a 0 al prestar, reservar o devolver dañado |
| Editar un libro sin stock (sus ejemplares están prestados) | Igual que antes: se puede editar y sigue `prestado` |

| Archivo | Qué cambió |
|---|---|
| [LibroService.java](backend/src/main/java/com/alejandria/biblioteca/service/LibroService.java) | `crear` exige stock ≥ 1 y siempre deja `disponible`; `actualizar` no deja bajar el stock a 0 y ya no pone `prestado` a mano |
| [libros-tab](frontend/src/app/pages/admin-panel/tabs/libros/) | Al registrar, el stock mínimo es 1 y lo indica debajo del campo |

La importación por CSV usa `crear`, así que una fila con stock 0 ahora sale como error de esa fila.

**Pruebas (primero la prueba, después el arreglo):** 4 nuevas en `LibroServiceTest`. `unLibroNuevoNoSePuedeRegistrarConStock0` y `alEditarNoSeBajaElStockA0SinUnPrestamoReservaODano` fallaron antes del cambio (el sistema aceptaba el 0) y después pasaron. Total: **158 del backend y 16 del frontend**, todas pasan.

**Para el informe:** quitar las frases sobre el "valor técnico prestado con stock 0" (criterio de aceptación del RF03 y el párrafo "Adicionar y actualizar" de 2.1.4): ahora un libro nuevo siempre entra disponible con al menos 1 ejemplar.

---

## Migración 03 en Neon (27/10) ✅

Al correr `./mvnw test` contra Neon desde la PC del equipo, pasaron **157 de 158**. Falló `ColaEsperaBdTest`: Neon rechazó la reserva con `en_espera` (`violates check constraint "chk_reserva_estado"`), porque la migración 03 solo se había ejecutado en la BD local de prueba. La prueba se deshizo sola: no dejó datos.

Se ejecutó `database/migraciones/migracion_03_reserva_cola.sql` en Neon (con las credenciales del `backend/.env`, en una transacción):

| | `chk_reserva_estado` en Neon |
|---|---|
| Antes | `pendiente`, `atendida`, `cancelada`, `vencida` |
| Después | `pendiente`, **`en_espera`**, `atendida`, `cancelada`, `vencida` |

La migración solo cambia la restricción: no toca ninguna fila. Después se volvió a correr `./mvnw test` contra Neon: **158 pruebas, todas pasan**.

---

## Datos de la demo (27/10) ✅

Con la base casi vacía, el dashboard, el historial, las reservas y los reportes no mostraban nada. Se agregó la sección 4 de [database/datos_demo.sql](database/datos_demo.sql) ("Movimientos de ejemplo") y **se ejecutó en Neon el 27/10**. Al principio estaba en un archivo aparte (`datos_demo_movimientos.sql`); se unió a `datos_demo.sql` para tener un solo archivo de datos. Se volvió a ejecutar el archivo completo en Neon y no duplicó nada. Las personas son ficticias. Las fechas se calculan desde el día en que se ejecuta (hora de Lima), así que el mes actual y los últimos 7 días tienen datos. Se puede ejecutar varias veces: la segunda vez no hace nada (comprobado).

| Qué | Cuántos | Detalle |
|---|---|---|
| Encargados nuevos | 2 | Miguel Torres (mañana) y Rosa Quispe (tarde). Con Carlos son 3 |
| Prestatarios nuevos | 7 | Lucía, Diego, Valeria, Andrés, Camila, Sebastián y José Luis (`inactivo`: baja lógica). Juan (`alumno@`) también tiene historial |
| Préstamos | 28 | 24 devueltos (2 ampliados, 1 dañado, 1 con retraso), 2 anulados y 2 sin devolver de hoy (salen **vencidos**, en rojo) |
| Reservas | 11 | 3 pendientes de recojo para mañana, 2 en la **cola de espera** de *Cien Años de Soledad*, 2 atendidas, 2 canceladas y 2 vencidas |
| Incidencias y sanciones | 2 | *Cosmos* devuelto dañado → multa de S/ 25.00 (cumplida). *El túnel* con 40 min de retraso → **suspensión activa** de Valeria por 4 días más |

Cómo quedan los libros: *Cosmos* en `mantenimiento` (su único ejemplar volvió dañado), *Rayuela* `prestado`, *Cien Años de Soledad* y *Design Patterns* `reservado`. El stock de cada libro se ajustó igual que lo haría el sistema.

- **Contraseña:** todas las cuentas nuevas usan la **misma contraseña de prueba** que `admin@alejandria.edu.pe` (el script copia su hash; la contraseña no está en el repositorio).
- **Indicadores del mes:** PDF = 1 retraso de 26 préstamos = **3.8 %** (cumple la meta ≤ 5 %). TIM = 1 daño de 26 = **3.8 %** (no cumple ≤ 1 %).
- **Para la demo:**
  - Valeria (`valeria.flores@…`) tiene una sanción activa: prestarle un libro da *"El prestatario tiene una sanción activa"*. Sirve para mostrar esa regla.
  - Los 2 préstamos vencidos se pueden devolver en vivo: el sistema avisa que se devolvieron con retraso.
  - Si alguien cancela la reserva pendiente de *Cien Años de Soledad* (la de Sebastián), el ejemplar pasa a Camila, primera de la cola.
- La cuenta de **Gerardo Guevara** (registrada desde la web) no se tocó.
- **Pruebas:** después de cargar los datos se volvió a correr `./mvnw test` contra Neon.
- **D11 cambió:** antes la idea era crear los préstamos y prestatarios en vivo durante la demo. Ahora ya hay datos; igual se pueden crear más en vivo.

---

## Migración 04 (27/10): Neon igual al script ✅

Se comparó Neon con `schema_postgresql.sql` tabla por tabla (columnas, restricciones e índices). Todo coincidía salvo **3 llaves foráneas de más** que no están en ningún script: las creó Hibernate solo, cuando estaba en modo `update` (antes de D7, 21/10).

| Llave que se quitó | Qué hacía | La que queda (del script) |
|---|---|---|
| `prestamo.fk8s1j7k9hshgq64d77enp1xrh5` | `id_encargado` → `usuario` | `fk_prestamo_encargado`: → `encargado` |
| `prestamo.fkmmgblj7y7aqgmmc5jdxqo0a5h` | `id_prestatario` → `usuario` | `fk_prestamo_prestatario`: → `prestatario` |
| `reserva.fklbed902g8psh7xfx0y84o2l4f` | `id_prestatario` → `usuario` | `fk_reserva_prestatario`: → `prestatario` |

Las quitadas repetían lo que ya controlan las del script, que son más estrictas (apuntan al perfil, no solo al usuario). [database/migraciones/migracion_04_quitar_fk_hibernate.sql](database/migraciones/migracion_04_quitar_fk_hibernate.sql) busca las llaves de `prestamo` y `reserva` que apuntan a `usuario` y las borra, sin depender de sus nombres (en otra base Hibernate les habría puesto otros). No toca ninguna fila.

**Ejecutada en Neon el 27/10** (dos veces: la segunda no hizo nada). Ahora Neon y `schema_postgresql.sql` coinciden. El script no cambia, porque nunca tuvo esas llaves. Después se corrió `./mvnw test` contra Neon.

---

## Carpeta de migraciones (27/10) ✅

Las 4 migraciones se movieron de `database/` a **`database/migraciones/`** (con `git mv`, así git sabe que son los mismos archivos). En `database/` quedan solo los archivos para crear la base: `schema_postgresql.sql` (que ya incluye todos los cambios) y `datos_demo.sql`, más el CSV de ejemplo.

**Para qué sirven las migraciones:** `schema_postgresql.sql` empieza con `DROP TABLE`, así que ejecutarlo en Neon borraría todos los datos. Cada migración aplica a Neon un solo cambio sin borrar nada. Las 4 ya están ejecutadas en Neon; una base nueva no las necesita.

Si el profe pregunta: *"El schema crea la base completa. Las migraciones son los cambios que aplicamos a Neon sin borrar los datos que ya tenía."*

También se borraron `docs/modelofisico.sql` y `docs/modelofisico_postgresql.sql`: eran el diseño inicial del Avance 2 y ya no coincidían con la base real (la que manda es `schema_postgresql.sql`). Siguen en el historial de git.

**README (27/10):** nueva estructura de `database/` y `docs/`, sección "Cuentas y datos de la demo" (sin la contraseña), endpoints revisados contra los controllers (faltaban categorías, autores, `GET /{id}` y que el admin también anula préstamos), Vitest en librerías y la variable `APP_HORARIO_CIERRE` para probar de noche sin cambiar `application.properties`.

---

## Revisión del código contra la documentación (27/10) ✅

Se comparó el código con el README, CLAUDE.md y la bitácora: versiones, permisos de cada endpoint, reglas del negocio, rutas, pruebas y datos de la demo. Casi todo coincidía. Lo que no:

### 🐞 La "hora de Lima" dependía de la PC
La bitácora y los comentarios `hora de lima` dicen que Java manda la hora de Lima, pero el backend no fijaba ninguna zona: usaba la de la PC (`Clock.systemDefaultZone()`). En la PC del equipo funciona porque está en Perú, pero en un servidor en GMT el horario de 08:00 a 20:00, los vencimientos y las sanciones se correrían 5 horas.

| Archivo | Qué cambió |
|---|---|
| [BibliotecaApplication.java](backend/src/main/java/com/alejandria/biblioteca/BibliotecaApplication.java) | `fijarZonaHoraria()`: antes de arrancar Spring pone la zona `America/Lima` para todo el backend (`TimeZone` es de Java, no es una librería nueva). En una PC de Perú no cambia nada |
| [ZonaHorariaTest.java](backend/src/test/java/com/alejandria/biblioteca/ZonaHorariaTest.java) (nuevo, 2 pruebas) | Con la PC en GMT, después de `fijarZonaHoraria` la zona es Lima; y las 01:00 GMT del 28/10 son las 20:00 del 27/10 en Lima. Al terminar devuelve la zona de la PC para no afectar a las demás pruebas |

**Primero la prueba:** `ZonaHorariaTest` no compilaba antes del cambio (el método no existía) y después pasó. Total: **160 pruebas del backend**, todas pasan contra Neon.

### Permisos que no estaban documentados (no se cambió el código)
- El **encargado** puede registrar y cancelar reservas a nombre de un prestatario (`POST /api/reservas`, `PUT /api/reservas/{id}/cancelar`; el comentario de `ReservaService` dice "en ventanilla"), pero su panel no tiene botón: solo por la API.
- El **administrador** puede anular préstamos (`DELETE /api/prestamos/{id}`), pero el botón "Anular" solo está en el panel del encargado.

Se documentaron en el README (perfiles y endpoints) y quedaron como pendiente del informe (I65 en `PLAN_INFORME.md`). Si el equipo decide quitarlos, basta con sacar el rol de `@RolesPermitidos`.

### Detalle menor (no se cambió)
`DashboardService`, `MonitoreoService` y `PrestatarioService` toman la hora con `LocalDateTime.now()` en vez de `horario.ahora()`. Ahora todos usan la hora de Lima, así que no hay error; solo que las pruebas no pueden fijarles la hora.

---

## Marcadores para la sustentación

En el código hay comentarios con estas palabras clave. En VS Code, **Ctrl+Shift+F** busca en todo el proyecto (Ctrl+F solo en el archivo abierto). Van sin tildes para que la búsqueda no falle.

| Pregunta del profe | Buscar |
|---|---|
| ¿Cuáles son los perfiles? | `perfiles` |
| ¿Dónde está el primer / segundo / tercer perfil? | `perfil 1` (admin), `perfil 2` (encargado), `perfil 3` (prestatario) |
| ¿Dónde se crea el administrador / el prestatario? | `crear perfil` (admin y encargado en el script SQL; prestatario en `AuthService.registro`) |
| ¿Cómo sabe el sistema qué perfil es? | `detectar rol` |
| ¿Dónde se valida la contraseña? | `regla contra` (backend y frontend) |
| ¿Dónde está cada criterio de la contraseña? | `criterio 1` … `criterio 5` |
| ¿Dónde están las contraseñas? | `contras en bd`, `bcrypt:` |
| ¿Cómo funciona el login? | `proceso login`, `token:` |
| Autorización: ¿qué puede hacer cada perfil? | `autorizacion`, `permisos admin`, `permisos encargado`, `permisos prestatario` |
| ¿Cuál es la ruta del CRUD? | `ruta crud` |
| CRUD de libros | `crud libro adicionar`, `crud libro consultar`, `crud libro actualizar`, `crud libro eliminar` |
| ¿Qué datos del libro registran? | `datos del libro` |
| CRUD de préstamos (el del negocio) | `crud prestamo adicionar`, `crud prestamo consultar`, `crud prestamo actualizar`, `crud prestamo eliminar` |
| Consulta de una tabla | `consulta una tabla` (backend y pantalla "Mis préstamos") |
| Consulta de dos tablas (JOIN) | `consulta dos tablas` |
| ¿Por qué la eliminación es lógica? | `eliminacion logica`, `por q logica` |
| ¿Dónde está la eliminación física? | `eliminacion fisica` (reservas canceladas o vencidas) |
| ¿Cómo se actualiza el stock / estado del libro? | `stock automatico`, `estado del libro` |
| Regla de las 3 horas | `3 horas` |
| Sanciones | `sancion activa` |
| Reservas | `reserva crear`, `reserva cancelar` |
| Librerías | `guava`, `logback`, `apache poi`, `commons csv`, `openpdf`, `actuator`, `reporte prestamos` |
| ¿Por qué no usan la hora de la BD? ¿Dónde se fija la hora de Lima? | `hora de lima` (incluye `BibliotecaApplication.fijarZonaHoraria`) |
| ¿Dónde validan el DNI y el correo? | `validar datos` |
| ¿Qué pasa si hay un error? | `manejo de errores` |
| ¿Cuándo se cierra la sesión? | `sesion 15 min` |
| ¿Quién puede llamar a la API? | `cors` |
| ¿La ampliación cuánto suma? | `ampliacion` |
| ¿Dónde están las alertas de 15 minutos? | `alertas 15 min` |
| ¿Qué pasa si devuelven un libro dañado? | `mantenimiento` |
| ¿Dónde está el dashboard y cómo se calculan los indicadores? | `dashboard`, `indicadores` |
| ¿De dónde salen las portadas? | `portadas` |
| ¿Dónde se valida el horario de 8:00 a 20:00? | `horario de atencion` |
| ¿Cómo funciona la cola de espera? | `cola de espera` |
| ¿Dónde se busca por título, autor, categoría o ISBN? | `consulta disponibilidad` |
| ¿Dónde está el historial con filtros? | `historial`, `filtro de fechas` |
| ¿Dónde están los reportes del RF08? | `reportes rf08`, `reporte devoluciones`, `reporte reservas`, `reporte incidencias`, `reporte bajas logicas`, `reporte stock`, `reporte indicadores`, `reporte tiempos` |
| ¿Dónde se crea el encargado desde el panel? | `crear perfil 2` |
| ¿Cómo se evita que un usuario tenga dos perfiles? | `perfil unico` |
| ¿Por qué la contraseña se mide en bytes? | `criterio 1 en bytes` |

---

## Pendientes generales

Lo que falta está separado en dos archivos locales (no se suben a GitHub):
- [PLAN_CODIGO.md](PLAN_CODIGO.md): errores, reglas, eliminación física, limpieza y sustentación (se trabaja con Claude Code).
- [PLAN_INFORME.md](PLAN_INFORME.md): correcciones del documento, con el diccionario de datos ya corregido y los datos del código para redactar (se trabaja junto con el informe V8, `docs/Avance_III_IntegradorV8_corregido.docx`).

Cada tarea del código dice si cambia algo en el informe. Esos cambios se anotan en la sección 6 de `PLAN_INFORME.md`, y el detalle de lo que hay que reescribir en la sección 7. Los dos planes se actualizaron el 27/10.

- [x] Commit del login (`44597ad`), del CRUD (`263c935`) y de librerías (`f15dd2a`)
- [x] Prioridad 5: `.jar`, README y comentario de `123456`
- [x] Commits del 27/10 en la rama `claude/lucid-galileo-7xcs2c`: `bec3b25` (RF que funcionaban en parte) y `a2fb017` (stock 0)
- [x] Ejecutar `database/migraciones/migracion_03_reserva_cola.sql` en Neon y correr `./mvnw test` contra Neon (27/10: 158 pruebas, todas pasan)
- [x] Commits del 27/10 (noche): hora de Lima, base de datos y documentación. La rama `claude/lucid-galileo-7xcs2c` pasó a `main` con un commit de unión (`git merge --no-ff`, igual que "Create a merge commit" de GitHub), así que `bec3b25` y `a2fb017` conservan su código
- [ ] 🔴 Actualizar el informe con la sección 7 de `PLAN_INFORME.md`

## Capturas para el anexo

| Prioridad | Captura | Estado |
|---|---|---|
| 1 | Diff de `application.properties` sin credenciales | ⬜ |
| 1 | Pantalla de login sin el botón ADMIN | ✅ `login.png` |
| 1 | DevTools: encabezado `Access-Control-Allow-Origin: http://localhost:4200` | ⬜ |
| 2 | Registro con contraseña débil (✗ en rojo) y válida (✓ en verde) | ✅ `registro-contrasena-debil.png`, `registro-contrasena-valida.png` |
| 2 | Login de cada perfil llegando a su pantalla | ⬜ |
| 2 | Prestatario escribiendo `/admin` y siendo redirigido a `/catalogo` | ⬜ (comprobado el 21/10; la captura tiene que mostrar la barra de direcciones, tómenla en vivo) |
| 2 | `GET /api/reservas` sin token → 401; con un prestatario → 403 | ⬜ |
| 2 | Token reusado después del logout → 401 | ⬜ |
| 3 | Migración ejecutada en Neon (8 ALTER correctos) | ✅ |
| 3 | Encargado: registrar préstamo con mensaje "Hora límite: ..." | ✅ `encargado-registrar-prestamo.png` |
| 3 | Encargado: tabla de préstamos activos (consulta con JOIN) | ✅ `encargado-prestamos-activos.png`, `admin-prestamos-activos.png` |
| 3 | Encargado: Ampliar (y el botón desactivado al segundo intento) | ✅ `encargado-prestamo-ampliado.png` |
| 3 | Encargado: modal de devolución (bueno / dañado) | ✅ `encargado-devolucion-danado.png` |
| 3 | Encargado: error al prestar un libro sin stock | ⬜ |
| 3 | Stock antes y después de prestar o devolver (tabla de libros del admin o Neon) | ⬜ |
| 3 | Admin: registrar, editar y dar de baja un libro (y que desaparezca del catálogo) | 🟡 editar: `admin-libros-editar.png` (registrar y dar de baja: en vivo) |
| 3 | Prestatario: reservar → aparece en "Mis Reservas" → cancelar | 🟡 `prestatario-mis-reservas.png` (reservar y cancelar: en vivo) |
| 3 | Código: `PrestamoDao` (consulta de una tabla + consulta con JOIN) y `PrestamoService.registrar` | ⬜ |
| 4 | Login bloqueado tras 3 intentos (mensaje "Demasiados intentos fallidos") | ✅ `login-bloqueado.png` |
| 4 | Contenido de `backend/logs/biblioteca.log` con logins fallidos y préstamos | ⬜ |
| 4 | Excel descargado y abierto (encabezado con color) | ⬜ |
| 4 | CSV descargado; importación de `ejemplo_importar_libros.csv` con el resultado | ⬜ |
| 4 | PDF descargado y abierto | ⬜ |
| 4 | Pestaña Monitoreo del admin con las métricas | ✅ `admin-monitoreo.png` |
| 4 | Navegador en `http://localhost:8081/actuator/health` (db: UP) | ⬜ |
| 4 | Terminal con `./mvnw test` → 160 pruebas, BUILD SUCCESS | ⬜ |
| 6 | Código de `BibliotecaApplication.fijarZonaHoraria` y `ZonaHorariaTest` pasando (hora de Lima) | ⬜ |
| 5 | Dashboard del admin (ya está en `docs/capturas/admin-dashboard.png`) | ✅ |
| 5 | Alertas del encargado (ya está en `docs/capturas/encargado-alertas-15min.png`) | ✅ |
| 5 | Catálogo con portadas reales (ya está en `docs/capturas/prestatario-catalogo.png`) | ✅ |
| 5 | Terminal con `npx ng test --watch=false` → 16 pruebas del frontend | ⬜ |
| 5 | Encargado: préstamos activos con una fila amarilla (por vencer) y una roja (vencida) y los avisos arriba | ⬜ |
| 5 | Encargado: modal de devolución con "Dañado" y el aviso de mantenimiento; admin: el libro en `mantenimiento` | 🟡 el modal: `encargado-devolucion-danado.png` (el libro en mantenimiento: en vivo) |
| 5 | Prestatario → Mis Préstamos (uno activo y uno devuelto), junto al código de `buscarPorPrestatario` | ✅ `prestatario-mis-prestamos.png` (el código: captura aparte) |
| 5 | Admin → pestaña Reservas: eliminar una cancelada (antes y después) y el mensaje al intentar con una pendiente | ✅ `admin-reservas.png` (antes), `admin-reserva-eliminada.png` (después) |
| 5 | Encargado: pestaña Reservas pendientes | ✅ `encargado-reservas-pendientes.png` |
| 5 | Admin: pestaña Reportes (Excel, PDF, CSV, importar CSV) | ✅ `admin-reportes.png` |
| 5 | `logs/biblioteca.log` con "eliminada físicamente de la BD" | ⬜ |
| 6 | Encargado: buscar libro por autor en Registrar préstamo (RF03) | ✅ `encargado-buscar-libro.png` |
| 6 | Encargado: reservas pendientes y tabla "En cola de espera" (RF04) | ✅ `encargado-cola-espera.png` |
| 6 | Encargado: historial filtrado por DNI, con hora límite 20:00 (RF07 y RF01) | ✅ `encargado-historial.png` |
| 6 | Encargado: pestaña Reportes (RF08) | ✅ `encargado-reportes.png` |
| 6 | Admin: pestaña Encargados con el registro y la tabla (RF09) | ✅ `admin-encargados.png` |
| 6 | Admin: historial (RF07) | ✅ `admin-historial.png` |
| 6 | Admin: reservas con una en cola (RF04) | ✅ `admin-reservas-cola.png` |
| 6 | Admin: reportes con el selector de los 8 tipos (RF08) | ✅ `admin-reportes-rf08.png` |
| 6 | Prestatario: Mis Préstamos con filtros (RF07) | ✅ `prestatario-mis-prestamos-filtros.png` |
| 6 | Prestatario: catálogo con estado y stock, botón "Unirme a la cola", modal de la cola y "Mis Reservas" con el puesto | ⬜ tómenlas en vivo (en la prueba no cargaron las portadas) |
| 6 | Un PDF o Excel de los reportes nuevos abierto (por ejemplo, reservas o tiempos) | ⬜ |
| 6 | Admin: "Registrar libro" con stock 0 y el error "Un libro nuevo debe tener al menos 1 ejemplar" (corrección del stock 0) | ⬜ |
| 6 | Admin: dashboard, historial, reservas y encargados **con los datos de la demo** (reemplazan a las capturas vacías) | ⬜ |
| 6 | Encargado: préstamo registrado después de las 17:00 con el aviso "ajustada a la hora de cierre" (RF01) | ⬜ (solo se puede tomar después de las 17:00; `encargado-historial.png` ya muestra un préstamo de las 18:21 con límite 20:00) |
| 4 | Código: `LimitadorIntentosLogin` (Guava), `ReporteService` (POI y CSV), `logback-spring.xml` | ⬜ |
| 5 | Terminal con `./mvnw clean package` → BUILD SUCCESS | ⬜ |
| 5 | Explorador mostrando `backend/target/biblioteca-alejandria.jar` | ⬜ |
| 5 | Terminal con `java -jar target/biblioteca-alejandria.jar` arrancando ("Started BibliotecaApplication") | ⬜ |
| 5 | README del repositorio en GitHub | ⬜ |
