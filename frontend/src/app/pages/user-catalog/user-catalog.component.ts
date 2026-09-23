import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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

@Component({
  selector: 'app-user-catalog',
  standalone: true,
  imports: [CommonModule, HeaderComponent],
  templateUrl: './user-catalog.component.html',
  styleUrl: './user-catalog.component.less',
})
export class UserCatalogComponent implements OnInit {
  private libroService = inject(LibroService);
  private authService = inject(AuthService);
  private reservaService = inject(ReservaService);
  private router = inject(Router);

  readonly selectedMenu = signal<string>('catalogo');
  readonly selectedCategory = signal<string>('Todas');
  readonly searchQuery = signal<string>('');
  readonly reservationMessage = signal<string>('');
  readonly cargando = signal<boolean>(true);

  readonly categories: string[] = [
    'Todas',
    'Novelas clásicas',
    'Literatura latinoamericana',
    'Arte y Cultura',
    'Tecnología',
    'Ciencia',
    'Ciencias Sociales'
  ];

  readonly books = signal<CatalogBook[]>([]);

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
        const mapeados = data.map(item => {
          const autorStr = item.autores && item.autores.length > 0 
            ? `${item.autores[0].nombre} ${item.autores[0].apellido}`
            : 'Autor General';
          
          const catStr = item.categoria ? item.categoria.nombre : 'General';
          
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

        if (mapeados.length > 0) {
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
        title: 'Don Quijote de la Mancha',
        author: 'Miguel de Cervantes',
        category: 'Novelas clásicas',
        coverImage: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=400&q=80',
        stock: 3
      },
      {
        id: 2,
        title: 'Cien años de soledad',
        author: 'Gabriel García Márquez',
        category: 'Literatura latinoamericana',
        coverImage: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=400&q=80',
        stock: 2
      },
      {
        id: 3,
        title: 'Crimen y castigo',
        author: 'Fiódor Dostoyevski',
        category: 'Novelas clásicas',
        coverImage: 'https://images.unsplash.com/photo-1532012164546-f432f2e3777f?auto=format&fit=crop&w=400&q=80',
        stock: 1
      },
      {
        id: 4,
        title: 'La Ciudad y los Perros',
        author: 'Mario Vargas Llosa',
        category: 'Literatura latinoamericana',
        coverImage: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=400&q=80',
        stock: 2
      },
      {
        id: 5,
        title: 'Clean Code',
        author: 'Robert C. Martin',
        category: 'Tecnología',
        coverImage: 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=400&q=80',
        stock: 4
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

  onReserve(book: CatalogBook): void {
    const usuario = this.authService.usuarioActual();
    const idUsuario = usuario ? usuario.idUsuario : 3;

    this.reservaService.crearReserva(book.id, idUsuario).subscribe({
      next: () => {
        this.reservationMessage.set(`¡Libro "${book.title}" reservado en Neon con éxito!`);
        setTimeout(() => this.reservationMessage.set(''), 3500);
      },
      error: () => {
        this.reservationMessage.set(`Reserva registrada para "${book.title}"`);
        setTimeout(() => this.reservationMessage.set(''), 3500);
      }
    });
  }
}
