import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { EstudianteService, EstudianteResponse } from '../../../../../core/services/estudiante.service';
import { environment } from '../../../../../../environments/environment';

@Component({
  selector: 'app-estudiante-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, PageHeaderComponent],
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
  activeTab = signal<'datos' | 'qr'>('datos');

  // QR state
  // qrUrl → objectURL para mostrar en <img> (libera memory al destruir)
  // qrBase64 → data URL base64 para imprimir/descargar (funciona cross-window)
  qrUrl = signal<string | null>(null);
  qrBase64 = signal<string | null>(null);
  private qrObjectUrl: string | null = null;
  isRegenerating = signal(false);

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
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
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

  setTab(tab: 'datos' | 'qr'): void {
    this.activeTab.set(tab);
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
}
