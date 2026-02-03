// User types
export interface User {
  id: number;
  username: string;
  role: "ADMIN" | "USER";
  filesOwned: number;
  lastLogin: string;
  createdAt: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface RegisterData extends LoginCredentials {
  role?: "ADMIN" | "USER";
}

export interface AuthResponse {
  success: boolean;
  token?: string;
  username?: string;
  role?: string;
  message?: string;
}

// File types
export interface FileItem {
  id: number;
  filename: string;
  owner: string;
  size: number;
  uploadDate: string;
  modifiedDate: string;
  type: "document" | "image" | "video" | "other";
}

export interface UploadResponse {
  success: boolean;
  fileId?: number;
  filename?: string;
  chunksUploaded?: number;
  message?: string;
}

// Metrics types
export interface DashboardMetrics {
  totalFiles: number;
  activeUsers: number;
  storageUsed: number;
  storageTotal: number;
  systemHealth: "healthy" | "degraded" | "critical";
  containers: ContainerStatus[];
  requestRate: DataPoint[];
  uploadsPerDay: DataPoint[];
  storageByType: StorageByType;
  recentActivity: ActivityItem[];
  loadBalancerStats: LoadBalancerStats;
  mlPrediction: MLPrediction;
}

export interface ContainerStatus {
  name: string;
  status: "running" | "stopped" | "starting";
  load: number;
}

export interface DataPoint {
  time: string;
  date: string;
  count: number;
}

export interface StorageByType {
  documents: number;
  images: number;
  videos: number;
  other: number;
}

export interface ActivityItem {
  user: string;
  action: string;
  details: string;
  time: string;
  icon?: string;
}

export interface LoadBalancerStats {
  totalRequests: number;
  healthyServers: number;
}

export interface MLPrediction {
  currentLoad: number;
  predictedLoad: number;
  confidence: number;
  lowerBound: number;
  upperBound: number;
  shouldScaleUp: boolean;
  shouldScaleDown: boolean;
  mlServiceHealthy: boolean;
  accuracy: number;
  mlEnabled: boolean;
}

// Chart component props
export interface ChartProps {
  data: DataPoint[];
  title?: string;
  color?: string;
  height?: number;
}

// Card component props
export interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: number;
  trendLabel?: string;
  color?: "blue" | "green" | "amber" | "red";
}

// Theme types
export type Theme = "light" | "dark";

export interface ThemeState {
  theme: Theme;
  toggleTheme: () => void;
}
