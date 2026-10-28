import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { FormatoReporte, ReporteService, TIPOS_REPORTE, TipoReporte } from '../../services/reporte.service';
import { mensajeDeError } from '../../utils/mensaje-error';

/** Descarga de reportes (RF08) con rango de fechas. La usan los paneles del administrador y del encargado. */
@Component({
  selector: 'app-reportes-descarga',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './reportes-descarga.component.html',
})
export class ReportesDescargaComponent {
  private reporteService = inject(ReporteService);

  readonly tipos = TIPOS_REPORTE;
  tipo = 'prestamos';
  desde = this.primerDiaDelMes();
  hasta = this.hoy();

  readonly descargando = signal(false);
  readonly mensajeOk = signal('');
  readonly mensajeError = signal('');

  get tipoElegido(): TipoReporte {
    return this.tipos.find(t => t.valor === this.tipo) ?? this.tipos[0];
  }

  // reportes rf08 (front): descarga el reporte elegido en excel, pdf o csv con el rango de fechas
  descargar(formato: FormatoReporte): void {
    this.mensajeOk.set('');
    this.mensajeError.set('');
    if (this.desde && this.hasta && this.desde > this.hasta) {
      this.mensajeError.set("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
      return;
    }
    const usaFechas = this.tipoElegido.usaFechas;
    this.descargando.set(true);
    this.reporteService.descargar(this.tipo, formato, usaFechas ? this.desde : '', usaFechas ? this.hasta : '').subscribe({
      next: () => {
        this.descargando.set(false);
        this.mensajeOk.set(`Reporte de ${this.tipoElegido.nombre.toLowerCase()} descargado en ${formato.toUpperCase()}`);
      },
      error: (err: HttpErrorResponse) => {
        this.descargando.set(false);
        this.mensajeError.set(mensajeDeError(err));
      }
    });
  }

  /** Fechas en formato AAAA-MM-DD (hora local) para los input type="date". */
  private hoy(): string {
    return this.formatear(new Date());
  }

  private primerDiaDelMes(): string {
    const d = new Date();
    return this.formatear(new Date(d.getFullYear(), d.getMonth(), 1));
  }

  private formatear(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
