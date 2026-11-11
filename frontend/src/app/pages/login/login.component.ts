import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { HeaderComponent } from '../../components/header/header.component';
import { mensajeDeError } from '../../utils/mensaje-error';

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

  // login: revisa q no esten vacios, llama al backend y si sale bien lo manda a la pantalla de su rol
  onLogin(): void {
    if (!this.email || !this.password) {
      this.errorMessage.set('Por favor ingrese usuario y contraseña');
      return;
    }

    this.cargando.set(true);
    this.errorMessage.set('');

    this.authService.login(this.email, this.password).subscribe({
      next: (respuesta) => {
        this.cargando.set(false);
        this.router.navigate([this.authService.rutaInicio(respuesta.usuario.rol)]);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMessage.set(mensajeDeError(err, 'Credenciales incorrectas'));
      }
    });
  }
}
