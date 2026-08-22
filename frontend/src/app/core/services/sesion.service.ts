import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SesionClaseResponse } from './dashboard.service';

@Injectable({
  providedIn: 'root'
})
export class SesionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/sesiones`;

  abrirSesion(id: number): Observable<SesionClaseResponse> {
    return this.http.post<SesionClaseResponse>(`${this.apiUrl}/${id}/abrir`, {});
  }

  cerrarSesion(id: number): Observable<SesionClaseResponse> {
    return this.http.post<SesionClaseResponse>(`${this.apiUrl}/${id}/cerrar`, {});
  }

  buscarPorId(id: number): Observable<SesionClaseResponse> {
    return this.http.get<SesionClaseResponse>(`${this.apiUrl}/${id}`);
  }
}
