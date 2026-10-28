import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ReporteService, ResultadoImportacion } from '../../../../services/reporte.service';
import { ReportesDescargaComponent } from '../../../../components/reportes-descarga/reportes-descarga.component';
import { mensajeDeError } from '../../../../utils/mensaje-error';

/** Pestaña "Reportes" del admin: los reportes del RF08 (Excel, PDF y CSV) e importar libros desde CSV. */
@Component({
  selector: 'app-reportes-tab',
  standalone: true,
  imports: [ReportesDescargaComponent],
  templateUrl: './reportes-tab.component.html',
})
export class ReportesTabComponent {
  private reporteService = inject(ReporteService);

  readonly mensajeError = signal('');
  readonly resultadoImportacion = signal<ResultadoImportacion | null>(null);
  archivoCsv: File | null = null;

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
    this.mensajeError.set('');
  }

  private mostrarError(err: HttpErrorResponse): void {
    this.mensajeError.set(mensajeDeError(err));
  }
}
