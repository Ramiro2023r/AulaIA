import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { HorarioService, HorarioResponse } from '../../../../../core/services/horario.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-horarios-list',
  standalone: true,
  imports: [CommonModule, RouterModule, PageHeaderComponent],
  templateUrl: './horarios-list.component.html',
  styles: [`
    .grid-horario {
      display: grid;
      grid-template-columns: 80px repeat(7, 1fr);
      gap: 1px;
      background-color: var(--sys-outline-variant);
    }
    .grid-header, .grid-cell {
      background-color: var(--sys-surface);
      padding: 0.5rem;
    }
    .grid-header {
      background-color: var(--sys-surface-container-light);
      font-weight: 500;
      text-align: center;
      font-size: 0.875rem;
    }
    .time-col {
      background-color: var(--sys-surface-container-light);
      font-size: 0.75rem;
      text-align: right;
      padding-right: 0.5rem;
      color: var(--sys-on-surface-variant);
    }
    .horario-card {
      background-color: var(--sys-primary-container);
      color: var(--sys-on-primary-container);
      border-radius: 0.25rem;
      padding: 0.25rem 0.5rem;
      font-size: 0.75rem;
      margin-bottom: 0.25rem;
      cursor: pointer;
      border-left: 3px solid var(--sys-primary);
      transition: opacity 0.2s;
    }
    .horario-card:hover {
      opacity: 0.9;
    }
  `]
})
export class HorariosListComponent implements OnInit {
  private horarioService = inject(HorarioService);
  private toast = inject(ToastService);
  private router = inject(Router);

  horarios = signal<HorarioResponse[]>([]);
  loading = signal(false);
  
  viewMode = signal<'lista' | 'semana'>('semana');
  diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

  // Agrupación para la vista semanal por día (1-7)
  horariosPorDia = computed(() => {
    const list = this.horarios();
    const map = new Map<number, HorarioResponse[]>();
    for (let i = 1; i <= 7; i++) {
      map.set(i, []);
    }
    list.forEach(h => {
      const diaArr = map.get(h.diaSemana);
      if (diaArr) {
        diaArr.push(h);
      }
    });
    // Ordenar los de cada día por horaInicio
    map.forEach((arr) => {
      arr.sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
    });
    return map;
  });

  ngOnInit(): void {
    this.cargarHorarios();
  }

  cargarHorarios(): void {
    this.loading.set(true);
    this.horarioService.listar().subscribe({
      next: (data) => {
        // Orden global para la vista lista: Dia -> Hora Inicio
        const ordenados = data.sort((a, b) => {
          if (a.diaSemana !== b.diaSemana) return a.diaSemana - b.diaSemana;
          return a.horaInicio.localeCompare(b.horaInicio);
        });
        this.horarios.set(ordenados);
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar horarios', 'error');
        this.loading.set(false);
      }
    });
  }

  crearHorario(): void {
    this.router.navigate(['/admin/horarios/nuevo']);
  }

  editarHorario(id: number): void {
    this.router.navigate(['/admin/horarios', id, 'editar']);
  }

  getNombreDia(dia: number): string {
    return this.diasSemana[dia - 1] || 'Desconocido';
  }

  formatTime(time: string): string {
    // time format is HH:mm:ss, just return HH:mm
    if (!time) return '';
    return time.substring(0, 5);
  }
}
