import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DocenteResponse {
  id: number;
  nombres: string;
  apellidos: string;
  activo: boolean;
  usuario: {
    id: number;
    username: string;
    rol: string;
    activo: boolean;
  };
  createdAt: string;
  updatedAt: string;
}

export interface DocenteRequest {
  username?: string;
  password?: string;
  nombres: string;
  apellidos: string;
}

export interface DocenteUpdateRequest {
  nombres: string;
  apellidos: string;
}

@Injectable({
  providedIn: 'root'
})
export class DocenteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/docentes`;

  listar(): Observable<DocenteResponse[]> {
    return this.http.get<DocenteResponse[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<DocenteResponse> {
    return this.http.get<DocenteResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: DocenteRequest): Observable<DocenteResponse> {
    return this.http.post<DocenteResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: DocenteUpdateRequest): Observable<DocenteResponse> {
    return this.http.put<DocenteResponse>(`${this.apiUrl}/${id}`, request);
  }

  desactivar(id: number): Observable<DocenteResponse> {
    return this.http.patch<DocenteResponse>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  restablecerPassword(id: number, password: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/reset-password`, { password });
  }
}
