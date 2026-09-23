import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LibroService, LibroBackend } from '../../services/libro.service';

export interface Book {
  id: number;
  title: string;
  author: string;
  year: number;
  editorial: string;
  category: string;
  stock: number;
  coverImage: string;
}

@Component({
  selector: 'app-books-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './books-section.component.html',
  styleUrl: './books-section.component.less',
})
export class BooksSectionComponent implements OnInit {
  private libroService = inject(LibroService);

  readonly searchTerm = input<string>('');
  readonly bookSelected = output<Book>();

  readonly books = signal<Book[]>([
    {
      id: 4,
      title: 'La Ciudad y los Perros',
      author: 'Mario Vargas Llosa',
      year: 1963,
      editorial: 'Seix Barral',
      category: 'Novela',
      stock: 2,
      coverImage: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=400&q=80',
    },
    {
      id: 101,
      title: 'Conversación en La Catedral',
      author: 'Mario Vargas Llosa',
      year: 1969,
      editorial: 'Seix Barral',
      category: 'Novela',
      stock: 3,
      coverImage: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=400&q=80',
    },
    {
      id: 102,
      title: 'La Fiesta del Chivo',
      author: 'Mario Vargas Llosa',
      year: 2000,
      editorial: 'Alfaguara',
      category: 'Novela',
      stock: 2,
      coverImage: 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&w=400&q=80',
    },
    {
      id: 103,
      title: 'La Tía Julia y el Escribidor',
      author: 'Mario Vargas Llosa',
      year: 1977,
      editorial: 'Seix Barral',
      category: 'Novela',
      stock: 1,
      coverImage: 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=400&q=80',
    }
  ]);

  ngOnInit(): void {
    this.libroService.getLibrosPorAutor('Mario').subscribe({
      next: (data: LibroBackend[]) => {
        if (data && data.length > 0) {
          const loaded: Book[] = data.map(item => ({
            id: item.idLibro,
            title: item.titulo,
            author: 'Mario Vargas Llosa',
            year: item.anioPublicacion,
            editorial: item.editorial,
            category: item.categoria ? item.categoria.nombre : 'Novela',
            stock: item.stock,
            coverImage: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=400&q=80'
          }));
          this.books.set(loaded);
        }
      },
      error: () => {}
    });
  }

  readonly filteredBooks = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.books();
    return this.books().filter(b => 
      b.title.toLowerCase().includes(term) ||
      b.author.toLowerCase().includes(term) ||
      b.category.toLowerCase().includes(term)
    );
  });

  selectBook(book: Book): void {
    this.bookSelected.emit(book);
  }

  scrollLeft(): void {}
  scrollRight(): void {}
}
