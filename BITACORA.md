# Bitácora de cambios – Sistema de Préstamos, Biblioteca de Alejandría

Registro de todo lo que se ha modificado en el código y en la base de datos, con el motivo de cada cambio.
Sirve para preparar la sustentación: cada integrante debe poder explicar cualquier parte.

---

## Cómo levantar el proyecto

1. **Credenciales de la BD**: crear `backend/.env` (copiar de [backend/.env.example](backend/.env.example)) y completar URL, usuario y contraseña de Neon. Este archivo **no se sube a GitHub**.
2. **Migraciones**: si la BD de Neon se creó con una versión anterior del script, ejecutar en el SQL Editor de Neon los archivos `database/migracion_XX_*.sql` en orden.
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
| 21/10 | Autenticación con **token propio** (UUID en memoria), expira a las **8 h**, el logout lo invalida en el backend | No agrega librerías (sin Spring Security ni JWT) y el equipo lo puede explicar completo |
| 21/10 | La regla de contraseña se exige al **registrarse**, no al iniciar sesión | Para no bloquear usuarios que ya existían |
| 21/10 | Contraseña con máximo **64** caracteres | BCrypt solo procesa hasta 72 bytes; con más, falla |
| 21/10 | Tabla `prestamo`: se mantienen los nombres del **script** (`fecha_prestamo`, `fecha_devolucion_prevista`, `fecha_devolucion_real`) | Decisión del equipo |
| 21/10 | Eliminación lógica: préstamo → estado `anulado`; libro → estado `baja` | No se borran filas |
| 21/10 | La ampliación de un préstamo suma **3 horas más** (constante `HORAS_AMPLIACION`) | Supuesto: la regla no dice cuánto. **Pendiente de confirmar** |
| 21/10 | Una devolución con estado `dañado` **no** crea una incidencia automáticamente | El encargado registra incidencias aparte |
| 21/10 | **Reservar aparta un ejemplar**: el stock baja 1 (con 0 el libro queda `reservado`). Cancelar o dejar vencer la reserva devuelve el ejemplar | Regla "al reservar, el stock se actualiza solo" |
| 21/10 | Si el encargado presta un libro que el prestatario tenía reservado, la reserva pasa a `atendida` y el stock **no** baja otra vez | El ejemplar ya estaba apartado |
| 21/10 | El límite de recojo de una reserva es el **final del turno** elegido; se puede reservar hasta **7 días** antes | Supuesto. Una reserva no recogida a tiempo pasa a `vencida` |
| 21/10 | El stock se maneja en **un solo lugar** (`LibroService`); préstamos y reservas lo reutilizan | No repetir código (SOLID) |

---

## Prioridad 1 – Seguridad ✅

| Cambio | Archivo(s) | Por qué |
|---|---|---|
| Se quitaron URL, usuario y contraseña de Neon escritos en el código; ahora solo se leen de variables de entorno o de `backend/.env` (`spring.config.import=optional:file:.env[.properties]`) | [application.properties](backend/src/main/resources/application.properties) | Las credenciales estaban en GitHub. **La contraseña de Neon se cambió** porque sigue en el historial de Git |
| Plantilla de variables sin valores | [backend/.env.example](backend/.env.example) | Para que cada integrante sepa qué configurar |
| Se quitó el botón ADMIN y `loginAdmin()` (tenía `admin@... / 123456` escrito) | [login.component.ts](frontend/src/app/pages/login/login.component.ts), [.html](frontend/src/app/pages/login/login.component.html), [.less](frontend/src/app/pages/login/login.component.less) | Credenciales visibles en el frontend |
| El login solo valida con **BCrypt** (se quitó la comparación en texto plano) | [AuthService.java](backend/src/main/java/com/backend/demo/service/AuthService.java) | Una contraseña sin hash nunca debe aceptarse |
| CORS: `"*"` → `http://localhost:4200` | Todos los controllers | Solo el frontend propio puede llamar a la API |
| `.gitignore` de la raíz: ignora `.env`, `CLAUDE.md` y `.claude/` en todo el repo; permite `.env.example` | [.gitignore](.gitignore), [backend/.gitignore](backend/.gitignore) | Que las reglas apliquen a todas las carpetas |

---

## Prioridad 2 – Login y autorización ✅

### Backend

| Archivo | Qué hace |
|---|---|
| [seguridad/ValidadorContrasena.java](backend/src/main/java/com/backend/demo/seguridad/ValidadorContrasena.java) | Regex: 8–64 caracteres, una minúscula, una mayúscula, un número y un símbolo especial |
| [service/SesionService.java](backend/src/main/java/com/backend/demo/service/SesionService.java) | Crea el token (UUID), lo guarda en memoria con usuario, rol y hora de vencimiento (+8 h); lo valida y lo invalida (logout) |
| [seguridad/RolesPermitidos.java](backend/src/main/java/com/backend/demo/seguridad/RolesPermitidos.java) | Anotación para marcar qué roles pueden usar un endpoint. Sin anotación, el endpoint es público |
| [seguridad/AutorizacionInterceptor.java](backend/src/main/java/com/backend/demo/seguridad/AutorizacionInterceptor.java) | Se ejecuta antes de cada endpoint: sin token o con token vencido responde **401**; con un rol no permitido responde **403** |
| [seguridad/WebConfig.java](backend/src/main/java/com/backend/demo/seguridad/WebConfig.java) | Registra el interceptor para `/api/**` |
| [seguridad/Rol.java](backend/src/main/java/com/backend/demo/seguridad/Rol.java), [Sesion.java](backend/src/main/java/com/backend/demo/seguridad/Sesion.java) | Constantes de roles y datos de una sesión |
| [service/AuthService.java](backend/src/main/java/com/backend/demo/service/AuthService.java) | Lógica de login, registro y logout (antes estaba en el controller). Detecta el rol buscando al usuario en `administrador` → `encargado` → si no, `PRESTATARIO`. Rechaza a los usuarios `inactivo` |
| [controller/AuthController.java](backend/src/main/java/com/backend/demo/controller/AuthController.java) | Solo recibe y responde. Nuevo endpoint `POST /api/auth/logout` |
| [exception/NegocioException.java](backend/src/main/java/com/backend/demo/exception/NegocioException.java), [ManejadorErrores.java](backend/src/main/java/com/backend/demo/exception/ManejadorErrores.java) | Los errores del negocio llegan al frontend como `{"mensaje": "..."}` con su código HTTP |
| [dao/UsuarioDao.java](backend/src/main/java/com/backend/demo/dao/UsuarioDao.java) | Consultas `esAdministrador`, `esEncargado`, `esPrestatario`, `registrarComoPrestatario` (reemplazan el `JdbcTemplate` del controller) |
| [dto/LoginRespuestaDTO.java](backend/src/main/java/com/backend/demo/dto/LoginRespuestaDTO.java) | La respuesta del login: token, fecha de vencimiento y datos del usuario |

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
- Los usuarios de prueba (`admin@`, `encargado@`, `alumno@alejandria.edu.pe`) tenían `123456`, que no cumple la regla. Se les copió el hash de una contraseña válida con un `UPDATE` en Neon.

---

## Prioridad 3 – CRUD de préstamos ✅ (falta probarlo completo en la web)

### 3.1 Base de datos y backend de préstamos ✅

**Migración** [database/migracion_01_prestamo.sql](database/migracion_01_prestamo.sql), ejecutada en Neon el 21/10 (también aplicada en [schema_postgresql.sql](database/schema_postgresql.sql)):
- `prestamo.ampliado BOOLEAN NOT NULL DEFAULT FALSE`
- `prestamo.estado_fisico_devolucion` (`bueno` / `dañado`)
- `estado_prestamo` acepta además `anulado`
- `estado_libro` acepta además `baja`

**Archivos nuevos:**

| Archivo | Qué hace |
|---|---|
| [entity/Prestamo.java](backend/src/main/java/com/backend/demo/entity/Prestamo.java) | Entidad JPA de la tabla `prestamo` |
| [dao/PrestamoDao.java](backend/src/main/java/com/backend/demo/dao/PrestamoDao.java) | Consulta de **una tabla** (`buscarPorPrestatario`), consulta con **JOIN** (`listarActivosConDetalle`: prestamo + usuario + libro), `marcarVencidos` y `tieneSancionActiva` (sancion → incidencia → prestamo) |
| [service/PrestamoService.java](backend/src/main/java/com/backend/demo/service/PrestamoService.java) | Todas las reglas del negocio (ver abajo) |
| [controller/PrestamoController.java](backend/src/main/java/com/backend/demo/controller/PrestamoController.java) | Endpoints REST con sus permisos |
| `dto/RegistroPrestamoDTO`, `DevolucionDTO`, `PrestamoDTO`, `PrestamoDetalleDTO` | Lo que entra y sale de la API |
| [dao/LibroDao.java](backend/src/main/java/com/backend/demo/dao/LibroDao.java) | `buscarParaActualizar`: lee el libro bloqueando su fila para que dos préstamos simultáneos no descuadren el stock |

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
[pages/encargado-panel/](frontend/src/app/pages/encargado-panel/), con estilos compartidos en [pages/panel-base.less](frontend/src/app/pages/panel-base.less) y el servicio [services/prestamo.service.ts](frontend/src/app/services/prestamo.service.ts).

| Pestaña | Qué hace | Endpoint |
|---|---|---|
| Préstamos activos | Tabla con prestatario, DNI, libro, inicio, hora límite y estado (`activo` / `vencido`). Botones **Ampliar** (se desactiva si ya se amplió), **Devolver** (modal: bueno / dañado) y **Anular** | `GET /activos` (JOIN), `PUT /{id}/ampliar`, `PUT /{id}/devolucion`, `DELETE /{id}` |
| Registrar préstamo | DNI del prestatario + libro (muestra el stock) | `POST /api/prestamos` |
| Reservas pendientes | Lista de reservas por recoger. El botón **Entregar** precarga el libro en "Registrar préstamo" | `GET /api/reservas` |

### 3.3 CRUD de libros ✅
**Backend:**

| Archivo | Qué hace |
|---|---|
| [service/LibroService.java](backend/src/main/java/com/backend/demo/service/LibroService.java) | Listar (sin los dados de baja), buscar, crear, actualizar y **dar de baja** (estado `baja`, no se borra la fila). No permite dar de baja un libro con préstamos sin devolver ni repetir un ISBN. También tiene `bloquearParaSalida`, `descontarStock` y `reponerStock`, que usan préstamos y reservas |
| [dto/LibroDTO.java](backend/src/main/java/com/backend/demo/dto/LibroDTO.java), [LibroFormDTO.java](backend/src/main/java/com/backend/demo/dto/LibroFormDTO.java) | Respuesta (con categoría y autores) y formulario |
| [controller/LibroController.java](backend/src/main/java/com/backend/demo/controller/LibroController.java) | `GET` públicos; `POST`, `PUT /{id}` y `DELETE /{id}` solo para el Administrador |
| [controller/CatalogoController.java](backend/src/main/java/com/backend/demo/controller/CatalogoController.java) | `GET /api/categorias` y `GET /api/autores` para el formulario |

**Frontend:** [pages/admin-panel/](frontend/src/app/pages/admin-panel/)
- Pestaña **Libros**: formulario para registrar o editar (título, ISBN, editorial, año, stock, categoría y autores) y una tabla con **Editar** y **Dar de baja**.
- Pestaña **Préstamos activos**: la misma consulta con JOIN, solo para ver.

### 3.4 Reservas conectadas al backend ✅
**Backend:**

| Archivo | Qué hace |
|---|---|
| [service/ReservaService.java](backend/src/main/java/com/backend/demo/service/ReservaService.java) | Crear (valida el prestatario, que no tenga otra reserva pendiente del mismo libro y la fecha de recojo; descuenta stock), cancelar (repone stock), `liberarVencidas` y `atenderSiExiste` (lo usa el préstamo) |
| [service/PrestatarioService.java](backend/src/main/java/com/backend/demo/service/PrestatarioService.java) | `validarHabilitado`: es prestatario, está activo y no tiene sanción activa (la usan préstamos y reservas) |
| [dto/ReservaRespuestaDTO.java](backend/src/main/java/com/backend/demo/dto/ReservaRespuestaDTO.java) | Reserva con título, autor, categoría y nombre del prestatario |
| [controller/ReservaController.java](backend/src/main/java/com/backend/demo/controller/ReservaController.java) | Ahora solo llama al service (antes tenía la lógica adentro) |

**Frontend:** [pages/user-catalog/](frontend/src/app/pages/user-catalog/)
- Se quitó la reserva escrita a mano ("La Ciudad y los Perros") y la lista de libros de respaldo.
- "Mis Reservas" se carga del backend (solo las pendientes).
- Reservar envía la fecha límite de recojo (fecha + fin del turno). Si el backend rechaza la reserva, el error se muestra en el modal; antes siempre decía "Reserva realizada".
- Cancelar llama al backend.
- Los libros con stock 0 muestran **Agotado** y no se pueden reservar.
- La fecha de recojo por defecto es hoy (antes estaba fija en 24/09/2026).

---

## Pendientes generales
- [ ] Actualizar el informe: mecanismo de autenticación (token de 8 h, 401/403, guards) y el máximo de 64 caracteres
- [ ] Actualizar el comentario de `schema_postgresql.sql` que todavía dice `'123456'`
- [ ] Confirmar cuántas horas suma una ampliación
- [ ] Pantallas de incidencias y sanciones (Encargado) y de usuarios (Admin): no están en las prioridades actuales
- [ ] Prioridad 4: Logback, POI (reporte Excel), Commons CSV, Guava, PDF (preguntar librería), JUnit
- [ ] Commit del **login** (prioridades 1 y 2) y commit del **CRUD** (prioridad 3), por separado

## Capturas para el anexo

| Prioridad | Captura | Estado |
|---|---|---|
| 1 | Diff de `application.properties` sin credenciales | ⬜ |
| 1 | Pantalla de login sin el botón ADMIN | ⬜ |
| 1 | DevTools: encabezado `Access-Control-Allow-Origin: http://localhost:4200` | ⬜ |
| 2 | Registro con contraseña débil (✗ en rojo) y válida (✓ en verde) | ⬜ |
| 2 | Login de cada perfil llegando a su pantalla | ⬜ |
| 2 | Prestatario escribiendo `/admin` y siendo redirigido a `/catalogo` | ⬜ |
| 2 | `GET /api/reservas` sin token → 401; con un prestatario → 403 | ⬜ |
| 2 | Token reusado después del logout → 401 | ⬜ |
| 3 | Migración ejecutada en Neon (8 ALTER correctos) | ✅ |
| 3 | Encargado: registrar préstamo con mensaje "Hora límite: ..." | ⬜ |
| 3 | Encargado: tabla de préstamos activos (consulta con JOIN) | ⬜ |
| 3 | Encargado: Ampliar (y el botón desactivado al segundo intento) | ⬜ |
| 3 | Encargado: modal de devolución (bueno / dañado) | ⬜ |
| 3 | Encargado: error al prestar un libro sin stock | ⬜ |
| 3 | Stock antes y después de prestar o devolver (tabla de libros del admin o Neon) | ⬜ |
| 3 | Admin: registrar, editar y dar de baja un libro (y que desaparezca del catálogo) | ⬜ |
| 3 | Prestatario: reservar → aparece en "Mis Reservas" → cancelar | ⬜ |
| 3 | Código: `PrestamoDao` (consulta de una tabla + consulta con JOIN) y `PrestamoService.registrar` | ⬜ |
