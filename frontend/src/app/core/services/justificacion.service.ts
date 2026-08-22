import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface JustificacionResponse {
  id: number;
  asistenciaId: number;
  estudianteNombre: string;
  estudianteApellidos: string;
  cursoNombre: string;
  fechaSesion: string;
  estadoAsistencia: string;
  motivo: string;
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
  revisadoPorNombre?: string;
  fechaRevision?: string;
  createdAt: string;
}

export interface EvaluarJustificacionRequest {
  estado: 'APROBADA' | 'RECHAZADA';
  observaciones?: string;
}

export interface JustificacionRequest {
  asistenciaId: number;
  motivo: string;
}

@Injectable({
  providedIn: 'root'
})
export class JustificacionService {
  private apiUrl = `${environment.apiUrl}/justificaciones`;

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<JustificacionResponse[]> {
    return this.http.get<JustificacionResponse[]>(this.apiUrl);
  }

  crear(request: JustificacionRequest): Observable<JustificacionResponse> {
    return this.http.post<JustificacionResponse>(this.apiUrl, request);
  }

  evaluar(id: number, request: EvaluarJustificacionRequest): Observable<JustificacionResponse> {
    return this.http.put<JustificacionResponse>(`${this.apiUrl}/${id}/evaluar`, request);
  }
}
