import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Autor, Categoria, LibroBackend, LibroForm, LibroService } from '../../../../services/libro.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { portadaPorIsbn, usarPortadaPorDefecto } from '../../../../utils/portada';

/** Pestaña "Libros" del admin: CRUD de libros. */
@Component({
  selector: 'app-libros-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './libros-tab.component.html',
})
export class LibrosTabComponent implements OnInit {
  private libroService = inject(LibroService);

  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');

  readonly libros = signal<LibroBackend[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly autores = signal<Autor[]>([]);

  /** null = creando un libro nuevo; número = editando ese libro. */
  readonly idEditando = signal<number | null>(null);
  readonly estadoEditando = signal<string | null>(null);
  form: LibroForm = this.formVacio();

  readonly portadaPorIsbn = portadaPorIsbn;
  readonly usarPortadaPorDefecto = usarPortadaPorDefecto;

  ngOnInit(): void {
    this.cargarLibros();
    this.libroService.getCategorias().subscribe(c => this.categorias.set(c));
    this.libroService.getAutores().subscribe(a => this.autores.set(a));
  }

  // crud libro consultar (front): llena la tabla de libros del admin
  cargarLibros(): void {
    this.libroService.getLibros().subscribe({
      next: data => this.libros.set(data),
      error: err => this.mostrarError(err)
    });
  }

  // crud libro adicionar / crud libro actualizar (front): si no esta editando crea, si esta editando actualiza
  guardar(): void {
    this.limpiarMensajes();
    const id = this.idEditando();
    const peticion = id === null
      ? this.libroService.crear(this.form)
      : this.libroService.actualizar(id, this.form);

    peticion.subscribe({
      next: libro => {
        this.mensajeOk.set(id === null ? `Libro "${libro.titulo}" registrado` : `Libro "${libro.titulo}" actualizado`);
        this.cancelarEdicion();
        this.cargarLibros();
      },
      error: err => this.mostrarError(err)
    });
  }

  editar(libro: LibroBackend): void {
    this.limpiarMensajes();
    this.idEditando.set(libro.idLibro);
    this.estadoEditando.set(libro.estadoLibro);
    this.form = {
      titulo: libro.titulo,
      isbn: libro.isbn,
      anioPublicacion: libro.anioPublicacion,
      editorial: libro.editorial,
      stock: libro.stock,
      idCategoria: libro.categoria?.idCategoria ?? null,
      idsAutores: (libro.autores ?? []).map(a => a.idAutor),
    };
  }

  cancelarEdicion(): void {
    this.idEditando.set(null);
    this.estadoEditando.set(null);
    this.form = this.formVacio();
  }

  // crud libro eliminar (front): boton dar de baja (eliminacion logica)
  darDeBaja(libro: LibroBackend): void {
    if (!confirm(`¿Dar de baja "${libro.titulo}"? Dejará de aparecer en el catálogo.`)) return;
    this.limpiarMensajes();
    this.libroService.darDeBaja(libro.idLibro).subscribe({
      next: res => { this.mensajeOk.set(res.mensaje); this.cargarLibros(); },
      error: err => this.mostrarError(err)
    });
  }

  nombreAutores(libro: LibroBackend): string {
    return (libro.autores ?? []).map(a => `${a.nombre} ${a.apellido}`).join(', ');
  }

  private formVacio(): LibroForm {
    return { titulo: '', isbn: '', anioPublicacion: null, editorial: '', stock: null, idCategoria: null, idsAutores: [] };
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }

  private mostrarError(err: HttpErrorResponse): void {
    this.mensajeError.set(mensajeDeError(err));
  }
}
