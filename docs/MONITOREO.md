# Monitoreo del sistema (semana 16)

En la semana 16 el profe pidió, para los anexos del informe:

1. **el nombre de la herramienta** de monitoreo;
2. **una captura de pantalla completa** con las métricas de **memoria, CPU e hilos**, tomada **mientras se usa el sistema** (registrar, consultar, eliminar y generar reportes);
3. **la explicación de cada métrica y de los valores obtenidos**, porque lo pregunta.

También dijo que el monitoreo es una **prueba no funcional de rendimiento**: no basta con que el sistema funcione, importa cuántos recursos usa. Resumen de la clase en `docs/clases/s16integradorresumen.txt`.

---

## 1. Las dos herramientas

| Herramienta | Qué es | Dónde se ve | Quién la usa |
|---|---|---|---|
| **Spring Boot Actuator + Micrometer** | Librería que va **dentro** del sistema y mide la aplicación mientras corre | Pestaña **Monitoreo** del administrador. Desde el servidor, también en `http://127.0.0.1:8081/actuator/metrics` | El administrador |
| **JConsole** (Java Monitoring and Management Console) | Herramienta de monitoreo que **viene con el JDK** de Java. No es código del sistema, no es una librería y no se instala aparte | Una ventana en el servidor, con **gráficos en el tiempo** | Quien administra el servidor |

**Por qué las dos:**
- La pestaña Monitoreo muestra el valor de **un momento**, más datos del negocio: el % de peticiones en 2 s o menos (RNF03), las sesiones y los préstamos activos.
- JConsole dibuja la **curva** de memoria, CPU e hilos **mientras se usa el sistema**, que es justo lo que pidió el profe.
- JConsole no se agregó a la aplicación: los usuarios no la ven y no hay que cambiar nada para usarla.

---

## 2. Paso a paso (10 a 15 minutos)

1. **Levantar el sistema con el `.jar`.** Si no está armado, ver `docs/DESPLIEGUE.md` (`npx ng build` y `./mvnw clean package`).
   ```powershell
   cd backend
   java -jar target\biblioteca-alejandria.jar
   ```
   Esperar a que salga `Started BibliotecaApplication`.
2. **Abrir JConsole** desde otra terminal. La ruta cambia según el JDK instalado; en la PC del equipo es:
   ```powershell
   & "C:\Program Files\Java\jdk-26\bin\jconsole.exe"
   ```
   Si no la encuentran: `Get-ChildItem "C:\Program Files\Java" -Recurse -Filter jconsole.exe`. Cualquier JDK 21 o superior la trae.
3. **Conectarse:** en *New Connection*, elegir **Local Process** → `target\biblioteca-alejandria.jar` → **Connect**. Va a aparecer *"Secure connection failed. Retry insecurely?"*: hacer **clic** en **Insecure connection** (con Enter no responde). Es una conexión dentro de la misma PC.
4. Ir a la pestaña **Overview** (resumen). Muestra 4 gráficos: *Heap Memory Usage* (memoria), *Threads* (hilos), *Classes* (clases) y *CPU Usage* (CPU). En *Time Range* se puede elegir "5 min" o "All".
5. **Usar el sistema de 5 a 10 minutos**, con JConsole abierto. Lo más simple es seguir la prueba completa de la sección 12 de `GUION_SUSTENTACION.md`:
   - entrar con los 3 perfiles;
   - registrar un préstamo y descargar su comprobante;
   - consultar el historial y el catálogo;
   - anular un préstamo y cancelar una reserva;
   - generar reportes en PDF y en Excel. Los reportes son lo que más CPU usa, así que se nota el pico.
6. **Capturas**, todas de **pantalla completa**, con el reloj de Windows a la vista:
   - JConsole → **Overview**, con las curvas de esos minutos de uso. Es la captura principal del Anexo G;
   - JConsole → **Memory** (*Used*, *Committed* y *Max*) y **Threads** (*Live*, *Peak* y *Daemon*), si piden el detalle;
   - la pestaña **Monitoreo** del sistema, en el mismo momento.
7. **Anotar los valores** en la columna "Valor obtenido" de la tabla del Anexo G (ver la sección 5).
8. Cerrar JConsole. Cerrarlo no afecta al sistema.

**Si el `.jar` no aparece en *Local Process*:**
- JConsole y el `.jar` tienen que correr con el **mismo usuario de Windows**: no abran una de las dos como administrador;
- `jps -l`, que está en la misma carpeta de JConsole, lista los procesos Java que se pueden ver.

---

## 3. Qué significa cada métrica

**Valores de referencia (24/11/2026):** se midió el `.jar` contra Neon en la PC del equipo (12 núcleos lógicos, unos 23 GB de RAM, recolector de basura G1). Primero 1.200 consultas seguidas al catálogo y después 4 usuarios a la vez (320 consultas más). En la demo los valores van a ser otros, y eso es normal: lo que se explica es **qué significan** y si están dentro de lo esperado.

| Métrica | En JConsole | En la pestaña Monitoreo | Unidad | Qué indica | Referencia (24/11) | Cómo leerla |
|---|---|---|---|---|---|---|
| **Memoria heap usada** | Heap Memory Usage → *Used* | "Memoria (heap)", el primer número (`jvm.memory.used`, área heap) | MB | Memoria que ocupan los objetos Java: los datos de cada petición, las sesiones, los reportes que se están armando y la basura que todavía no se recogió | 238 MB recién arrancado; de 330 a 400 MB con uso | Sube con cada petición y **baja de golpe** cuando pasa el recolector de basura (el gráfico tiene forma de sierra). Es normal. Habría un problema si después de cada recolección quedara cada vez más alta (una fuga de memoria) o si se acercara al máximo |
| **Memoria comprometida** | *Committed* | — (`jvm.memory.committed`) | MB | Memoria que Windows ya le entregó a Java, usada o no | 540 MB | Java la pide por adelantado para no pedirla en cada petición. No es lo mismo que la usada |
| **Memoria máxima** | *Max* | "Memoria (heap)", el segundo número (`jvm.memory.max`) | MB | Tope del heap. Por defecto, Java toma la cuarta parte de la RAM | 5.984 MB | Usamos entre 4 % y 7 % del máximo: hay mucho margen |
| **Memoria no heap** | Memory → *Non-Heap* | — | MB | Las clases cargadas (Spring, Hibernate, POI, OpenPDF...) y el código ya compilado | 130 a 150 MB | Casi fija después de arrancar |
| **CPU del proceso** | *CPU Usage* | "CPU del proceso" (`process.cpu.usage`) | % de **toda** la máquina (los 12 núcleos) | Cuánto procesador usa el sistema | 11,6 % al arrancar; 0,3 % en reposo; hasta 2,2 % con 4 usuarios a la vez | El pico del arranque es normal: carga unas 20.000 clases y compila. En uso normal casi no se nota: 2 % de 12 núcleos es la cuarta parte de un núcleo. Los reportes en Excel y PDF son lo que más CPU usa |
| **Hilos vivos** | Threads → *Live threads* (también *Peak* y *Daemon*) | "Hilos activos" (`jvm.threads.live`) | Cantidad | Tareas que Java ejecuta al mismo tiempo | 41 vivos (pico 42; 34 de ellos *daemon*, de servicio) | **Estable:** no crece con las peticiones, porque Tomcat reutiliza sus hilos. Si creciera sin parar, habría tareas que no terminan. Al conectar JConsole suben unos pocos más: son los de la propia conexión de JConsole |
| **Clases cargadas** | *Classes* | — | Cantidad | Clases Java en memoria (las nuestras y las de las librerías) | 20.129 al arrancar; 20.753 después | Casi fija; sube un poco la primera vez que se usa cada parte del sistema |

**De dónde salen los 41 hilos** (con `jcmd <pid> Thread.print`, del JDK):

| Hilos | Cuántos | Para qué |
|---|---|---|
| `http-nio-8080-exec-1` a `-10` | 10 | Atienden las peticiones del sistema (puerto 8080). Tomcat los reutiliza |
| `http-nio-127.0.0.1-8081-exec-1` a `-10` | 10 | Atienden el monitoreo de Actuator (puerto 8081, solo desde el servidor) |
| `Acceptor`, `Poller`, `Catalina-utility`, `container` | 10 | Reciben las conexiones nuevas y hacen el mantenimiento de los dos Tomcat |
| `HikariPool-1:housekeeper` | 1 | Cuida el pool de 10 conexiones a Neon |
| Compiladores (`C1`/`C2 CompilerThread`), `Finalizer`, `Reference Handler`, `Signal Dispatcher`, `Common-Cleaner` y otros de Java | Unos 10 | Servicio interno de Java |

**Otras métricas de la pestaña Monitoreo** (Tabla 62 del informe), con la referencia del 24/11:
- Peticiones HTTP: 1.520, 0 con error.
- Tiempo de respuesta promedio: 198 ms. Casi todo es el viaje a Neon, que está en la nube.
- La petición más lenta tardó 0,88 s: **todas** respondieron en 2 s o menos, así que cumple el RNF03.
- El recolector de basura pausó 1 vez en 6 minutos, durante 6 ms.

### Medición con JConsole durante la prueba completa (25/11, de 00:00 a 00:02)

Capturas en `docs/capturas/2026-11-24/monitoreo/` y `sistema/16-admin-monitoreo-en-uso.png`. Estos son los valores que van en la columna "Valor obtenido" del Anexo G:

| Métrica | Valor obtenido | Cómo explicarlo |
|---|---|---|
| Memoria heap usada | De 115 a 187 MB (de un máximo de 6,1 GB) | Subió mientras se registraban préstamos, reservas y reportes |
| Memoria comprometida | 553 MB | Lo que Windows ya le dio a Java |
| Recolector de basura | 22 recolecciones "young" en 0,1 s en total | Limpia la memoria sin detener el sistema |
| CPU del proceso | Picos de hasta 1 % | Los picos coinciden con los reportes en PDF |
| Hilos vivos | De 47 a 50 (pico 50) | Los 41 de siempre más los de la conexión de JConsole (RMI) |
| Clases cargadas | De 20.300 a 21.910 | Suben un poco la primera vez que se usa cada parte |
| Peticiones (pestaña Monitoreo) | 61, 0 con error, 623,7 ms de promedio | El promedio incluye las primeras consultas a Neon, más lentas |
| Peticiones en 2 s o menos | **95,1 % → Cumple RNF03** | Ver D34, abajo |

**D34 (24/11): por qué el RNF03 cuenta desde el inicio.** Micrometer guarda por defecto el histograma ("cuántas peticiones tardaron hasta 2 s") solo de los **últimos 2 minutos**, mientras que el total de peticiones es desde que arrancó el sistema. Al dividir uno por el otro, el porcentaje bajaba solo con el tiempo: el 24/11 la pestaña marcó "28 % — No cumple" con un promedio de 570 ms.

`service/MonitoreoConfig` es un `MeterFilter` de Micrometer que deja ese histograma sin ventana, así que el % se cuenta desde el inicio, igual que el total. Lo prueba `MonitoreoServiceTest` con un reloj falso.

---

## 4. Qué decir (30 segundos)

> "Monitoreamos con dos herramientas. **Spring Boot Actuator**, que está dentro del sistema y alimenta la pestaña Monitoreo del administrador, y **JConsole**, que viene con el JDK y la abrimos en el servidor mientras usábamos el sistema: registramos préstamos, consultamos, anulamos y generamos reportes.
> En **memoria**, el heap usado se movió entre 240 y 400 MB de un máximo de casi 6.000; sube con el uso y baja cuando pasa el recolector de basura, esa es la forma de sierra del gráfico.
> El **CPU** tuvo un pico al arrancar y en uso estuvo por debajo del 3 % de la máquina; lo que más consume son los reportes.
> Los **hilos** se mantuvieron en unos 41: 10 de Tomcat para atender las peticiones, 10 para el monitoreo y el resto son de servicio. No crecen con el uso porque Tomcat los reutiliza."

---

## 5. Para el informe (Anexo G)

Lo que hay que agregar está en **I80.1 de `PLAN_INFORME.md`**:
- el nombre de las dos herramientas, al inicio del anexo;
- la captura de JConsole y la de la pestaña Monitoreo;
- la columna "Valor obtenido" en la Tabla 62, con los valores **de su propia captura**;
- un párrafo que los interprete. Puede ser el de la sección 4, con sus números.

---

## 6. Memoria en Render

En Render, el plan gratuito da 512 MB de RAM. Si Java arranca sin decir cuánta memoria usar, dentro de un contenedor toma por defecto **la cuarta parte** para el heap: unos 128 MB. En la PC medimos unos 240 MB de heap usados recién arrancado (con basura todavía sin recoger).

Por eso, desde el 24/11 (D25), el `Dockerfile` arranca con `-XX:MaxRAMPercentage=75`: el heap puede llegar a 3/4 de la memoria del contenedor, unos 384 MB. Es una opción estándar de Java, no una librería. Cuando lo publiquen, revisen la pestaña Monitoreo: la "Memoria (heap)" máxima tiene que decir unos 384 MB.
