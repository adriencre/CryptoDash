import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent],
  template: `
    <div class="flex min-h-screen bg-[#0b0f1a] text-slate-100">
      <app-sidebar />
      <div class="flex-1 flex flex-col min-w-0">
        <header class="shrink-0 h-14 border-b border-slate-800 bg-slate-900/30 px-6 flex items-center">
          <span class="text-slate-500 text-sm">Espace de trading</span>
        </header>
        <main class="flex-1 min-w-0 p-6 lg:p-8 overflow-auto">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class MainLayoutComponent {}
