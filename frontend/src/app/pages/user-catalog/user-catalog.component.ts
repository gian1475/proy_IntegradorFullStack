import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HeaderComponent } from '../../components/header/header.component';
import { LibroService, LibroBackend } from '../../services/libro.service';
import { AuthService } from '../../services/auth.service';
import { ReservaBackend, ReservaService } from '../../services/reserva.service';

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
export class UserCatalogComponent implements OnInit {
  private libroService = inject(LibroService);
  private authService = inject(AuthService);
  private reservaService = inject(ReservaService);

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

  private defaultCovers: Record<string, string> = {
    'don quijote': 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=400&q=80',
    'cien años': 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=400&q=80',
    'crimen': 'https://images.unsplash.com/photo-1532012164546-f432f2e3777f?auto=format&fit=crop&w=400&q=80',
    'ciudad': 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=400&q=80',
    'clean': 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=400&q=80',
    'java': 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=400&q=80',
  };

  ngOnInit(): void {
    this.cargarLibros();
    this.cargarReservas();
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
            ? `${item.autores[0].nombre} ${item.autores[0].apellido}`
            : 'Autor desconocido',
          category: item.categoria ? item.categoria.nombre : '',
          coverImage: this.portada(item.titulo),
          stock: item.stock
        })));
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorCatalogo.set(this.mensajeDeError(err));
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
              coverImage: this.portada(r.tituloLibro),
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
      error: (err: HttpErrorResponse) => this.errorReserva.set(this.mensajeDeError(err))
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
        alert(this.mensajeDeError(err));
      }
    });
  }

  cerrarModalCanceladaExitosa(): void {
    this.modalCanceladaExitosa.set(false);
  }

  private portada(titulo: string): string {
    const lower = titulo.toLowerCase();
    const clave = Object.keys(this.defaultCovers).find(k => lower.includes(k));
    return clave ? this.defaultCovers[clave] : this.defaultCovers['don quijote'];
  }

  /** Fecha de hoy en formato AAAA-MM-DD (hora local, para el input type="date"). */
  private hoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private mensajeDeError(err: HttpErrorResponse): string {
    return err.status === 0
      ? 'No se pudo conectar con el backend (puerto 8080)'
      : err.error?.mensaje || 'Ocurrió un error inesperado';
  }
}
