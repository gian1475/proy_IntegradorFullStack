-- =====================================================================
-- MODELO FÍSICO TRANSPILADO A POSTGRESQL: Sistema de Biblioteca
-- Basado fielmente en: docs/modelofisico.sql y docs/diccionario de datos.docx
-- Proyecto: Sistema Web de control de préstamo de Libros - Biblioteca de Alejandría
-- =====================================================================

-- Opcional: Crear base de datos (ejecutar por separado en pgAdmin o psql si no existe)
-- CREATE DATABASE biblioteca WITH ENCODING 'UTF8';
-- \c biblioteca;

-- ---------------------------------------------------------------------
-- 0. Limpieza previa de tablas en cascada (para ejecución limpia)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS sancion CASCADE;
DROP TABLE IF EXISTS incidencia CASCADE;
DROP TABLE IF EXISTS prestamo CASCADE;
DROP TABLE IF EXISTS reserva CASCADE;
DROP TABLE IF EXISTS libro_autor CASCADE;
DROP TABLE IF EXISTS libro CASCADE;
DROP TABLE IF EXISTS autor CASCADE;
DROP TABLE IF EXISTS categoria CASCADE;
DROP TABLE IF EXISTS prestatario CASCADE;
DROP TABLE IF EXISTS encargado CASCADE;
DROP TABLE IF EXISTS administrador CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;

-- ---------------------------------------------------------------------
-- Tabla: USUARIO (superclase de la generalización ISA)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario      SERIAL PRIMARY KEY,
    nombre          VARCHAR(50)  NOT NULL,
    apellido        VARCHAR(50)  NOT NULL,
    dni             CHAR(8)      NOT NULL,
    correo          VARCHAR(100) NOT NULL,
    contrasena      VARCHAR(255) NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_usuario_dni UNIQUE (dni),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT chk_usuario_estado 
        CHECK (estado IN ('activo', 'inactivo', 'suspendido'))
);

-- ---------------------------------------------------------------------
-- Subtipos de USUARIO (herencia vía FK 1:1 hacia USUARIO)
-- ---------------------------------------------------------------------
CREATE TABLE administrador (
    id_usuario      INTEGER PRIMARY KEY,
    nivel_acceso    VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_administrador_usuario 
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE encargado (
    id_usuario      INTEGER PRIMARY KEY,
    turno           VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_encargado_usuario 
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE prestatario (
    id_usuario          INTEGER PRIMARY KEY,
    tiempos_suspendido  INTEGER NOT NULL DEFAULT 0 CHECK (tiempos_suspendido >= 0),
    CONSTRAINT fk_prestatario_usuario 
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: CATEGORIA
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id_categoria    SERIAL PRIMARY KEY,
    nombre          VARCHAR(50)  NOT NULL,
    descripcion     VARCHAR(255) NULL,
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- Tabla: AUTOR
-- ---------------------------------------------------------------------
CREATE TABLE autor (
    id_autor        SERIAL PRIMARY KEY,
    nombre          VARCHAR(50) NOT NULL,
    apellido        VARCHAR(50) NOT NULL,
    nacionalidad    VARCHAR(50) NULL
);

-- ---------------------------------------------------------------------
-- Tabla: LIBRO
-- Nota: En PostgreSQL el tipo YEAR de MySQL se reemplaza por INTEGER/SMALLINT
-- ---------------------------------------------------------------------
CREATE TABLE libro (
    id_libro            SERIAL PRIMARY KEY,
    id_categoria        INTEGER NOT NULL,
    titulo              VARCHAR(150) NOT NULL,
    isbn                VARCHAR(20)  NOT NULL,
    anio_publicacion    INTEGER      NOT NULL CHECK (anio_publicacion >= 1000 AND anio_publicacion <= 2100),
    editorial           VARCHAR(100) NOT NULL,
    stock               INTEGER      NOT NULL DEFAULT 0 CHECK (stock >= 0),
    estado_libro        VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_libro_isbn UNIQUE (isbn),
    CONSTRAINT fk_libro_categoria 
        FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_libro_estado 
        CHECK (estado_libro IN ('disponible', 'prestado', 'reservado', 'mantenimiento'))
);

-- ---------------------------------------------------------------------
-- Tabla: LIBRO_AUTOR (resuelve la relación M:N entre LIBRO y AUTOR)
-- ---------------------------------------------------------------------
CREATE TABLE libro_autor (
    id_libro    INTEGER NOT NULL,
    id_autor    INTEGER NOT NULL,
    PRIMARY KEY (id_libro, id_autor),
    CONSTRAINT fk_libroautor_libro 
        FOREIGN KEY (id_libro) REFERENCES libro(id_libro)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_libroautor_autor 
        FOREIGN KEY (id_autor) REFERENCES autor(id_autor)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: PRESTAMO
-- Nota técnica: Se emplea TIMESTAMP en PostgreSQL para soportar el control
-- de las 3 horas de uso en sala y calcular retrasos con precisión horaria.
-- ---------------------------------------------------------------------
CREATE TABLE prestamo (
    id_prestamo                 SERIAL PRIMARY KEY,
    id_prestatario               INTEGER NOT NULL,
    id_encargado                 INTEGER NOT NULL,
    id_libro                     INTEGER NOT NULL,
    fecha_prestamo               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_devolucion_prevista    TIMESTAMP NOT NULL,
    fecha_devolucion_real        TIMESTAMP NULL,
    estado_prestamo              VARCHAR(20) NOT NULL,
    CONSTRAINT fk_prestamo_prestatario 
        FOREIGN KEY (id_prestatario) REFERENCES prestatario(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_prestamo_encargado 
        FOREIGN KEY (id_encargado) REFERENCES encargado(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_prestamo_libro 
        FOREIGN KEY (id_libro) REFERENCES libro(id_libro)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_prestamo_estado 
        CHECK (estado_prestamo IN ('activo', 'devuelto', 'vencido')),
    CONSTRAINT chk_prestamo_fechas 
        CHECK (fecha_devolucion_real IS NULL OR fecha_devolucion_real >= fecha_prestamo)
);

-- ---------------------------------------------------------------------
-- Tabla: INCIDENCIA
-- ---------------------------------------------------------------------
CREATE TABLE incidencia (
    id_incidencia       SERIAL PRIMARY KEY,
    id_prestamo         INTEGER NOT NULL,
    tipo_incidencia     VARCHAR(50)  NOT NULL,
    descripcion         VARCHAR(255) NULL,
    fecha_incidencia    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_incidencia   VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_incidencia_prestamo 
        FOREIGN KEY (id_prestamo) REFERENCES prestamo(id_prestamo)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_incidencia_tipo 
        CHECK (tipo_incidencia IN ('retraso', 'daño', 'pérdida')),
    CONSTRAINT chk_incidencia_estado 
        CHECK (estado_incidencia IN ('pendiente', 'resuelta'))
);

-- ---------------------------------------------------------------------
-- Tabla: SANCION (relación opcional 1 a 0..1 con INCIDENCIA)
-- ---------------------------------------------------------------------
CREATE TABLE sancion (
    id_sancion      SERIAL PRIMARY KEY,
    id_incidencia   INTEGER NOT NULL,
    tipo_sancion    VARCHAR(50)  NOT NULL,
    descripcion     VARCHAR(255) NULL,
    fecha_inicio    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin       TIMESTAMP NULL,
    estado_sancion  VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_sancion_incidencia UNIQUE (id_incidencia),
    CONSTRAINT fk_sancion_incidencia 
        FOREIGN KEY (id_incidencia) REFERENCES incidencia(id_incidencia)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_sancion_tipo 
        CHECK (tipo_sancion IN ('advertencia', 'suspensión', 'multa')),
    CONSTRAINT chk_sancion_estado 
        CHECK (estado_sancion IN ('activa', 'cumplida', 'cancelada')),
    CONSTRAINT chk_sancion_fechas 
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- ---------------------------------------------------------------------
-- Tabla: RESERVA
-- ---------------------------------------------------------------------
CREATE TABLE reserva (
    id_reserva          SERIAL PRIMARY KEY,
    id_prestatario      INTEGER NOT NULL,
    id_libro            INTEGER NOT NULL,
    fecha_reserva       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    posicion_cola       INTEGER NOT NULL DEFAULT 1 CHECK (posicion_cola >= 1),
    fecha_recojo_limite TIMESTAMP NULL,
    estado_reserva      VARCHAR(20) NOT NULL,
    CONSTRAINT fk_reserva_prestatario 
        FOREIGN KEY (id_prestatario) REFERENCES prestatario(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_reserva_libro 
        FOREIGN KEY (id_libro) REFERENCES libro(id_libro)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_reserva_estado 
        CHECK (estado_reserva IN ('pendiente', 'atendida', 'cancelada', 'vencida'))
);

-- ---------------------------------------------------------------------
-- Índices para optimización de consultas y reportes
-- ---------------------------------------------------------------------
CREATE INDEX idx_prestamo_fecha ON prestamo (fecha_prestamo);
CREATE INDEX idx_prestamo_estado ON prestamo (estado_prestamo);
CREATE INDEX idx_reserva_estado ON reserva (estado_reserva);
CREATE INDEX idx_libro_titulo ON libro (titulo);
CREATE INDEX idx_libro_isbn ON libro (isbn);
CREATE INDEX idx_usuario_dni ON usuario (dni);
