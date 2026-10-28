-- =====================================================================
-- DATOS PARA LA DEMO
-- Parte A (secciones 1 a 3): 21 libros reales con sus autores. Los datos (ISBN, editorial y año
--   de la edición) se verificaron en Open Library (openlibrary.org) el 21/10/2026.
--   La portada de cada libro se muestra con su ISBN.
-- Parte B (sección 4, agregada el 27/10): personal, prestatarios, préstamos, reservas,
--   incidencias y sanciones de ejemplo, para que el dashboard, el historial y los reportes
--   no se vean vacíos.
-- Se puede ejecutar varias veces: no duplica nada.
-- Ejecutar DESPUÉS de schema_postgresql.sql y de las migraciones 01 a 04.
-- Las dos partes ya están en Neon (la A desde el 21/10 y la B desde el 27/10).
-- =====================================================================

-- 1. Autores (solo se insertan si no existen). El número de orden hace que los id salgan
--    en el orden real de los autores (por ejemplo, Gamma antes que Helm en Design Patterns)
INSERT INTO autor (nombre, apellido, nacionalidad)
SELECT v.nombre, v.apellido, v.nacionalidad
FROM (VALUES
    ( 1, 'José María',   'Arguedas',             'Peruana'),
    ( 2, 'Juan',         'Rulfo',                'Mexicana'),
    ( 3, 'Julio',        'Cortázar',             'Argentina'),
    ( 4, 'Miguel',       'de Cervantes Saavedra','Española'),
    ( 5, 'Ernesto',      'Sabato',               'Argentina'),
    ( 6, 'Yuval Noah',   'Harari',               'Israelí'),
    ( 7, 'Eduardo',      'Galeano',              'Uruguaya'),
    ( 8, 'Nicolás',      'Maquiavelo',           'Italiana'),
    ( 9, 'José Carlos',  'Mariátegui',           'Peruana'),
    (10, 'Jean-Jacques', 'Rousseau',             'Suiza'),
    (11, 'Stephen',      'Hawking',              'Británica'),
    (12, 'Carl',         'Sagan',                'Estadounidense'),
    (13, 'Charles',      'Darwin',               'Británica'),
    (14, 'Andrew',       'Hunt',                 'Estadounidense'),
    (15, 'David',        'Thomas',               'Británica'),
    (16, 'Erich',        'Gamma',                'Suiza'),
    (17, 'Richard',      'Helm',                 'Australiana'),
    (18, 'Ralph',        'Johnson',              'Estadounidense'),
    (19, 'John',         'Vlissides',            'Estadounidense'),
    (20, 'Martin',       'Fowler',               'Británica'),
    (21, 'Thomas H.',    'Cormen',               'Estadounidense'),
    (22, 'Charles E.',   'Leiserson',            'Estadounidense'),
    (23, 'Ronald L.',    'Rivest',               'Estadounidense'),
    (24, 'Clifford',     'Stein',                'Estadounidense'),
    (25, 'Raymond',      'Murphy',               'Británica'),
    (26, 'Ferdinand',    'de Saussure',          'Suiza')
) AS v(orden, nombre, apellido, nacionalidad)
WHERE NOT EXISTS (SELECT 1 FROM autor a WHERE a.nombre = v.nombre AND a.apellido = v.apellido)
ORDER BY v.orden; -- en este orden, para que los id respeten el orden real de los autores de cada libro

-- 2. Libros (si el ISBN ya existe, no se vuelve a insertar)
INSERT INTO libro (id_categoria, titulo, isbn, anio_publicacion, editorial, stock, estado_libro)
SELECT c.id_categoria, v.titulo, v.isbn, v.anio, v.editorial, v.stock, 'disponible'
FROM (VALUES
    -- Arte y Cultura (literatura)
    ('Arte y Cultura',    'Conversación en La Catedral',                        '9789871106905', 2004, 'Suma de Letras',            2),
    ('Arte y Cultura',    'Los ríos profundos',                                 '9788437613215', 1995, 'Cátedra',                   3),
    ('Arte y Cultura',    'Pedro Páramo',                                       '9788408045755', 2002, 'Planeta',                   2),
    ('Arte y Cultura',    'Rayuela',                                            '9789681902001', 2000, 'Alfaguara',                 1),
    ('Arte y Cultura',    'Crónica de una muerte anunciada',                    '9789588886176', 2013, 'Debolsillo',                3),
    ('Arte y Cultura',    'Don Quijote de la Mancha',                           '9788441426764', 2011, 'Edaf',                      2),
    ('Arte y Cultura',    'El túnel',                                           '9789584242457', 2014, 'Planeta',                   2),
    -- Ciencias Sociales
    ('Ciencias Sociales', 'Sapiens. De animales a dioses',                      '9788466347518', 2023, 'Debolsillo',                3),
    ('Ciencias Sociales', 'Las venas abiertas de América Latina',               '9788432320248', 2021, 'Siglo XXI de España',       2),
    ('Ciencias Sociales', 'El príncipe',                                        '9788417181161', 2014, 'Navona',                    2),
    ('Ciencias Sociales', '7 ensayos de interpretación de la realidad peruana', '9788466000321', 1979, 'Biblioteca Ayacucho',       2),
    ('Ciencias Sociales', 'El contrato social',                                 '9788430601134', 2012, 'Taurus',                    1),
    -- Ciencia
    ('Ciencia',           'Breve historia del tiempo',                          '9786079377373', 2019, 'Crítica',                   2),
    ('Ciencia',           'Cosmos',                                             '9788432036262', 1985, 'Planeta',                   1),
    ('Ciencia',           'El origen de las especies',                          '9788467033298', 1998, 'Austral',                   2),
    -- Tecnología
    ('Tecnología',        'The Pragmatic Programmer',                           '9780201616224', 1999, 'Addison-Wesley',            2),
    ('Tecnología',        'Design Patterns',                                    '9780201633610', 1995, 'Addison-Wesley',            1),
    ('Tecnología',        'Refactoring',                                        '9780201485677', 1999, 'Addison-Wesley',            2),
    ('Tecnología',        'Introduction to Algorithms',                         '9780070131514', 2001, 'McGraw-Hill',               1),
    -- Idioma
    ('Idioma',            'English Grammar in Use',                             '9780521189392', 2012, 'Cambridge University Press', 3),
    ('Idioma',            'Curso de lingüística general',                       '9788420602271', 1997, 'Alianza',                   1)
) AS v(categoria, titulo, isbn, anio, editorial, stock)
JOIN categoria c ON c.nombre = v.categoria
ON CONFLICT (isbn) DO NOTHING;

-- 3. Autores de cada libro (tabla libro_autor)
INSERT INTO libro_autor (id_libro, id_autor)
SELECT l.id_libro, a.id_autor
FROM (VALUES
    ('9789871106905', 'Mario',        'Vargas Llosa'),
    ('9788437613215', 'José María',   'Arguedas'),
    ('9788408045755', 'Juan',         'Rulfo'),
    ('9789681902001', 'Julio',        'Cortázar'),
    ('9789588886176', 'Gabriel',      'García Márquez'),
    ('9788441426764', 'Miguel',       'de Cervantes Saavedra'),
    ('9789584242457', 'Ernesto',      'Sabato'),
    ('9788466347518', 'Yuval Noah',   'Harari'),
    ('9788432320248', 'Eduardo',      'Galeano'),
    ('9788417181161', 'Nicolás',      'Maquiavelo'),
    ('9788466000321', 'José Carlos',  'Mariátegui'),
    ('9788430601134', 'Jean-Jacques', 'Rousseau'),
    ('9786079377373', 'Stephen',      'Hawking'),
    ('9788432036262', 'Carl',         'Sagan'),
    ('9788467033298', 'Charles',      'Darwin'),
    ('9780201616224', 'Andrew',       'Hunt'),
    ('9780201616224', 'David',        'Thomas'),
    ('9780201633610', 'Erich',        'Gamma'),
    ('9780201633610', 'Richard',      'Helm'),
    ('9780201633610', 'Ralph',        'Johnson'),
    ('9780201633610', 'John',         'Vlissides'),
    ('9780201485677', 'Martin',       'Fowler'),
    ('9780070131514', 'Thomas H.',    'Cormen'),
    ('9780070131514', 'Charles E.',   'Leiserson'),
    ('9780070131514', 'Ronald L.',    'Rivest'),
    ('9780070131514', 'Clifford',     'Stein'),
    ('9780521189392', 'Raymond',      'Murphy'),
    ('9788420602271', 'Ferdinand',    'de Saussure')
) AS v(isbn, nombre, apellido)
JOIN libro l ON l.isbn = v.isbn
JOIN autor a ON a.nombre = v.nombre AND a.apellido = v.apellido
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 4. MOVIMIENTOS DE EJEMPLO (agregado el 27/10)
-- Las personas son ficticias (nombres, DNI y correos inventados).
-- Las fechas se calculan desde HOY (hora de Lima): el mes actual y los últimos 7 días tienen datos.
-- Todas las cuentas nuevas usan la MISMA contraseña de prueba del administrador (se copia su hash;
-- la contraseña no se escribe en el repositorio).
-- Si los datos ya están (existe el DNI de Miguel Torres), no hace nada.
-- =====================================================================

DO $$
DECLARE
    hoy        DATE      := (now() AT TIME ZONE 'America/Lima')::date;  -- hora de lima, no la de la BD
    ahora      TIMESTAMP := (now() AT TIME ZONE 'America/Lima')::timestamp;
    clave      VARCHAR(255);
    enc        INTEGER[] := '{}';   -- 1 = Carlos (ya existe), 2 = Miguel, 3 = Rosa
    pre        INTEGER[] := '{}';   -- prestatarios 1 a 7 (nuevos) y 8 = Juan (ya existe)
    prestamos  INTEGER[] := '{}';   -- id de cada préstamo, por su número en la lista
    r          RECORD;
    id_nuevo   INTEGER;
    id_libro_r INTEGER;
    inicio     TIMESTAMP;
    limite     TIMESTAMP;
    id_inc     INTEGER;
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE dni = '46281935') THEN
        RAISE NOTICE 'Los datos de la demo ya estaban cargados: no se hizo nada';
        RETURN;
    END IF;

    SELECT contrasena INTO clave FROM usuario WHERE correo = 'admin@alejandria.edu.pe';
    enc := enc || (SELECT id_usuario FROM usuario WHERE correo = 'encargado@alejandria.edu.pe');

    -- ---------- 1. Dos encargados más (perfil 2) ----------
    FOR r IN SELECT * FROM (VALUES
        (1, 'Miguel', 'Torres Salazar', '46281935', 'miguel.torres@alejandria.edu.pe', 'mañana'),
        (2, 'Rosa',   'Quispe Mamani',  '45120378', 'rosa.quispe@alejandria.edu.pe',   'tarde')
    ) AS t(n, nombre, apellido, dni, correo, turno) ORDER BY n LOOP
        INSERT INTO usuario (nombre, apellido, dni, correo, contrasena, estado)
        VALUES (r.nombre, r.apellido, r.dni, r.correo, clave, 'activo') RETURNING id_usuario INTO id_nuevo;
        INSERT INTO encargado (id_usuario, turno) VALUES (id_nuevo, r.turno);
        enc := enc || id_nuevo;
    END LOOP;

    -- ---------- 2. Prestatarios (perfil 3). José Luis está dado de baja (eliminación lógica) ----------
    FOR r IN SELECT * FROM (VALUES
        (1, 'Lucía',      'Ramírez Soto',      '71234561', 'lucia.ramirez@alumnos.alejandria.edu.pe',      'activo'),
        (2, 'Diego',      'Huamán Paredes',    '72345672', 'diego.huaman@alumnos.alejandria.edu.pe',       'activo'),
        (3, 'Valeria',    'Flores Gutiérrez',  '73456783', 'valeria.flores@alumnos.alejandria.edu.pe',     'activo'),
        (4, 'Andrés',     'Castillo Rojas',    '74567894', 'andres.castillo@alumnos.alejandria.edu.pe',    'activo'),
        (5, 'Camila',     'Rojas Medina',      '75678905', 'camila.rojas@alumnos.alejandria.edu.pe',       'activo'),
        (6, 'Sebastián',  'Vargas León',       '76789016', 'sebastian.vargas@alumnos.alejandria.edu.pe',   'activo'),
        (7, 'José Luis',  'Paredes Cárdenas',  '77890127', 'joseluis.paredes@alumnos.alejandria.edu.pe',   'inactivo')
    ) AS t(n, nombre, apellido, dni, correo, estado) ORDER BY n LOOP
        INSERT INTO usuario (nombre, apellido, dni, correo, contrasena, estado)
        VALUES (r.nombre, r.apellido, r.dni, r.correo, clave, r.estado) RETURNING id_usuario INTO id_nuevo;
        INSERT INTO prestatario (id_usuario, tiempos_suspendido) VALUES (id_nuevo, 0);
        pre := pre || id_nuevo;
    END LOOP;
    pre := pre || (SELECT id_usuario FROM usuario WHERE correo = 'alumno@alejandria.edu.pe');

    -- ---------- 3. Préstamos en sala ----------
    -- dias = hace cuántos días; hora = inicio; minutos = cuánto estuvo en sala (NULL = no se devolvió).
    -- Hora límite = inicio + 3 h (+1 h si se amplió), sin pasar de las 20:00 (horario de atención)
    FOR r IN SELECT * FROM (VALUES
        ( 1, 1, 'Clean Code',                                        26, '09:00', 120, false, 'bueno',  'devuelto', 1),
        ( 2, 2, 'Sapiens. De animales a dioses',                     24, '10:30', 170, false, 'bueno',  'devuelto', 2),
        ( 3, 3, 'Cien Años de Soledad',                              22, '15:00', 150, false, 'bueno',  'devuelto', 3),
        ( 4, 4, 'Introduction to Algorithms',                        20, '09:30', 230, true,  'bueno',  'devuelto', 1),
        ( 5, 5, 'Pedro Páramo',                                      18, '16:00', 100, false, 'bueno',  'devuelto', 3),
        ( 6, 8, 'Core Java Volume I',                                16, '11:00', 165, false, 'bueno',  'devuelto', 2),
        ( 7, 6, 'English Grammar in Use',                            14, '14:30',  90, false, 'bueno',  'devuelto', 3),
        ( 8, 1, 'Breve historia del tiempo',                         13, '10:00', 170, false, 'bueno',  'devuelto', 1),
        ( 9, 2, 'Design Patterns',                                   12, '17:30', 130, false, 'bueno',  'devuelto', 3),
        (10, 3, 'Don Quijote de la Mancha',                          11, '09:00', 175, false, 'bueno',  'devuelto', 2),
        (11, 7, 'El príncipe',                                       10, '12:00', 120, false, 'bueno',  'devuelto', 1),
        (12, 4, 'Refactoring',                                        9, '15:30', NULL, false, NULL,    'anulado',  3),
        (13, 5, 'Rayuela',                                            8, '10:00', 160, false, 'bueno',  'devuelto', 2),
        (14, 8, 'Los ríos profundos',                                 6, '09:30', 150, false, 'bueno',  'devuelto', 1),
        (15, 6, 'Cosmos',                                             6, '14:00', 140, false, 'dañado', 'devuelto', 3),
        (16, 1, 'Crónica de una muerte anunciada',                    5, '11:00', 120, false, 'bueno',  'devuelto', 2),
        (17, 2, 'Sapiens. De animales a dioses',                      5, '16:00', 175, false, 'bueno',  'devuelto', 3),
        (18, 4, 'The Pragmatic Programmer',                           4, '10:00', 210, true,  'bueno',  'devuelto', 1),
        (19, 3, 'El túnel',                                           3, '15:00', 220, false, 'bueno',  'devuelto', 3),
        (20, 5, 'Conversación en La Catedral',                        2, '09:00', 140, false, 'bueno',  'devuelto', 2),
        (21, 8, '7 ensayos de interpretación de la realidad peruana', 2, '14:30', 120, false, 'bueno',  'devuelto', 3),
        (22, 6, 'El origen de las especies',                          1, '10:00', 130, false, 'bueno',  'devuelto', 1),
        (23, 1, 'La Ciudad y los Perros',                             1, '16:30', 175, false, 'bueno',  'devuelto', 3),
        (24, 2, 'Curso de lingüística general',                       1, '12:00', NULL, false, NULL,    'anulado',  2),
        (25, 4, 'Clean Code',                                         0, '15:00', NULL, false, NULL,    'activo',   3),
        (26, 5, 'Rayuela',                                            0, '17:30', NULL, false, NULL,    'activo',   3),
        (27, 2, 'El contrato social',                                 0, '09:00', 110, false, 'bueno',  'devuelto', 1),
        (28, 1, 'Core Java Volume I',                                 0, '10:30', 150, false, 'bueno',  'devuelto', 2)
    ) AS t(n, p, titulo, dias, hora, minutos, ampliado, fisico, estado, e) ORDER BY n LOOP
        inicio := (hoy - r.dias) + r.hora::time;
        limite := LEAST(inicio + INTERVAL '3 hours' + CASE WHEN r.ampliado THEN INTERVAL '1 hour' ELSE INTERVAL '0' END,
                        (hoy - r.dias) + TIME '20:00');
        INSERT INTO prestamo (id_prestatario, id_encargado, id_libro, fecha_prestamo, fecha_devolucion_prevista,
                              fecha_devolucion_real, ampliado, estado_fisico_devolucion, estado_prestamo)
        VALUES (pre[r.p], enc[r.e], (SELECT id_libro FROM libro WHERE titulo = r.titulo), inicio, limite,
                CASE WHEN r.minutos IS NULL THEN NULL ELSE inicio + r.minutos * INTERVAL '1 minute' END,
                r.ampliado, r.fisico,
                -- un préstamo sin devolver queda 'vencido' si su hora límite ya pasó
                CASE WHEN r.estado = 'activo' AND limite < ahora THEN 'vencido' ELSE r.estado END)
        RETURNING id_prestamo INTO id_nuevo;
        prestamos[r.n] := id_nuevo;
    END LOOP;

    -- stock automatico: los 2 préstamos sin devolver se llevaron un ejemplar cada uno
    UPDATE libro SET stock = stock - 1,
                     estado_libro = CASE WHEN stock - 1 = 0 THEN 'prestado' ELSE estado_libro END
    WHERE titulo IN ('Clean Code', 'Rayuela');
    -- mantenimiento: Cosmos (1 ejemplar) volvió dañado, no regresa al stock
    UPDATE libro SET stock = stock - 1,
                     estado_libro = CASE WHEN stock - 1 = 0 THEN 'mantenimiento' ELSE estado_libro END
    WHERE titulo = 'Cosmos';

    -- ---------- 4. Incidencias y sanciones ----------
    -- Cosmos devuelto dañado: multa de S/ 25.00, ya pagada
    INSERT INTO incidencia (id_prestamo, tipo_incidencia, descripcion, fecha_incidencia, estado_incidencia)
    SELECT prestamos[15], 'daño', 'Páginas 40 a 52 mojadas', fecha_devolucion_real, 'resuelta'
    FROM prestamo WHERE id_prestamo = prestamos[15] RETURNING id_incidencia INTO id_inc;
    INSERT INTO sancion (id_incidencia, tipo_sancion, monto, descripcion, fecha_inicio, fecha_fin, estado_sancion)
    SELECT id_inc, 'multa', 25.00, 'Reposición parcial del ejemplar', fecha_devolucion_real,
           fecha_devolucion_real + INTERVAL '2 days', 'cumplida'
    FROM prestamo WHERE id_prestamo = prestamos[15];

    -- El túnel devuelto con 40 min de retraso: suspensión activa hasta dentro de 4 días.
    -- sancion activa: Valeria no puede pedir préstamos ni reservar mientras dure
    INSERT INTO incidencia (id_prestamo, tipo_incidencia, descripcion, fecha_incidencia, estado_incidencia)
    SELECT prestamos[19], 'retraso', 'Devuelto 40 minutos después de la hora límite', fecha_devolucion_real, 'pendiente'
    FROM prestamo WHERE id_prestamo = prestamos[19] RETURNING id_incidencia INTO id_inc;
    INSERT INTO sancion (id_incidencia, tipo_sancion, monto, descripcion, fecha_inicio, fecha_fin, estado_sancion)
    SELECT id_inc, 'suspensión', NULL, 'Suspensión de 7 días por retraso', fecha_devolucion_real,
           (hoy + 4) + TIME '20:00', 'activa'
    FROM prestamo WHERE id_prestamo = prestamos[19];
    UPDATE usuario SET estado = 'suspendido' WHERE id_usuario = pre[3];
    UPDATE prestatario SET tiempos_suspendido = 1 WHERE id_usuario = pre[3];

    -- ---------- 5. Reservas ----------
    -- dias_reserva / dias_recojo = hace cuántos días (-1 = mañana). En la cola no hay fecha de recojo
    FOR r IN SELECT * FROM (VALUES
        -- historial: atendidas (se convirtieron en préstamo), canceladas y vencidas
        (5, 'Rayuela',                                            9, '18:00',  8, '11:30', 'atendida',  1),
        (8, '7 ensayos de interpretación de la realidad peruana', 3, '19:30',  2, '17:30', 'atendida',  1),
        (2, 'Cien Años de Soledad',                              15, '20:10', 14, '14:30', 'cancelada', 1),
        (6, 'Sapiens. De animales a dioses',                      4, '08:15',  3, '11:30', 'cancelada', 1),
        (4, 'Pedro Páramo',                                      10, '21:00',  9, '11:30', 'vencida',   1),
        (1, 'Don Quijote de la Mancha',                           2, '13:00',  1, '14:30', 'vencida',   1),
        -- vigentes: pendientes de recojo (mañana) y la cola de espera de Cien Años de Soledad
        (1, 'Design Patterns',                                    0, '19:15', -1, '14:30', 'pendiente', 1),
        (2, 'Breve historia del tiempo',                          0, '20:40', -1, '20:00', 'pendiente', 1),
        (6, 'Cien Años de Soledad',                               0, '18:05', -1, '20:00', 'pendiente', 1),
        (5, 'Cien Años de Soledad',                               0, '20:30', NULL, NULL,  'en_espera', 1),
        (4, 'Cien Años de Soledad',                               0, '21:05', NULL, NULL,  'en_espera', 2)
    ) AS t(p, titulo, dias_reserva, hora_reserva, dias_recojo, hora_recojo, estado, posicion) LOOP
        SELECT id_libro INTO id_libro_r FROM libro WHERE titulo = r.titulo;
        INSERT INTO reserva (id_prestatario, id_libro, fecha_reserva, posicion_cola, fecha_recojo_limite, estado_reserva)
        VALUES (pre[r.p], id_libro_r, (hoy - r.dias_reserva) + r.hora_reserva::time, r.posicion,
                CASE WHEN r.dias_recojo IS NULL THEN NULL ELSE (hoy - r.dias_recojo) + r.hora_recojo::time END,
                r.estado);
        -- stock automatico: cada reserva pendiente aparta un ejemplar; la cola no aparta nada
        IF r.estado = 'pendiente' THEN
            UPDATE libro SET stock = stock - 1,
                             estado_libro = CASE WHEN stock - 1 = 0 THEN 'reservado' ELSE estado_libro END
            WHERE id_libro = id_libro_r;
        END IF;
    END LOOP;

    RAISE NOTICE 'Datos de la demo cargados: 2 encargados, 7 prestatarios, 28 préstamos, 11 reservas, 2 incidencias y 2 sanciones';
END $$;
