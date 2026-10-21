package com.backend.demo.service;

import com.backend.demo.dao.AutorDao;
import com.backend.demo.dao.CategoriaDao;
import com.backend.demo.dao.LibroDao;
import com.backend.demo.dao.PrestamoDao;
import com.backend.demo.dto.LibroDTO;
import com.backend.demo.dto.LibroFormDTO;
import com.backend.demo.entity.Autor;
import com.backend.demo.entity.Categoria;
import com.backend.demo.entity.Libro;
import com.backend.demo.exception.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public LibroService(LibroDao libroDao, CategoriaDao categoriaDao, AutorDao autorDao, PrestamoDao prestamoDao) {
        this.libroDao = libroDao;
        this.categoriaDao = categoriaDao;
        this.autorDao = autorDao;
        this.prestamoDao = prestamoDao;
    }

    // ---------- CONSULTAR ----------

    @Transactional(readOnly = true)
    public List<LibroDTO> listar() {
        return aDTO(libroDao.findByEstadoLibroNotOrderByTitulo(BAJA));
    }

    @Transactional(readOnly = true)
    public List<LibroDTO> buscarPorTitulo(String texto) {
        if (texto == null || texto.isBlank()) {
            return listar();
        }
        return aDTO(sinBajas(libroDao.findByTituloContainingIgnoreCase(texto.trim())));
    }

    @Transactional(readOnly = true)
    public List<LibroDTO> buscarPorAutor(String nombre) {
        return aDTO(sinBajas(libroDao.findByAutorNombreOrApellido(nombre, nombre)));
    }

    @Transactional(readOnly = true)
    public LibroDTO obtener(Integer idLibro) {
        return LibroDTO.desde(buscar(idLibro));
    }

    // ---------- ADICIONAR ----------

    @Transactional
    public LibroDTO crear(LibroFormDTO form) {
        validar(form);
        if (libroDao.existsByIsbn(form.getIsbn().trim())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe un libro con ese ISBN");
        }

        Libro libro = new Libro();
        copiarDatos(form, libro);
        libro.setEstadoLibro(form.getStock() > 0 ? "disponible" : "prestado");
        return LibroDTO.desde(libroDao.save(libro));
    }

    // ---------- ACTUALIZAR ----------

    @Transactional
    public LibroDTO actualizar(Integer idLibro, LibroFormDTO form) {
        validar(form);
        Libro libro = buscar(idLibro);
        if (BAJA.equals(libro.getEstadoLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "No se puede editar un libro dado de baja");
        }
        if (libroDao.existsByIsbnAndIdLibroNot(form.getIsbn().trim(), idLibro)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe otro libro con ese ISBN");
        }

        copiarDatos(form, libro);

        // El estado sigue al stock: sin ejemplares -> 'prestado'; con ejemplares -> 'disponible'
        if (libro.getStock() == 0 && "disponible".equals(libro.getEstadoLibro())) {
            libro.setEstadoLibro("prestado");
        } else if (libro.getStock() > 0 && "prestado".equals(libro.getEstadoLibro())) {
            libro.setEstadoLibro("disponible");
        }
        return LibroDTO.desde(libroDao.save(libro));
    }

    // ---------- ELIMINAR (lógico) ----------

    @Transactional
    public void darDeBaja(Integer idLibro) {
        Libro libro = buscar(idLibro);
        if (BAJA.equals(libro.getEstadoLibro())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro ya está dado de baja");
        }
        if (prestamoDao.existsByLibroIdLibroAndEstadoPrestamoIn(idLibro, List.of("activo", "vencido"))) {
            throw new NegocioException(HttpStatus.CONFLICT, "El libro tiene préstamos sin devolver");
        }
        libro.setEstadoLibro(BAJA);
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

    /** Devuelve un ejemplar: sube el stock; si estaba 'prestado' o 'reservado', vuelve a 'disponible'. */
    @Transactional
    public void reponerStock(Integer idLibro) {
        Libro libro = libroDao.buscarParaActualizar(idLibro)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
        libro.setStock(libro.getStock() + 1);
        if ("prestado".equals(libro.getEstadoLibro()) || "reservado".equals(libro.getEstadoLibro())) {
            libro.setEstadoLibro("disponible");
        }
        libroDao.save(libro);
    }

    // ---------- Auxiliares ----------

    private Libro buscar(Integer idLibro) {
        return libroDao.findById(idLibro)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
    }

    private void validar(LibroFormDTO form) {
        if (form.getTitulo() == null || form.getTitulo().isBlank()
                || form.getIsbn() == null || form.getIsbn().isBlank()
                || form.getEditorial() == null || form.getEditorial().isBlank()
                || form.getAnioPublicacion() == null || form.getStock() == null || form.getIdCategoria() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Complete todos los campos obligatorios del libro");
        }
        if (form.getStock() < 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El stock no puede ser negativo");
        }
        if (form.getAnioPublicacion() < 1000 || form.getAnioPublicacion() > 2100) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El año de publicación no es válido");
        }
    }

    private void copiarDatos(LibroFormDTO form, Libro libro) {
        Categoria categoria = categoriaDao.findById(form.getIdCategoria())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        libro.setTitulo(form.getTitulo().trim());
        libro.setIsbn(form.getIsbn().trim());
        libro.setAnioPublicacion(form.getAnioPublicacion());
        libro.setEditorial(form.getEditorial().trim());
        libro.setStock(form.getStock());
        libro.setCategoria(categoria);

        if (form.getIdsAutores() != null) {
            List<Autor> autores = autorDao.findAllById(form.getIdsAutores());
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
