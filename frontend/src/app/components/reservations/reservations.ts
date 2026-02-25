import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReservationService, Reservation } from '../../services/reservation';

@Component({
  selector: 'app-reservations',
  imports: [CommonModule],
  templateUrl: './reservations.html',
  styleUrl: './reservations.scss',
})
export class ReservationsComponent implements OnInit {
  reservations: Reservation[] = [];
  loading = false;
  error = '';

  constructor(private reservationService: ReservationService) {}

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading = true;
    this.reservationService.getAll().subscribe({
      next: (data) => {
        this.reservations = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar reservaciones: ' + err.message;
        this.loading = false;
      }
    });
  }
}
