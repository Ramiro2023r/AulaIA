import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
export interface SesionClaseResponse {
  id: number | null;
  horarioId: number;
  fecha: string;
  estado: 'ABIERTA' | 'CERRADA' | 'CANCELADA' | 'PROGRAMADA' | null;
  horaApertura: string | null;
  horaCierre: string | null;
  horaInicio: string;
  horaFin: string;
  curso: { id: number; nombre: string };
  seccion: { id: number; nombre: string };
  docente: { id: number; nombres: string; apellidos: string };
}

export interface EstadisticasAsistencia {
  presentes: number;
  tardanzas: number;
  ausentes: number;
  totalEstudiantes: number;
  porcentajeAsistencia: number;
}

export interface EstudianteRiesgoDto {
  estudianteId: number;
  estudianteNombre: string;
  cursoNombre: string;
  seccionNombre: string;
  cantidadFaltas: number;
  porcentajeAsistencia: number;
}

export interface AsistenciaRecienteDto {
  estudianteNombre: string;
  cursoNombre: string;
  estado: string;
  horaRegistro: string;
}

export interface DashboardDocenteResponse {
  claseActual: SesionClaseResponse | null;
  claseActualAsistentes: number | null;
  claseActualTotalEstudiantes: number | null;
  clasesDelDia: SesionClaseResponse[];
  estadisticas: EstadisticasAsistencia;
  estudiantesRiesgo: EstudianteRiesgoDto[];
  ultimosRegistros: AsistenciaRecienteDto[];
}

export interface TendenciaAsistenciaDto {
  fecha: string;
  porcentajeAsistencia: number;
}

export interface AdminDashboardResponse {
  totalEstudiantes: number;
  totalDocentes: number;
  totalSecciones: number;
  asistenciaHoyPorcentaje: number;
  distribucionEstadoHoy: { [key: string]: number };
  tendencia7Dias: TendenciaAsistenciaDto[];
  justificacionesPendientes: any[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/dashboard`;

  obtenerResumenDocente(): Observable<DashboardDocenteResponse> {
    return this.http.get<DashboardDocenteResponse>(`${this.apiUrl}/docente`);
  }

  obtenerResumenAdmin(): Observable<AdminDashboardResponse> {
    return this.http.get<AdminDashboardResponse>(`${this.apiUrl}/admin`);
  }
}
