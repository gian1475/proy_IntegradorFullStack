# Pendientes para agregar a los archivos locales

Sesión en la nube del **03/11**, rama `claude/happy-ritchie-8835za`, commits `a3ddc17` (código) y `bc08a49` (docs).
Este archivo **no está en git** (ni commiteado ni subido). Aquí va todo lo que hay que copiar a `CLAUDE.md`, `PLAN_CODIGO.md` y `PLAN_INFORME.md` en la PC, y las dudas que tengo sobre lo que hice.

Leyenda: ⬜ falta · ✅ hecho · ❓ duda (decídanla ustedes)

---

## 1. Qué cambiar en `CLAUDE.md`

- ⬜ **Perfiles y permisos → Administrador:** cambiar "registrar encargados" por:
  > gestionar usuarios (registrar encargados; editar, dar de baja y reactivar prestatarios y encargados: RF09, RF12 y RF13)
- ⬜ **Reglas del negocio:** cambiar "usuarios `inactivo` (RF13, pendiente)" por:
  > usuarios `inactivo` (RF13, 03/11): no se da de baja a un prestatario con un libro en sala (préstamo `activo` o `vencido`); sus reservas `pendiente` y `en_espera` se cancelan y se cierran sus sesiones abiertas. El admin puede reactivarlo (vuelve a `activo`, o a `suspendido` si le queda una suspensión activa). Al administrador no se le edita ni se le da de baja. RF12: se editan nombres, apellidos, DNI, correo y el turno del encargado; no la contraseña ni el estado. Solo baja lógica: no hay borrado físico de usuarios.
- ⬜ **Estructura actual:** en `admin-panel/tabs` la pestaña `encargados` ahora se llama `usuarios`.
- ⬜ **Estado:** cambiar el título "Estado al 27/10" por "Estado al 03/11". Agregar "editar y dar de baja usuarios (RF12 y RF13, 03/11)". Cambiar "205 pruebas del backend y 24 del frontend" por **"232 del backend y 38 del frontend"**. Ojo: las 232 no se han corrido contra Neon, solo contra el PostgreSQL local (ver la duda D14).
- ⬜ **Pendientes:**
  - Cambiar "RF12 y RF13: editar y dar de baja usuarios" por "~~RF12 y RF13~~ (hecho el 03/11 en la rama `claude/happy-ritchie-8835za`, commits `a3ddc17` y `bc08a49`; falta pasarla a `main`)".
  - Corregir la línea del commit `246d4d8`: dice **"sin push"**, pero ya está en `origin/main`.
- ⬜ **(Opcional) Nota para las sesiones en la nube:**
  > En la nube: `mvnw` no descarga Maven (usar `mvn`), y el contenedor está en UTC. Correr las pruebas con `TZ=America/Lima mvn test -Duser.timezone=America/Lima` y abrir el navegador de prueba con la zona `America/Lima`.

## 2. Qué marcar en `PLAN_CODIGO.md`

- ⬜ Marcar **C6, C7 y C22** como hechos (03/11).
- ❓ No recibí `PLAN_CODIGO.md` en la sesión, así que no sé qué pide exactamente cada una. Revisen que C22 no pida algo más (por ejemplo, el borrado físico o un campo de teléfono).

## 3. Qué anotar en `PLAN_INFORME.md`

### Sección 6 (qué cambia en el informe)
- ⬜ RF12 y RF13 implementados (03/11).
- ⬜ No hay borrado físico de usuarios (decisión del equipo).
- ⬜ La pestaña "Encargados" del admin ahora es "Usuarios".
- ⬜ Endpoints nuevos: `GET /api/usuarios`, `PUT /api/usuarios/{id}`, `DELETE /api/usuarios/{id}` (baja lógica) y `PUT /api/usuarios/{id}/reactivar`.
- ⬜ Pruebas: 232 del backend y 38 del frontend.

### Sección 7 (qué reescribir)
1. ⬜ **Ficha RF12 (Tabla 14):** en la descripción, cambiar "nombres, DNI, contacto y estado" por "nombres, apellidos, DNI, correo y, si es encargado, el turno. El estado cambia al dar de baja o reactivar (RF13) y con las sanciones (RF06)". El criterio de aceptación no cambia.
2. ⬜ **Ficha RF13 (Tabla 15):**
   - Borrar "La eliminación física solo se permitirá para usuarios sin préstamos ni reservas registrados".
   - Agregar "No se da de baja a un prestatario con un libro en sala; sus reservas pendientes o en cola se cancelan. El administrador puede reactivarlo".
   - En el criterio, agregar "y se cierra su sesión abierta".
3. ⬜ **3.5 (integridad):** cambiar "las excepciones de borrado físico son RF13 y RF17" por "la única excepción de borrado físico es RF17".
4. ⬜ **Párrafo de eliminación lógica y física:** borrar "RF13 prevé borrar físicamente usuarios sin préstamos ni reservas, función pendiente".
5. ⬜ **Permisos del Administrador:** agregar "gestionar usuarios (editar, dar de baja y reactivar)".
6. ⬜ **Pendientes para la entrega final:** quitar RF12 y RF13 (y también RF09 y RF06, que ya estaban hechos).
7. ⬜ **Backlog / Sprint:** marcar PB06 "Gestión de usuarios (actualización y baja lógica)" como terminado.
8. ⬜ **Diccionario, `usuario.estado`:** "inactivo = baja lógica (RF13); el administrador puede reactivarlo".
9. ⬜ **Anexo de la API:** los 4 endpoints nuevos.
10. ⬜ **Figuras:** cambiar la del panel "Encargados" por la de "Usuarios".

## 4. Capturas para tomar (con Neon)

| # | Captura | Archivo sugerido |
|---|---|---|
| 1 | Pestaña **Usuarios** con la tabla (José Luis `inactivo` con "Reactivar"); reemplaza a `admin-encargados.png` | `admin-usuarios.png` |
| 2 | Formulario "Editar encargado" con el turno | `admin-usuario-editar.png` |
| 3 | Error "El DNI ya se encuentra registrado" | `admin-usuario-dni-repetido.png` |
| 4 | Confirmación de "Dar de baja" y el mensaje con el estado `inactivo` (Sebastián, Lucía o Diego, que tienen una reserva pendiente). Cambia datos: mejor en la demo | `admin-usuario-baja.png` |
| 5 | Rechazo "El prestatario tiene un libro prestado…" (Andrés o Camila) | `admin-usuario-baja-rechazada.png` |
| 6 | Login del usuario dado de baja: "El usuario se encuentra inactivo" | `login-usuario-inactivo.png` |
| 7 | Reporte de bajas lógicas con ese usuario | — |
| 8 | Código: `UsuarioService.darDeBaja` y `UsuarioController` (el `DELETE` que no borra) | — |
| 9 | Terminal: `./mvnw test` → 232 y `npx ng test --watch=false` → 38 | — |

Las capturas de referencia de la prueba local están en la carpeta temporal de la sesión (`scratchpad/capturas_rf12_rf13/`). No sirven para el anexo porque no son de Neon.

## 5. Por hacer

- ⬜ Pasar la rama `claude/happy-ritchie-8835za` a `main`. Desde la PC:
  ```
  git fetch origin claude/happy-ritchie-8835za
  git checkout main
  git merge --no-ff origin/claude/happy-ritchie-8835za
  ```
- ⬜ Correr `./mvnw test` **contra Neon** (las pruebas de BD de esta sesión solo se probaron en el PostgreSQL local).
- ⬜ Lo que sigue en los pendientes: RF15 (editar la fecha y la franja de una reserva), L3 (opcional), TAP e IPI (opcional), actualizar el informe y tomar las capturas.

---

## 6. Dudas: cosas que agregué y de las que no estoy seguro

| # | Qué hice | Por qué tengo la duda | Qué propongo |
|---|---|---|---|
| D1 | La baja usa **`DELETE /api/usuarios/{id}`** aunque no borra la fila | Un `DELETE` que no borra puede confundir en la sustentación ("¿por qué DELETE si es lógica?") | Mantenerlo: es el mismo patrón que libros (`baja`) y préstamos (`anulado`) y no obliga a abrir `PATCH` en CORS. La respuesta para el profe: "eliminar en la API, lógica en la BD" |
| D2 | **Al administrador no se le edita ni se le da de baja** (403). Tampoco sale en la tabla | No lo preguntaron; lo hice para que el admin no se deje fuera a sí mismo | Si quieren que el admin edite sus propios datos, sería un cambio aparte |
| D3 | Al dar de baja se **cierran sus sesiones abiertas** (`SesionService.cerrarSesionesDe`) | No estaba en el pedido ni en el informe. Las sesiones están en memoria: si el backend se reinicia da igual, porque todos vuelven a entrar | Mantenerlo y agregarlo al criterio de aceptación de RF13 (ya está en la sección 3) |
| D4 | Cambié **`IncidenciaService`** (RF06): una suspensión ya no pone `suspendido` a un usuario `inactivo` | Es código de RF06, fuera de la tarea. Sin esto, una suspensión por daño sobre un usuario dado de baja lo "reactivaba" a medias | Mantenerlo. Tiene su prueba (`unaSuspensionNoReactivaAUnPrestatarioDadoDeBaja`) |
| D5 | Quité `listarEncargados()` del **servicio del frontend**, pero el endpoint `GET /api/usuarios/encargados` del backend **sigue** (lo usa `UsuarioBdTest`) | Quedó un endpoint que ninguna pantalla usa | Dejarlo (no molesta y está documentado) o borrarlo en otra tarea |
| D6 | Se puede **editar a un usuario `inactivo`** | No sé si tiene sentido editar a alguien dado de baja | Dejarlo: sirve para corregir datos antes de reactivarlo |
| D7 | Si un prestatario con **multa pendiente** se da de baja, la multa queda como está. Al reactivarlo vuelve a `activo`, pero la multa lo sigue bloqueando | Puede confundir que diga "activo" y no pueda pedir libros | Es lo que dice RF06 (la multa bloquea sin suspender). Solo tenerlo claro en la demo |
| D8 | **Reactivar no devuelve** las reservas que se cancelaron en la baja | Es lo esperado, pero alguien podría preguntarlo | Dejarlo así |
| D9 | El mensaje dice "**fue dado de baja**" y "**fue reactivado**", en masculino, también para Lucía o Rosa | Queda raro con nombres femeninos | Cambiarlo a "Se dio de baja a Lucía Ramírez Soto (estado inactivo)" y "Se reactivó a…". Es una línea en `usuarios-tab.component.ts` y su prueba. Díganme y lo hago |
| D10 | El formulario "Editar" aparece **entre** el registro de encargados y la tabla | Si la tabla es larga, al pulsar "Editar" abajo no se ve el formulario sin subir | Agregar un `scrollIntoView` al abrir la edición, o poner el formulario arriba |
| D11 | RF12 del informe dice "**contacto**": lo tomé como el **correo** | La BD no tiene teléfono | Si "contacto" era teléfono, habría que cambiar la BD (migración nueva). Propongo cambiar la palabra en el informe (ver la sección 3) |
| D12 | **No hay borrado físico** de usuarios (lo eligieron ustedes) | El informe actual sí lo menciona | Ya está en la sección 3: hay que cambiar la ficha RF13 y 3.5 |
| D13 | **No toqué la BD** | — | Confirmado: el CHECK ya aceptaba `inactivo`. No hace falta migración |
| D14 | Las pruebas de BD (`UsuarioBdTest` +3) solo se corrieron en **PostgreSQL 16 local**, no en Neon | Neon debería dar lo mismo (mismo script), pero no está comprobado | Correr `./mvnw test` en la PC contra Neon antes del merge |
| D15 | **Error latente de zona horaria en el frontend:** `expiraEn` llega en hora de Lima **sin zona**, y un navegador en otra zona horaria toma la sesión como vencida al instante | No pasa en Perú, pero en la nube lo vi (el navegador en UTC no podía entrar) | Anotarlo como mejora opcional (mandar `expiraEn` con zona o comparar en el backend). No lo cambié |
| D16 | **Prueba frágil:** `PrestamoDaoTest.lasConsultasDelDashboardCuentanBienEnLaBd` falla si la JVM no está en hora de Lima entre las 00:00 y las 05:00 UTC | En la PC de Lima no pasa; en la nube sí | Opcional: que la prueba use `HorarioAtencion.ahora()` o `ZoneId.of("America/Lima")` en vez de `LocalDateTime.now()` |
| D17 | En la BD **local** de la nube puse una contraseña temporal a las cuentas para probar | Solo afecta a la BD local de la sesión, que se borra. **Neon no se tocó** | Nada que hacer |
| D18 | Hice **commits y push** en la rama de la sesión | `CLAUDE.md` dice "no hagas commits", pero ustedes lo permitieron para esta sesión | Revisen la rama antes del merge |
