import { create } from "zustand";
import { persist } from "zustand/middleware";
import { authApi } from "../services/api";
import type { User, AuthState } from "../types";

interface AuthStore extends AuthState {
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => void;
  register: (
    username: string,
    password: string,
    role?: string,
  ) => Promise<boolean>;
  checkAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      isAuthenticated: false,
      user: null,
      token: null,
      loading: false,
      error: null,

      login: async (username: string, password: string) => {
        set({ loading: true, error: null });
        try {
          const response = await authApi.login(username, password);
          if (response.success) {
            const user: User = {
              id: 0,
              username: response.username,
              role: response.role as "ADMIN" | "USER",
              filesOwned: 0,
              lastLogin: new Date().toISOString(),
              createdAt: new Date().toISOString(),
            };
            localStorage.setItem("cloudfs_token", response.token);
            set({
              isAuthenticated: true,
              user,
              token: response.token,
              loading: false,
            });
            return true;
          }
          set({ error: response.message || "Login failed", loading: false });
          return false;
        } catch (error: unknown) {
          // Demo mode fallback when backend is unavailable
          if (username === "admin" && password === "admin") {
            const demoToken = "demo_token_" + Date.now();
            const user: User = {
              id: 1,
              username: "admin",
              role: "ADMIN",
              filesOwned: 12,
              lastLogin: new Date().toISOString(),
              createdAt: "2024-01-01T00:00:00Z",
            };
            localStorage.setItem("cloudfs_token", demoToken);
            set({
              isAuthenticated: true,
              user,
              token: demoToken,
              loading: false,
            });
            return true;
          }
          const err = error as { response?: { data?: { message?: string } } };
          set({
            error:
              err.response?.data?.message ||
              "Login failed - Backend not running. Use admin/admin for demo.",
            loading: false,
          });
          return false;
        }
      },

      logout: () => {
        localStorage.removeItem("cloudfs_token");
        localStorage.removeItem("cloudfs_user");
        set({
          isAuthenticated: false,
          user: null,
          token: null,
        });
      },

      register: async (username: string, password: string, role = "USER") => {
        set({ loading: true, error: null });
        try {
          const response = await authApi.register(username, password, role);
          set({ loading: false });
          return response.success;
        } catch (error: unknown) {
          const err = error as { response?: { data?: { message?: string } } };
          set({
            error: err.response?.data?.message || "Registration failed",
            loading: false,
          });
          return false;
        }
      },

      checkAuth: async () => {
        const token = localStorage.getItem("cloudfs_token");
        if (!token) {
          set({ isAuthenticated: false, user: null, token: null });
          return;
        }
        try {
          const response = await authApi.getCurrentUser();
          if (response.success) {
            set({
              isAuthenticated: true,
              user: {
                id: 0,
                username: response.username,
                role: response.role as "ADMIN" | "USER",
                filesOwned: 0,
                lastLogin: new Date().toISOString(),
                createdAt: new Date().toISOString(),
              },
              token,
            });
          }
        } catch {
          set({ isAuthenticated: false, user: null, token: null });
          localStorage.removeItem("cloudfs_token");
        }
      },
    }),
    {
      name: "cloudfs_auth",
      partialize: (state) => ({
        isAuthenticated: state.isAuthenticated,
        user: state.user,
        token: state.token,
      }),
    },
  ),
);

// Theme store
interface ThemeStore {
  theme: "light" | "dark";
  toggleTheme: () => void;
}

export const useThemeStore = create<ThemeStore>()(
  persist(
    (set, get) => ({
      theme: "dark",
      toggleTheme: () => {
        const newTheme = get().theme === "dark" ? "light" : "dark";
        if (newTheme === "dark") {
          document.documentElement.classList.add("dark");
        } else {
          document.documentElement.classList.remove("dark");
        }
        set({ theme: newTheme });
      },
    }),
    {
      name: "cloudfs_theme",
    },
  ),
);
