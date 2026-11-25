# Guía para presentar la semana 15: despliegue, prueba completa y los 3 reportes en PDF

Lo que pidió el profe en la semana 15 (`docs/clases/s15integradorresumen.txt`) y dónde está cada cosa:

| # | Lo que pide | Estado | Dónde |
|---|---|---|---|
| 1 | Requisitos del **servidor** y del **cliente**, empaquetado y distribución | ✅ | Informe vDelta, **Anexo F** (Tabla 62) y [DESPLIEGUE.md](DESPLIEGUE.md) |
| 2 | **Dos capturas** de pantalla completa con fecha y hora: el empaquetado y el contenido extraído | ✅ (son 3) | [capturas/2026-11-24/despliegue/](capturas/2026-11-24/despliegue/), Figuras 132 a 134 |
| 3 | Decir **qué usuario y qué perfil** se usa antes de empezar | Se dice en vivo | Paso 2 de esta guía |
| 4 | **Proceso completo**: iniciar sesión → registrar la operación → emitir el PDF | ✅ | Paso 2 |
| 5 | Reporte de **una operación individual** en PDF | ✅ | Comprobante del préstamo y constancia de la reserva |
| 6 | Reporte **por fechas** en PDF | ✅ | Reportes → **Préstamos** con un rango |
| 7 | Reporte de **indicadores** del capítulo 1, en PDF | ✅ | Reportes → **Indicadores de gestión** (TAP, PDF, IPI y TIM, apartado 1.2.2) |
| 8 | Registrar datos **nuevos** y mostrar que el reporte **cambia** | Se hace en vivo | Paso 4 |

Pruebas automáticas al 25/11/2026: **backend 342, todas pasan contra Neon; frontend 90, todas pasan.**

---

## 0. Antes de que llegue el profe

Conviene presentar con el **`.jar`**, porque eso mismo es el despliegue que se explica en el paso 1.

```bash
cd proy_IntegradorFullStack/frontend
npx ng build
cd ../backend
./mvnw clean package -DskipTests
java -jar target/biblioteca-alejandria.jar      # desde backend/, porque ahí está el .env
```
Abrir **http://localhost:8080**. Si algo falla, usar el plan B de siempre: `./mvnw spring-boot:run` + `npm start` y abrir `http://localhost:4200`.

Dejar abiertas tres ventanas:
- **Normal:** el encargado (`encargado@alejandria.edu.pe`).
- **Incógnito:** el prestatario, por ejemplo `lucia.ramirez@alumnos.alejandria.edu.pe`. **No usar a Valeria**: tiene una sanción activa y no se le puede prestar.
- **Otra pestaña:** el admin (`admin@alejandria.edu.pe`), en la pestaña **Reportes**.

Todas las cuentas usan la contraseña de prueba del equipo.

Tener a la mano:
- el informe abierto en el **capítulo 1, apartado 1.2.2** (la tabla de los 4 indicadores) y en el **Anexo F**;
- los PDF ya generados por si algo falla: [capturas/2026-11-24/pdf/](capturas/2026-11-24/pdf/).

---

## 1. Despliegue (2 a 3 minutos)

**Qué decir:**
- El sistema se distribuye como **un solo archivo**: `biblioteca-alejandria.jar`.
- Adentro van el backend (Spring Boot: API, seguridad, reglas, reportes y logs) y las pantallas de Angular ya compiladas (carpeta `static/`).
- La base de datos está en otro servidor: **Neon** (PostgreSQL en la nube).

| Lado | Qué necesita |
|---|---|
| **Servidor** | Java 21, el `.jar`, las credenciales de Neon en variables de entorno (o el `.env`), conexión a internet para llegar a Neon y el puerto 8080 (en la nube, el que diga `PORT`) |
| **Cliente** | Solo un navegador actual (Chrome, Edge o Firefox) con JavaScript, y conexión con el servidor. **No se instala nada.** Para abrir los reportes: un lector de PDF y Excel o LibreOffice |

**Cómo se empaqueta:**
1. `npx ng build` compila las pantallas.
2. `./mvnw clean package` las copia a `target/classes/static` y genera el `.jar`.

**Mostrar las capturas** (pantalla completa, con la fecha y la hora de Windows):
- `01-empaquetado-pantalla-completa.png`: el empaquetado.
- `02-jar-desempaquetado-pantalla-completa.png`: el contenido extraído con `tar -xf`. Adentro se ven `BOOT-INF/classes`, `BOOT-INF/lib` (las librerías), `META-INF` (el manifiesto) y `org` (el cargador de Spring Boot).
- `03-index-html-del-jar-pantalla-completa.png`: el `index.html` de las pantallas.

**Si pide hacerlo en vivo**, desempaquetarlo en una carpeta aparte:
```bash
mkdir jar-extraido && cd jar-extraido
tar -xf ../target/biblioteca-alejandria.jar
dir
```

---

## 2. Prueba completa del sistema

Antes de empezar, **decir en voz alta qué usuario y qué perfil se usa**. El profe lo pidió así porque lo que se puede hacer depende del perfil.

### 2a. Préstamo: Encargado → registrar → comprobante en PDF
> "Ingreso como **Encargado** (`encargado@alejandria.edu.pe`). El encargado es quien atiende en ventanilla y registra los préstamos."

1. Iniciar sesión como encargado → pestaña **Registrar préstamo**.
2. Escribir el **DNI** de Lucía (sale en Admin → Usuarios) y elegir un libro **disponible**.
3. **Registrar** → aparece el mensaje con la hora límite (3 horas, o las 20:00 si se cierra antes).
4. Clic en **"Descargar comprobante (PDF)"** y abrir el PDF.
5. Señalar los datos del comprobante: N° del préstamo, **prestatario y su DNI**, libro e ISBN, encargado, inicio, hora límite, estado y la nota con las reglas.

### 2b. Reserva: Prestatario → reservar → constancia en PDF
> "Ahora ingreso como **Prestatario** (Lucía). Ella reserva desde el catálogo y necesita el documento de **su propia** reserva."

1. En la ventana de incógnito → **Catálogo** → elegir un libro → **Reservar**.
2. En el modal "Reserva Realizada" → **"Descargar constancia (PDF)"**.
3. En **Mis Reservas** está la columna **Constancia**, y en **Mis Préstamos** la columna **Comprobante**: ahí aparece el préstamo del paso 2a.

**Si pregunta "¿y si Lucía intenta bajar el comprobante de otra persona?"** El sistema responde **403**: el prestatario solo descarga los suyos. Lo comprueba `ComprobanteEndpointsTest`.

---

## 3. Los 3 reportes en PDF

| Reporte que pide | En el sistema | Cómo se genera |
|---|---|---|
| **De una operación individual** | Comprobante del préstamo / constancia de la reserva | El del paso 2. También desde Préstamos activos, Historial y la pestaña Reservas del admin |
| **Por fechas** | **Préstamos** | Admin → Reportes → *Préstamos* → **Desde** y **Hasta** = hoy → **PDF** |
| **De indicadores** | **Indicadores de gestión** | Admin → Reportes → *Indicadores de gestión* → rango del mes → **PDF** |

**Contrastar los indicadores con el capítulo 1** (el profe lo pidió): poner el informe al lado del PDF.

| Código | Indicador | Fórmula | Línea base | Meta |
|---|---|---|---|---|
| TAP | Tiempo promedio de atención por préstamo | Promedio de minutos desde que el encargado empieza a llenar el formulario hasta que registra | 6 min | ≤ 2 min |
| PDF | Porcentaje de devoluciones fuera de plazo | Préstamos devueltos con retraso / préstamos × 100 | 15 % | ≤ 5 % |
| IPI | Índice de precisión del inventario | Títulos que coinciden con el conteo físico / títulos auditados × 100 | 70 % | ≥ 95 % |
| TIM | Tasa de incidencias mensual | Préstamos con daño o pérdida / préstamos × 100 | 2 % | ≤ 1 % |

Si un indicador no tiene datos en el rango, el PDF dice **"Sin dato"**: no se inventa el valor.

---

## 4. Demostrar que los reportes se actualizan

El profe dijo que abrir un reporte que ya tenía datos no basta. Hay que hacerlo en este orden:

1. **Antes:** Admin → Reportes → *Préstamos* con el rango **hoy a hoy** → PDF. Anotar cuántas filas tiene (N).
2. **Registrar un préstamo nuevo** como encargado (paso 2a; si ya se hizo, hacer otro).
3. **Después:** generar el mismo reporte con el mismo rango → ahora tiene **N + 1** filas, y la nueva es el préstamo recién registrado (mismo N° que el comprobante).
4. Lo mismo con *Indicadores de gestión* (rango hoy): en la columna **Detalle** sube el total de préstamos ("… de N préstamos") y el TAP pasa a promediar un préstamo más.
5. Extra, si pregunta por la devolución: devolver ese préstamo como **Dañado** → el TIM sube y el libro aparece en *Devoluciones* e *Incidencias*.

---

## 5. Si pregunta por la eliminación lógica y la física

Ya está en [GUIA_REVISION_11_NOV.md](GUIA_REVISION_11_NOV.md#3-eliminación-lógica-y-física). Resumen:
- **Lógica:** dar de baja un libro → en Neon la fila **sigue**, con `estado_libro = 'baja'`. Sale en el reporte **Bajas lógicas** con la fecha y la hora de la baja.
- **Física:** eliminar una reserva cancelada o vencida → `SELECT` en Neon antes (1 fila) y después (0 filas).

---

## 6. Preguntas probables

| Pregunta | Respuesta corta | Dónde mostrarlo |
|---|---|---|
| ¿Qué herramienta arma el PDF? | **OpenPDF**, la misma de los reportes. No se agregó ninguna librería | `ComprobanteService.java`, [LIBRERIAS.md](LIBRERIAS.md) |
| ¿Por qué el comprobante es distinto de un reporte? | El reporte es un **listado** por fechas; el comprobante es **un solo préstamo**, con los datos del prestatario | `ComprobanteService` vs `ReporteService` |
| ¿Qué endpoint lo genera? | `GET /api/prestamos/{id}/comprobante` y `GET /api/reservas/{id}/constancia` | `PrestamoController`, `ReservaController` |
| ¿Quién puede descargarlo? | Los tres perfiles; el prestatario **solo los suyos** (si no, 403) | `ComprobanteEndpointsTest` |
| ¿El cliente se conecta a la base de datos? | **No.** El navegador solo llama a la API; las credenciales están en el servidor | Anexo F |
| ¿Qué pasa si el servidor está en otra zona horaria? | Nada: el backend fija la hora de Lima al arrancar | `BibliotecaApplication` |
| ¿Cómo saben que funciona? | 342 pruebas del backend contra Neon y 90 del frontend, todas en verde | `./mvnw test`, `npx ng test --watch=false` |

---

## Checklist final
- [ ] El `.jar` se generó hoy y arranca (`http://localhost:8080`, login como admin OK).
- [ ] Las tres sesiones abiertas: encargado, prestatario (incógnito) y admin.
- [ ] Hay al menos un libro **disponible** para prestar y otro para reservar.
- [ ] Hora: entre **08:00 y 20:00** (fuera de ese horario no se puede prestar).
- [ ] El informe abierto en el apartado **1.2.2** y en el **Anexo F**.
- [ ] Los PDF de respaldo en `docs/capturas/2026-11-24/pdf/`.
- [ ] Los cambios del día (fecha de baja, migración 06) **subidos a GitHub** antes de presentar.
- [ ] Confirmar el **día y la hora** del grupo (30/11 o 02/12).
