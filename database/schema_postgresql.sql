-- =====================================================================
-- BASE DE DATOS POSTGRESQL: Sistema de Biblioteca (Biblioteca de Alejandría)
-- Transpilado directamente desde docs/modelofisico.sql y diccionario_de_datos.md
-- =====================================================================

-- 0. Limpieza previa de tablas en cascada
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

-- 1. Tabla: USUARIO (Superclase ISA)
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

-- 2. Subtipos de USUARIO (Herencia JOINED)
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

-- 3. Tabla: CATEGORIA
CREATE TABLE categoria (
    id_categoria    SERIAL PRIMARY KEY,
    nombre          VARCHAR(50)  NOT NULL,
    descripcion     VARCHAR(255) NULL,
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
);

-- 4. Tabla: AUTOR
CREATE TABLE autor (
    id_autor        SERIAL PRIMARY KEY,
    nombre          VARCHAR(50) NOT NULL,
    apellido        VARCHAR(50) NOT NULL,
    nacionalidad    VARCHAR(50) NULL
);

-- 5. Tabla: LIBRO
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

-- 6. Tabla: LIBRO_AUTOR (M:N)
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

-- 7. Tabla: PRESTAMO (Control de 3 horas de uso en sala)
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

-- 8. Tabla: INCIDENCIA
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

-- 9. Tabla: SANCION
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

-- 10. Tabla: RESERVA
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

-- Índices
CREATE INDEX idx_prestamo_fecha ON prestamo (fecha_prestamo);
CREATE INDEX idx_prestamo_estado ON prestamo (estado_prestamo);
CREATE INDEX idx_reserva_estado ON reserva (estado_reserva);
CREATE INDEX idx_libro_titulo ON libro (titulo);
CREATE INDEX idx_libro_isbn ON libro (isbn);
CREATE INDEX idx_usuario_dni ON usuario (dni);

-- =====================================================================
-- DATOS SEMILLA (SEEDS) PARA INICIAR EL SISTEMA
-- =====================================================================
INSERT INTO categoria (nombre, descripcion) VALUES
('Ciencias Sociales', 'Sociología, derecho, economía e historia'),
('Ciencia', 'Física, química, matemáticas y biología'),
('Arte y Cultura', 'Literatura, arte, música y filosofía'),
('Tecnología', 'Ingeniería de software, desarrollo web y sistemas'),
('Idioma', 'Gramática, idiomas extranjeros y lingüística')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO autor (nombre, apellido, nacionalidad) VALUES
('Gabriel', 'García Márquez', 'Colombiana'),
('Robert', 'Martin', 'Estadounidense'),
('Mario', 'Vargas Llosa', 'Peruana'),
('Cay', 'Horstmann', 'Alemana');

INSERT INTO libro (id_categoria, titulo, isbn, anio_publicacion, editorial, stock, estado_libro) VALUES
(4, 'Clean Code', '9780132350884', 2008, 'Prentice Hall', 3, 'disponible'),
(4, 'Core Java Volume I', '9780135166307', 2020, 'Prentice Hall', 2, 'disponible'),
(3, 'Cien Años de Soledad', '9780307474728', 1967, 'Sudamericana', 1, 'disponible'),
(3, 'La Ciudad y los Perros', '9788420471839', 1963, 'Seix Barral', 2, 'disponible')
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO libro_autor (id_libro, id_autor) VALUES
(1, 2), (2, 4), (3, 1), (4, 3)
ON CONFLICT DO NOTHING;

-- Usuarios iniciales (Contraseña de prueba: '123456' con hash BCrypt)
-- Hash: $2a$10$wT8B1mJ0e75aZkmcQhKqC.qO9NqZc4WvhK0bLkmjC19a.uA3oQ5u2
INSERT INTO usuario (nombre, apellido, dni, correo, contrasena, estado) VALUES
('Admin', 'Principal', '11111111', 'admin@alejandria.edu.pe', '$2a$10$wT8B1mJ0e75aZkmcQhKqC.qO9NqZc4WvhK0bLkmjC19a.uA3oQ5u2', 'activo'),
('Carlos', 'Librero', '22222222', 'encargado@alejandria.edu.pe', '$2a$10$wT8B1mJ0e75aZkmcQhKqC.qO9NqZc4WvhK0bLkmjC19a.uA3oQ5u2', 'activo'),
('Juan', 'Estudiante', '33333333', 'alumno@alejandria.edu.pe', '$2a$10$wT8B1mJ0e75aZkmcQhKqC.qO9NqZc4WvhK0bLkmjC19a.uA3oQ5u2', 'activo')
ON CONFLICT (dni) DO NOTHING;

INSERT INTO administrador (id_usuario, nivel_acceso) VALUES (1, 'SUPER_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO encargado (id_usuario, turno) VALUES (2, 'mañana') ON CONFLICT DO NOTHING;
INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (3, 0) ON CONFLICT DO NOTHING;
