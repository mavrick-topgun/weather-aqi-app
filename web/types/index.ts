export interface Location {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  timezone: string;
  createdAt: string;
}

export interface LocationRequest {
  name: string;
  latitude: number;
  longitude: number;
  timezone?: string;
}

export interface WeatherInfo {
  temperatureMax: number | null;
  temperatureMin: number | null;
  precipitation: number | null;
  windSpeed: number | null;
  windDirection: number | null;
  uvIndex: number | null;
}

export interface AqiInfo {
  value: number | null;
  pm25: number | null;
  ozone: number | null;
}

export interface DailyForecast {
  date: string;
  score: number;
  recommendation: Recommendation;
  temperatureMax: number | null;
  temperatureMin: number | null;
  aqi: number | null;
}

export type Recommendation = 'Great' | 'Okay' | 'Caution' | 'Avoid';

export interface ForecastResponse {
  locationId: number;
  locationName: string;
  score: number;
  recommendation: Recommendation;
  reasons: string[];
  weather: WeatherInfo;
  aqi: AqiInfo;
  forecast: DailyForecast[];
}

export interface AqiTrend {
  date: string;
  value: number | null;
}

export interface TemperatureTrend {
  date: string;
  min: number | null;
  max: number | null;
}

export interface ScoreTrend {
  date: string;
  score: number;
  recommendation: Recommendation;
}

export interface TrendsResponse {
  locationId: number;
  locationName: string;
  period: number;
  aqi: AqiTrend[];
  temperature: TemperatureTrend[];
  scores: ScoreTrend[];
}

export interface ApiError {
  code: string;
  message: string;
}

export interface GeocodingResult {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  country: string | null;
  countryCode: string | null;
  admin1: string | null;  // State/Province
  timezone: string | null;
}
