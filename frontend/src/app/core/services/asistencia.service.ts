import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type MetodoRegistro = 'QR' | 'CODIGO';

export interface RegistrarAsistenciaRequest {
  codigo: string;
  metodo: MetodoRegistro;
  sesionId?: number; // Optional until we have session management
}

export interface AsistenciaResponse {
  success: boolean;
  nombre: string;
  hora: string;
  estado: 'PRESENTE' | 'TARDANZA' | 'FALTA';
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private http = inject(HttpClient);

  registrar(data: RegistrarAsistenciaRequest): Observable<AsistenciaResponse> {
    return this.http.post<AsistenciaResponse>(`${environment.apiUrl}/asistencias/registrar`, data);
  }
}
