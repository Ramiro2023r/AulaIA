import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageHeaderComponent } from '../../../shared/components/ui/page-header/page-header.component';
import { ButtonComponent } from '../../../shared/components/ui/button/button.component';
import { InputComponent } from '../../../shared/components/ui/input/input.component';
import { SelectComponent, SelectOption } from '../../../shared/components/ui/select/select.component';
import { SkeletonComponent } from '../../../shared/components/ui/skeleton/skeleton.component';
import { CursoService, CursoResponse } from '../../../core/services/curso.service';
import { SeccionService, SeccionResponse } from '../../../core/services/seccion.service';
import { EstudianteService, EstudianteResponse } from '../../../core/services/estudiante.service';
import { ReporteService, ReporteFiltrosDto, ReporteAsistenciaDto, ReporteResumen } from '../../../core/services/reporte.service';
import { JustificacionService } from '../../../core/services/justificacion.service';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    ButtonComponent,
    InputComponent,
    SelectComponent,
    SkeletonComponent
  ],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.scss']
})
export class ReportesComponent implements OnInit {
  private reporteService = inject(ReporteService);
  private cursoService = inject(CursoService);
  private seccionService = inject(SeccionService);
  private estudianteService = inject(EstudianteService);
  private justificacionService = inject(JustificacionService);
  private toast = inject(ToastService);
  private route = inject(ActivatedRoute);

  cursos = signal<CursoResponse[]>([]);
  secciones = signal<SeccionResponse[]>([]);
  estudiantes = signal<EstudianteResponse[]>([]);

  // Filtros como signals separados
  fechaInicio = signal<string>('');
  fechaFin = signal<string>('');
  cursoId = signal<number | null>(null);
  seccionId = signal<number | null>(null);
  estudianteId = signal<number | null>(null);
  estadoAsistencia = signal<string>('');

  // Opciones para selects (formato SelectOption)
  cursoOptions = computed<SelectOption[]>(() =>
    this.cursos().map(c => ({ label: c.nombre, value: c.id }))
  );
  seccionOptions = computed<SelectOption[]>(() =>
    this.secciones().map(s => ({ label: `${s.grado.nombre} - ${s.nombre}`, value: s.id }))
  );
  estudianteOptions = computed<SelectOption[]>(() =>
    this.estudiantes().map(e => ({ label: `${e.apellidos}, ${e.nombres} (${e.codigo})`, value: e.id }))
  );

  estadosOptions: SelectOption[] = [
    { label: 'Presente', value: 'PRESENTE' },
    { label: 'Tardanza', value: 'TARDANZA' },
    { label: 'Ausente', value: 'AUSENTE' },
    { label: 'Justificado', value: 'JUSTIFICADO' }
  ];

  reporteData = signal<ReporteAsistenciaDto[]>([]);
  loading = signal(false);
  exportando = signal(false);

  resumen = computed<ReporteResumen>(() => this.reporteService.calcularResumen(this.reporteData()));

  // Estado para el modal de justificación
  showJustificarDialog = signal(false);
  justificarMotivo = signal('');
  justificarAsistenciaId = signal<number | null>(null);

  ngOnInit(): void {
    this.cargarDatosFiltros();
    this.route.queryParams.subscribe(params => {
      if (params['estudianteId']) {
        this.estudianteId.set(+params['estudianteId']);
        // Esperamos a que los estudiantes se carguen para poder generar el reporte con su ID válido
        // Si bien la BD no requiere que esté en el select, es mejor para la UI
        setTimeout(() => this.generarReporte(), 500); 
      }
    });
  }

  cargarDatosFiltros(): void {
    this.cursoService.listar().subscribe({
      next: (data) => this.cursos.set(data),
      error: () => this.toast.show('Error al cargar cursos', 'error')
    });

    this.seccionService.listar().subscribe({
      next: (data) => this.secciones.set(data),
      error: () => this.toast.show('Error al cargar secciones', 'error')
    });

    this.estudianteService.listar({ activo: true }).subscribe({
      next: (data) => this.estudiantes.set(data),
      error: () => this.toast.show('Error al cargar estudiantes', 'error')
    });
  }

  buscarEstudiantes(termino: string): void {
    if (!termino || termino.length < 2) return;
    this.estudianteService.listar({ nombre: termino, activo: true }).subscribe({
      next: (data) => this.estudiantes.set(data),
      error: () => {}
    });
  }

  getFiltros(): ReporteFiltrosDto {
    const f: ReporteFiltrosDto = {};
    if (this.fechaInicio()) f.fechaInicio = this.fechaInicio();
    if (this.fechaFin()) f.fechaFin = this.fechaFin();
    if (this.cursoId()) f.cursoId = this.cursoId()!;
    if (this.seccionId()) f.seccionId = this.seccionId()!;
    if (this.estudianteId()) f.estudianteId = this.estudianteId()!;
    if (this.estadoAsistencia()) f.estadoAsistencia = this.estadoAsistencia();
    return f;
  }

  generarReporte(): void {
    this.loading.set(true);
    this.reporteService.generarReporte(this.getFiltros()).subscribe({
      next: (data) => {
        this.reporteData.set(data);
        this.loading.set(false);
        if (data.length === 0) {
          this.toast.show('No se encontraron registros con los filtros actuales', 'info');
        } else {
          this.toast.show(`Reporte generado: ${data.length} registros`, 'success');
        }
      },
      error: (err) => {
        console.error('Error generando reporte', err);
        this.loading.set(false);
        this.toast.show('Error al generar reporte', 'error');
      }
    });
  }

  exportarExcel(): void {
    if (this.reporteData().length === 0) {
      this.toast.show('Genere el reporte primero', 'warning');
      return;
    }
    this.exportando.set(true);
    this.reporteService.descargarExcel(this.getFiltros()).subscribe({
      next: (blob) => {
        this.descargarArchivo(blob, 'reporte_asistencias.xlsx');
        this.exportando.set(false);
        this.toast.show('Excel descargado correctamente', 'success');
      },
      error: (err) => {
        console.error('Error descargando Excel', err);
        this.exportando.set(false);
        this.toast.show('Error al descargar Excel', 'error');
      }
    });
  }

  exportarPdf(): void {
    if (this.reporteData().length === 0) {
      this.toast.show('Genere el reporte primero', 'warning');
      return;
    }
    this.exportando.set(true);
    this.reporteService.descargarPdf(this.getFiltros()).subscribe({
      next: (blob) => {
        this.descargarArchivo(blob, 'reporte_asistencias.pdf');
        this.exportando.set(false);
        this.toast.show('PDF descargado correctamente', 'success');
      },
      error: (err) => {
        console.error('Error descargando PDF', err);
        this.exportando.set(false);
        this.toast.show('Error al descargar PDF', 'error');
      }
    });
  }

  limpiarFiltros(): void {
    this.fechaInicio.set('');
    this.fechaFin.set('');
    this.cursoId.set(null);
    this.seccionId.set(null);
    this.estudianteId.set(null);
    this.estadoAsistencia.set('');
    this.reporteData.set([]);
  }

  private descargarArchivo(blob: Blob, nombre: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  getEstadoBadgeClass(estado: string): string {
    switch (estado) {
      case 'PRESENTE': return 'bg-success/20 text-success';
      case 'TARDANZA': return 'bg-warning/20 text-warning';
      case 'AUSENTE': return 'bg-error/20 text-error';
      case 'JUSTIFICADO': return 'bg-primary/20 text-primary';
      default: return 'bg-neutral/20 text-neutral';
    }
  }

  hayResultados(): boolean {
    return this.reporteData().length > 0;
  }

  estaCargando(): boolean {
    return this.loading();
  }

  estaExportando(): boolean {
    return this.exportando();
  }

  // --- Lógica de Justificación ---

  puedeJustificar(item: ReporteAsistenciaDto): boolean {
    return (item.estadoAsistencia === 'AUSENTE' || item.estadoAsistencia === 'TARDANZA') && 
           (!item.justificacionEstado || !['PENDIENTE', 'APROBADA'].includes(item.justificacionEstado));
  }

  abrirJustificarDialog(item: ReporteAsistenciaDto): void {
    if (item.asistenciaId) {
      this.justificarAsistenciaId.set(item.asistenciaId);
      this.justificarMotivo.set('');
      this.showJustificarDialog.set(true);
    } else {
      this.toast.show('No se encontró el ID de asistencia', 'error');
    }
  }

  cerrarJustificarDialog(): void {
    this.showJustificarDialog.set(false);
    this.justificarAsistenciaId.set(null);
    this.justificarMotivo.set('');
  }

  enviarJustificacion(): void {
    const id = this.justificarAsistenciaId();
    const motivo = this.justificarMotivo();
    
    if (!id) return;
    if (!motivo || motivo.trim().length < 5) {
      this.toast.show('El motivo debe tener al menos 5 caracteres', 'warning');
      return;
    }

    this.justificacionService.crear({ asistenciaId: id, motivo: motivo }).subscribe({
      next: () => {
        this.toast.show('Justificación enviada correctamente', 'success');
        this.cerrarJustificarDialog();
        // Recargar el reporte para que se refleje el estado PENDIENTE
        this.generarReporte();
      },
      error: () => {
        this.toast.show('Error al enviar justificación', 'error');
      }
    });
  }
}
