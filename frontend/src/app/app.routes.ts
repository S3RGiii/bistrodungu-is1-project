import { Routes } from '@angular/router';
import { ReservationsComponent } from './components/reservations/reservations';
import { TablesComponent } from './components/tables/tables';
import { OrdersComponent } from './components/orders/orders';

export const routes: Routes = [
  { path: '', redirectTo: '/reservations', pathMatch: 'full' },
  { path: 'reservations', component: ReservationsComponent },
  { path: 'tables', component: TablesComponent },
  { path: 'orders', component: OrdersComponent },
  { path: '**', redirectTo: '/reservations' }
];
