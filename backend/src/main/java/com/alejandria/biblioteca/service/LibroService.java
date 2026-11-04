package com.alejandria.biblioteca.service;

import com.alejandria.biblioteca.dao.AutorDao;
import com.alejandria.biblioteca.dao.CategoriaDao;
import com.alejandria.biblioteca.dao.LibroDao;
import com.alejandria.biblioteca.dao.PrestamoDao;
import com.alejandria.biblioteca.dao.ReservaDao;
import com.alejandria.biblioteca.dto.libro.LibroDTO;
import com.alejandria.biblioteca.dto.libro.LibroFormDTO;
import com.alejandria.biblioteca.entity.Autor;
import com.alejandria.biblioteca.entity.Categoria;
import com.alejandria.biblioteca.entity.Libro;
import com.alejandria.biblioteca.exception.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;

/** CRUD de libros. Los libros dados de baja no se borran: quedan con estado 'baja' y no salen en el catálogo. */
@Service
public class LibroService {

    private static final String BAJA = "baja";

    private final LibroDao libroDao;
    private final CategoriaDao categoriaDao;
    private final AutorDao autorDao;
    private final PrestamoDao prestamoDao;
    private final ReservaDao reservaDao;
    private final ColaEsperaService colaEspera;

    public LibroService(LibroDao libroDao, CategoriaDao categoriaDao, AutorDao autorDao,
                        PrestamoDao prestamoDao, ReservaDao reservaDao, ColaEsperaService colaEspera) {
        this.libroDao = libroDao;
        this.categoriaDao = categoriaDao;
        this.autorDao = autorDao;
        this.prestamoDao = prestamoDao;
        this.reservaDao = reservaDao;
        this.colaEspera = colaEspera;
    }

    // ---------- CONSULTAR ----------

    // crud libro consultar: trae los libros menos los dados de baja.
    // se muestra: titulo, isbn, año, editorial, stock, estado, categoria y autores.
    // OJO: NO es la consulta de una tabla. con @EntityGraph usa 4 tablas (libro, categoria, libro_autor y autor)
    // en un solo JOIN. la consulta de una tabla del sistema es PrestamoDao.buscarPorPrestatario
    @Transactional(readOnly = true)
    public List<LibroDTO> listar() {
        return aDTO(libroDao.findByEstadoLibroNotOrderByTitulo(BAJA));
    }

    /** RF03: busca por título, autor, categoría o código (ISBN). Cada libro trae su stock y su estado. */
    @Transactional(readOnly = true)
    public List<LibroDTO> buscar(String texto) {
        if (!StringUtils.hasText(texto)) { // null, "" o solo espacios (StringUtils de Spring)
            return listar();
        }
        return aDTO(libroDao.buscarEnCatalogo(texto.trim()));
    }

    @Transactional(readOnly = true)
    public List<LibroDTO> buscarPorAutor(String nombre) {
        return aDTO(sinBajas(libroDao.buscarPorAutor(nombre)));
    }

    @Transactional(readOnly = true)
    public LibroDTO obtener(Integer idLibro) {
        return LibroDTO.desde(buscar(idLibro));
    }

    /** Categorías para el formulario de libros. */
    @Transactional(readOnly = true)
    public List<LibroDTO.CategoriaResumen> listarCategorias() {
        return categoriaDao.findAll().stream()
                .map(c -> new LibroDTO.CategoriaResumen(c.getIdCategoria(), c.getNombre()))
                .toList();
    }

    /** Autores para el formulario de libros. */
    @Transactional(readOnly = true)
    public List<LibroDTO.AutorResumen> listarAutores() {
        return autorDao.findAll().stream()
                .map(a -> new LibroDTO.AutorResumen(a.getIdAutor(), a.getNombre(), a.getApellido()))
                .toList();
    }

    // ---------- ADICIONAR ----------

    // crud libro adicionar: aqui se registra el libro nuevo.
    // datos del libro q se registran: titulo, isbn, editorial, año de publicacion, stock (numero de copias),
    // categoria y autores. se guardan en la tabla libro y los autores en la tabla libro_autor
    @Transactional
    public LibroDTO crear(LibroFormDTO form) {
        // validaciones antes de guardar: campos obligatorios, stock no negativo y año valido los revisan
        // las anotaciones de LibroFormDTO (bean validation). aqui quedan las reglas q necesitan la bd
        // estado del libro (RF03): sin ejemplares, el estado depende del movimiento q se llevo el ultimo
        // (prestamo -> 'prestado', reserva -> 'reservado', daño -> 'mantenimiento'). un libro nuevo no tuvo
        // ningun movimiento, asi q entra con al menos 1 ejemplar. antes con 0 quedaba 'prestado' sin prestamo
        if (form.stock() < 1) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Un libro nuevo debe tener al menos 1 ejemplar");
        }
        // y q el isbn no este repetido
        if (libroDao.existsByIsbn(form.isbn().trim())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe un libro con ese ISBN");
        }

        Libro libro = new Libro();
        copiarDatos(form, libro);
        libro.setEstadoLibro("disponible"); // estado del libro: siempre entra disponible
        return LibroDTO.desde(libroDao.save(libro));
    }

    // ---------- ACTUALIZAR ----------

    // crud libro actualizar: se pueden cambiar los mismos datos del registro (titulo, isbn, editorial,
    // año, stock, categoria y autores). lo q mas se actualiza es el stock (numero de copias)
    @Transactional
    public LibroDTO actualizar(Integer idLibro, LibroFormDTO form) {
        Libro libro = buscar(idLibro);
        if (BAJA.equals(libro.getEstadoLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "No se puede editar un libro dado de baja");
        }
        if (libroDao.existsByIsbnAndIdLibroNot(form.isbn().trim(), idLibro)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe otro libro con ese ISBN");
        }
        // estado del libro: el stock solo llega a 0 con un prestamo, una reserva o una devolucion dañada.
        // si el admin lo bajara a 0 a mano, el libro quedaria 'prestado' sin ningun prestamo
        if (form.stock() == 0 && libro.getStock() > 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El stock no puede bajar a 0 desde el formulario: los ejemplares salen al prestarlos, reservarlos o devolverlos dañados");
        }

        copiarDatos(form, libro);

        // cola de espera: si el admin agrega ejemplares y hay prestatarios esperando, primero se reparten a la cola
        int repartidos = 0;
        while (libro.getStock() > 0 && colaEspera.asignarAlSiguiente(idLibro)) {
            libro.setStock(libro.getStock() - 1);
            repartidos++;
        }

        // El estado sigue al stock: con ejemplares vuelve a 'disponible'
        // (también sale de 'mantenimiento' cuando el admin repone el ejemplar reparado)
        if (libro.getStock() == 0 && repartidos > 0) {
            libro.setEstadoLibro("reservado"); // los ejemplares nuevos quedaron apartados para la cola
        } else if (libro.getStock() > 0 && estaSinEjemplares(libro)) {
            libro.setEstadoLibro("disponible");
        }
        return LibroDTO.desde(libroDao.save(libro));
    }

    // ---------- ELIMINAR (lógico) ----------

    // crud libro eliminar -> eliminacion logica: no se hace DELETE, se hace un UPDATE del estado a 'baja'
    // y el libro ya no sale en el catalogo.
    // por q logica: el libro tiene historial, los prestamos y reservas lo apuntan con llave foranea.
    // si se borra la fila la bd no deja (o habria q borrar sus prestamos) y los reportes e indicadores
    // de meses pasados cambiarian. ademas el reporte de registros eliminados necesita q la fila siga ahi
    @Transactional
    public void darDeBaja(Integer idLibro) {
        Libro libro = buscar(idLibro);
        if (BAJA.equals(libro.getEstadoLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro ya está dado de baja");
        }
        // no se puede dar de baja si alguien lo tiene prestado
        if (prestamoDao.existsByLibroIdLibroAndEstadoPrestamoIn(idLibro, List.of("activo", "vencido"))) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro tiene préstamos sin devolver");
        }
        // ni si alguien lo tiene reservado (RF14: "con un préstamo o reserva activa")
        if (reservaDao.existsByLibroIdLibroAndEstadoReserva(idLibro, "pendiente")) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro tiene reservas pendientes");
        }
        // ni si hay prestatarios en su cola de espera (RF04)
        if (reservaDao.existsByLibroIdLibroAndEstadoReserva(idLibro, ColaEsperaService.EN_ESPERA)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro tiene prestatarios en la cola de espera");
        }
        libro.setEstadoLibro(BAJA); // eliminacion logica: aqui cambia el estado
        libroDao.save(libro);
    }

    // ---------- STOCK (lo usan préstamos y reservas) ----------

    /**
     * Lee el libro con bloqueo y revisa que se pueda prestar o reservar.
     * El bloqueo evita que dos operaciones a la vez se lleven el último ejemplar.
     */
    @Transactional
    public Libro bloquearParaSalida(Integer idLibro) {
        Libro libro = libroDao.buscarParaActualizar(idLibro)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
        if (BAJA.equals(libro.getEstadoLibro()) || "mantenimiento".equals(libro.getEstadoLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro no está disponible");
        }
        return libro;
    }

    /** Saca un ejemplar: baja el stock; si llega a 0, el libro pasa a 'prestado' o 'reservado'. */
    // stock automatico (objetivo especifico 3): al prestar o reservar el stock baja 1 solo,
    // y si llega a 0 el estado del libro cambia solo a 'prestado' o 'reservado'
    @Transactional
    public void descontarStock(Libro libro, String estadoSinStock) {
        if (libro.getStock() <= 0) {
            throw new NegocioException(HttpStatus.CONFLICT, "No hay ejemplares disponibles de este libro");
        }
        libro.setStock(libro.getStock() - 1);
        if (libro.getStock() == 0) {
            libro.setEstadoLibro(estadoSinStock);
        }
        libroDao.save(libro);
    }

    /** Devuelve un ejemplar: sube el stock; si estaba sin ejemplares, vuelve a 'disponible'. */
    // stock automatico: al devolver (en buen estado), anular un prestamo o cancelar una reserva el stock sube 1
    // y el libro vuelve a 'disponible'
    @Transactional
    public void reponerStock(Integer idLibro) {
        Libro libro = libroDao.buscarParaActualizar(idLibro)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));

        // cola de espera: si alguien espera este libro, el ejemplar es para el. el stock no sube
        // y el libro queda 'reservado' (no hay ejemplares libres para otros)
        if (colaEspera.asignarAlSiguiente(idLibro)) {
            if (libro.getStock() == 0) {
                libro.setEstadoLibro("reservado");
            }
            libroDao.save(libro);
            return;
        }

        libro.setStock(libro.getStock() + 1);
        if (estaSinEjemplares(libro)) {
            libro.setEstadoLibro("disponible");
        }
        libroDao.save(libro);
    }

    /** RF02: un ejemplar devuelto dañado no vuelve al stock; si era el último, el libro queda en 'mantenimiento'. */
    // mantenimiento: el ejemplar dañado se va a reparar, por eso NO se suma al stock.
    // el sistema guarda titulos con stock (no cada ejemplar), asi q si ya no queda ninguno el libro pasa a
    // 'mantenimiento'. cuando lo reparen, el admin le sube el stock y vuelve a 'disponible'
    @Transactional
    public void registrarEjemplarDanado(Integer idLibro) {
        Libro libro = libroDao.buscarParaActualizar(idLibro)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
        if (libro.getStock() == 0) {
            libro.setEstadoLibro("mantenimiento");
        }
        libroDao.save(libro);
    }

    /** Estados que significan "no hay ejemplares para prestar": salen de ahí cuando vuelve a haber stock. */
    private boolean estaSinEjemplares(Libro libro) {
        String estado = libro.getEstadoLibro();
        return "prestado".equals(estado) || "reservado".equals(estado) || "mantenimiento".equals(estado);
    }

    // ---------- Auxiliares ----------

    private Libro buscar(Integer idLibro) {
        return libroDao.findById(idLibro)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
    }

    private void copiarDatos(LibroFormDTO form, Libro libro) {
        Categoria categoria = categoriaDao.findById(form.idCategoria())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        libro.setTitulo(form.titulo().trim());
        libro.setIsbn(form.isbn().trim());
        libro.setAnioPublicacion(form.anioPublicacion());
        libro.setEditorial(form.editorial().trim());
        libro.setStock(form.stock());
        libro.setCategoria(categoria);

        if (form.idsAutores() != null) {
            List<Autor> autores = autorDao.findAllById(form.idsAutores());
            libro.setAutores(new HashSet<>(autores));
        }
    }

    private List<Libro> sinBajas(List<Libro> libros) {
        return libros.stream().filter(l -> !BAJA.equals(l.getEstadoLibro())).toList();
    }

    private List<LibroDTO> aDTO(List<Libro> libros) {
        return libros.stream().map(LibroDTO::desde).toList();
    }
}
