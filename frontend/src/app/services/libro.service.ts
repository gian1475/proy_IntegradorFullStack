import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LibroBackend {
  idLibro: number;
  titulo: string;
  isbn: string;
  anioPublicacion: number;
  editorial: string;
  stock: number;
  estadoLibro: string;
  categoria?: {
    idCategoria: number;
    nombre: string;
  };
  autores?: Array<{
    idAutor: number;
    nombre: string;
    apellido: string;
  }>;
}

/** Datos del formulario de libros (crear / actualizar). */
export interface LibroForm {
  titulo: string;
  isbn: string;
  anioPublicacion: number | null;
  editorial: string;
  stock: number | null;
  idCategoria: number | null;
  idsAutores: number[];
}

export interface Categoria {
  idCategoria: number;
  nombre: string;
}

export interface Autor {
  idAutor: number;
  nombre: string;
  apellido: string;
}

@Injectable({
  providedIn: 'root'
})
export class LibroService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/libros';

  getLibros(): Observable<LibroBackend[]> {
    return this.http.get<LibroBackend[]>(this.apiUrl);
  }

  getLibrosPorAutor(nombre: string): Observable<LibroBackend[]> {
    return this.http.get<LibroBackend[]>(`${this.apiUrl}/autor/${nombre}`);
  }

  buscar(query: string): Observable<LibroBackend[]> {
    return this.http.get<LibroBackend[]>(`${this.apiUrl}/buscar?q=${encodeURIComponent(query)}`);
  }

  crear(form: LibroForm): Observable<LibroBackend> {
    return this.http.post<LibroBackend>(this.apiUrl, form);
  }

  actualizar(idLibro: number, form: LibroForm): Observable<LibroBackend> {
    return this.http.put<LibroBackend>(`${this.apiUrl}/${idLibro}`, form);
  }

  darDeBaja(idLibro: number): Observable<{ mensaje: string }> {
    return this.http.delete<{ mensaje: string }>(`${this.apiUrl}/${idLibro}`);
  }

  getCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>('http://localhost:8080/api/categorias');
  }

  getAutores(): Observable<Autor[]> {
    return this.http.get<Autor[]>('http://localhost:8080/api/autores');
  }
}
