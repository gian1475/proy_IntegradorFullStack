import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../../components/header/header.component';
import { HeroBannerComponent } from '../../components/hero-banner/hero-banner.component';
import { BooksSectionComponent, Book } from '../../components/books-section/books-section.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, HeaderComponent, HeroBannerComponent, BooksSectionComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.less',
})
export class HomeComponent {
  readonly searchQuery = signal<string>('');
  readonly showLocationModal = signal<boolean>(false);
  readonly selectedBook = signal<Book | null>(null);

  onSearchTermChange(query: string): void {
    this.searchQuery.set(query);
  }

  openLocationModal(): void {
    this.showLocationModal.set(true);
  }

  closeLocationModal(): void {
    this.showLocationModal.set(false);
  }

  openBookModal(book: Book): void {
    this.selectedBook.set(book);
  }

  closeBookModal(): void {
    this.selectedBook.set(null);
  }
}
