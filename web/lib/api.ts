import type {
  Location,
  LocationRequest,
  ForecastResponse,
  TrendsResponse,
  ApiError,
  GeocodingResult
} from '@/types';

const API_BASE = '/api';

class ApiClient {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE}${endpoint}`;

    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    if (!response.ok) {
      const error: ApiError = await response.json().catch(() => ({
        code: 'UNKNOWN_ERROR',
        message: 'An unexpected error occurred',
      }));
      throw new Error(error.message);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  // Locations
  async getLocations(): Promise<Location[]> {
    return this.request<Location[]>('/locations');
  }

  async getLocation(id: number): Promise<Location> {
    return this.request<Location>(`/locations/${id}`);
  }

  async createLocation(data: LocationRequest): Promise<Location> {
    return this.request<Location>('/locations', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async deleteLocation(id: number): Promise<void> {
    return this.request<void>(`/locations/${id}`, {
      method: 'DELETE',
    });
  }

  // Forecast & Trends
  async getForecast(locationId: number): Promise<ForecastResponse> {
    return this.request<ForecastResponse>(`/locations/${locationId}/forecast`);
  }

  async getTrends(locationId: number, period: number = 14): Promise<TrendsResponse> {
    return this.request<TrendsResponse>(
      `/locations/${locationId}/trends?period=${period}`
    );
  }

  // Geocoding
  async searchLocations(query: string, limit: number = 5): Promise<GeocodingResult[]> {
    if (!query || query.trim().length < 2) {
      return [];
    }
    return this.request<GeocodingResult[]>(
      `/geocoding/search?query=${encodeURIComponent(query)}&limit=${limit}`
    );
  }
}

export const api = new ApiClient();
