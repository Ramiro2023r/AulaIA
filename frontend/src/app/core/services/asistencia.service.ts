import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type MetodoRegistro = 'QR' | 'CODIGO';

export interface RegistrarAsistenciaRequest {
  codigo: string;
  metodo: MetodoRegistro;
  sesionId?: number; // Optional until we have session management
}

/** Respuesta del registro inmediato de asistencia en Modo Aula. */
export interface RegistrarAsistenciaResponse {
  success: boolean;
  nombre: string;
  hora: string;
  estado: 'PRESENTE' | 'TARDANZA';
  mensaje: string;
}

export interface AsistenciaResponse {
  id: number;
  sesionId: number;
  estudianteId: number;
  estudianteNombre: string;
  estudianteApellido: string;
  fechaHora: string;
  estado: 'PRESENTE' | 'TARDE' | 'AUSENTE' | 'JUSTIFICADO';
  metodo: MetodoRegistro;
  observacion: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface AsistenciaFiltros {
  fecha?: string;
  estado?: string;
  seccion?: number;
  curso?: number;
  estudiante?: number;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private http = inject(HttpClient);

  registrar(data: RegistrarAsistenciaRequest): Observable<RegistrarAsistenciaResponse> {
    return this.http.post<RegistrarAsistenciaResponse>(`${environment.apiUrl}/asistencias/registrar`, data);
  }

  listar(filtros?: AsistenciaFiltros): Observable<PageResponse<AsistenciaResponse>> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.fecha) params = params.set('fecha', filtros.fecha);
      if (filtros.estado) params = params.set('estado', filtros.estado);
      if (filtros.seccion) params = params.set('seccion', filtros.seccion);
      if (filtros.curso) params = params.set('curso', filtros.curso);
      if (filtros.estudiante) params = params.set('estudiante', filtros.estudiante);
      if (filtros.page !== undefined) params = params.set('page', filtros.page);
      if (filtros.size !== undefined) params = params.set('size', filtros.size);
    }
    return this.http.get<PageResponse<AsistenciaResponse>>(`${environment.apiUrl}/asistencias`, { params });
  }

  correccionManual(id: number, estado: string, motivo: string): Observable<AsistenciaResponse> {
    return this.http.put<AsistenciaResponse>(`${environment.apiUrl}/asistencias/${id}/correccion`, { estado, motivo });
  }
}
