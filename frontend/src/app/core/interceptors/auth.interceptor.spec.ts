import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    router = jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/admin/dashboard' });
    router.navigate.and.resolveTo(true);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('no adjunta Authorization al login y reemplaza el JWT anterior al iniciar sesión', () => {
    localStorage.setItem('aulaia_token', 'jwt-antiguo');
    authService = TestBed.inject(AuthService);

    authService.login({ usuario: 'docente@aulaia.com', contrasena: '123456' }).subscribe();

    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({
      accessToken: 'jwt-nuevo',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 2, username: 'docente@aulaia.com', rol: 'DOCENTE' }
    });

    expect(localStorage.getItem('aulaia_token')).toBe('jwt-nuevo');
    expect(JSON.parse(localStorage.getItem('aulaia_user')!)).toEqual({
      id: 2,
      username: 'docente@aulaia.com',
      rol: 'DOCENTE'
    });
  });

  it('adjunta el JWT actual a una petición protegida', () => {
    localStorage.setItem('aulaia_token', 'jwt-actual');
    authService = TestBed.inject(AuthService);

    http.get('/api/v1/estudiantes').subscribe();

    const request = httpMock.expectOne('/api/v1/estudiantes');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-actual');
    request.flush([]);
  });

  it('limpia la sesión y redirige al login ante 401 con JWT almacenado', () => {
    localStorage.setItem('aulaia_token', 'jwt-expirado');
    localStorage.setItem('aulaia_user', JSON.stringify({ id: 1, username: 'admin', rol: 'ADMIN' }));
    sessionStorage.setItem('aulaia_token', 'jwt-expirado');
    sessionStorage.setItem('aulaia_user', 'sesion-anterior');
    authService = TestBed.inject(AuthService);

    http.get('/api/v1/estudiantes').subscribe({ error: () => undefined });

    const request = httpMock.expectOne('/api/v1/estudiantes');
    request.flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('aulaia_token')).toBeNull();
    expect(localStorage.getItem('aulaia_user')).toBeNull();
    expect(sessionStorage.getItem('aulaia_token')).toBeNull();
    expect(sessionStorage.getItem('aulaia_user')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('logout elimina token y usuario de ambos almacenamientos', () => {
    localStorage.setItem('aulaia_token', 'jwt-actual');
    localStorage.setItem('aulaia_user', 'usuario');
    sessionStorage.setItem('aulaia_token', 'jwt-actual');
    sessionStorage.setItem('aulaia_user', 'usuario');
    authService = TestBed.inject(AuthService);

    authService.logout();

    expect(localStorage.getItem('aulaia_token')).toBeNull();
    expect(localStorage.getItem('aulaia_user')).toBeNull();
    expect(sessionStorage.getItem('aulaia_token')).toBeNull();
    expect(sessionStorage.getItem('aulaia_user')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
