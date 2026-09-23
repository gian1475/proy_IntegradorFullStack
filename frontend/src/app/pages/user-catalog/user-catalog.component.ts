import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../../components/header/header.component';
import { LibroService, LibroBackend } from '../../services/libro.service';
import { AuthService } from '../../services/auth.service';
import { ReservaService } from '../../services/reserva.service';

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

  fechaSeleccionada: string = '2026-09-24';
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
  readonly misReservas = signal<ReservaActiva[]>([
    {
      idReserva: 1,
      idLibro: 4,
      titulo: 'La Ciudad y los Perros',
      autor: 'Mario Vargas Llosa',
      categoria: 'Arte y Cultura',
      coverImage: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=400&q=80',
      fechaRecojo: '24/09/2026',
      horario: '14:30 - 17:30'
    }
  ]);

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
  }

  cargarLibros(): void {
    this.cargando.set(true);
    this.libroService.getLibros().subscribe({
      next: (data: LibroBackend[]) => {
        if (data && data.length > 0) {
          const mapeados = data.map(item => {
            const autorStr = item.autores && item.autores.length > 0 
              ? `${item.autores[0].nombre} ${item.autores[0].apellido}`
              : 'Autor';
            
            const catStr = item.categoria ? item.categoria.nombre : 'Arte y Cultura';
            
            let cover = 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=400&q=80';
            const lower = item.titulo.toLowerCase();
            for (const key of Object.keys(this.defaultCovers)) {
              if (lower.includes(key)) {
                cover = this.defaultCovers[key];
                break;
              }
            }

            return {
              id: item.idLibro,
              title: item.titulo,
              author: autorStr,
              category: catStr,
              coverImage: cover,
              stock: item.stock
            };
          });
          this.books.set(mapeados);
        } else {
          this.cargarFallback();
        }
        this.cargando.set(false);
      },
      error: () => {
        this.cargarFallback();
        this.cargando.set(false);
      }
    });
  }

  private cargarFallback(): void {
    this.books.set([
      {
        id: 1,
        title: 'Clean Code',
        author: 'Robert Martin',
        category: 'Tecnología',
        coverImage: 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=400&q=80',
        stock: 3
      },
      {
        id: 2,
        title: 'Core Java Volume I',
        author: 'Cay Horstmann',
        category: 'Tecnología',
        coverImage: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=400&q=80',
        stock: 2
      },
      {
        id: 3,
        title: 'Cien Años de Soledad',
        author: 'Gabriel García Márquez',
        category: 'Arte y Cultura',
        coverImage: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=400&q=80',
        stock: 1
      },
      {
        id: 4,
        title: 'La Ciudad y los Perros',
        author: 'Mario Vargas Llosa',
        category: 'Arte y Cultura',
        coverImage: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=400&q=80',
        stock: 2
      },
      {
        id: 5,
        title: 'Don Quijote de la Mancha',
        author: 'Miguel de Cervantes',
        category: 'Arte y Cultura',
        coverImage: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=400&q=80',
        stock: 3
      }
    ]);
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
    this.modalConfiguracion.set(book);
  }

  cerrarModalConfiguracion(): void {
    this.modalConfiguracion.set(null);
  }

  confirmarReserva(book: CatalogBook): void {
    const usuario = this.authService.usuarioActual();
    const idUsuario = usuario ? usuario.idUsuario : 3;

    this.reservaService.crearReserva(book.id, idUsuario).subscribe({
      next: (res) => {
        this.agregarReservaLocal(book, res.idReserva || Date.now());
      },
      error: () => {
        this.agregarReservaLocal(book, Date.now());
      }
    });

    this.modalConfiguracion.set(null);
    this.modalReservaRealizada.set(true);
  }

  private agregarReservaLocal(book: CatalogBook, idReserva: number): void {
    const nueva: ReservaActiva = {
      idReserva: idReserva,
      idLibro: book.id,
      titulo: book.title,
      autor: book.author,
      categoria: book.category,
      coverImage: book.coverImage,
      fechaRecojo: this.fechaSeleccionada,
      horario: this.horarioSeleccionado
    };
    this.misReservas.update(lista => [nueva, ...lista]);
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
    this.misReservas.update(lista => lista.filter(r => r.idReserva !== item.idReserva));
    this.modalCancelar.set(null);
    this.modalCanceladaExitosa.set(true);
  }

  cerrarModalCanceladaExitosa(): void {
    this.modalCanceladaExitosa.set(false);
  }
}
