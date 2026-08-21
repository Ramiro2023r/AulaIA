import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface HorarioCursoResumen {
  id: number;
  nombre: string;
}

export interface HorarioSeccionResumen {
  id: number;
  nombre: string;
}

export interface HorarioDocenteResumen {
  id: number;
  nombres: string;
  apellidos: string;
  activo: boolean;
}

export interface HorarioResponse {
  id: number;
  diaSemana: number;
  horaInicio: string; // HH:mm:ss
  horaFin: string; // HH:mm:ss
  toleranciaMinutos: number;
  minutosAntesApertura: number;
  activo: boolean;
  curso: HorarioCursoResumen;
  seccion: HorarioSeccionResumen;
  docente: HorarioDocenteResumen;
  createdAt: string;
  updatedAt: string;
}

export interface HorarioRequest {
  cursoId: number;
  seccionId: number;
  docenteId: number;
  diaSemana: number;
  horaInicio: string; // HH:mm
  horaFin: string; // HH:mm
  toleranciaMinutos: number;
  minutosAntesApertura: number;
}

@Injectable({
  providedIn: 'root'
})
export class HorarioService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/horarios`;

  listar(filtros?: { docente?: number; seccion?: number; curso?: number; dia?: number }): Observable<HorarioResponse[]> {
    let params = new HttpParams();
    if (filtros) {
      if (filtros.docente) params = params.set('docente', filtros.docente);
      if (filtros.seccion) params = params.set('seccion', filtros.seccion);
      if (filtros.curso) params = params.set('curso', filtros.curso);
      if (filtros.dia) params = params.set('dia', filtros.dia);
    }
    return this.http.get<HorarioResponse[]>(this.apiUrl, { params });
  }

  buscarPorId(id: number): Observable<HorarioResponse> {
    return this.http.get<HorarioResponse>(`${this.apiUrl}/${id}`);
  }

  crear(request: HorarioRequest): Observable<HorarioResponse> {
    return this.http.post<HorarioResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: HorarioRequest): Observable<HorarioResponse> {
    return this.http.put<HorarioResponse>(`${this.apiUrl}/${id}`, request);
  }
}
