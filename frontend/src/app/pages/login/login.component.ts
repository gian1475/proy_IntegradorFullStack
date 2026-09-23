import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HeaderComponent } from '../../components/header/header.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HeaderComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.less',
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email: string = '';
  password: string = '';
  errorMessage = signal<string>('');
  cargando = signal<boolean>(false);

  onLogin(): void {
    if (!this.email || !this.password) {
      this.errorMessage.set('Por favor ingrese usuario y contraseña');
      return;
    }

    this.cargando.set(true);
    this.errorMessage.set('');

    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/catalogo']);
      },
      error: (err) => {
        this.cargando.set(false);
        if (err.status === 0) {
          this.errorMessage.set('No se pudo conectar con el servidor backend (puerto 8080). Verifica que el backend esté iniciado.');
        } else {
          const msg = err.error?.mensaje || 'Credenciales incorrectas';
          this.errorMessage.set(msg);
        }
      }
    });
  }

  loginAdmin(): void {
    this.email = 'admin@alejandria.edu.pe';
    this.password = '123456';
    this.onLogin();
  }
}
