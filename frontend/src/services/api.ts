import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

// Create axios instance with defaults
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add auth token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("cloudfs_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("cloudfs_token");
      localStorage.removeItem("cloudfs_user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export default api;

// Auth API
export const authApi = {
  login: async (username: string, password: string) => {
    const response = await api.post("/auth/login", { username, password });
    return response.data;
  },
  register: async (username: string, password: string, role = "USER") => {
    const response = await api.post("/auth/register", {
      username,
      password,
      role,
    });
    return response.data;
  },
  logout: async () => {
    const response = await api.get("/auth/logout");
    return response.data;
  },
  getCurrentUser: async () => {
    const response = await api.get("/auth/me");
    return response.data;
  },
};

// Files API
export const filesApi = {
  list: async () => {
    const response = await api.get("/files");
    return response.data;
  },
  get: async (id: number) => {
    const response = await api.get(`/files/${id}`);
    return response.data;
  },
  create: async (filename: string, content: string) => {
    const response = await api.post("/files/create", { filename, content });
    return response.data;
  },
  upload: async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.post("/files/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },
  delete: async (id: number) => {
    const response = await api.delete(`/files/${id}`);
    return response.data;
  },
  share: async (id: number, username: string, permission = "READ") => {
    const response = await api.post(`/files/${id}/share`, {
      username,
      permission,
    });
    return response.data;
  },
};

// Users API
export const usersApi = {
  list: async () => {
    const response = await api.get("/users");
    return response.data;
  },
  get: async (id: number) => {
    const response = await api.get(`/users/${id}`);
    return response.data;
  },
  promote: async (id: number) => {
    const response = await api.post(`/users/${id}/promote`);
    return response.data;
  },
  demote: async (id: number) => {
    const response = await api.post(`/users/${id}/demote`);
    return response.data;
  },
  delete: async (id: number) => {
    const response = await api.delete(`/users/${id}`);
    return response.data;
  },
};

// Metrics API
export const metricsApi = {
  getDashboard: async () => {
    const response = await api.get("/metrics/dashboard");
    return response.data;
  },
  getSummary: async () => {
    const response = await api.get("/metrics/summary");
    return response.data;
  },
  getML: async () => {
    const response = await api.get("/metrics/ml");
    return response.data;
  },
};
