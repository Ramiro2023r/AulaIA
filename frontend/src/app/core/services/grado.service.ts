import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GradoResponse {
  id: number;
  nombre: string;
  nivel: string;
  orden: number;
  activo: boolean;
  createdAt: string;
}

export interface GradoRequest {
  nombre: string;
  nivel: string;
  orden: number;
}

@Injectable({
  providedIn: 'root'
})
export class GradoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/grados`;

  listar(): Observable<GradoResponse[]> {
    return this.http.get<GradoResponse[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<GradoResponse> {
    return this.http.get<GradoResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: GradoRequest): Observable<GradoResponse> {
    return this.http.post<GradoResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: GradoRequest): Observable<GradoResponse> {
    return this.http.put<GradoResponse>(`${this.apiUrl}/${id}`, request);
  }

  desactivar(id: number): Observable<GradoResponse> {
    return this.http.patch<GradoResponse>(`${this.apiUrl}/${id}/desactivar`, {});
  }
}
