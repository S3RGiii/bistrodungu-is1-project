import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RestaurantTable {
  id?: number;
  tableNumber: number;
  capacity: number;
  status?: 'AVAILABLE' | 'OCCUPIED' | 'RESERVED';
}

@Injectable({
  providedIn: 'root',
})
export class TableService {
  private apiUrl = `${environment.apiUrl}/tables`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<RestaurantTable[]> {
    return this.http.get<RestaurantTable[]>(this.apiUrl);
  }

  getAvailable(): Observable<RestaurantTable[]> {
    return this.http.get<RestaurantTable[]>(`${this.apiUrl}/available`);
  }

  getById(id: number): Observable<RestaurantTable> {
    return this.http.get<RestaurantTable>(`${this.apiUrl}/${id}`);
  }

  create(table: RestaurantTable): Observable<RestaurantTable> {
    return this.http.post<RestaurantTable>(this.apiUrl, table);
  }

  update(id: number, table: RestaurantTable): Observable<RestaurantTable> {
    return this.http.put<RestaurantTable>(`${this.apiUrl}/${id}`, table);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
