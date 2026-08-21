import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuditoriaResponse {
  id: number;
  usuarioUsername: string | null;
  entidad: string;
  entidadId: number | null;
  accion: string;
  valorAnterior: string | null;
  valorNuevo: string | null;
  ipOrigen: string | null;
  fechaHora: string;
}

export interface AuditoriaFiltros {
  usuario?: string;
  entidad?: string;
  accion?: string;
  desde?: string;
  hasta?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private apiUrl = `${environment.apiUrl}/auditoria`;

  constructor(private http: HttpClient) {}

  listar(filtros: AuditoriaFiltros = {}): Observable<AuditoriaResponse[]> {
    let params = new HttpParams();
    if (filtros.usuario) params = params.set('usuario', filtros.usuario);
    if (filtros.entidad)  params = params.set('entidad',  filtros.entidad);
    if (filtros.accion)   params = params.set('accion',   filtros.accion);
    if (filtros.desde)    params = params.set('desde',    filtros.desde);
    if (filtros.hasta)    params = params.set('hasta',    filtros.hasta);
    return this.http.get<AuditoriaResponse[]>(this.apiUrl, { params });
  }
}
