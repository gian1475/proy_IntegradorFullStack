import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../../environments/environment';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Fila de la consulta de dos tablas: libro JOIN categoria. */
interface LibroCategoria {
  idLibro: number;
  titulo: string;      // libro
  isbn: string;        // libro
  stock: number;       // libro
  estadoLibro: string; // libro
  categoria: string;   // categoria
}

/** Pestaña "Libros por categoría": CONSULTA DE DOS TABLAS (libro + categoria). */
@Component({
  selector: 'app-libros-categoria-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './libros-categoria-tab.component.html',
})
export class LibrosCategoriaTabComponent implements OnInit {
  private http = inject(HttpClient);

  readonly categorias = signal<{ idCategoria: number; nombre: string }[]>([]);
  readonly libros = signal<LibroCategoria[]>([]);
  readonly mensajeError = signal('');
  categoria = '';

  ngOnInit(): void {
    this.http.get<{ idCategoria: number; nombre: string }[]>(`${environment.apiUrl}/categorias`)
      .subscribe({ next: c => this.categorias.set(c), error: err => this.mensajeError.set(mensajeDeError(err)) });
    this.consultar();
  }

  // GET /api/libros-por-categoria?categoria=... -> SELECT ... FROM libro l JOIN categoria c ...
  consultar(): void {
    const params = new HttpParams().set('categoria', this.categoria);
    this.http.get<LibroCategoria[]>(`${environment.apiUrl}/libros-por-categoria`, { params }).subscribe({
      next: data => { this.libros.set(data); this.mensajeError.set(''); },
      error: err => this.mensajeError.set(mensajeDeError(err))
    });
  }
}
