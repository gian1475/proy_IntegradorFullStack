-- =====================================================================
-- DATOS PARA LA DEMO: 21 libros reales con sus autores
-- Los datos (ISBN, editorial y año de la edición) se verificaron en Open Library
-- (openlibrary.org) el 21/10/2026. La portada de cada libro se muestra con su ISBN.
-- Se puede ejecutar varias veces: no duplica autores ni libros.
-- Ejecutar DESPUÉS de schema_postgresql.sql (usa sus categorías y autores).
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
