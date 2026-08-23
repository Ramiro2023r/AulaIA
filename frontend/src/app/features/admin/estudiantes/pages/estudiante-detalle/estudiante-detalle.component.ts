import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { ApoderadoDisponible, ApoderadoEstudianteRequest, ApoderadoTelegramOption, AsociarApoderadoRequest, EstudianteService, EstudianteResponse } from '../../../../../core/services/estudiante.service';
import { environment } from '../../../../../../environments/environment';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-estudiante-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, PageHeaderComponent],
  templateUrl: './estudiante-detalle.component.html',
})
export class EstudianteDetalleComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private estudianteService = inject(EstudianteService);
  private http = inject(HttpClient);

  estudiante = signal<EstudianteResponse | null>(null);
  loading = signal(true);
  error = signal(false);

  // Tab state
  activeTab = signal<'datos' | 'apoderados' | 'qr'>('datos');

  // QR state
  // qrUrl → objectURL para mostrar en <img> (libera memory al destruir)
  // qrBase64 → data URL base64 para imprimir/descargar (funciona cross-window)
  qrUrl = signal<string | null>(null);
  qrBase64 = signal<string | null>(null);
  private qrObjectUrl: string | null = null;
  isRegenerating = signal(false);

  // Telegram state
  telegramStatus = signal<'NO_VINCULADO' | 'PENDIENTE' | 'VINCULADO' | 'ERROR'>('NO_VINCULADO');
  telegramQrBase64 = signal<string | null>(null);
  telegramExpiresAt = signal<string | null>(null);
  isGeneratingTelegramQr = signal(false);
  telegramError = signal<string | null>(null);
  apoderadosTelegram = signal<ApoderadoTelegramOption[]>([]);
  apoderadoSeleccionadoId = signal<number | null>(null);
  isSavingApoderado = signal(false);
  apoderadoError = signal<string | null>(null);
  apoderadoExito = signal<string | null>(null);
  modoGestionApoderado = signal<'nuevo' | 'existente' | null>(null);
  busquedaApoderado = signal('');
  apoderadosDisponibles = signal<ApoderadoDisponible[]>([]);
  apoderadoExistenteSeleccionadoId = signal<number | null>(null);
  isSearchingApoderados = signal(false);
  isAssociatingApoderado = signal(false);
  relacionApoderadoExistente: AsociarApoderadoRequest = {
    parentesco: 'MADRE',
    principal: false,
  };
  nuevoApoderado: ApoderadoEstudianteRequest = {
    nombres: '',
    apellidos: '',
    telefono: '',
    parentesco: 'MADRE',
    principal: false,
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.cargarEstudiante(Number(id));
    } else {
      this.error.set(true);
      this.loading.set(false);
    }
  }

  ngOnDestroy(): void {
    if (this.qrObjectUrl) URL.revokeObjectURL(this.qrObjectUrl);
  }

  cargarEstudiante(id: number): void {
    this.estudianteService.buscarPorId(id).subscribe({
      next: (data) => {
        this.estudiante.set(data);
        this.loading.set(false);
        this.cargarQrBlob(id);
        this.cargarApoderadosTelegram(id);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  cargarApoderadosTelegram(estudianteId: number): void {
    this.estudianteService.listarApoderadosParaTelegram(estudianteId).subscribe({
      next: (apoderados) => {
        this.apoderadosTelegram.set(apoderados);
        const seleccionado = apoderados.find(apoderado => apoderado.activo) ?? null;
        this.apoderadoSeleccionadoId.set(seleccionado?.id ?? null);
        this.actualizarEstadoTelegram(seleccionado);
      },
      error: () => {
        this.apoderadosTelegram.set([]);
        this.apoderadoSeleccionadoId.set(null);
        this.telegramError.set('No se pudieron cargar los apoderados del estudiante.');
      }
    });
  }

  seleccionarApoderado(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.apoderadoSeleccionadoId.set(value ? Number(value) : null);
    this.telegramError.set(null);
    const seleccionado = this.apoderadosTelegram().find(apoderado => apoderado.id === this.apoderadoSeleccionadoId()) ?? null;
    this.actualizarEstadoTelegram(seleccionado);
  }

  cargarQrBlob(id: number, nocache = false): void {
    const url = `${environment.apiUrl}/estudiantes/${id}/qr${nocache ? '?t=' + Date.now() : ''}`;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        // ObjectURL para mostrar en pantalla (eficiente)
        if (this.qrObjectUrl) URL.revokeObjectURL(this.qrObjectUrl);
        this.qrObjectUrl = URL.createObjectURL(blob);
        this.qrUrl.set(this.qrObjectUrl);

        // Base64 para imprimir y descargar (funciona en ventanas nuevas)
        const reader = new FileReader();
        reader.onloadend = () => this.qrBase64.set(reader.result as string);
        reader.readAsDataURL(blob);
      },
      error: () => {
        this.qrUrl.set(null);
        this.qrBase64.set(null);
      }
    });
  }

  setTab(tab: 'datos' | 'apoderados' | 'qr'): void {
    this.activeTab.set(tab);
  }

  abrirRegistroApoderado(): void {
    this.modoGestionApoderado.set('nuevo');
    this.limpiarMensajesApoderado();
  }

  abrirBusquedaApoderado(): void {
    this.modoGestionApoderado.set('existente');
    this.busquedaApoderado.set('');
    this.apoderadosDisponibles.set([]);
    this.apoderadoExistenteSeleccionadoId.set(null);
    this.relacionApoderadoExistente = { parentesco: 'MADRE', principal: false };
    this.limpiarMensajesApoderado();
  }

  cerrarGestionApoderado(): void {
    this.modoGestionApoderado.set(null);
    this.apoderadosDisponibles.set([]);
    this.apoderadoExistenteSeleccionadoId.set(null);
    this.limpiarMensajesApoderado();
  }

  buscarApoderadosExistentes(): void {
    const est = this.estudiante();
    if (!est || this.isSearchingApoderados()) return;

    this.isSearchingApoderados.set(true);
    this.apoderadoError.set(null);
    this.apoderadoExito.set(null);
    this.estudianteService.buscarApoderadosDisponibles(est.id, this.busquedaApoderado()).subscribe({
      next: (apoderados) => {
        this.apoderadosDisponibles.set(apoderados);
        this.apoderadoExistenteSeleccionadoId.set(null);
        this.isSearchingApoderados.set(false);
      },
      error: () => {
        this.apoderadosDisponibles.set([]);
        this.isSearchingApoderados.set(false);
        this.apoderadoError.set('No se pudieron buscar apoderados existentes. Inténtalo nuevamente.');
      }
    });
  }

  seleccionarApoderadoExistente(id: number): void {
    this.apoderadoExistenteSeleccionadoId.set(id);
    this.apoderadoError.set(null);
  }

  asociarApoderadoExistente(): void {
    const est = this.estudiante();
    const apoderadoId = this.apoderadoExistenteSeleccionadoId();
    if (!est || apoderadoId === null || this.isAssociatingApoderado()) {
      if (apoderadoId === null) {
        this.apoderadoError.set('Selecciona un apoderado para asociarlo.');
      }
      return;
    }

    this.isAssociatingApoderado.set(true);
    this.apoderadoError.set(null);
    this.apoderadoExito.set(null);
    this.estudianteService.asociarApoderadoExistente(est.id, apoderadoId, this.relacionApoderadoExistente).subscribe({
      next: (apoderado) => {
        this.apoderadosTelegram.update(actuales => [...actuales, apoderado]);
        if (this.apoderadoSeleccionadoId() === null && apoderado.activo) {
          this.apoderadoSeleccionadoId.set(apoderado.id);
        }
        this.isAssociatingApoderado.set(false);
        this.modoGestionApoderado.set(null);
        this.apoderadosDisponibles.set([]);
        this.apoderadoExistenteSeleccionadoId.set(null);
        this.apoderadoExito.set('Apoderado existente asociado correctamente.');
        this.actualizarEstadoTelegram(apoderado);
      },
      error: (err) => {
        this.isAssociatingApoderado.set(false);
        if (err.status === 403) {
          this.apoderadoError.set('Solo un administrador puede asociar apoderados.');
        } else if (err.error?.code === 'PARENT_ALREADY_ASSOCIATED') {
          this.apoderadoError.set('Este apoderado ya está asociado al estudiante.');
        } else if (err.error?.code === 'PARENT_INACTIVE') {
          this.apoderadoError.set('El apoderado seleccionado está inactivo.');
        } else {
          this.apoderadoError.set('No se pudo asociar el apoderado. Inténtalo nuevamente.');
        }
      }
    });
  }

  guardarApoderado(): void {
    const est = this.estudiante();
    if (!est || this.isSavingApoderado()) return;

    const nombres = this.nuevoApoderado.nombres.trim();
    const apellidos = this.nuevoApoderado.apellidos.trim();
    if (!nombres || !apellidos) {
      this.apoderadoError.set('Completa los nombres y apellidos del apoderado.');
      return;
    }

    this.isSavingApoderado.set(true);
    this.apoderadoError.set(null);
    this.apoderadoExito.set(null);
    this.estudianteService.crearApoderado(est.id, {
      nombres,
      apellidos,
      telefono: (this.nuevoApoderado.telefono ?? '').trim() || null,
      parentesco: this.nuevoApoderado.parentesco,
      principal: this.nuevoApoderado.principal,
    }).subscribe({
      next: (apoderado) => {
        this.apoderadosTelegram.update(actuales => [...actuales, apoderado]);
        if (this.apoderadoSeleccionadoId() === null && apoderado.activo) {
          this.apoderadoSeleccionadoId.set(apoderado.id);
        }
        this.nuevoApoderado = {
          nombres: '', apellidos: '', telefono: '', parentesco: 'MADRE', principal: false,
        };
        this.apoderadoExito.set('Apoderado registrado y asociado correctamente.');
        this.isSavingApoderado.set(false);
      },
      error: (err) => {
        this.isSavingApoderado.set(false);
        this.apoderadoError.set(err.status === 403
          ? 'Solo un administrador puede registrar apoderados.'
          : 'No se pudo registrar el apoderado. Inténtalo nuevamente.');
      }
    });
  }

  private limpiarMensajesApoderado(): void {
    this.apoderadoError.set(null);
    this.apoderadoExito.set(null);
  }

  private actualizarEstadoTelegram(apoderado: ApoderadoTelegramOption | null): void {
    this.telegramQrBase64.set(null);
    this.telegramExpiresAt.set(null);
    this.telegramStatus.set(apoderado?.telegramVinculado ? 'VINCULADO' : 'NO_VINCULADO');
  }

  descargarQR(): void {
    const base64 = this.qrBase64();
    if (!base64) return;

    const a = document.createElement('a');
    a.href = base64;
    a.download = `QR_${this.estudiante()?.codigo ?? 'estudiante'}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  imprimirQR(): void {
    const base64 = this.qrBase64();
    if (!base64) return;

    const est = this.estudiante();
    const nombre = est ? `${est.apellidos}, ${est.nombres}` : '';
    const codigo = est?.codigo ?? '';

    const win = window.open('', '_blank');
    if (!win) {
      alert('Tu navegador bloqueó la ventana emergente. Permite popups para este sitio e inténtalo de nuevo.');
      return;
    }

    win.document.write(`
      <!DOCTYPE html>
      <html lang="es">
        <head>
          <meta charset="UTF-8">
          <title>QR - ${nombre}</title>
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 100vh;
              font-family: 'Segoe UI', Arial, sans-serif;
              background: #fff;
            }
            .card {
              border: 2px solid #e0e0e0;
              border-radius: 16px;
              padding: 32px;
              text-align: center;
              max-width: 320px;
            }
            .school-name {
              font-size: 13px;
              font-weight: 600;
              color: #555;
              letter-spacing: 1px;
              text-transform: uppercase;
              margin-bottom: 16px;
            }
            img {
              width: 260px;
              height: 260px;
              display: block;
              margin: 0 auto;
            }
            .divider {
              border: none;
              border-top: 1px solid #e0e0e0;
              margin: 16px 0;
            }
            .student-name {
              font-size: 18px;
              font-weight: 700;
              color: #1a1a1a;
              margin-bottom: 4px;
            }
            .student-code {
              font-size: 13px;
              color: #777;
              font-family: monospace;
              letter-spacing: 1px;
            }
            .instructions {
              font-size: 11px;
              color: #999;
              margin-top: 12px;
              line-height: 1.4;
            }
            @media print {
              body { background: #fff; }
            }
          </style>
        </head>
        <body>
          <div class="card">
            <p class="school-name">AulaIA — Código de Asistencia</p>
            <img src="${base64}" alt="QR ${codigo}" />
            <hr class="divider" />
            <p class="student-name">${nombre}</p>
            <p class="student-code">${codigo}</p>
            <p class="instructions">Presenta este código al ingresar al aula<br>para registrar tu asistencia automáticamente.</p>
          </div>
          <script>
            window.onload = function() {
              setTimeout(function() { window.print(); }, 300);
            };
          </script>
        </body>
      </html>
    `);
    win.document.close();
  }

  regenerarQR(): void {
    const est = this.estudiante();
    if (!est) return;

    if (confirm('¿Está seguro de regenerar el código QR? El código anterior dejará de funcionar inmediatamente.')) {
      this.isRegenerating.set(true);
      this.estudianteService.regenerarQr(est.id).subscribe({
        next: () => {
          this.cargarQrBlob(est.id, true);
          this.isRegenerating.set(false);
          alert('Código QR regenerado exitosamente.');
        },
        error: () => {
          this.isRegenerating.set(false);
          alert('Ocurrió un error al regenerar el código QR.');
        }
      });
    }
  }

  generarQrTelegram(): void {
    const est = this.estudiante();
    const apoderadoId = this.apoderadoSeleccionadoId();
    if (!est) return;
    if (apoderadoId === null) {
      this.telegramError.set(this.apoderadosTelegram().length === 0
        ? 'No hay apoderados registrados para este estudiante.'
        : 'Selecciona un apoderado activo antes de generar el QR.');
      return;
    }

    this.isGeneratingTelegramQr.set(true);
    this.telegramError.set(null);

    this.estudianteService.generarVinculacionTelegram(est.id, apoderadoId).subscribe({
      next: async (res) => {
        try {
          const qrDataUrl = await QRCode.toDataURL(res.telegramUrl, { width: 260, margin: 2 });
          this.telegramQrBase64.set(qrDataUrl);
          this.telegramStatus.set('PENDIENTE');
          this.telegramExpiresAt.set(res.expiresAt);
        } catch (err) {
          console.error('Error generando QR de Telegram', err);
          this.telegramError.set('Ocurrió un error al generar la imagen del QR.');
        } finally {
          this.isGeneratingTelegramQr.set(false);
        }
      },
      error: (err) => {
        this.isGeneratingTelegramQr.set(false);
        if (err.status === 409) {
          this.telegramError.set('La integración con Telegram está deshabilitada.');
        } else if (err.error?.code === 'TELEGRAM_APODERADO_REQUIRED') {
          this.telegramError.set('Selecciona un apoderado antes de generar el QR.');
        } else if (err.error?.code === 'TELEGRAM_APODERADO_INACTIVE') {
          this.telegramError.set('El apoderado seleccionado está inactivo.');
        } else if (err.status === 400 && err.error?.message?.includes('configurado')) {
          this.telegramError.set('El bot de Telegram no está configurado correctamente.');
        } else {
          this.telegramError.set('Ocurrió un error de red al intentar generar la vinculación.');
        }
      }
    });
  }

  descargarQrTelegram(): void {
    const base64 = this.telegramQrBase64();
    if (!base64) return;
    const a = document.createElement('a');
    a.href = base64;
    a.download = `Telegram_QR_${this.estudiante()?.codigo ?? 'estudiante'}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }
}
