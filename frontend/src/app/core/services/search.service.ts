import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of } from 'rxjs';
import { EstudianteService } from './estudiante.service';
import { DocenteService } from './docente.service';
import { AuthService } from './auth.service';

export interface SearchResult {
  type: 'ESTUDIANTE' | 'DOCENTE';
  id: number;
  title: string;
  subtitle: string;
  route: string;
  icon: string;
}

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private estudianteService = inject(EstudianteService);
  private docenteService = inject(DocenteService);
  private authService = inject(AuthService);

  search(query: string): Observable<SearchResult[]> {
    if (!query || query.trim().length < 2) {
      return of([]);
    }

    const q = query.toLowerCase().trim();

    const currentUserRole = this.authService.currentUser()?.rol || 'ADMIN';
    const isAdmin = currentUserRole === 'ADMIN';

    const requests: any = {
      estudiantes: this.estudianteService.listar({ buscar: q })
    };

    if (isAdmin) {
      requests.docentes = this.docenteService.listar();
    }

    return forkJoin(requests).pipe(
      map((results: any) => {
        const unifiedResults: SearchResult[] = [];

        // Mapear Estudiantes (ya vienen filtrados por el backend si tiene soporte, o todos)
        // Nota: EstudianteService.listar(q) trae content si es Page, o array si no. 
        // Verificamos si es PageResponse
        const estudiantesList = (results.estudiantes as any).content || results.estudiantes;

        if (Array.isArray(estudiantesList)) {
          estudiantesList.slice(0, 5).forEach((e: any) => {
            unifiedResults.push({
              type: 'ESTUDIANTE',
              id: e.id,
              title: `${e.apellidos}, ${e.nombres}`,
              subtitle: `Código: ${e.codigo || '-'}`,
              route: isAdmin ? `/admin/estudiantes/${e.id}` : `/docente/reportes?estudianteId=${e.id}`,
              icon: 'school'
            });
          });
        }

        // Mapear Docentes (filtramos manual)
        // Ocultar si el rol actual es DOCENTE (no debe buscar a otros docentes)
        if (isAdmin && Array.isArray(results.docentes)) {
          results.docentes
            .filter((d: any) => 
              d.nombres?.toLowerCase().includes(q) || 
              d.apellidos?.toLowerCase().includes(q) ||
              d.usuario?.username?.toLowerCase().includes(q)
            )
            .slice(0, 5)
            .forEach((d: any) => {
              unifiedResults.push({
                type: 'DOCENTE',
                id: d.id,
                title: `${d.apellidos}, ${d.nombres}`,
                subtitle: `Usuario: ${d.usuario?.username || '-'}`,
                route: `/admin/docentes/${d.id}/editar`,
                icon: 'person_4'
              });
            });
        }

        return unifiedResults;
      })
    );
  }
}
