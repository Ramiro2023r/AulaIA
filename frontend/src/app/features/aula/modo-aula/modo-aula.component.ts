import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BrowserQRCodeReader, IScannerControls } from '@zxing/browser';
import { AsistenciaService, MetodoRegistro, AsistenciaResponse } from '../../../core/services/asistencia.service';
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
  private asistenciaService = inject(AsistenciaService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);
  public voiceService = inject(VoiceService);

  // Estados de cámara
  cameraState = signal<CameraState>('pending');
  scanResultState = signal<ScanResultState>('idle');
  lastResult = signal<AsistenciaResponse | null>(null);
  errorMessage = signal<string | null>(null);
  
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
    // We will initialize the camera in ngAfterViewInit or manually.
    // However, it's safer to do it after view init when the video element is bound, 
    // but we can also use a small timeout to let the view render.
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

      // Elige la cámara trasera si está disponible
      const selectedDeviceId = videoInputDevices[videoInputDevices.length - 1].deviceId;

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
    this.router.navigate(['/admin/dashboard']); // O la ruta que corresponda al usuario
  }

  // --- Procesamiento ---

  processCode(codigo: string, metodo: MetodoRegistro): void {
    this.isProcessing = true;
    this.lastScannedCode = codigo;
    this.scanResultState.set('loading');
    this.lastResult.set(null);
    this.errorMessage.set(null);

    this.asistenciaService.registrar({ codigo, metodo }).subscribe({
      next: (res) => {
        this.scanResultState.set('success');
        this.lastResult.set(res);
        
        if (res.estado === 'PRESENTE') {
          this.voiceService.speak(`¡Hola ${res.nombre}! Tu asistencia fue registrada correctamente. ¡Que tengas una excelente clase!`);
        } else if (res.estado === 'TARDANZA') {
          this.voiceService.speak(`${res.nombre}, asistencia registrada con tardanza.`);
        }
        
        this.resetScanner(4000);
      },
      error: (err) => {
        this.scanResultState.set('error');
        const msg = err?.error?.message || 'Error al registrar asistencia';
        this.errorMessage.set(msg);
        
        if (err.status === 409) {
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
