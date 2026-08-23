import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { BrowserQRCodeReader, IScannerControls } from '@zxing/browser';
import { AsistenciaService, MetodoRegistro, RegistrarAsistenciaResponse } from '../../../core/services/asistencia.service';
import { SesionService } from '../../../core/services/sesion.service';
import { SesionClaseResponse } from '../../../core/services/dashboard.service';
import { ToastService } from '../../../shared/services/toast.service';
import { VoiceService } from '../../../core/services/voice.service';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ModalComponent } from '../../../shared/components/ui/modal/modal.component';

type CameraState = 'pending' | 'ready' | 'denied' | 'unavailable';
type ScanResultState = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-modo-aula',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalComponent],
  templateUrl: './modo-aula.component.html',
  styleUrls: ['./modo-aula.component.scss']
})
export class ModoAulaComponent implements OnInit, OnDestroy {
  @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;

  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private asistenciaService = inject(AsistenciaService);
  private sesionService = inject(SesionService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  public voiceService = inject(VoiceService);
  
  // ID de la sesión de la clase actual
  private sesionId: number | null = null;

  // Estados de cámara
  cameraState = signal<CameraState>('pending');
  scanResultState = signal<ScanResultState>('idle');
  lastResult = signal<RegistrarAsistenciaResponse | null>(null);
  errorMessage = signal<string | null>(null);
  sesion = signal<SesionClaseResponse | null>(null);
  
  // Modal de registro manual
  showManualModal = signal(false);
  manualForm = this.fb.group({
    codigo: ['', [Validators.required, Validators.minLength(4)]]
  });

  // ZXing
  private codeReader = new BrowserQRCodeReader();
  private controls: IScannerControls | null = null;
  private isProcessing = false;
  private lastScannedCode: string | null = null;

  ngOnInit(): void {
    // Obtenemos el sesionId desde los query params de la ruta
    this.route.queryParams.subscribe(params => {
      const sesionId = Number(params['sesionId']);
      if (Number.isInteger(sesionId) && sesionId > 0) {
        this.sesionId = sesionId;
        this.cargarContextoSesion(sesionId);
      } else {
        console.warn('No se recibió un sesionId en la ruta.');
      }
    });
  }

  ngAfterViewInit(): void {
    this.startScanner();
  }

  ngOnDestroy(): void {
    this.stopScanner();
  }

  async startScanner(): Promise<void> {
    this.cameraState.set('pending');
    try {
      const videoInputDevices = await BrowserQRCodeReader.listVideoInputDevices();
      
      if (videoInputDevices.length === 0) {
        this.cameraState.set('unavailable');
        return;
      }

      // Usar undefined para que el navegador decida la cámara predeterminada
      // o el primer dispositivo si undefined falla en algunas versiones.
      const selectedDeviceId = undefined;

      this.controls = await this.codeReader.decodeFromVideoDevice(
        selectedDeviceId, 
        this.videoElement.nativeElement, 
        (result, error, controls) => {
          if (result && !this.isProcessing) {
            const code = result.getText();
            if (code !== this.lastScannedCode) {
              this.processCode(code, 'QR');
            }
          }
        }
      );
      this.cameraState.set('ready');
    } catch (err) {
      console.error('Error iniciando cámara:', err);
      this.cameraState.set('denied');
    }
  }

  stopScanner(): void {
    if (this.controls) {
      this.controls.stop();
      this.controls = null;
    }
  }

  retryCamera(): void {
    this.startScanner();
  }

  closeModoAula(): void {
    this.router.navigate(['/docente/dashboard']);
  }

  private cargarContextoSesion(sesionId: number): void {
    this.sesionService.buscarPorId(sesionId).subscribe({
      next: (sesion) => this.sesion.set(sesion),
      error: () => {
        // La asistencia sigue validándose con el sesionId en el backend.
        // Simplemente no se muestran datos que no podamos confirmar.
        this.sesion.set(null);
      }
    });
  }

  // --- Procesamiento ---

  processCode(codigo: string, metodo: MetodoRegistro): void {
    this.isProcessing = true;
    this.lastScannedCode = codigo;
    this.scanResultState.set('loading');
    this.lastResult.set(null);
    this.errorMessage.set(null);

    if (!this.sesionId) {
      this.scanResultState.set('error');
      this.errorMessage.set('Falta el ID de la sesión. Asegúrate de abrir la clase desde el dashboard.');
      this.voiceService.speak('Error. No se encontró una sesión activa.');
      this.resetScanner(3000);
      return;
    }

    this.asistenciaService.registrar({ codigo, metodo, sesionId: this.sesionId }).subscribe({
      next: (res) => {
        this.scanResultState.set('success');
        this.lastResult.set(res);
        
        const nombre = res.nombre?.trim() || 'estudiante';
        if (res.estado === 'PRESENTE') {
          this.voiceService.speak(`¡Hola ${nombre}! Tu asistencia fue registrada correctamente. ¡Que tengas una excelente clase!`);
        } else if (res.estado === 'TARDANZA') {
          this.voiceService.speak(`${nombre}, asistencia registrada con tardanza.`);
        }
        
        this.resetScanner(4000);
      },
      error: (err) => {
        this.scanResultState.set('error');
        const errorCode = err?.error?.code;
        const msg = errorCode === 'STUDENT_NOT_IN_SECTION'
          ? 'Este estudiante no pertenece a la sección de esta clase.'
          : err?.error?.message || 'Error al registrar asistencia';
        this.errorMessage.set(msg);
        
        if (errorCode === 'STUDENT_NOT_IN_SECTION') {
          this.voiceService.speak('Este estudiante no pertenece a la sección de esta clase.');
        } else if (err.status === 409) {
          this.voiceService.speak('Este código ya ha sido registrado.');
        } else if (err.status === 404) {
          this.voiceService.speak('Código no válido o estudiante no encontrado.');
        } else {
          this.voiceService.speak('Ocurrió un error al registrar.');
        }
        
        this.resetScanner(3000);
      }
    });
  }

  resetScanner(delayMs: number = 2000): void {
    // Bloqueo temporal para evitar escanear el mismo código múltiples veces muy rápido y para mostrar la UI
    setTimeout(() => {
      this.isProcessing = false;
      this.scanResultState.set('idle');
      this.lastResult.set(null);
      this.errorMessage.set(null);
      // Limpiar lastScannedCode después de unos segundos para permitir volver a escanearlo si fuera necesario
      setTimeout(() => this.lastScannedCode = null, 3000);
    }, delayMs);
  }

  // --- Manual ---

  openManualModal(): void {
    this.showManualModal.set(true);
    this.manualForm.reset();
  }

  closeManualModal(): void {
    this.showManualModal.set(false);
  }

  submitManual(): void {
    if (this.manualForm.invalid) return;
    const codigo = this.manualForm.value.codigo;
    this.closeManualModal();
    this.processCode(codigo!, 'CODIGO');
  }
}
