import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface IaConsultaRequest {
  pregunta: string;
  contexto?: string;
}

export interface IaConsultaResponse {
  respuesta: string;
  iaDisponible: boolean;
  datosAnalisis?: any;
}

export interface IaResumenResponse {
  respuesta: string;
  iaDisponible: boolean;
  datosAnalisis?: any;
}

export interface IaHealthResponse {
  respuesta: string;
  iaDisponible: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class IaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ia`;

  private getHeaders() {
    const provider = localStorage.getItem('aulaia_ai_provider') || 'gemini';
    const key = localStorage.getItem('aulaia_ai_key') || '';
    return {
      'X-AI-Provider': provider,
      'X-AI-Key': key
    };
  }

  consultar(request: IaConsultaRequest): Observable<IaConsultaResponse> {
    return this.http.post<IaConsultaResponse>(`${this.apiUrl}/consulta`, request, { headers: this.getHeaders() });
  }

  obtenerResumen(): Observable<IaResumenResponse> {
    return this.http.get<IaResumenResponse>(`${this.apiUrl}/resumen`, { headers: this.getHeaders() });
  }

  healthCheck(): Observable<IaHealthResponse> {
    return this.http.get<IaHealthResponse>(`${this.apiUrl}/health`, { headers: this.getHeaders() });
  }
}