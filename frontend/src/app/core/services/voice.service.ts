import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class VoiceService {
  private synth: SpeechSynthesis | null = null;
  private voice: SpeechSynthesisVoice | null = null;
  
  // Controles
  enabled = signal(true);
  rate = signal(1.0);
  volume = signal(1.0);
  lang = 'es-PE'; // Español de Perú por defecto

  constructor() {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      this.synth = window.speechSynthesis;
      this.loadVoices();
      
      // Los navegadores cargan las voces de forma asíncrona
      if (this.synth.onvoiceschanged !== undefined) {
        this.synth.onvoiceschanged = () => this.loadVoices();
      }
    }
  }

  private loadVoices() {
    if (!this.synth) return;
    const voices = this.synth.getVoices();
    // Intentar encontrar español de Perú, si no hay, español general, si no hay, la primera disponible.
    this.voice = voices.find(v => v.lang === this.lang) 
              || voices.find(v => v.lang.startsWith('es')) 
              || voices[0] 
              || null;
  }

  isSupported(): boolean {
    return this.synth !== null;
  }

  speak(text: string) {
    if (!this.synth || !this.enabled()) {
      return; // Fallback silencioso si no está soportado o está silenciado
    }

    // Cancelar cualquier audio en reproducción
    this.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    if (this.voice) {
      utterance.voice = this.voice;
    }
    utterance.lang = this.lang;
    utterance.rate = this.rate();
    utterance.volume = this.volume();

    this.synth.speak(utterance);
  }

  cancel() {
    if (this.synth && this.synth.speaking) {
      this.synth.cancel();
    }
  }

  toggleMute() {
    this.enabled.update(v => !v);
    if (!this.enabled()) {
      this.cancel();
    }
  }
}
