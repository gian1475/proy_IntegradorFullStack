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
├── schema_postgresql.sql        Script completo de la base de datos con datos iniciales
├── migracion_01_prestamo.sql    Cambios para una base creada con una versión anterior (préstamos)
├── migracion_02_sancion_monto.sql  Monto de las multas
├── migracion_03_reserva_cola.sql   Estado 'en_espera' para la cola de espera de reservas
├── datos_demo.sql               21 libros reales con sus autores (datos verificados en Open Library)
└── ejemplo_importar_libros.csv  Archivo de ejemplo para importar libros
```

Flujo de una petición: **controller → service → dao → base de datos**.

## Arquitectura

- **MVC** (con API REST): Modelo = entidades y services; Vista = Angular; Controlador = `@RestController`.
- **DAO**: todo el acceso a datos está en `dao/`; los services no escriben SQL.
- **DTO**: la API recibe y devuelve DTOs, no las entidades.
- **SOLID**: cada service tiene una sola responsabilidad (por ejemplo, el stock solo se maneja en `LibroService` y lo reutilizan préstamos y reservas).
- **Pruebas unitarias** con JUnit 5 y Mockito.

## Perfiles

| Perfil | Pantalla | Puede |
|---|---|---|
| Administrador | `/admin` | Dashboard con los indicadores de gestión, gestionar libros (CRUD), ver préstamos activos y el historial, eliminar reservas canceladas o vencidas, registrar encargados, descargar reportes, ver el monitoreo |
| Encargado | `/encargado` | Registrar préstamos, ampliarlos, registrar devoluciones, anular préstamos, atender reservas y ver la cola de espera, ver el historial, descargar reportes |
| Prestatario | `/catalogo` | Ver el catálogo (stock y estado de cada libro), reservar, entrar a la cola de espera si no hay stock, cancelar sus reservas, ver y filtrar sus préstamos |

El backend valida el rol en cada endpoint (`@RolesPermitidos`): sin sesión responde **401** y con un rol no permitido responde **403**.

## Cómo levantar el proyecto

1. **Credenciales**: copiar `backend/.env.example` como `backend/.env` y completar la URL, el usuario y la contraseña de Neon. El `.env` no se sube a GitHub.
2. **Base de datos**: si es una base nueva, ejecutar en el SQL Editor de Neon `database/schema_postgresql.sql` y después `database/datos_demo.sql` (libros para la demo). Si ya existía, ejecutar las migraciones que falten (`migracion_01_prestamo.sql`, `migracion_02_sancion_monto.sql`, `migracion_03_reserva_cola.sql`) y `datos_demo.sql`. Todos se pueden ejecutar más de una vez.
   **Horario de atención:** el backend no registra préstamos fuera de 08:00–20:00 y la hora límite nunca pasa de las 20:00 (`app.horario.apertura` y `app.horario.cierre` en `application.properties`). Para probar de noche se puede poner `app.horario.cierre=23:59` y volverlo a `20:00` antes de la demo.
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

154 pruebas del backend: regla de contraseña (también en bytes), validación de datos del usuario, perfil único, bloqueo por intentos fallidos, sesión de 15 minutos, cálculo de la hora límite (3 horas), horario de atención (08:00–20:00), ampliación (1 hora, sin reservas, sin pasar del cierre), devolución (dañado → mantenimiento), anulación, baja de libros, búsqueda del catálogo, cola de espera de reservas, eliminación física de reservas, historial con filtros, los 8 reportes, registro de encargados, indicadores del dashboard, monto de las multas, manejo de errores y consultas contra la BD real (con la hora de Lima). Las pruebas que usan la BD se deshacen al terminar: no dejan datos guardados.

Frontend (alertas de 15 minutos, estado y búsqueda del catálogo, regla de contraseña), 16 pruebas:

```
cd frontend
npx ng test --watch=false
```

## Endpoints principales

| Recurso | Endpoints | Quién |
|---|---|---|
| Autenticación | `POST /api/auth/login`, `POST /api/auth/registro`, `POST /api/auth/logout` | Público |
| Libros | `GET /api/libros`, `GET /buscar?q=` (título, autor, categoría o ISBN) (públicos); `POST`, `PUT /{id}`, `DELETE /{id}` | Administrador |
| Préstamos | `POST /api/prestamos`, `GET /activos`, `GET /historial?dni=&libro=&desde=&hasta=`, `GET /prestatario/{id}`, `PUT /{id}/ampliar`, `PUT /{id}/devolucion`, `DELETE /{id}` | Encargado (y Administrador en las consultas) |
| Reservas | `GET /api/reservas`, `GET /usuario/{id}`, `POST` (sin stock entra a la cola de espera), `PUT /{id}/cancelar`; `DELETE /{id}` (solo admin) | Según el rol |
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
| JUnit 5 + Mockito | Pruebas unitarias |

## Monitoreo

Actuator corre en el puerto **8081** y solo responde desde la misma PC:

- `http://localhost:8081/actuator/health` – estado de la aplicación, la base de datos y el disco
- `http://localhost:8081/actuator/info` – nombre y versión
- `http://localhost:8081/actuator/metrics` – todas las métricas

El administrador ve un resumen en la pestaña **Monitoreo** de su panel.
