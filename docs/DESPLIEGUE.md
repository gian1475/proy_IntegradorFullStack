# Despliegue en la nube (B5)

El sistema se publica como **una sola aplicación**: un `.jar` de Spring Boot que trae adentro las pantallas de Angular. Por eso la API y las pantallas están en la **misma dirección**:
- el frontend llama a `/api` y no hace falta CORS;
- no hay que configurar la URL del backend en ningún lado.

La base de datos sigue en **Neon**.

```
Navegador ──► https://<su-app>.onrender.com ──► .jar (Spring Boot: pantallas + /api) ──► Neon (PostgreSQL)
```

## Qué se cambió en el código para poder desplegar (04/11)

| Antes | Ahora | Dónde |
|---|---|---|
| `http://localhost:8080` escrito en 12 servicios del frontend | `environment.apiUrl`: en desarrollo `http://localhost:8080/api`; en el build de producción, `/api` | `frontend/src/environments/`, `angular.json` (`fileReplacements`) |
| CORS solo aceptaba `http://localhost:4200` | Configurable: `app.cors.origenes` (variable `APP_CORS_ORIGENES`) | `SeguridadConfig.java` |
| Puerto fijo 8080 | `server.port=${PORT:8080}`: los servicios en la nube mandan el puerto en `PORT` | `application.properties` |
| El `.jar` solo tenía la API | Si antes se compiló el frontend, el `pom.xml` lo mete en `static/` del `.jar` | `pom.xml` (`<resources>`) |
| Recargar `/admin` no tenía quién respondiera | `FrontendController` entrega `index.html` en las rutas de Angular | `controller/FrontendController.java` |
| — | `Dockerfile` que compila el frontend y el backend y deja una imagen con Java 21 y el `.jar` | `Dockerfile`, `.dockerignore` |

**En la PC no cambia nada:** `npm start` (puerto 4200) y `./mvnw spring-boot:run` (puerto 8080) siguen funcionando igual.

## Opción A: un solo `.jar` en la PC (para la demo, sin `npm start`)

```
cd frontend
npx ng build
cd ../backend
./mvnw clean package
java -jar target/biblioteca-alejandria.jar
```

Abrir `http://localhost:8080`. El `.jar` lee las credenciales de `backend/.env` igual que siempre, así que hay que ejecutarlo desde `backend/`.

## Opción B: Render (servicio en la nube con plan gratuito)

> Estos pasos son los generales de Render. Revísenlos en su panel, porque los nombres de los botones pueden cambiar. **El despliegue en Render no se pudo probar desde la sesión en la nube**: la red del entorno bloquea esos servicios. El `Dockerfile` sí se construyó y se ejecutó con Docker en la sesión.

1. **Neon antes que nada:** en el SQL Editor de Neon, ejecutar las migraciones que falten. Hoy falta la **05** (`database/migraciones/migracion_05_tap_ipi.sql`). Con `ddl-auto=validate` el backend **no arranca** si la BD no coincide con el código.
2. Antes de publicar, correr las pruebas en la PC (`cd backend` y `./mvnw test`). El `Dockerfile` no las corre, porque necesitan la BD.
3. Pasar la rama a `main` y subirla a GitHub.
4. En Render: **New → Web Service** → conectar el repositorio `gian1475/proy_IntegradorFullStack` → rama `main`. Render detecta el `Dockerfile` de la raíz (Runtime: **Docker**). Plan: **Free**.
5. **Environment** (variables de entorno). Copiar los valores de `backend/.env`; **nunca** ponerlos en el código ni en el `Dockerfile`:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<host-de-neon>/neondb?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = el usuario de Neon
   - `SPRING_DATASOURCE_PASSWORD` = la contraseña de Neon

   `PORT` la pone Render solo. No hace falta `APP_CORS_ORIGENES`, porque todo está en la misma dirección.
6. **Create Web Service.** La primera construcción tarda varios minutos (descarga Node, Maven y las librerías). Cuando diga *Live*, abrir `https://<nombre>.onrender.com` e iniciar sesión.
7. Cada `git push` a `main` vuelve a desplegar solo.

### Qué tener en cuenta (para el informe)

- **Plan gratuito:** Render apaga el servicio si pasa un rato sin visitas, y la primera visita después tarda alrededor de un minuto en despertarlo. Para RNF01 (99 % en el horario de atención) haría falta un plan pagado o un servicio que no se apague. Escríbanlo así en el informe: el despliegue gratuito sirve para la demostración.
- **Monitoreo:** Actuator sigue en el puerto 8081, solo para la misma máquina, y Render no lo publica. El administrador ve las métricas en la pestaña **Monitoreo**, que no cambia.
- **Logs:** en el panel de Render (pestaña *Logs*), además de `logs/biblioteca.log` dentro del contenedor.
- **Hora:** el backend fija la hora de Lima al arrancar (`fijarZonaHoraria`), así que da igual la zona del servidor.

## Opción C: frontend y backend separados (no recomendado)

Si algún día el frontend va en otro servicio (por ejemplo, un sitio estático):
1. En `frontend/src/environments/environment.prod.ts`, poner en `apiUrl` la URL completa del backend (`https://<backend>/api`).
2. En el backend, poner la variable `APP_CORS_ORIGENES=https://<frontend>` (si son varias, separadas por comas).
3. El sitio estático tiene que redirigir todas las rutas a `index.html`.
