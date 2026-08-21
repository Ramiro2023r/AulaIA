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

export interface DashboardDocenteResponse {
  claseActual: SesionClaseResponse | null;
  clasesDelDia: SesionClaseResponse[];
  estadisticas: EstadisticasAsistencia;
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
}
