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
    descripcion?: string;
  };
  autores?: Array<{
    idAutor: number;
    nombre: string;
    apellido: string;
    nacionalidad?: string;
  }>;
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
}
