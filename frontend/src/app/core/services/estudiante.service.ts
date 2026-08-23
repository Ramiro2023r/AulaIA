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

  listar(filtros?: { codigo?: string; nombre?: string; buscar?: string; seccion?: number; activo?: boolean }): Observable<EstudianteResponse[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.codigo) params = params.set('codigo', filtros.codigo);
      if (filtros.nombre) params = params.set('nombre', filtros.nombre);
      if (filtros.buscar) params = params.set('buscar', filtros.buscar);
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

  listarApoderadosParaTelegram(estudianteId: number): Observable<ApoderadoTelegramOption[]> {
    return this.http.get<ApoderadoTelegramOption[]>(`${this.apiUrl}/${estudianteId}/apoderados`);
  }

  crearApoderado(estudianteId: number, request: ApoderadoEstudianteRequest): Observable<ApoderadoTelegramOption> {
    return this.http.post<ApoderadoTelegramOption>(`${this.apiUrl}/${estudianteId}/apoderados`, request);
  }

  buscarApoderadosDisponibles(estudianteId: number, buscar: string): Observable<ApoderadoDisponible[]> {
    let params = new HttpParams();
    if (buscar.trim()) params = params.set('buscar', buscar.trim());
    return this.http.get<ApoderadoDisponible[]>(`${this.apiUrl}/${estudianteId}/apoderados/disponibles`, { params });
  }

  asociarApoderadoExistente(estudianteId: number, apoderadoId: number,
                            request: AsociarApoderadoRequest): Observable<ApoderadoTelegramOption> {
    return this.http.post<ApoderadoTelegramOption>(`${this.apiUrl}/${estudianteId}/apoderados/${apoderadoId}`, request);
  }

  generarVinculacionTelegram(estudianteId: number, apoderadoId: number): Observable<TelegramVinculacionLinkResponse> {
    return this.http.post<TelegramVinculacionLinkResponse>(`${this.apiUrl}/${estudianteId}/telegram/vinculacion`, {
      apoderadoId
    });
  }
}

export interface TelegramVinculacionLinkResponse {
  status: string;
  telegramUrl: string;
  expiresAt: string;
}

export interface ApoderadoTelegramOption {
  id: number;
  nombres: string;
  apellidos: string;
  parentesco: string;
  principal: boolean;
  activo: boolean;
  telegramVinculado?: boolean;
}

export interface ApoderadoEstudianteRequest {
  nombres: string;
  apellidos: string;
  telefono?: string | null;
  parentesco: 'MADRE' | 'PADRE' | 'TUTOR' | 'OTRO';
  principal: boolean;
}

export interface ApoderadoDisponible {
  id: number;
  nombres: string;
  apellidos: string;
  telefono: string | null;
}

export interface AsociarApoderadoRequest {
  parentesco: 'MADRE' | 'PADRE' | 'TUTOR' | 'OTRO';
  principal: boolean;
}
