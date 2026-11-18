# Guía S13: pruebas de software (clase del lunes 02/11, entrega del 04/11)

Lo que pidió el profe para el miércoles 04/11, dónde está en el proyecto, cómo ejecutarlo, cómo mostrarlo en la app y qué tabla revisar en Neon.
Al final hay **preguntas probables con su respuesta**.

> Estado al 11/11/2026: **114 de 114** pruebas del S13 pasan contra Neon.

---

## Resumen rápido

| Pide el S13 | Prueba (archivo) | En la app (admin) | Tabla en Neon |
|---|---|---|---|
| Tercer commit | `9f2e698` en GitHub | — | — |
| Prueba unitaria de **login** | `AuthServiceLoginTest` | Pantalla de **Login** | `usuario` |
| Prueba de **suma** | `LibroServiceTest`, `PrestamoServiceTest` | Encargado → **devolución** (stock +1) | `libro.stock`, `prestamo` |
| Prueba de **modificación** | `UsuarioGestionServiceTest`, `ReservaActualizacionTest` | **Usuarios → Editar**, **Libros → Editar**, **Reservas → Cambiar fecha** | `usuario`, `encargado`, `libro`, `reserva` |
| Pruebas de **integración con la BD** | `UsuarioBdTest`, `ConcurrenciaStockTest`, `LibroDaoTest`, `ReporteBdTest` | — (se corren con Maven) | Neon (se deshacen solas) |
| **Contraseña** (criterios) | `ValidadorContrasenaTest` | **Registro** y **Usuarios → Contraseña** | `usuario.contrasena` (hash BCrypt) |
| **Tipos de datos, longitud y rangos** | `ValidacionDtoTest` | **Libros → Registrar libro**, **Usuarios → Registrar encargado** | `libro`, `usuario` |

**Ejecutar todas las del S13 de una vez** (desde `backend/`):
```bash
cd backend
./mvnw test -Dtest="AuthServiceLoginTest,ValidadorContrasenaTest,ValidacionDtoTest,PrestamoServiceTest,LibroServiceTest,UsuarioGestionServiceTest,UsuarioBdTest,ConcurrenciaStockTest"
```
Resultado esperado: `Tests run: 114, Failures: 0, Errors: 0` → `BUILD SUCCESS`.

---

## ¿Se puede mostrar todo desde el ADMIN?

**Casi todo.** Hay 4 cosas que el admin no puede hacer (el backend responde 403) y necesitan otro usuario.

### ✅ Se muestra desde Admin
| Lo que pide | Dónde |
|---|---|
| Búsqueda | Catálogo, buscar por ISBN |
| Consulta de **2 tablas** | Pestaña **Libros por categoría** |
| Agregar, modificar y dar de baja **libros** | **Libros** (solo el admin puede; el encargado no) |
| Agregar, modificar, dar de baja y reactivar **usuarios** | **Usuarios** |
| Regla de la **contraseña** | **Usuarios → Contraseña** (poner una débil y sale el mensaje) |
| Tipos, longitud y rangos | **Libros** (año 3000) y **Usuarios → Registrar encargado** (DNI de 4 dígitos) |
| Modificar reserva | **Reservas → Cambiar fecha** |
| Eliminación lógica y **física** | **Reservas → Cancelar** y luego **Eliminar** |
| Los **6 reportes** | **Reportes** |
| Login (3 intentos) | Pantalla de login, antes de entrar |

### ❌ Lo que NO puede el admin
| Lo que pide | Quién lo hace | Alternativa |
|---|---|---|
| Consulta de **1 tabla** ("Mis Préstamos") | **Prestatario** | Entrar 1 minuto como `lucia.ramirez@alumnos.alejandria.edu.pe` |
| **Suma** con la devolución (stock +1) | **Encargado** | Desde admin: **Libros → Editar** y subir el stock (ej. 3 → 4) → **Guardar cambios**. También es una suma; en Neon se ve en `libro.stock` |
| **Crear una reserva** (para luego cancelarla y eliminarla) | **Prestatario** o **Encargado** | Si ya hay una `cancelada` o `vencida` en **Reservas**, eliminar esa directamente |
| Registrar, ampliar, devolver o anular préstamos | **Encargado** | No lo pide la guía; solo si el profe pregunta |

**Por qué:** el admin gestiona el catálogo y los usuarios; el encargado atiende la ventanilla (préstamos y devoluciones); el prestatario solo ve lo suyo. Los permisos están en cada controller con `@RolesAllowed`.

### Preparar antes de la revisión
- [ ] En Admin → **Reservas**, ver si hay alguna **cancelada o vencida**. Si no hay, **Cancelar** una pendiente (así queda lista para **Eliminar**).
- [ ] Tener **otra ventana en incógnito** con el **prestatario** logueado en "Mis Préstamos" (consulta de 1 tabla), para no cerrar sesión delante del profe.
- [ ] Elegir un libro **sin préstamos activos ni reservas** para darlo de baja en vivo.

---

## 0. Tercer commit

- **Commit:** `9f2e698`, "Tercer avance: pruebas de login, validaciones e integración con la BD; D8, D13, D14 y D20".
- **Fecha y autor:** 04/11/2026, 07:37 (gian_1475). Está subido a `origin/main`.
- **En GitHub:** https://github.com/gian1475/proy_IntegradorFullStack/commits/main (es el 5.º de arriba).
- **Terminal:** `git show --stat 9f2e698`
- **Qué trae:** `AuthServiceLoginTest` (nuevo), `ValidacionDtoTest` (+50 líneas), `LibroDaoTest` (integración), `HorarioAtencionTest`.

---

## 1. Prueba unitaria del login

**Ejecutar:** `./mvnw test -Dtest=AuthServiceLoginTest` → 5 pruebas.

| Prueba | Qué comprueba |
|---|---|
| `conLaContrasenaCorrectaEntraYRecibeUnToken` | Con usuario y contraseña bien, entra y recibe su token |
| `elCorreoSeAceptaConMayusculasYEspacios` | `" ADMIN@alejandria.edu.pe "` igual entra |
| `conLaContrasenaIncorrectaNoEntra` | Contraseña mal → 401 "Credenciales incorrectas" |
| `unCorreoQueNoExisteRecibeElMismoMensaje` | No revela si el correo existe (seguridad) |
| `alTercerIntentoFallidoQuedaBloqueadoAunqueDespuesPongaLaCorrecta` | 3 intentos fallidos → bloqueado (RF10) |

**En la app:**
1. Abrir `http://localhost:4200/login`.
2. Poner una contraseña mal 3 veces → sale el mensaje de bloqueo.

**En Neon:**
```sql
SELECT id_usuario, nombre, correo, estado, contrasena FROM usuario WHERE correo = 'admin@alejandria.edu.pe';
```
La contraseña sale como `$2a$10$...`: es el **hash BCrypt**, nunca el texto.

---

## 2. Prueba de la suma

Una "suma" del negocio: **devolver un libro suma 1 al stock**, y **ampliar un préstamo suma 1 hora**.

**Ejecutar:** `./mvnw test -Dtest=LibroServiceTest,PrestamoServiceTest`

| Prueba | Qué suma |
|---|---|
| `LibroServiceTest.alDevolverseOtroEjemplarElLibroSaleDeMantenimiento` | stock 0 → **1** y el libro vuelve a `disponible` |
| `PrestamoServiceTest.devolverEnBuenEstadoReponeElStock` | Al devolver en buen estado llama a `reponerStock` (+1) |
| `PrestamoServiceTest.ampliarSumaUnaHoraYMarcaAmpliado` | Hora límite + **1 hora** (RF05) |

**En la app (desde Admin):**
1. **Libros** → **Editar** un libro → subir el stock en 1 → **Guardar cambios**.
2. La tabla de libros muestra el nuevo stock.

**Con el Encargado (si hay tiempo):**
1. **Préstamos activos** → **Devolver** (estado "bueno").
2. Admin → **Libros**: el stock de ese libro subió 1.

**En Neon (antes y después):**
```sql
SELECT id_libro, titulo, stock, estado_libro FROM libro WHERE titulo ILIKE '%<titulo>%';
SELECT id_prestamo, fecha_devolucion_prevista, fecha_devolucion_real, ampliado, estado_prestamo
FROM prestamo ORDER BY id_prestamo DESC LIMIT 5;
```

---

## 3. Prueba de modificación de registros

**Ejecutar:** `./mvnw test -Dtest=UsuarioGestionServiceTest,ReservaActualizacionTest,UsuarioBdTest`

| Prueba | Qué modifica |
|---|---|
| `cambiaLosDatosDelPrestatarioLimpiandoEspaciosYMayusculas` | Edita nombre, DNI y correo |
| `alEncargadoTambienLeCambiaElTurno` | Cambia el turno (`mañana` / `tarde`) |
| `elDniNoPuedeSerElDeOtroUsuario` | No deja poner un DNI repetido |
| `cambiaLaFechaYLaFranjaDeUnaReservaPendiente` | Cambia la fecha de recojo (RF15) |
| `UsuarioBdTest.alEncargadoSeLeCambiaElTurnoEnLaTablaEncargado` | Igual, pero **contra Neon** |

### Dónde se agrega y modifica en la app del ADMINISTRADOR

| Qué | Pestaña | Botón | Tabla en Neon |
|---|---|---|---|
| **Agregar libro** | **Libros** | Formulario "Registrar libro" (arriba) → **Registrar** | `libro` (+ `libro_autor`) |
| **Modificar libro** (solo el **stock**) | **Libros** | **Editar** en la fila → cambiar el stock → **Guardar cambios**. Los demás campos salen bloqueados | `libro.stock` |
| Eliminar libro (lógica) | **Libros** | **Dar de baja** | `libro.estado_libro = 'baja'` |
| **Agregar encargado** | **Usuarios** | **Registrar encargado** | `usuario` + `encargado` (turno) |
| **Modificar usuario** | **Usuarios** | **Editar** → **Guardar cambios** | `usuario` (y `encargado.turno`) |
| Cambiar contraseña | **Usuarios** | **Contraseña** → **Guardar contraseña** | `usuario.contrasena` |
| Eliminar usuario (lógica) / volver | **Usuarios** | **Dar de baja** / **Reactivar** | `usuario.estado` (`inactivo` / `activo`) |
| **Modificar reserva** | **Reservas** | **Cambiar fecha** → **Guardar** | `reserva.fecha_recojo_limite` |
| Cancelar reserva (lógica) | **Reservas** | **Cancelar** | `reserva.estado_reserva = 'cancelada'` |
| Eliminar reserva (**física**) | **Reservas** | **Eliminar** (solo canceladas o vencidas) | `reserva` (la fila desaparece) |
| Registrar conteo físico | **Auditoría** | **Registrar** | `auditoria_inventario` |
| Consultar 2 tablas | **Libros por categoría** | combo de categoría | `libro` + `categoria` |
| Reportes | **Reportes** | PDF, Excel o CSV | varias |

**Demostración de "modificar":**
1. Admin → **Usuarios** → **Editar** un encargado → cambiar el turno a "tarde" → **Guardar cambios**.
2. En Neon, antes y después:
   ```sql
   SELECT u.id_usuario, u.nombre, u.dni, u.correo, u.estado, e.turno
   FROM usuario u JOIN encargado e ON e.id_usuario = u.id_usuario;
   ```

---

## 4. Pruebas de integración con la BD

Comprueban que **el programa y la base de datos funcionan juntos**: guardan de verdad en Neon y leen lo guardado. Tienen `@SpringBootTest` + `@Transactional`: al terminar se hace **rollback** y **no dejan datos**.

**Ejecutar:** `./mvnw test -Dtest=UsuarioBdTest,ConcurrenciaStockTest,ReporteBdTest`

| Prueba | Qué comprueba en Neon |
|---|---|
| `UsuarioBdTest.elEncargadoQuedaEnLaTablaEncargadoConSuTurno` | Registrar encargado escribe en `usuario` **y** en `encargado` |
| `UsuarioBdTest.laBajaEsLogicaYCancelaLaReservaPendiente` | La baja deja la fila con `inactivo` y cancela su reserva |
| `UsuarioBdTest.alEditarNoSePuedeUsarElDniDeOtroUsuario` | La restricción `UNIQUE` del DNI la respeta la BD |
| `ConcurrenciaStockTest.elUltimoEjemplarSoloSePrestaUnaVez` | Dos préstamos **al mismo tiempo** del último ejemplar: solo uno pasa (bloqueo `@Lock`, RNF05) |
| `ReporteBdTest` (12) | Los reportes corren contra la BD real, con y sin fechas |
| `LibroDaoTest.consultaDeDosTablasLibroYCategoria` | La consulta `libro JOIN categoria` trae datos y filtra |

En la consola sale `Database JDBC URL [jdbc:postgresql://...neon.tech...]`: es la prueba de que se conectó a Neon.

---

## 5. Validaciones del sistema

### 5a. Criterios de la contraseña
**Regla:** 8 a 64 caracteres, al menos una mayúscula, una minúscula, un número y un símbolo, y máximo 72 bytes (límite de BCrypt).
**Código:** `seguridad/ValidadorContrasena.java` (expresión regular).
**Ejecutar:** `./mvnw test -Dtest=ValidadorContrasenaTest` → 14 ejecuciones en 5 métodos.

| Contraseña | Resultado | Por qué |
|---|---|---|
| `Abcdefg1!` | ✅ | Cumple todo |
| `Ab1!` | ❌ | Menos de 8 |
| `abcdefg1!` | ❌ | Sin mayúscula |
| `ABCDEFG1!` | ❌ | Sin minúscula |
| `Abcdefgh!` | ❌ | Sin número |
| `Abcdefg12` | ❌ | Sin símbolo |
| 65 caracteres | ❌ | Pasa de 64 |

**En la app:**
1. `http://localhost:4200/registro` → poner `abc123` → sale el mensaje con la regla.
2. Admin → **Usuarios** → **Contraseña** → muestra los requisitos.

### 5b. Tipos de datos, longitud mín./máx. y rangos
**Código:** anotaciones en los DTO (`@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`, `@Email`, `@DecimalMin`), por ejemplo en `RegistroDTO` y `LibroFormDTO`.
**Ejecutar:** `./mvnw test -Dtest=ValidacionDtoTest` → 31 ejecuciones.

| Tipo de validación | Regla | Prueba |
|---|---|---|
| **Tipo / formato** | DNI = exactamente 8 dígitos (`1234567A` ❌) | `rechazaDniQueNoTieneOchoDigitos` |
| **Tipo / formato** | Correo válido (`juan@` ❌) | `rechazaCorreoMalEscrito` |
| **Valores permitidos** | Estado físico solo `bueno` o `dañado` | `laDevolucionSoloAceptaBuenoODaniado` |
| **Longitud máx.** | Nombre ≤ 50, correo ≤ 100 | `rechazaNombreDeMasDeCincuentaCaracteres`, `rechazaCorreoDeMasDeCienCaracteres` |
| **Longitud mín. / obligatorio** | Apellido y título no vacíos | `rechazaApellidoVacio`, `elLibroSinTituloNoPasa` |
| **Rango** | Año del libro de 1000 a 2100 | `elAnioDelLibroVaDe1000A2100` |
| **Rango** | Stock no negativo | `elLibroNoAceptaStockNegativoNiAnioFueraDeRango` |
| **Rango** | Suspensión de 1 a 30 días (0, 31 y -5 ❌) | `laSuspensionRechazaMenosDe1OMasDe30Dias` |
| **Rango** | Multa > 0 y con 2 decimales como máximo | `elMontoDeLaMultaEsMayorQue0YConHasta2Decimales` |

**En la app:**
1. Admin → **Libros** → "Registrar libro" → año `3000` → **Registrar** → mensaje de error. El campo ya tiene `min=1000 max=2100` y el título `maxlength=150`.
2. Admin → **Usuarios** → **Registrar encargado** → DNI `1234` → "El DNI debe tener 8 dígitos".

**En Neon:** la BD también tiene sus reglas (`CHECK`, `UNIQUE`, `NOT NULL`):
```sql
SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
WHERE conrelid = 'libro'::regclass OR conrelid = 'usuario'::regclass;
```

---

## Tablas de Neon: qué guarda cada una

| Tabla | Qué guarda | Se usa para |
|---|---|---|
| `usuario` | Datos de todos: nombre, DNI, correo, contraseña (hash), estado | Login, Usuarios |
| `administrador` / `encargado` / `prestatario` | El perfil de cada usuario (herencia). `encargado` tiene el `turno` | Perfil único, turno |
| `libro` | Título, ISBN, stock, `estado_libro` (`disponible`, `prestado`, `reservado`, `mantenimiento`, `baja`) | Libros, stock, baja lógica |
| `categoria` | Las categorías | Consulta de 2 tablas |
| `autor` + `libro_autor` | Autores y qué libro escribió cada uno | Catálogo |
| `prestamo` | Préstamos: fechas, `ampliado`, `estado_prestamo` (`activo`, `devuelto`, `vencido`, `anulado`) | Préstamos, devoluciones |
| `reserva` | Reservas: fechas, `posicion_cola`, `estado_reserva` | Reservas, eliminación física |
| `incidencia` / `sancion` | Retraso, daño, pérdida y su sanción | RF06 |
| `auditoria_inventario` | Conteos físicos | Indicador IPI |

---

## Preguntas probables (por si acaso)

**¿Qué diferencia hay entre prueba unitaria y de integración?**
La unitaria prueba **una clase sola**: la BD se simula con Mockito (`@Mock`), es rápida y no necesita internet. Ejemplo: `AuthServiceLoginTest`. La de integración usa **la BD real** (Neon) con `@SpringBootTest` y comprueba que el código y la BD funcionan juntos. Ejemplo: `UsuarioBdTest`.

**¿Las pruebas de integración ensucian la base de datos?**
No. Tienen `@Transactional`: al terminar cada prueba se hace **rollback** y no queda nada guardado. `ConcurrenciaStockTest` no puede usar rollback porque usa dos hilos; por eso crea su libro de prueba y lo **borra al final** (`@AfterEach`).

**¿Qué es `@ParameterizedTest` con `@ValueSource`?**
Ejecuta la **misma prueba con varios valores**. Por ejemplo, una prueba con 7 contraseñas inválidas = 7 ejecuciones sin repetir código.

**¿Qué es `assertTrue`, `assertFalse`, `assertEquals`?**
Son las comprobaciones. Si el resultado no es el esperado, la prueba sale en **rojo**. `assertEquals(esperado, real)`.

**¿Qué es Mockito / `@Mock` / `when(...).thenReturn(...)`?**
Simula a quien la clase necesita, por ejemplo el DAO, para probar solo la lógica. `when(dao.findById(1)).thenReturn(libro)` significa: "cuando pidan el libro 1, devuelve este libro de prueba". `verify(...)` comprueba que se llamó a un método.

**¿Qué herramienta de pruebas usan?**
**JUnit 5** (la del S13 para Java), **Mockito** para simular y **Spring Boot Test** para la integración. En el frontend: **Vitest** (`npx ng test --watch=false`).

**¿Dónde se valida, en el frontend o en el backend?**
En los **dos**. El frontend (`maxlength`, `min`, `max`) ayuda al usuario, pero se puede saltar. El backend (Bean Validation en los DTO) es el que manda y responde **400** con el mensaje. La BD tiene además `CHECK` y `UNIQUE` como última barrera.

**¿Por qué la contraseña tiene máximo 72 bytes?**
BCrypt, el que cifra la contraseña, solo usa los primeros 72 bytes. Con más, dos contraseñas distintas podrían dar el mismo hash. La `ñ` y las tildes ocupan 2 bytes; por eso se mide en bytes y no solo en caracteres.

**¿Cómo se guarda la contraseña?**
Con **BCrypt**: se guarda el hash (`$2a$10$...`), nunca el texto. Al iniciar sesión se compara con `passwordEncoder.matches()`.

**¿Cómo evitan la inyección SQL?**
Las consultas usan **parámetros** (`:idPrestatario`, `:categoria`) con JPA. El valor nunca se pega dentro del SQL, así que un texto como `' OR 1=1 --` se trata como un dato, no como código.

**¿Qué pasa si fallan 3 intentos de login?**
Queda bloqueado **15 minutos** (`LimitadorIntentosLogin.TIEMPO_BLOQUEO`), aunque luego ponga la contraseña correcta. Lo prueba `alTercerIntentoFallidoQuedaBloqueadoAunqueDespuesPongaLaCorrecta`.

**¿Cuántas pruebas tiene el proyecto?**
Unas **307 en el backend** y **76 en el frontend**. Las del S13 son 114 y pasan todas.
⚠️ Si corren `./mvnw test` completo, fallan 3 de `LibroDaoTest`, porque el libro "Clean Code" ya no está en Neon. Es un tema de datos, no del código.

**¿Qué son las pruebas de caja negra?**
Pruebas donde solo se mira la **entrada y la salida**, sin ver el código. Es lo que se hace en la app: poner un DNI de 7 dígitos y ver que sale el mensaje de error.

**¿Qué pruebas no funcionales tienen? (no eran obligatorias)**
- **Concurrencia (RNF05):** `ConcurrenciaStockTest`.
- **Rendimiento (RNF03):** pestaña **Monitoreo**, % de peticiones en 2 s o menos.
- **Sesión de 15 minutos (RNF04):** `SesionServiceTest`.
