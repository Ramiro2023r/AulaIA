import { Injectable, signal } from '@angular/core';

const THEME_KEY = 'aulaia_theme';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  isDarkMode = signal(this.getInitialTheme());

  constructor() {
    this.applyTheme(this.isDarkMode());
  }

  toggleTheme() {
    const isDark = !this.isDarkMode();
    this.isDarkMode.set(isDark);
    this.applyTheme(isDark);
    localStorage.setItem(THEME_KEY, isDark ? 'dark' : 'light');
  }

  private getInitialTheme(): boolean {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored) {
      return stored === 'dark';
    }
    // Check OS preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return true;
    }
    return false;
  }

  private applyTheme(isDark: boolean) {
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }
}
