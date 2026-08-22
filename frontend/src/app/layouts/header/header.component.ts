import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, catchError, filter } from 'rxjs/operators';
import { of } from 'rxjs';
import { ClickOutsideDirective } from '../../shared/directives/click-outside.directive';
import { SearchService, SearchResult } from '../../core/services/search.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, ClickOutsideDirective],
  templateUrl: './header.component.html',
})
export class HeaderComponent {
  @Input() pageTitle: string = '';
  @Output() menuToggled = new EventEmitter<void>();
  private auth = inject(AuthService);
  private router = inject(Router);
  private searchService = inject(SearchService);
  
  showProfileMenu = signal(false);
  
  // Búsqueda
  searchControl = new FormControl('');
  searchResults = signal<SearchResult[]>([]);
  isSearching = signal(false);
  showSearchResults = signal(false);

  ngOnInit(): void {
    this.setupSearch();
  }

  private setupSearch(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter(query => {
        if (!query || query.length < 2) {
          this.searchResults.set([]);
          this.showSearchResults.set(false);
          this.isSearching.set(false);
          return false;
        }
        return true;
      }),
      switchMap(query => {
        this.isSearching.set(true);
        this.showSearchResults.set(true);
        return this.searchService.search(query!).pipe(
          catchError(() => of([]))
        );
      })
    ).subscribe(results => {
      this.searchResults.set(results);
      this.isSearching.set(false);
    });
  }

  closeSearch(): void {
    this.showSearchResults.set(false);
  }

  onResultClick(route: string): void {
    this.closeSearch();
    this.searchControl.setValue('', { emitEvent: false });
    this.router.navigateByUrl(route);
  }

  toggleMenu(): void {
    this.menuToggled.emit();
  }

  toggleProfileMenu(): void {
    this.showProfileMenu.update(v => !v);
  }

  logout(): void {
    this.auth.logout();
  }

  get userName(): string {
    const user = this.auth.currentUser();
    return user ? user.username : 'Usuario';
  }

  get userRole(): string {
    const user = this.auth.currentUser();
    return user ? user.rol : '';
  }
}
