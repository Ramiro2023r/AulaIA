import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SeccionResumen {
  id: number;
  nombre: string;
}

export interface EstudianteResponse {
  id: number;
  codigo: string;
  nombres: string;
  apellidos: string;
  seccion: SeccionResumen;
  activo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EstudianteRequest {
  codigo: string;
  nombres: string;
  apellidos: string;
  seccionId: number;
}

@Injectable({
  providedIn: 'root'
})
export class EstudianteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/estudiantes`;

  listar(filtros?: { codigo?: string; nombre?: string; seccion?: number; activo?: boolean }): Observable<EstudianteResponse[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.codigo) params = params.set('codigo', filtros.codigo);
      if (filtros.nombre) params = params.set('nombre', filtros.nombre);
      if (filtros.seccion) params = params.set('seccion', filtros.seccion);
      if (filtros.activo !== undefined) params = params.set('activo', filtros.activo);
    }
    return this.http.get<EstudianteResponse[]>(this.apiUrl, { params });
  }

  buscarPorId(id: number): Observable<EstudianteResponse> {
    return this.http.get<EstudianteResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: EstudianteRequest): Observable<EstudianteResponse> {
    return this.http.post<EstudianteResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: EstudianteRequest): Observable<EstudianteResponse> {
    return this.http.put<EstudianteResponse>(`${this.apiUrl}/${id}`, request);
  }

  desactivar(id: number): Observable<EstudianteResponse> {
    return this.http.patch<EstudianteResponse>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  regenerarQr(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/regenerar-qr`, {});
  }
}
