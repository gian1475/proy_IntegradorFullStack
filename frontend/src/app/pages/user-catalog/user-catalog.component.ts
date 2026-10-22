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

export interface CatalogBook {
  id: number;
  title: string;
  author: string;
  category: string;
  coverImage: string;
  stock: number;
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

  /** Para usarla en el (error) de las imágenes del template. */
  readonly usarPortadaPorDefecto = usarPortadaPorDefecto;

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
          category: item.categoria ? item.categoria.nombre : '',
          coverImage: portadaPorIsbn(item.isbn),
          stock: item.stock
        })));
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorCatalogo.set(mensajeDeError(err));
        this.cargando.set(false);
      }
    });
  }

  /** Mis reservas: se piden al backend (solo las pendientes de recojo). */
  cargarReservas(): void {
    const usuario = this.authService.usuarioActual();
    if (!usuario) return;

    this.reservaService.getReservasPorUsuario(usuario.idUsuario).subscribe({
      next: (data: ReservaBackend[]) => {
        this.misReservas.set(data
          .filter(r => r.estadoReserva === 'pendiente')
          .map(r => {
            const limite = new Date(r.fechaRecojoLimite);
            return {
              idReserva: r.idReserva,
              idLibro: r.idLibro,
              titulo: r.tituloLibro,
              autor: r.autor,
              categoria: r.categoria,
              coverImage: portadaPorIsbn(r.isbnLibro),
              fechaRecojo: limite.toLocaleDateString('es-PE'),
              horario: 'hasta las ' + limite.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
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
      const matchQuery = !term ||
        b.title.toLowerCase().includes(term) ||
        b.author.toLowerCase().includes(term);
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

  // reserva crear (front): el prestatario elige fecha y turno de recojo
  confirmarReserva(book: CatalogBook): void {
    const usuario = this.authService.usuarioActual();
    if (!usuario) return;

    // El límite de recojo es el final del turno elegido. Ej: "08:30 - 11:30" -> 11:30
    const horaFin = this.horarioSeleccionado.split(' - ')[1];
    const fechaRecojoLimite = `${this.fechaSeleccionada}T${horaFin}:00`;

    this.errorReserva.set('');
    this.reservaService.crearReserva(book.id, usuario.idUsuario, fechaRecojoLimite).subscribe({
      next: () => {
        this.modalConfiguracion.set(null);
        this.modalReservaRealizada.set(true);
        this.cargarReservas();
        this.cargarLibros();
      },
      error: (err: HttpErrorResponse) => this.errorReserva.set(mensajeDeError(err))
    });
  }

  cerrarModalReservaRealizada(): void {
    this.modalReservaRealizada.set(false);
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
