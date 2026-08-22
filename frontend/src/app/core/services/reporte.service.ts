import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReporteAsistenciaDto {
  fecha: string;
  asistenciaId: number;
  estudianteNombreCompleto: string;
  cursoNombre: string;
  seccionNombre: string;
  estadoAsistencia: string;
  justificacionEstado: string;
}

export interface ReporteFiltrosDto {
  fechaInicio?: string;
  fechaFin?: string;
  cursoId?: number;
  seccionId?: number;
  estudianteId?: number;
  estadoAsistencia?: string;
}

export interface ReporteResumen {
  total: number;
  presentes: number;
  tardanzas: number;
  ausentes: number;
  justificados: number;
  porcentajeAsistencia: number;
}

@Injectable({
  providedIn: 'root'
})
export class ReporteService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/reportes`;

  generarReporte(filtros: ReporteFiltrosDto): Observable<ReporteAsistenciaDto[]> {
    let params = new HttpParams();
    if (filtros.fechaInicio) params = params.set('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) params = params.set('fechaFin', filtros.fechaFin);
    if (filtros.cursoId) params = params.set('cursoId', filtros.cursoId);
    if (filtros.seccionId) params = params.set('seccionId', filtros.seccionId);
    if (filtros.estudianteId) params = params.set('estudianteId', filtros.estudianteId);
    if (filtros.estadoAsistencia) params = params.set('estadoAsistencia', filtros.estadoAsistencia);
    return this.http.get<ReporteAsistenciaDto[]>(`${this.apiUrl}/asistencia`, { params });
  }

  descargarExcel(filtros: ReporteFiltrosDto): Observable<Blob> {
    let params = new HttpParams();
    if (filtros.fechaInicio) params = params.set('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) params = params.set('fechaFin', filtros.fechaFin);
    if (filtros.cursoId) params = params.set('cursoId', filtros.cursoId);
    if (filtros.seccionId) params = params.set('seccionId', filtros.seccionId);
    if (filtros.estudianteId) params = params.set('estudianteId', filtros.estudianteId);
    if (filtros.estadoAsistencia) params = params.set('estadoAsistencia', filtros.estadoAsistencia);
    return this.http.get(`${this.apiUrl}/asistencia/excel`, { params, responseType: 'blob' });
  }

  descargarPdf(filtros: ReporteFiltrosDto): Observable<Blob> {
    let params = new HttpParams();
    if (filtros.fechaInicio) params = params.set('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) params = params.set('fechaFin', filtros.fechaFin);
    if (filtros.cursoId) params = params.set('cursoId', filtros.cursoId);
    if (filtros.seccionId) params = params.set('seccionId', filtros.seccionId);
    if (filtros.estudianteId) params = params.set('estudianteId', filtros.estudianteId);
    if (filtros.estadoAsistencia) params = params.set('estadoAsistencia', filtros.estadoAsistencia);
    return this.http.get(`${this.apiUrl}/asistencia/pdf`, { params, responseType: 'blob' });
  }

  calcularResumen(datos: ReporteAsistenciaDto[]): ReporteResumen {
    const total = datos.length;
    const presentes = datos.filter(d => d.estadoAsistencia === 'PRESENTE').length;
    const tardanzas = datos.filter(d => d.estadoAsistencia === 'TARDANZA').length;
    const ausentes = datos.filter(d => d.estadoAsistencia === 'AUSENTE').length;
    const justificados = datos.filter(d => d.estadoAsistencia === 'JUSTIFICADO').length;
    const asistieron = presentes + tardanzas + justificados;
    const porcentajeAsistencia = total > 0 ? Math.round((asistieron / total) * 100) : 0;

    return { total, presentes, tardanzas, ausentes, justificados, porcentajeAsistencia };
  }
}