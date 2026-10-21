import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { HeaderComponent } from '../../components/header/header.component';
import { AuthService } from '../../services/auth.service';
import { Autor, Categoria, LibroBackend, LibroForm, LibroService } from '../../services/libro.service';
import { PrestamoDetalle, PrestamoService } from '../../services/prestamo.service';

type Pestana = 'libros' | 'prestamos';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [HeaderComponent, FormsModule, DatePipe],
  templateUrl: './admin-panel.component.html',
  styleUrl: '../panel-base.less',
})
export class AdminPanelComponent implements OnInit {
  authService = inject(AuthService);
  private libroService = inject(LibroService);
  private prestamoService = inject(PrestamoService);

  readonly pestana = signal<Pestana>('libros');
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');

  readonly libros = signal<LibroBackend[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly autores = signal<Autor[]>([]);
  readonly prestamos = signal<PrestamoDetalle[]>([]);

  /** null = creando un libro nuevo; número = editando ese libro. */
  readonly idEditando = signal<number | null>(null);
  form: LibroForm = this.formVacio();

  ngOnInit(): void {
    this.cargarLibros();
    this.libroService.getCategorias().subscribe(c => this.categorias.set(c));
    this.libroService.getAutores().subscribe(a => this.autores.set(a));
  }

  cambiarPestana(p: Pestana): void {
    this.pestana.set(p);
    this.limpiarMensajes();
    if (p === 'libros') this.cargarLibros();
    if (p === 'prestamos') {
      this.prestamoService.listarActivos().subscribe({
        next: data => this.prestamos.set(data),
        error: err => this.mostrarError(err)
      });
    }
  }

  cargarLibros(): void {
    this.libroService.getLibros().subscribe({
      next: data => this.libros.set(data),
      error: err => this.mostrarError(err)
    });
  }

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
    this.form = this.formVacio();
  }

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
    this.mensajeError.set(err.status === 0
      ? 'No se pudo conectar con el backend (puerto 8080)'
      : err.error?.mensaje || 'Ocurrió un error inesperado');
  }
}
