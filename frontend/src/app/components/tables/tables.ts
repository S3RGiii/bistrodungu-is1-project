import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableService, RestaurantTable } from '../../services/table';

@Component({
  selector: 'app-tables',
  imports: [CommonModule],
  templateUrl: './tables.html',
  styleUrl: './tables.scss',
})
export class TablesComponent implements OnInit {
  tables: RestaurantTable[] = [];
  loading = false;
  error = '';

  constructor(private tableService: TableService) {}

  ngOnInit(): void {
    this.loadTables();
  }

  loadTables(): void {
    this.loading = true;
    this.tableService.getAll().subscribe({
      next: (data) => {
        this.tables = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar mesas: ' + err.message;
        this.loading = false;
      }
    });
  }
}
