import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CursoResponse {
  id: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CursoRequest {
  nombre: string;
  descripcion: string;
}

@Injectable({
  providedIn: 'root'
})
export class CursoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/cursos`;

  listar(): Observable<CursoResponse[]> {
    return this.http.get<CursoResponse[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<CursoResponse> {
    return this.http.get<CursoResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: CursoRequest): Observable<CursoResponse> {
    return this.http.post<CursoResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: CursoRequest): Observable<CursoResponse> {
    return this.http.put<CursoResponse>(`${this.apiUrl}/${id}`, request);
  }

  // Si el backend tuviera endpoint para desactivar/activar individualmente
  desactivar(id: number): Observable<CursoResponse> {
    return this.http.patch<CursoResponse>(`${this.apiUrl}/${id}/desactivar`, {});
  }
}
