import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LibroService, LibroBackend } from '../../services/libro.service';
import { portadaPorIsbn, usarPortadaPorDefecto } from '../../utils/portada';

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

  // datos reales: los libros salen de la bd. antes habia 4 libros escritos a mano (3 no existian en la bd)
  readonly books = signal<Book[]>([]);
  readonly usarPortadaPorDefecto = usarPortadaPorDefecto;
  readonly cargando = signal(true);
  readonly error = signal(false);

  ngOnInit(): void {
    this.libroService.getLibrosPorAutor('Mario').subscribe({
      next: (data: LibroBackend[]) => {
        this.books.set(data.map(item => ({
          id: item.idLibro,
          title: item.titulo,
          author: (item.autores ?? []).map(a => `${a.nombre} ${a.apellido}`).join(', '),
          year: item.anioPublicacion,
          editorial: item.editorial,
          category: item.categoria?.nombre ?? '',
          stock: item.stock,
          coverImage: portadaPorIsbn(item.isbn)
        })));
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      }
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
