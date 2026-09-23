# DICCIONARIO DE DATOS

3.4.2.1. Diccionario Detallado por Tabla

Tabla 1: USUARIO

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_usuario | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único del usuario | 
|  | nombre | VARCHAR | 50 | NOT NULL | Nombre del usuario | 
|  | apellido | VARCHAR | 50 | NOT NULL | Apellido del usuario | 
|  | dni | CHAR | 8 | NOT NULL, UNIQUE | Documento Nacional de Identidad | 
|  | correo | VARCHAR | 100 | NOT NULL, UNIQUE | Correo electrónico | 
|  | contraseña | VARCHAR | 255 | NOT NULL | Contraseña cifrada ( BCrypt ) | 
|  | estado | VARCHAR | 20 | NOT NULL, CHECK | Estado: 'activo', 'inactivo', 'suspendido' | 

Fuente: Elaboración propia

Tabla 2: ADMINISTRADOR

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK/FK | id_usuario | INT | 11 | PRIMARY KEY, FOREIGN KEY | Referencia al usuario (herencia) | 
|  | nivel_acceso | VARCHAR | 30 | NOT NULL | Nivel de acceso del administrador | 

Fuente: Elaboración propia

Tabla 3: ENCARGADO

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK/FK | id_usuario | INT | 11 | PRIMARY KEY, FOREIGN KEY | Referencia al usuario (herencia) | 
|  | turno | VARCHAR | 20 | NOT NULL | Turno del encargado (mañana/tarde) | 

Fuente: Elaboración propia

Tabla 4: PRESTATARIO

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK/FK | id_usuario | INT | 11 | PRIMARY KEY, FOREIGN KEY | Referencia al usuario (herencia) | 
|  | tiempos_suspendido | INT | 11 | NOT NULL, DEFAULT 0 | Cantidad de suspensiones acumuladas | 

Fuente: Elaboración propia

Tabla 5: CATEGORIA

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_categoria | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único de la categoría | 
|  | nombre | VARCHAR | 50 | NOT NULL, UNIQUE | Nombre de la categoría | 
|  | descripcion | VARCHAR | 255 | NULL | Descripción de la categoría | 

Fuente: Elaboración propia

Tabla 6: AUTOR

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_autor | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único del autor | 
|  | nombre | VARCHAR | 50 | NOT NULL | Nombre del autor | 
|  | apellido | VARCHAR | 50 | NOT NULL | Apellido del autor | 
|  | nacionalidad | VARCHAR | 50 | NULL | Nacionalidad del autor | 

Fuente: Elaboración propia

Tabla 7: LIBRO

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_libro | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único del libro | 
| FK | id_categoria | INT | 11 | FOREIGN KEY, NOT NULL | Referencia a la categoría | 
|  | titulo | VARCHAR | 150 | NOT NULL | Título del libro | 
|  | isbn | VARCHAR | 20 | NOT NULL, UNIQUE | Código ISBN | 
|  | año_publicacion | YEAR | 4 | NOT NULL | Año de publicación | 
|  | editorial | VARCHAR | 100 | NOT NULL | Editorial | 
|  | stock | INT | 11 | NOT NULL, DEFAULT 0 | Cantidad disponible | 
|  | estado_libro | VARCHAR | 20 | NOT NULL, CHECK | Estado: 'disponible', 'prestado', 'reservado', 'mantenimiento' | 

Fuente: Elaboración propia

Tabla 8: LIBRO_AUTOR

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK/FK | id_libro | INT | 11 | PRIMARY KEY, FOREIGN KEY | Referencia al libro | 
| PK/FK | id_autor | INT | 11 | PRIMARY KEY, FOREIGN KEY | Referencia al autor | 

Fuente: Elaboración propia

Nota: Esta tabla resuelve la relación muchos a muchos (M:N) entre LIBRO y AUTOR.

Tabla 9: PRESTAMO

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_prestamo | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único del préstamo | 
| FK | id_prestatario | INT | 11 | FOREIGN KEY, NOT NULL | Usuario que solicita el préstamo | 
| FK | id_encargado | INT | 11 | FOREIGN KEY, NOT NULL | Encargado que gestiona el préstamo | 
| FK | id_libro | INT | 11 | FOREIGN KEY, NOT NULL | Libro prestado | 
|  | fecha_prestamo | DATE | — | NOT NULL | Fecha del préstamo | 
|  | fecha_devolucion_prevista | DATE | — | NOT NULL | Fecha prevista de devolución | 

Fuente: Elaboración propia

Tabla 10: INCIDENCIA

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_incidencia | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único de la incidencia | 
| FK | id_prestamo | INT | 11 | FOREIGN KEY, NOT NULL | Préstamo asociado | 
|  | tipo_incidencia | VARCHAR | 50 | NOT NULL | Tipo: 'retraso', 'daño', 'pérdida' | 
|  | descripcion | VARCHAR | 255 | NOT NULL | Descripción de la incidencia | 
|  | fecha_incidencia | DATE | — | NOT NULL | Fecha de registro | 
|  | estado_incidencia | VARCHAR | 20 | NOT NULL, CHECK | Estado: 'pendiente', 'resuelta' | 

Fuente: Elaboración propia

Tabla 11: SANCION

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_sancion | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único de la sanción | 
| FK | id_incidencia | INT | 11 | FOREIGN KEY, NOT NULL, UNIQUE | Incidencia que origina la sanción | 
|  | tipo_sancion | VARCHAR | 50 | NOT NULL | Tipo: 'advertencia', 'suspensión', 'multa' | 
|  | descripcion | VARCHAR | 255 | NULL | Descripción de la sanción | 
|  | fecha_inicio | DATE | — | NOT NULL | Fecha de inicio | 
|  | fecha_fin | DATE | — | NULL | Fecha de fin | 
|  | estado_sancion | VARCHAR | 20 | NOT NULL, CHECK | Estado: 'activa', 'cumplida', 'cancelada' | 

Fuente: Elaboración propia

Tabla 12: RESERVA

| Clave | Columna | Tipo de Dato | Longitud | Restricción | Descripción | 
| --- | --- | --- | --- | --- | --- | 
| PK | id_reserva | INT | 11 | PRIMARY KEY, AUTO_INCREMENT | Identificador único de la reserva | 
| FK | id_prestatario | INT | 11 | FOREIGN KEY, NOT NULL | Usuario que reserva | 
| FK | id_libro | INT | 11 | FOREIGN KEY, NOT NULL | Libro reservado | 
|  | fecha_reserva | DATE | — | NOT NULL | Fecha de la reserva | 
|  | posicion_cola | INT | 11 | NOT NULL | Posición en la cola de espera | 
|  | fecha_recojo_limite | DATE | — | NOT NULL | Fecha límite para recoger | 
|  | estado_reserva | VARCHAR | 20 | NOT NULL, CHECK | Estado: 'pendiente', 'atendida', 'cancelada', 'vencida' | 

Fuente: Elaboración propia

