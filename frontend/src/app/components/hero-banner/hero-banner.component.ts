import { Component, computed, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface BannerSlide {
  id: number;
  tag: string;
  title: string;
  subtitle: string;
  image: string;
}

@Component({
  selector: 'app-hero-banner',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './hero-banner.component.html',
  styleUrl: './hero-banner.component.less',
})
export class HeroBannerComponent {
  readonly locationClick = output<void>();

  readonly slides: BannerSlide[] = [
    {
      id: 1,
      tag: 'Nuevos Ingresos',
      title: 'Biblioteca de Alejandría',
      subtitle: 'Acceso a novedades bibliográficas y préstamo en sala de lectura para la comunidad universitaria.',
      image: 'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=1200&q=80',
    },
    {
      id: 2,
      tag: 'Colección Destacada',
      title: 'Ciencia, Tecnología y Letras',
      subtitle: 'Reserva tus ejemplares en línea y recógelos en los turnos de mañana o tarde.',
      image: 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=1200&q=80',
    }
  ];

  readonly currentSlideIndex = signal<number>(0);
  readonly currentSlide = computed(() => this.slides[this.currentSlideIndex()]);

  nextSlide(): void {
    const next = (this.currentSlideIndex() + 1) % this.slides.length;
    this.currentSlideIndex.set(next);
  }

  prevSlide(): void {
    const prev = (this.currentSlideIndex() - 1 + this.slides.length) % this.slides.length;
    this.currentSlideIndex.set(prev);
  }

  goToSlide(index: number): void {
    this.currentSlideIndex.set(index);
  }

  onLocationClick(): void {
    this.locationClick.emit();
  }
}
