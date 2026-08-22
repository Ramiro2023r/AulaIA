import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageHeaderComponent } from '../../../shared/components/ui/page-header/page-header.component';
import { CardComponent } from '../../../shared/components/ui/card/card.component';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-ajustes',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, CardComponent],
  template: `
    <app-page-header title="Configuración">
      <span class="text-sm text-on-surface-variant">Preferencias y ajustes del sistema</span>
    </app-page-header>

    <div class="grid gap-6 mt-6">
      <!-- Perfil de usuario -->
      <app-card class="p-6">
        <h3 class="font-headline-sm text-headline-sm mb-4">Mi Perfil</h3>
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <label class="block text-sm font-medium text-on-surface-variant mb-1">Usuario</label>
            <p class="text-on-surface font-mono bg-surface-container px-2 py-1 rounded inline-block">{{ auth.currentUser()?.username }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-on-surface-variant mb-1">Rol de Acceso</label>
            <p class="text-on-surface">
              <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    [ngClass]="auth.currentUser()?.rol === 'ADMIN' ? 'bg-primary/20 text-primary' : 'bg-success/20 text-success'">
                {{ auth.currentUser()?.rol }}
              </span>
            </p>
          </div>
        </div>
      </app-card>

      <!-- Inteligencia Artificial -->
      <app-card class="p-6">
        <h3 class="font-headline-sm text-headline-sm mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">auto_awesome</span>
          Configuración de AulaIA
        </h3>
        <p class="text-sm text-on-surface-variant mb-4">
          Configura el proveedor y las credenciales del Asistente IA. Estas claves se guardan localmente en tu navegador de forma segura.
        </p>
        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="md:col-span-1">
              <label class="block text-sm font-medium text-on-surface-variant mb-1">Proveedor de IA</label>
              <select [ngModel]="aiProvider()" (ngModelChange)="aiProvider.set($event)" class="input-text w-full">
                <option value="gemini">Google Gemini</option>
                <option value="claude">Anthropic Claude</option>
                <option value="groq">Groq (Rápida/Gratuita)</option>
                <option value="codex">OpenAI Codex</option>
              </select>
            </div>
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-on-surface-variant mb-1">API Key</label>
              <div class="flex gap-2">
                <input type="password" [ngModel]="aiKey()" (ngModelChange)="aiKey.set($event)" class="input-text flex-1" placeholder="Pega aquí tu API Key...">
                <button class="btn btn-primary" (click)="saveAiConfig()">Guardar</button>
              </div>
              <p *ngIf="aiSaved()" class="text-success text-xs mt-2 transition-opacity animate-fade-in">Configuración guardada en este navegador.</p>
            </div>
          </div>
        </div>
      </app-card>

      <!-- Preferencias -->
      <app-card class="p-6">
        <h3 class="font-headline-sm text-headline-sm mb-4">Preferencias Visuales</h3>
        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-[20px]">dark_mode</span> Modo oscuro
              </p>
              <p class="text-sm text-on-surface-variant">Activar tema oscuro en toda la aplicación</p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" class="sr-only peer" [checked]="theme.isDarkMode()" (change)="theme.toggleTheme()">
              <div class="w-11 h-6 bg-outline-variant peer-focus:ring-2 peer-focus:ring-primary rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
            </label>
          </div>
        </div>
      </app-card>

      <!-- Cambiar contraseña -->
      <app-card class="p-6">
        <h3 class="font-headline-sm text-headline-sm mb-4">Seguridad</h3>
        <p class="text-sm text-on-surface-variant mb-4">Cambiar tu contraseña de acceso.</p>
        <button class="btn btn-secondary text-sm">
          <span class="material-symbols-outlined mr-2 text-[18px]">lock_reset</span>
          Cambiar contraseña
        </button>
      </app-card>
    </div>
  `
})
export class AjustesComponent {
  auth = inject(AuthService);
  theme = inject(ThemeService);

  aiProvider = signal(localStorage.getItem('aulaia_ai_provider') || 'gemini');
  aiKey = signal(localStorage.getItem('aulaia_ai_key') || '');
  aiSaved = signal(false);

  saveAiConfig() {
    localStorage.setItem('aulaia_ai_provider', this.aiProvider());
    localStorage.setItem('aulaia_ai_key', this.aiKey());
    this.aiSaved.set(true);
    setTimeout(() => this.aiSaved.set(false), 3000);
  }
}