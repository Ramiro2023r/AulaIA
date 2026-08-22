import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageHeaderComponent } from '../../../shared/components/ui/page-header/page-header.component';
import { ButtonComponent } from '../../../shared/components/ui/button/button.component';
import { InputComponent } from '../../../shared/components/ui/input/input.component';
import { SkeletonComponent } from '../../../shared/components/ui/skeleton/skeleton.component';
import { IaService, IaConsultaRequest, IaConsultaResponse } from '../../../core/services/ia.service';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-ia-asistente',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    ButtonComponent,
    InputComponent,
    SkeletonComponent
  ],
  templateUrl: './ia-asistente.component.html',
  styleUrls: ['./ia-asistente.component.scss']
})
export class IaAsistenteComponent implements OnInit {
  private iaService = inject(IaService);
  private toast = inject(ToastService);

  pregunta = signal('');
  loading = signal(false);
  respuesta = signal<IaConsultaResponse | null>(null);
  iaDisponible = signal<boolean | null>(null);

  preguntasRapidas = [
    '¿Quiénes faltaron hoy?',
    'Resumen de esta semana',
    'Estudiantes con tardanzas',
    'Tendencia del mes',
    '¿Cómo estuvo la asistencia de 6º A?',
    'Alumnos con mayor ausentismo'
  ];

  ngOnInit(): void {
    this.verificarDisponibilidad();
  }

  verificarDisponibilidad(): void {
    this.iaService.healthCheck().subscribe({
      next: (res) => this.iaDisponible.set(res.iaDisponible),
      error: () => this.iaDisponible.set(false)
    });
  }

  enviarConsulta(): void {
    const p = this.pregunta().trim();
    if (!p) return;

    this.loading.set(true);
    this.respuesta.set(null);

    const request: IaConsultaRequest = { pregunta: p };

    this.iaService.consultar(request).subscribe({
      next: (res) => {
        this.respuesta.set(res);
        this.loading.set(false);
        if (!res.iaDisponible) {
          this.toast.show('Servicio IA no disponible temporalmente', 'warning');
        }
      },
      error: (err) => {
        console.error('Error en consulta IA', err);
        this.loading.set(false);
        this.respuesta.set({
          respuesta: 'Ocurrió un error al procesar la consulta. Intente nuevamente.',
          iaDisponible: false
        });
        this.toast.show('Error en consulta IA', 'error');
      }
    });
  }

  usarPreguntaRapida(pregunta: string): void {
    this.pregunta.set(pregunta);
    this.enviarConsulta();
  }

  obtenerResumen(): void {
    this.loading.set(true);
    this.respuesta.set(null);
    
    this.iaService.obtenerResumen().subscribe({
      next: (res) => {
        this.respuesta.set(res);
        this.loading.set(false);
        if (!res.iaDisponible) {
          this.toast.show('Servicio IA no disponible temporalmente', 'warning');
        }
      },
      error: (err) => {
        console.error('Error obteniendo resumen IA', err);
        this.loading.set(false);
        this.respuesta.set({
          respuesta: 'Ocurrió un error al obtener el resumen. Intente nuevamente.',
          iaDisponible: false
        });
        this.toast.show('Error en resumen IA', 'error');
      }
    });
  }

  limpiar(): void {
    this.pregunta.set('');
    this.respuesta.set(null);
  }

  hayRespuesta(): boolean {
    return this.respuesta() !== null;
  }

  esExitoso(): boolean {
    return this.respuesta()?.iaDisponible === true;
  }
}