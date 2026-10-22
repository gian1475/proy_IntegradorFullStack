import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormatoReporte, ReporteService, ResultadoImportacion } from '../../../../services/reporte.service';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Pestaña "Reportes" del admin: Excel (Apache POI), PDF (OpenPDF) y CSV (Commons CSV). */
@Component({
  selector: 'app-reportes-tab',
  standalone: true,
  templateUrl: './reportes-tab.component.html',
})
export class ReportesTabComponent {
  private reporteService = inject(ReporteService);

  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');
  readonly descargando = signal(false);
  readonly resultadoImportacion = signal<ResultadoImportacion | null>(null);
  archivoCsv: File | null = null;

  // reporte prestamos (front): descarga el excel, pdf o csv
  descargar(formato: FormatoReporte): void {
    this.limpiarMensajes();
    this.descargando.set(true);
    this.reporteService.descargarPrestamos(formato).subscribe({
      next: () => {
        this.descargando.set(false);
        this.mensajeOk.set(`Reporte ${formato.toUpperCase()} descargado`);
      },
      error: err => { this.descargando.set(false); this.mostrarError(err); }
    });
  }

  seleccionarArchivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.archivoCsv = input.files?.[0] ?? null;
    this.resultadoImportacion.set(null);
  }

  // commons csv importar (front): manda el archivo al backend y muestra cuantos se crearon
  importarCsv(): void {
    if (!this.archivoCsv) {
      this.mensajeError.set('Seleccione un archivo CSV');
      return;
    }
    this.limpiarMensajes();
    this.reporteService.importarLibros(this.archivoCsv).subscribe({
      next: resultado => this.resultadoImportacion.set(resultado),
      error: err => this.mostrarError(err)
    });
  }

  private limpiarMensajes(): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
  }

  private mostrarError(err: HttpErrorResponse): void {
    this.mensajeError.set(mensajeDeError(err));
  }
}
