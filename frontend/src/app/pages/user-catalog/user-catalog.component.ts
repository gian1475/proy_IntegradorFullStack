import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HeaderComponent } from '../../components/header/header.component';
import { LibroService, LibroBackend } from '../../services/libro.service';
import { AuthService } from '../../services/auth.service';
import { ReservaBackend, ReservaService } from '../../services/reserva.service';
import { Prestamo, PrestamoService } from '../../services/prestamo.service';
import { mensajeDeError } from '../../utils/mensaje-error';
import { portadaPorIsbn, usarPortadaPorDefecto } from '../../utils/portada';
import { coincideBusqueda, etiquetaEstado } from '../../utils/estado-libro';

export interface CatalogBook {
  id: number;
  title: string;
  author: string;
  /** Todos los autores juntos (para buscar). */
  allAuthors: string;
  category: string;
  isbn: string;
  coverImage: string;
  stock: number;
  /** Estado agregado del título (RF03): disponible, prestado, reservado o mantenimiento. */
  estado: string;
}

export interface ReservaActiva {
  idReserva: number;
  idLibro: number;
  titulo: string;
  autor: string;
  categoria: string;
  coverImage: string;
  fechaRecojo: string;
  horario: string;
  /** RF04: está en la cola de espera (todavía no tiene ejemplar ni fecha de recojo). */
  enCola: boolean;
  posicion: number;
}

@Component({
  selector: 'app-user-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent],
  templateUrl: './user-catalog.component.html',
  styleUrl: './user-catalog.component.less',
})
// permisos prestatario: lo q ve el prestatario -> el catalogo, sus reservas (reservar y cancelar) y sus prestamos
export class UserCatalogComponent implements OnInit {
  private libroService = inject(LibroService);
  private authService = inject(AuthService);
  private reservaService = inject(ReservaService);
  private prestamoService = inject(PrestamoService);

  readonly selectedMenu = signal<string>('catalogo');
  readonly selectedCategory = signal<string>('Todas');
  readonly searchQuery = signal<string>('');
  readonly cargando = signal<boolean>(true);

  readonly modalConfiguracion = signal<CatalogBook | null>(null);
  readonly modalReservaRealizada = signal<boolean>(false);
  readonly modalCancelar = signal<ReservaActiva | null>(null);
  readonly modalCanceladaExitosa = signal<boolean>(false);
  /** Puesto en la cola cuando la reserva entró a la cola de espera (RF04). */
  readonly modalEnCola = signal<number | null>(null);

  fechaSeleccionada: string = this.hoy();
  horarioSeleccionado: string = '08:30 - 11:30';

  readonly categories: string[] = [
    'Todas',
    'Ciencias Sociales',
    'Ciencia',
    'Arte y Cultura',
    'Tecnología',
    'Idioma'
  ];

  readonly books = signal<CatalogBook[]>([]);
  readonly misReservas = signal<ReservaActiva[]>([]);
  readonly errorReserva = signal<string>('');
  readonly errorCatalogo = signal<string>('');

  // Mis préstamos (consulta de una tabla)
  readonly misPrestamos = signal<Prestamo[]>([]);
  readonly errorPrestamos = signal<string>('');
  readonly prestamosEnCurso = computed(() =>
    this.misPrestamos().filter(p => p.estadoPrestamo === 'activo' || p.estadoPrestamo === 'vencido').length
  );

  // historial (front, RF07): el prestatario filtra su propio historial por libro y por rango de fechas.
  // se filtra en pantalla para q la consulta siga siendo de una sola tabla
  readonly filtroLibro = signal('');
  readonly filtroDesde = signal('');
  readonly filtroHasta = signal('');
  readonly prestamosFiltrados = computed(() => {
    const libro = this.filtroLibro().trim().toLowerCase();
    const desde = this.filtroDesde();
    const hasta = this.filtroHasta();
    return this.misPrestamos().filter(p => {
      const dia = p.fechaPrestamo.slice(0, 10); // AAAA-MM-DD, se compara como texto
      return (!libro || this.tituloDe(p.idLibro).toLowerCase().includes(libro))
        && (!desde || dia >= desde)
        && (!hasta || dia <= hasta);
    });
  });

  /** Para usarla en el (error) de las imágenes del template. */
  readonly usarPortadaPorDefecto = usarPortadaPorDefecto;
  readonly etiquetaEstado = etiquetaEstado;

  ngOnInit(): void {
    this.cargarLibros();
    this.cargarReservas();
    this.cargarPrestamos();
  }

  // consulta una tabla (front): "Mis préstamos" pide GET /api/prestamos/prestatario/{id}, que solo lee la
  // tabla prestamo. de esa tabla se muestra una parte: inicio, hora limite, devolucion, ampliado, estado fisico
  // y estado. no se muestran id_prestatario (es el mismo usuario) ni id_encargado.
  // el titulo no viene en la consulta: se busca por id_libro en el catalogo q ya esta cargado en pantalla
  cargarPrestamos(): void {
    const usuario = this.authService.usuarioActual();
    if (!usuario) return;

    this.errorPrestamos.set('');
    this.prestamoService.listarPorPrestatario(usuario.idUsuario).subscribe({
      // eliminacion logica: los prestamos anulados no se le muestran al prestatario
      next: data => this.misPrestamos.set(data.filter(p => p.estadoPrestamo !== 'anulado')),
      error: (err: HttpErrorResponse) => this.errorPrestamos.set(mensajeDeError(err))
    });
  }

  tituloDe(idLibro: number): string {
    return this.books().find(b => b.id === idLibro)?.title ?? `Libro #${idLibro}`;
  }

  cargarLibros(): void {
    this.cargando.set(true);
    this.errorCatalogo.set('');
    this.libroService.getLibros().subscribe({
      next: (data: LibroBackend[]) => {
        this.books.set(data.map(item => ({
          id: item.idLibro,
          title: item.titulo,
          author: item.autores && item.autores.length > 0
            ? `${item.autores[0].nombre} ${item.autores[0].apellido}` + (item.autores.length > 1 ? ' y otros' : '')
            : 'Autor desconocido',
          allAuthors: (item.autores ?? []).map(a => `${a.nombre} ${a.apellido}`).join(', '),
          category: item.categoria ? item.categoria.nombre : '',
          isbn: item.isbn,
          coverImage: portadaPorIsbn(item.isbn),
          stock: item.stock,
          estado: item.estadoLibro
        })));
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorCatalogo.set(mensajeDeError(err));
        this.cargando.set(false);
      }
    });
  }

  /** Mis reservas: se piden al backend (las pendientes de recojo y las que están en la cola de espera). */
  cargarReservas(): void {
    const usuario = this.authService.usuarioActual();
    if (!usuario) return;

    this.reservaService.getReservasPorUsuario(usuario.idUsuario).subscribe({
      next: (data: ReservaBackend[]) => {
        this.misReservas.set(data
          .filter(r => r.estadoReserva === 'pendiente' || r.estadoReserva === 'en_espera')
          .map(r => {
            const limite = r.fechaRecojoLimite ? new Date(r.fechaRecojoLimite) : null;
            return {
              idReserva: r.idReserva,
              idLibro: r.idLibro,
              titulo: r.tituloLibro,
              autor: r.autor,
              categoria: r.categoria,
              coverImage: portadaPorIsbn(r.isbnLibro),
              fechaRecojo: limite ? limite.toLocaleDateString('es-PE') : '',
              horario: limite ? 'hasta las ' + limite.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '',
              enCola: r.estadoReserva === 'en_espera',
              posicion: r.posicionCola
            };
          }));
      },
      error: () => this.misReservas.set([])
    });
  }

  readonly filteredBooks = computed(() => {
    const term = this.searchQuery().trim().toLowerCase();
    const cat = this.selectedCategory();

    return this.books().filter(b => {
      const matchCat = cat === 'Todas' || b.category.toLowerCase() === cat.toLowerCase();
      // consulta disponibilidad (RF03): titulo, autor, categoria o codigo isbn
      const matchQuery = coincideBusqueda(term, { titulo: b.title, autores: b.allAuthors, categoria: b.category, isbn: b.isbn });
      return matchCat && matchQuery;
    });
  });

  selectMenu(menu: string): void {
    this.selectedMenu.set(menu);
    if (menu === 'prestamos') this.cargarPrestamos(); // al entrar se vuelve a consultar
  }

  selectCategory(cat: string): void {
    this.selectedCategory.set(cat);
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
  }

  abrirModalConfiguracion(book: CatalogBook): void {
    this.errorReserva.set('');
    this.modalConfiguracion.set(book);
  }

  cerrarModalConfiguracion(): void {
    this.modalConfiguracion.set(null);
  }

  // reserva crear (front): el prestatario elige fecha y turno de recojo.
  // cola de espera (RF04): si no hay stock no se elige fecha, se manda null y el backend lo pone en la cola
  confirmarReserva(book: CatalogBook): void {
    const usuario = this.authService.usuarioActual();
    if (!usuario) return;

    // El límite de recojo es el final del turno elegido. Ej: "08:30 - 11:30" -> 11:30
    const horaFin = this.horarioSeleccionado.split(' - ')[1];
    const fechaRecojoLimite = book.stock === 0 ? null : `${this.fechaSeleccionada}T${horaFin}:00`;

    this.errorReserva.set('');
    this.reservaService.crearReserva(book.id, usuario.idUsuario, fechaRecojoLimite).subscribe({
      next: reserva => {
        this.modalConfiguracion.set(null);
        if (reserva.estadoReserva === 'en_espera') {
          this.modalEnCola.set(reserva.posicionCola);
        } else {
          this.modalReservaRealizada.set(true);
        }
        this.cargarReservas();
        this.cargarLibros();
      },
      error: (err: HttpErrorResponse) => this.errorReserva.set(mensajeDeError(err))
    });
  }

  cerrarModalReservaRealizada(): void {
    this.modalReservaRealizada.set(false);
  }

  cerrarModalEnCola(): void {
    this.modalEnCola.set(null);
  }

  limpiarFiltrosPrestamos(): void {
    this.filtroLibro.set('');
    this.filtroDesde.set('');
    this.filtroHasta.set('');
  }

  abrirModalCancelar(item: ReservaActiva): void {
    this.modalCancelar.set(item);
  }

  cerrarModalCancelar(): void {
    this.modalCancelar.set(null);
  }

  // reserva cancelar (front)
  ejecutarCancelacion(item: ReservaActiva): void {
    this.reservaService.cancelar(item.idReserva).subscribe({
      next: () => {
        this.modalCancelar.set(null);
        this.modalCanceladaExitosa.set(true);
        this.cargarReservas();
        this.cargarLibros();
      },
      error: (err: HttpErrorResponse) => {
        this.modalCancelar.set(null);
        alert(mensajeDeError(err));
      }
    });
  }

  cerrarModalCanceladaExitosa(): void {
    this.modalCanceladaExitosa.set(false);
  }

  /** Fecha de hoy en formato AAAA-MM-DD (hora local, para el input type="date"). */
  private hoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
