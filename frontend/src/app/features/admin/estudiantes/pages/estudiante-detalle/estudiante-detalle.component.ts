import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { PageHeaderComponent } from '../../../../../shared/components/ui/page-header/page-header.component';
import { EstudianteService, EstudianteResponse } from '../../../../../core/services/estudiante.service';
import { environment } from '../../../../../../environments/environment';

@Component({
  selector: 'app-estudiante-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, PageHeaderComponent],
  templateUrl: './estudiante-detalle.component.html',
})
export class EstudianteDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private estudianteService = inject(EstudianteService);

  estudiante = signal<EstudianteResponse | null>(null);
  loading = signal(true);
  error = signal(false);

  // Tab state
  activeTab = signal<'datos' | 'qr'>('datos');
  
  // QR state
  qrUrl = signal<string | null>(null);
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

  cargarEstudiante(id: number): void {
    this.estudianteService.buscarPorId(id).subscribe({
      next: (data) => {
        this.estudiante.set(data);
        this.qrUrl.set(`${environment.apiUrl}/estudiantes/${id}/qr`);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  setTab(tab: 'datos' | 'qr'): void {
    this.activeTab.set(tab);
  }

  descargarQR(): void {
    const url = this.qrUrl();
    if (!url) return;
    
    // Create a temporary anchor to trigger download
    const a = document.createElement('a');
    a.href = url;
    a.download = `QR_${this.estudiante()?.codigo}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  imprimirQR(): void {
    const url = this.qrUrl();
    if (!url) return;

    const win = window.open('');
    if (win) {
      win.document.write(`
        <html>
          <head>
            <title>Imprimir QR - ${this.estudiante()?.nombres}</title>
            <style>
              body { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; font-family: sans-serif; }
              img { width: 300px; height: 300px; }
              .info { margin-top: 20px; text-align: center; }
            </style>
          </head>
          <body>
            <img src="${url}" onload="window.print(); window.close();" />
            <div class="info">
              <h2>${this.estudiante()?.apellidos}, ${this.estudiante()?.nombres}</h2>
              <p>${this.estudiante()?.codigo}</p>
            </div>
          </body>
        </html>
      `);
      win.document.close();
    }
  }

  regenerarQR(): void {
    const est = this.estudiante();
    if (!est) return;

    if (confirm('¿Está seguro de regenerar el código QR? El código anterior dejará de funcionar inmediatamente.')) {
      this.isRegenerating.set(true);
      this.estudianteService.regenerarQr(est.id).subscribe({
        next: () => {
          // Force image reload by appending a timestamp query param
          this.qrUrl.set(`${environment.apiUrl}/estudiantes/${est.id}/qr?t=${new Date().getTime()}`);
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
