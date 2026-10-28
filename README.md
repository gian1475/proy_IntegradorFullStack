# Sistema Web de Control de Préstamos – Biblioteca de Alejandría

Proyecto del curso **Integrador I: Sistemas-Software** (UTP).
Controla los préstamos de libros **en sala** (máximo 3 horas), las devoluciones, las reservas y el stock de cada libro.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Data JPA, Maven |
| Base de datos | PostgreSQL en Neon (nube) |
| Frontend | Angular 22 (componentes standalone y signals), LESS; Bootstrap 5.3 solo en el dashboard |
| Seguridad | BCrypt (`spring-security-crypto`) para las contraseñas, token de sesión propio |

## Estructura

```
backend/src/main/java/com/alejandria/biblioteca/
├── BibliotecaApplication.java   Arranca la aplicación
├── controller/   Endpoints REST: reciben la petición y llaman al service
├── service/      Reglas del negocio (préstamos, libros, reservas, login, sesión, reportes, monitoreo)
├── dao/          Acceso a la base de datos (interfaces de Spring Data JPA y consultas SQL/JPQL)
├── entity/       Entidades JPA (una por tabla)
├── dto/          Objetos que entran y salen de la API, agrupados por módulo:
│   ├── auth/       login y registro
│   ├── usuario/    datos del usuario
│   ├── libro/      libros, categorías y autores
│   ├── prestamo/   préstamos y devoluciones
│   ├── reserva/    reservas
│   └── reporte/    reportes, importación CSV y monitoreo
├── seguridad/    Validación de contraseña y datos, roles, sesión, CORS e interceptor de autorización
└── exception/    Errores con su código HTTP y su mensaje

frontend/src/
├── styles/panel.less   Estilos compartidos de los paneles
└── app/
    ├── pages/          Pantallas: home, login, registro, catálogo y los paneles
    │   ├── admin-panel/       Panel del Administrador; cada pestaña está en tabs/
    │   │   └── tabs/          dashboard, libros, prestamos, reservas, encargados, reportes, monitoreo
    │   └── encargado-panel/   Panel del Encargado; cada pestaña está en tabs/
    │       └── tabs/          activos, registrar, reservas
    ├── components/     Header, banner, secciones de libros, historial de préstamos y descarga de reportes
    │                   (estos dos últimos los usan los paneles del admin y del encargado)
    ├── services/       Llamadas al backend (auth, libros, préstamos, reservas, reportes, usuarios)
    ├── guards/         Bloqueo de rutas por rol
    ├── interceptors/   Agrega el token a cada petición y renueva la sesión
    └── utils/          Funciones compartidas (mensajes de error, alertas, estado del libro, regla de contraseña)

database/
├── schema_postgresql.sql        Crea la base completa (estructura y datos iniciales). Ya incluye todas las migraciones
├── datos_demo.sql               21 libros reales con sus autores (datos verificados en Open Library) y
│                                personal, prestatarios, préstamos, reservas y sanciones ficticios para la demo
├── ejemplo_importar_libros.csv  Archivo de ejemplo para importar libros
└── migraciones/                 Cambios aplicados a Neon sin borrar sus datos (una base nueva no los necesita)
    ├── migracion_01_prestamo.sql             Préstamos: ampliación, estado físico y eliminación lógica
    ├── migracion_02_sancion_monto.sql        Monto de las multas
    ├── migracion_03_reserva_cola.sql         Estado 'en_espera' para la cola de espera de reservas
    └── migracion_04_quitar_fk_hibernate.sql  Quita 3 llaves foráneas que creó Hibernate

docs/
├── Avance_III_IntegradorV8_corregido.docx  Informe del Avance III
└── capturas/                               Capturas de pantalla para los anexos del informe

BITACORA.md   Todo lo que se cambió, por qué, y los marcadores para buscar cada parte en el código
```

Flujo de una petición: **controller → service → dao → base de datos**.

## Arquitectura

- **MVC** (con API REST): Modelo = entidades y services; Vista = Angular; Controlador = `@RestController`.
- **DAO**: todo el acceso a datos está en `dao/`; los services no escriben SQL.
- **DTO**: la API recibe y devuelve DTOs, no las entidades.
- **SOLID**: cada service tiene una sola responsabilidad (por ejemplo, el stock solo se maneja en `LibroService` y lo reutilizan préstamos y reservas).
- **TDD**: primero la prueba, después el código. JUnit 5 y Mockito en el backend, Vitest en el frontend.

## Perfiles

| Perfil | Pantalla | Puede |
|---|---|---|
| Administrador | `/admin` | Dashboard con los indicadores de gestión, gestionar libros (CRUD), ver préstamos activos y el historial, eliminar reservas canceladas o vencidas, registrar encargados, descargar reportes, ver el monitoreo. Por la API (sin botón en su panel) también puede anular préstamos |
| Encargado | `/encargado` | Registrar préstamos, ampliarlos, registrar devoluciones, anular préstamos, atender reservas y ver la cola de espera, ver el historial, descargar reportes. Por la API (sin botón en su panel) también puede registrar y cancelar reservas a nombre de un prestatario (ventanilla) |
| Prestatario | `/catalogo` | Ver el catálogo (stock y estado de cada libro), reservar, entrar a la cola de espera si no hay stock, cancelar sus reservas, ver y filtrar sus préstamos |

El backend valida el rol en cada endpoint (`@RolesPermitidos`): sin sesión responde **401** y con un rol no permitido responde **403**.

## Cómo levantar el proyecto

1. **Credenciales**: copiar `backend/.env.example` como `backend/.env` y completar la URL, el usuario y la contraseña de Neon. El `.env` no se sube a GitHub.
2. **Base de datos**: si es una base nueva, ejecutar en el SQL Editor de Neon `database/schema_postgresql.sql` y después `database/datos_demo.sql` (libros y movimientos de ejemplo para la demo). Si ya existía, ejecutar las migraciones que falten, que están en `database/migraciones/` (`migracion_01_prestamo.sql`, `migracion_02_sancion_monto.sql`, `migracion_03_reserva_cola.sql`, `migracion_04_quitar_fk_hibernate.sql`) y `datos_demo.sql`. Todos se pueden ejecutar más de una vez.
   **Horario de atención:** el backend no registra préstamos fuera de 08:00–20:00 y la hora límite nunca pasa de las 20:00 (`app.horario.apertura` y `app.horario.cierre` en `application.properties`). Para probar de noche, sin tocar el archivo, definir la variable antes de levantar el backend: `$env:APP_HORARIO_CIERRE="23:59"` (PowerShell). En el repositorio el cierre se queda en `20:00`.
   **Hora:** el backend siempre usa la hora de Lima (`America/Lima`, se fija al arrancar en `BibliotecaApplication`), aunque la PC o el servidor estén en otra zona. El servidor de Neon está en hora GMT, por eso la hora la manda Java y no la BD.
3. **Backend** (desde la carpeta `backend/`, porque ahí busca el `.env`):
   ```
   cd backend
   ./mvnw spring-boot:run
   ```
4. **Frontend** (en otra terminal):
   ```
   cd frontend
   npm install
   npm start
   ```
   Abrir `http://localhost:4200`.

## Cuentas y datos de la demo

| Perfil | Cuentas |
|---|---|
| Administrador | `admin@alejandria.edu.pe` |
| Encargado | `encargado@alejandria.edu.pe` (Carlos), `miguel.torres@…` (mañana), `rosa.quispe@…` (tarde) |
| Prestatario | `alumno@alejandria.edu.pe` (Juan) y 7 más con correo `@alumnos.alejandria.edu.pe` (uno de ellos, José Luis, `inactivo`) |

Todas usan la misma **contraseña de prueba**, que maneja el equipo: no está en el repositorio (en la BD solo está su hash BCrypt). Para cambiarla: `backend\herramientas\cambiar-contrasena.cmd`. Las personas de la demo son ficticias.

`datos_demo.sql` carga 21 libros reales, 28 préstamos (devueltos, ampliados, uno dañado, uno con retraso, anulados y 2 vencidos de hoy), 11 reservas (2 en la cola de espera de *Cien Años de Soledad*) y 2 sanciones. Las fechas se calculan desde el día en que se ejecuta, así que el dashboard y los reportes del mes tienen datos. Casos para mostrar en vivo:

- **Sanción activa:** a Valeria (`valeria.flores@…`) no se le puede prestar ni reservar.
- **Devolución con retraso:** los 2 préstamos vencidos de hoy.
- **Cola de espera:** si se cancela la reserva pendiente de *Cien Años de Soledad*, el ejemplar pasa a Camila, la primera de la cola.
- **Mantenimiento:** *Cosmos* volvió dañado y era su único ejemplar.

## Empaquetado (.jar)

```
cd backend
./mvnw clean package
java -jar target/biblioteca-alejandria.jar
```

`clean package` compila, ejecuta las pruebas y genera `target/biblioteca-alejandria.jar`. El `.jar` debe ejecutarse desde `backend/` para que encuentre el `.env` (o con las variables `SPRING_DATASOURCE_*` definidas en el sistema).

## Pruebas

```
cd backend
./mvnw test
```

160 pruebas del backend: zona horaria (hora de Lima), regla de contraseña (también en bytes), validación de datos del usuario, perfil único, bloqueo por intentos fallidos, sesión de 15 minutos, cálculo de la hora límite (3 horas), horario de atención (08:00–20:00), ampliación (1 hora, sin reservas, sin pasar del cierre), devolución (dañado → mantenimiento), anulación, baja de libros, búsqueda del catálogo, cola de espera de reservas, eliminación física de reservas, historial con filtros, los 8 reportes, registro de encargados, indicadores del dashboard, monto de las multas, manejo de errores y consultas contra la BD real (con la hora de Lima). Las pruebas que usan la BD se deshacen al terminar: no dejan datos guardados.

Frontend (alertas de 15 minutos, estado y búsqueda del catálogo, regla de contraseña), 16 pruebas:

```
cd frontend
npx ng test --watch=false
```

## Endpoints principales

| Recurso | Endpoints | Quién |
|---|---|---|
| Autenticación | `POST /api/auth/login`, `POST /api/auth/registro`, `POST /api/auth/logout` | Público |
| Libros | `GET /api/libros`, `GET /{id}`, `GET /buscar?q=` (título, autor, categoría o ISBN), `GET /autor/{nombre}` (públicos); `POST`, `PUT /{id}`, `DELETE /{id}` | Administrador |
| Categorías y autores | `GET /api/categorias`, `GET /api/autores` (listas para los formularios) | Público |
| Préstamos | `POST /api/prestamos`, `PUT /{id}/ampliar`, `PUT /{id}/devolucion` | Encargado |
| | `GET /activos`, `GET /{id}`, `GET /historial?dni=&libro=&desde=&hasta=`, `DELETE /{id}` (anular) | Encargado y Administrador |
| | `GET /prestatario/{id}` | Los 3 perfiles (el prestatario, solo los suyos) |
| Reservas | `GET /api/reservas` | Administrador y Encargado |
| | `GET /usuario/{id}` | Los 3 perfiles (el prestatario, solo las suyas) |
| | `POST` (sin stock entra a la cola de espera), `PUT /{id}/cancelar` | Prestatario (solo las suyas) y Encargado |
| | `DELETE /{id}` (eliminación física) | Administrador |
| Reportes | `GET /api/reportes/{tipo}/{formato}?desde=&hasta=` (tipo: `prestamos`, `devoluciones`, `reservas`, `incidencias`, `bajas`, `stock`, `indicadores`, `tiempos`; formato: `excel`, `pdf`, `csv`) | Administrador y Encargado |
| Importar libros | `POST /api/reportes/libros/csv` | Administrador |
| Encargados | `GET /api/usuarios/encargados`, `POST /api/usuarios/encargados` | Administrador |
| Monitoreo | `GET /api/monitoreo` | Administrador |
| Dashboard | `GET /api/dashboard` | Administrador |

Las eliminaciones de libros y préstamos son **lógicas**: el registro no se borra, cambia su estado (`baja` / `anulado`). Las reservas canceladas o vencidas se eliminan **físicamente** (`DELETE`), solo el administrador.

## Librerías

| Librería | Uso |
|---|---|
| PostgreSQL JDBC + Spring Data JPA | Conexión y consultas a la base de datos |
| Spring Security Crypto (BCrypt) | Guardar las contraseñas cifradas |
| Google Guava | Bloquear el login 15 minutos después de 3 intentos fallidos |
| Apache POI | Los reportes en Excel |
| Apache Commons CSV | Exportar los reportes e importar libros en CSV |
| OpenPDF | Los reportes en PDF |
| Logback | Registro de logins y préstamos en `backend/logs/biblioteca.log` |
| Spring Boot Actuator | Monitoreo: estado de la aplicación y de la BD, memoria, CPU, peticiones |
| Bootstrap 5.3 | Diseño del dashboard del administrador (encerrado en `.dashboard-bootstrap` para no cambiar las otras pantallas) |
| JUnit 5 + Mockito | Pruebas del backend |
| Vitest | Pruebas del frontend |

## Monitoreo

Actuator corre en el puerto **8081** y solo responde desde la misma PC:

- `http://localhost:8081/actuator/health` – estado de la aplicación, la base de datos y el disco
- `http://localhost:8081/actuator/info` – nombre y versión
- `http://localhost:8081/actuator/metrics` – todas las métricas

El administrador ve un resumen en la pestaña **Monitoreo** de su panel.
