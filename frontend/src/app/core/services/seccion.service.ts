import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GradoResumen {
  id: number;
  nombre: string;
}

export interface SeccionResponse {
  id: number;
  grado: GradoResumen;
  nombre: string;
  periodoAcademico: string;
  activo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SeccionRequest {
  gradoId: number;
  nombre: string;
  periodoAcademico: string;
}

@Injectable({
  providedIn: 'root'
})
export class SeccionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/secciones`;

  listar(): Observable<SeccionResponse[]> {
    return this.http.get<SeccionResponse[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<SeccionResponse> {
    return this.http.get<SeccionResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: SeccionRequest): Observable<SeccionResponse> {
    return this.http.post<SeccionResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: SeccionRequest): Observable<SeccionResponse> {
    return this.http.put<SeccionResponse>(`${this.apiUrl}/${id}`, request);
  }

  desactivar(id: number): Observable<SeccionResponse> {
    return this.http.patch<SeccionResponse>(`${this.apiUrl}/${id}/desactivar`, {});
  }
}
