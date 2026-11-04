import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LibroBackend, LibroService } from '../../../../services/libro.service';
import { AuditoriaMes, AuditoriaService } from '../../../../services/auditoria.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';
import { etiquetaEstado } from '../../../../utils/estado-libro';

/**
 * Pestaña "Auditoría" del admin (IPI, apartado 1.2.2): registrar el conteo físico de cada título y ver
 * el índice de precisión del inventario del mes (títulos que coinciden / títulos auditados).
 */
@Component({
  selector: 'app-auditoria-tab',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './auditoria-tab.component.html',
})
export class AuditoriaTabComponent implements OnInit {
  private libroService = inject(LibroService);
  private auditoriaService = inject(AuditoriaService);

  readonly libros = signal<LibroBackend[]>([]);
  readonly mes = signal<AuditoriaMes | null>(null);
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly etiquetaEstado = etiquetaEstado;

  /** Lo que el admin escribió en "Conteo físico" de cada libro (por idLibro). */
  conteos: Record<number, number | null> = {};

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.libroService.getLibros().subscribe({
      next: data => this.libros.set(data),
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
    this.auditoriaService.resumenDelMes().subscribe({
      next: data => this.mes.set(data),
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }

  /** Último conteo de este mes de un libro (la lista viene con la más reciente primero). */
  ultimoConteo(idLibro: number) {
    return this.mes()?.auditorias.find(a => a.idLibro === idLibro);
  }

  // auditoria inventario (front): manda el conteo; el backend guarda tambien el stock q tiene el sistema
  registrar(libro: LibroBackend): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
    const conteo = this.conteos[libro.idLibro];
    if (conteo === null || conteo === undefined || conteo < 0 || !Number.isInteger(conteo)) {
      this.mensajeError.set('Escriba cuántos ejemplares contó (0 o más)');
      return;
    }
    this.auditoriaService.registrar(libro.idLibro, conteo).subscribe({
      next: a => {
        this.mensajeOk.set(a.coincide
          ? `${a.tituloLibro}: coincide con el sistema (${a.conteoFisico}).`
          : `${a.tituloLibro}: NO coincide. Sistema ${a.stockSistema}, conteo físico ${a.conteoFisico}. Si el conteo es correcto, corrija el stock en la pestaña Libros.`);
        delete this.conteos[libro.idLibro];
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.mensajeError.set(mensajeDeError(err))
    });
  }
}
