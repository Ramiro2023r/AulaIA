import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export type UserRole = 'ADMIN' | 'DOCENTE';

export interface AuthUser {
  id: number;
  nombre: string;
  apellido: string;
  usuario: string;
  rol: UserRole;
}

export interface LoginRequest {
  usuario: string;
  contrasena: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

const TOKEN_KEY = 'aulaia_token';
const USER_KEY  = 'aulaia_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http   = inject(HttpClient);
  private router = inject(Router);

  // Estado reactivo
  private _token       = signal<string | null>(this.loadToken());
  private _currentUser = signal<AuthUser | null>(this.loadUser());

  // Computados públicos
  readonly token       = this._token.asReadonly();
  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn  = computed(() => !!this._token());
  readonly role        = computed(() => this._currentUser()?.rol ?? null);

  // ─── Login ───────────────────────────────────────────────────────────────
  login(credentials: LoginRequest) {
    const payload = {
      username: credentials.usuario,
      password: credentials.contrasena
    };
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap(response => {
        this.persist(response.accessToken, response.user);
      }),
      catchError(err => {
        return throwError(() => err);
      })
    );
  }

  // ─── Logout ──────────────────────────────────────────────────────────────
  logout(): void {
    this.clear();
    this.router.navigate(['/login']);
  }

  // ─── Token helpers ───────────────────────────────────────────────────────
  getToken(): string | null {
    return this._token();
  }

  // ─── Private ─────────────────────────────────────────────────────────────
  private persist(token: string, user: AuthUser): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this._token.set(token);
    this._currentUser.set(user);
  }

  private clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._token.set(null);
    this._currentUser.set(null);
  }

  private loadToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private loadUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    try {
      return (raw && raw !== 'undefined') ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
