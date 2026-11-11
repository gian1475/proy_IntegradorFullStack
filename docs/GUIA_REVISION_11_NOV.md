# Guía para la revisión: búsqueda, consulta, eliminación y los 6 reportes

Orden sugerido: **levantar → búsqueda → consultas → eliminaciones → reportes**. Para cada punto se indica qué hacer en la app y qué consulta SQL pegar en Neon (SQL Editor) para comprobar el dato.

---

## 0. Levantar el sistema

Terminal 1, el backend (tiene que ser desde `backend/` porque ahí está el `.env`):
```bash
cd proy_IntegradorFullStack/backend
./mvnw spring-boot:run
```
Terminal 2, el frontend:
```bash
cd proy_IntegradorFullStack/frontend
npm install        # solo la primera vez en esta PC
npm start
```
Abrir **http://localhost:4200**.

Usuarios (todos con la contraseña de prueba del equipo):
- Admin: `admin@alejandria.edu.pe`
- Encargado: `encargado@alejandria.edu.pe`
- Prestatario: por ejemplo `lucia.ramirez@alumnos.alejandria.edu.pe`

> Si no hay datos de demo, ejecutar `database/datos_demo.sql` en el SQL Editor de Neon (se puede correr más de una vez).
> Las pruebas automáticas de los reportes contra Neon: `cd backend && ./mvnw test -Dtest=ReporteBdTest,ReporteServiceTest`

### ¿Todo desde el Admin?
Casi todo: búsqueda, consulta de 2 tablas, eliminación lógica y física, y los 6 reportes se muestran como **admin**. Las excepciones:
- **Consulta de 1 tabla** ("Mis Préstamos") → solo el **prestatario**. Tenerlo abierto en **otra ventana en incógnito**.
- **Crear una reserva** → prestatario o encargado. Si ya hay una `cancelada` o `vencida` en Admin → **Reservas**, usar esa para la eliminación física. Si no, **Cancelar** una pendiente y luego **Eliminar**.

Detalle completo en [GUIA_S13_PRUEBAS.md](GUIA_S13_PRUEBAS.md#se-puede-mostrar-todo-desde-el-admin).

---

## 1. Búsqueda (un elemento concreto)

**En la app:** Catálogo → escribir el **ISBN** o el título de un libro → aparecen sus datos (stock, estado, autores).
También: Encargado → Préstamos → escribir el **DNI** del prestatario (ej. `71234561`) → aparecen sus datos.

**Qué explicar:** se busca **un** registro por su código.
- Endpoint: `GET /api/libros/buscar?texto=...` y `GET /api/libros/{id}`.
- Código: `LibroDao.buscarEnCatalogo`.

**SQL en Neon:**
```sql
SELECT * FROM libro WHERE isbn = '<isbn>';
SELECT id_usuario, nombre, apellido, dni, correo, estado FROM usuario WHERE dni = '71234561';
```

---

## 2. Consultas

### 2a. Consulta de UNA tabla: historial del prestatario
**En la app:** entrar como prestatario → **Mis Préstamos**.
- Tabla: `prestamo`.
- Campos: `fecha_prestamo`, `fecha_devolucion_prevista`, `fecha_devolucion_real`, `estado_prestamo`.
- Código: `PrestamoDao` (línea 29), `SELECT * FROM prestamo WHERE id_prestatario = ?`.

```sql
SELECT * FROM prestamo WHERE id_prestatario = (SELECT id_usuario FROM usuario WHERE dni='71234561')
ORDER BY fecha_prestamo DESC;
```

### 2b. Consulta de DOS tablas: libros por categoría
**En la app:** Admin → pestaña **"Libros por categoría"** → elegir una categoría en el combo (o "Todas").
Cada columna dice de qué tabla sale:

| Tabla | Campos que aporta |
|---|---|
| `libro` | id, título, ISBN, stock, estado |
| `categoria` | nombre de la categoría |
| Unión | `libro.id_categoria = categoria.id_categoria` |

- No muestra los libros dados de baja: la eliminación lógica se respeta.
- Código: `LibroDao.consultarConCategoria`. Endpoint: `GET /api/libros-por-categoria?categoria=...`.

```sql
SELECT l.id_libro, l.titulo, l.isbn, l.stock, l.estado_libro,  -- tabla libro
       c.nombre                                                 -- tabla categoria
FROM libro l
JOIN categoria c ON c.id_categoria = l.id_categoria
WHERE l.estado_libro <> 'baja'
ORDER BY c.nombre, l.titulo;
```

(Extra, si pregunta por más tablas: **Préstamos activos** une 3: `prestamo` + `usuario` + `libro`.)

---

## 3. Eliminación lógica y física

### 3a. Lógica (la fila se queda y cambia su estado)
**En la app:** Admin → **Libros** → elegir uno sin préstamos activos → **Dar de baja**.
1. **Antes**, mostrar en Neon:
   ```sql
   SELECT id_libro, titulo, estado_libro FROM libro WHERE titulo ILIKE '%<titulo>%';
   ```
2. Dar de baja en la app.
3. **Después**, ejecutar la misma consulta: la fila **sigue**, pero ahora con `estado_libro = 'baja'`.
4. Mostrar que el resto del sistema la excluye: el libro **ya no sale en el Catálogo** ni se puede prestar.
5. Mostrar que se conserva para el reporte: el libro sale en **Reportes → Bajas lógicas**.

Otras bajas lógicas: préstamo → **Anular** (encargado), queda `anulado`; usuario → **Dar de baja** (admin), queda `inactivo`.

### 3b. Física (la fila se borra de la tabla)
**En la app:** Admin → **Reservas** → una reserva **cancelada o vencida** → **Eliminar**.
(Si no hay ninguna: como prestatario, reservar un libro y cancelarlo.)
1. **Antes**: `SELECT * FROM reserva WHERE id_reserva = <id>;` → 1 fila.
2. Eliminar en la app.
3. **Después**: la misma consulta → **0 filas**, porque ya no existe.
- Endpoint: `DELETE /api/reservas/{id}`.
- Solo se permite con reservas canceladas o vencidas (RF17).

---

## 4. Los 6 reportes

**En la app:** Admin → pestaña **Reportes** → elegir el reporte → poner el rango de fechas → descargar en **PDF**, **Excel** o **CSV**.

| # | Lo que pide el profe | Reporte en la app | Qué mostrar |
|---|---|---|---|
| 1 | Operación principal | **Préstamos** | Todos los préstamos con prestatario, encargado, libro, fechas y estado |
| 2 | Por fechas | **Préstamos** (o Devoluciones) **con un rango** | Generarlo sin fechas y después con un rango corto: salen menos filas |
| 3 | Por indicadores | **Indicadores de gestión** | TAP, PDF, IPI y TIM (cap. 1, apartado 1.2.2) con su línea base, meta y si cumple |
| 4 | Máx., mín. y promedio | **Préstamos por día (máx., mín. y promedio)** ⭐ nuevo | Préstamos de cada día, el **día con más**, el **día con menos**, el **promedio por día** y debajo el **detalle de los préstamos** de esos días, para contarlos a mano. Complemento: "Tiempos de préstamo" (mín., prom. y máx. de minutos en sala) |
| 5 | Registros eliminados | **Bajas lógicas** | Libros en `baja`, préstamos `anulado` y usuarios `inactivo` |
| 6 | Ingresos, utilidad o stock | **Stock por categoría** ⭐ con detalle | Totales por categoría y, debajo, el **detalle por libro**: la suma de sus ejemplares da el total |

**SQL para comprobar el reporte 4 en Neon:**
```sql
SELECT DATE(fecha_prestamo) AS dia, COUNT(*) AS prestamos
FROM prestamo WHERE estado_prestamo <> 'anulado'
GROUP BY DATE(fecha_prestamo) ORDER BY prestamos DESC;

SELECT ROUND(AVG(c), 2) AS promedio_por_dia FROM (
  SELECT COUNT(*) c FROM prestamo WHERE estado_prestamo <> 'anulado' GROUP BY DATE(fecha_prestamo)) t;
```

**SQL para comprobar el reporte 6:**
```sql
SELECT c.nombre, COUNT(*) AS titulos, SUM(l.stock) AS ejemplares
FROM libro l JOIN categoria c ON c.id_categoria = l.id_categoria
WHERE l.estado_libro <> 'baja' GROUP BY c.nombre ORDER BY c.nombre;
```

**SQL para comprobar el reporte 5:**
```sql
SELECT 'libro' tipo, titulo, estado_libro FROM libro WHERE estado_libro = 'baja'
UNION ALL SELECT 'prestamo', id_prestamo::text, estado_prestamo FROM prestamo WHERE estado_prestamo = 'anulado'
UNION ALL SELECT 'usuario', dni, estado FROM usuario WHERE estado = 'inactivo';
```

---

## Checklist antes de que llegue el profe
- [ ] Backend y frontend levantados, y login como admin OK.
- [ ] Hay préstamos en **varios días distintos** (si no, el reporte 4 queda pobre).
- [ ] Hay al menos **una reserva cancelada o vencida** para borrarla en vivo.
- [ ] Hay un libro **sin préstamos activos ni cola** para darlo de baja en vivo.
- [ ] Neon abierto en el SQL Editor con las consultas de esta guía a la mano.
- [ ] Horario: entre 08:00 y 20:00 se puede prestar. Si no, `app.horario.cierre` en `application.properties`.
