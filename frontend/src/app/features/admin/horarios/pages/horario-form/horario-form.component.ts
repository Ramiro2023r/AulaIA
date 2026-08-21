import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { HorarioService } from '../../../../../core/services/horario.service';
import { CursoService, CursoResponse } from '../../../../../core/services/curso.service';
import { SeccionService, SeccionResponse } from '../../../../../core/services/seccion.service';
import { DocenteService, DocenteResponse } from '../../../../../core/services/docente.service';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-horario-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './horario-form.component.html',
})
export class HorarioFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private horarioService = inject(HorarioService);
  private cursoService = inject(CursoService);
  private seccionService = inject(SeccionService);
  private docenteService = inject(DocenteService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form!: FormGroup;
  isEdit = signal(false);
  horarioId = signal<number | null>(null);
  loading = signal(false);

  // Combos
  cursos = signal<CursoResponse[]>([]);
  secciones = signal<SeccionResponse[]>([]);
  docentes = signal<DocenteResponse[]>([]);

  diasSemana = [
    { id: 1, nombre: 'Lunes' },
    { id: 2, nombre: 'Martes' },
    { id: 3, nombre: 'Miércoles' },
    { id: 4, nombre: 'Jueves' },
    { id: 5, nombre: 'Viernes' },
    { id: 6, nombre: 'Sábado' },
    { id: 7, nombre: 'Domingo' }
  ];

  ngOnInit(): void {
    this.initForm();
    this.cargarListas();
    
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEdit.set(true);
        this.horarioId.set(Number(id));
        this.cargarHorario(Number(id));
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      cursoId: ['', [Validators.required]],
      seccionId: ['', [Validators.required]],
      docenteId: ['', [Validators.required]],
      diaSemana: ['', [Validators.required, Validators.min(1), Validators.max(7)]],
      horaInicio: ['', [Validators.required]],
      horaFin: ['', [Validators.required]],
      toleranciaMinutos: [10, [Validators.required, Validators.min(0)]],
      minutosAntesApertura: [15, [Validators.required, Validators.min(0)]]
    });
  }

  cargarListas(): void {
    this.cursoService.listar().subscribe(c => this.cursos.set(c.filter(x => x.activo)));
    this.seccionService.listar().subscribe(s => this.secciones.set(s.filter(x => x.activo)));
    this.docenteService.listar().subscribe(d => this.docentes.set(d.filter(x => x.activo)));
  }

  cargarHorario(id: number): void {
    this.loading.set(true);
    this.horarioService.buscarPorId(id).subscribe({
      next: (horario) => {
        this.form.patchValue({
          cursoId: horario.curso.id,
          seccionId: horario.seccion.id,
          docenteId: horario.docente.id,
          diaSemana: horario.diaSemana,
          horaInicio: horario.horaInicio.substring(0, 5), // Format HH:mm
          horaFin: horario.horaFin.substring(0, 5),
          toleranciaMinutos: horario.toleranciaMinutos,
          minutosAntesApertura: horario.minutosAntesApertura
        });
        this.loading.set(false);
      },
      error: () => {
        this.toast.show('Error al cargar horario', 'error');
        this.router.navigate(['/admin/horarios']);
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.value;
    // Basic validation in frontend before sending
    if (value.horaInicio >= value.horaFin) {
      this.toast.show('La hora de fin debe ser posterior a la hora de inicio', 'error');
      return;
    }

    this.loading.set(true);

    const request = {
      ...value,
      cursoId: Number(value.cursoId),
      seccionId: Number(value.seccionId),
      docenteId: Number(value.docenteId),
      diaSemana: Number(value.diaSemana)
    };
    
    if (this.isEdit()) {
      this.horarioService.actualizar(this.horarioId()!, request).subscribe({
        next: () => {
          this.toast.show('Horario actualizado con éxito', 'success');
          this.router.navigate(['/admin/horarios']);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.horarioService.crear(request).subscribe({
        next: () => {
          this.toast.show('Horario creado con éxito', 'success');
          this.router.navigate(['/admin/horarios']);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  private handleError(err: any): void {
    this.loading.set(false);
    // Here we catch 400 Bad Request conflicts
    // Error messages from backend should explain the overlap (e.g. "El docente ya tiene una clase en ese horario")
    const msg = err.error?.message || 'Error al guardar el horario (Posible cruce de horas)';
    this.toast.show(msg, 'error');
  }
}
