-- =====================================================================
-- MODELO FÍSICO: Sistema de Biblioteca
-- Generado a partir del modelo lógico (diagrama Crow's Foot) y el
-- diccionario de datos del proyecto.
-- Motor: InnoDB (soporta FOREIGN KEY y transacciones)
-- Requiere MySQL 8.0.16+ para que las restricciones CHECK se apliquen
-- (en versiones anteriores se aceptan pero se ignoran silenciosamente).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS biblioteca
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE biblioteca;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Tabla: USUARIO (superclase de la generalización ISA)
-- ---------------------------------------------------------------------
CREATE TABLE USUARIO (
    id_usuario      INT UNSIGNED AUTO_INCREMENT,
    nombre          VARCHAR(50)  NOT NULL,
    apellido        VARCHAR(50)  NOT NULL,
    dni             CHAR(8)      NOT NULL,
    correo          VARCHAR(100) NOT NULL,
    contrasena      VARCHAR(255) NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uq_usuario_dni (dni),
    UNIQUE KEY uq_usuario_correo (correo),
    CONSTRAINT chk_usuario_estado
        CHECK (estado IN ('activo', 'inactivo', 'suspendido'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Subtipos de USUARIO (cada uno hereda vía FK 1:1 hacia USUARIO)
-- ---------------------------------------------------------------------
CREATE TABLE ADMINISTRADOR (
    id_usuario      INT UNSIGNED NOT NULL,
    nivel_acceso    VARCHAR(30)  NOT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_administrador_usuario
        FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE ENCARGADO (
    id_usuario      INT UNSIGNED NOT NULL,
    turno           VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_encargado_usuario
        FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE PRESTATARIO (
    id_usuario          INT UNSIGNED NOT NULL,
    tiempos_suspendido  INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_prestatario_usuario
        FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: CATEGORIA
-- ---------------------------------------------------------------------
CREATE TABLE CATEGORIA (
    id_categoria    INT UNSIGNED AUTO_INCREMENT,
    nombre          VARCHAR(50)  NOT NULL,
    descripcion     VARCHAR(255) NULL,
    PRIMARY KEY (id_categoria),
    UNIQUE KEY uq_categoria_nombre (nombre)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: AUTOR
-- ---------------------------------------------------------------------
CREATE TABLE AUTOR (
    id_autor        INT UNSIGNED AUTO_INCREMENT,
    nombre          VARCHAR(50) NOT NULL,
    apellido        VARCHAR(50) NOT NULL,
    nacionalidad    VARCHAR(50) NULL,
    PRIMARY KEY (id_autor)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: LIBRO
-- ---------------------------------------------------------------------
CREATE TABLE LIBRO (
    id_libro            INT UNSIGNED AUTO_INCREMENT,
    id_categoria        INT UNSIGNED NOT NULL,
    titulo              VARCHAR(150) NOT NULL,
    isbn                VARCHAR(20)  NOT NULL,
    anio_publicacion    YEAR         NOT NULL,
    editorial           VARCHAR(100) NOT NULL,
    stock               INT UNSIGNED NOT NULL DEFAULT 0,
    estado_libro        VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_libro),
    UNIQUE KEY uq_libro_isbn (isbn),
    CONSTRAINT fk_libro_categoria
        FOREIGN KEY (id_categoria) REFERENCES CATEGORIA(id_categoria)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_libro_estado
        CHECK (estado_libro IN ('disponible', 'prestado', 'reservado', 'mantenimiento'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: LIBRO_AUTOR (resuelve la M:N entre LIBRO y AUTOR)
-- ---------------------------------------------------------------------
CREATE TABLE LIBRO_AUTOR (
    id_libro    INT UNSIGNED NOT NULL,
    id_autor    INT UNSIGNED NOT NULL,
    PRIMARY KEY (id_libro, id_autor),
    CONSTRAINT fk_libroautor_libro
        FOREIGN KEY (id_libro) REFERENCES LIBRO(id_libro)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_libroautor_autor
        FOREIGN KEY (id_autor) REFERENCES AUTOR(id_autor)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: PRESTAMO
-- ---------------------------------------------------------------------
CREATE TABLE PRESTAMO (
    id_prestamo                 INT UNSIGNED AUTO_INCREMENT,
    id_prestatario               INT UNSIGNED NOT NULL,
    id_encargado                 INT UNSIGNED NOT NULL,
    id_libro                     INT UNSIGNED NOT NULL,
    fecha_prestamo                DATE NOT NULL,
    fecha_devolucion_prevista    DATE NOT NULL,
    fecha_devolucion_real        DATE NULL,
    estado_prestamo               VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_prestamo),
    CONSTRAINT fk_prestamo_prestatario
        FOREIGN KEY (id_prestatario) REFERENCES PRESTATARIO(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_prestamo_encargado
        FOREIGN KEY (id_encargado) REFERENCES ENCARGADO(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_prestamo_libro
        FOREIGN KEY (id_libro) REFERENCES LIBRO(id_libro)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_prestamo_estado
        CHECK (estado_prestamo IN ('activo', 'devuelto', 'vencido')),
    CONSTRAINT chk_prestamo_fechas
        CHECK (fecha_devolucion_real IS NULL OR fecha_devolucion_real >= fecha_prestamo)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: INCIDENCIA
-- ---------------------------------------------------------------------
CREATE TABLE INCIDENCIA (
    id_incidencia       INT UNSIGNED AUTO_INCREMENT,
    id_prestamo         INT UNSIGNED NOT NULL,
    tipo_incidencia     VARCHAR(50)  NOT NULL,
    descripcion         VARCHAR(255) NULL,
    fecha_incidencia    DATE NOT NULL,
    estado_incidencia   VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_incidencia),
    CONSTRAINT fk_incidencia_prestamo
        FOREIGN KEY (id_prestamo) REFERENCES PRESTAMO(id_prestamo)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_incidencia_estado
        CHECK (estado_incidencia IN ('pendiente', 'resuelta'))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: SANCION (relación opcional 1 a (0,1) con INCIDENCIA:
-- la FK con UNIQUE vive en SANCION, no toda incidencia tiene sanción)
-- ---------------------------------------------------------------------
CREATE TABLE SANCION (
    id_sancion      INT UNSIGNED AUTO_INCREMENT,
    id_incidencia   INT UNSIGNED NOT NULL,
    tipo_sancion    VARCHAR(50)  NOT NULL,
    descripcion     VARCHAR(255) NULL,
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE NULL,
    estado_sancion  VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_sancion),
    UNIQUE KEY uq_sancion_incidencia (id_incidencia),
    CONSTRAINT fk_sancion_incidencia
        FOREIGN KEY (id_incidencia) REFERENCES INCIDENCIA(id_incidencia)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_sancion_estado
        CHECK (estado_sancion IN ('activa', 'cumplida', 'cancelada')),
    CONSTRAINT chk_sancion_fechas
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: RESERVA
-- ---------------------------------------------------------------------
CREATE TABLE RESERVA (
    id_reserva          INT UNSIGNED AUTO_INCREMENT,
    id_prestatario      INT UNSIGNED NOT NULL,
    id_libro            INT UNSIGNED NOT NULL,
    fecha_reserva       DATE NOT NULL,
    posicion_cola       INT UNSIGNED NOT NULL,
    fecha_recojo_limite DATE NULL,
    estado_reserva      VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_reserva),
    CONSTRAINT fk_reserva_prestatario
        FOREIGN KEY (id_prestatario) REFERENCES PRESTATARIO(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_reserva_libro
        FOREIGN KEY (id_libro) REFERENCES LIBRO(id_libro)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_reserva_estado
        CHECK (estado_reserva IN ('pendiente', 'atendida', 'cancelada', 'vencida'))
) ENGINE=InnoDB;

-- =====================================================================
-- Índices adicionales recomendados para las columnas FK más consultadas
-- (MySQL crea uno automático por cada FK, pero estos ayudan a consultas
-- de reportes como "préstamos por libro" o "incidencias por usuario")
-- =====================================================================
CREATE INDEX idx_prestamo_fecha ON PRESTAMO (fecha_prestamo);
CREATE INDEX idx_reserva_estado ON RESERVA (estado_reserva);