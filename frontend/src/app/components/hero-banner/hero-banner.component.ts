import { Component, computed, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface BannerSlide {
  id: number;
  title: string;
  subtitle: string;
  buttonText: string;
  image: string;
}

@Component({
  selector: 'app-hero-banner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hero-banner.component.html',
  styleUrl: './hero-banner.component.less',
})
export class HeroBannerComponent {
  readonly locationClick = output<void>();

  readonly slides: BannerSlide[] = [
    {
      id: 1,
      title: 'Ven y visitanos en Luz del Saber',
      subtitle: 'Acércate a nuestra biblioteca y encuentra tu libro favorito para leer',
      buttonText: 'Ver Ubicación',
      image: 'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=1600&q=80',
    },
    {
      id: 2,
      title: 'Explora Nuestro Catálogo Digital',
      subtitle: 'Miles de títulos en literatura, tecnología y ciencias a tu alcance',
      buttonText: 'Ver Ubicación',
      image: 'https://images.unsplash.com/photo-1507842229443-50953a928427?auto=format&fit=crop&w=1600&q=80',
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
