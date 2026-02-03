import { useState } from "react";
import { motion } from "framer-motion";
import {
  Moon,
  Sun,
  Bell,
  RefreshCw,
  User,
  Shield,
  Palette,
} from "lucide-react";
import { GlassCard, Button } from "../components/ui/Card";
import { useThemeStore, useAuthStore } from "../stores";

export default function SettingsPage() {
  const { theme, toggleTheme } = useThemeStore();
  const { user } = useAuthStore();
  const [refreshInterval, setRefreshInterval] = useState(10);
  const [notifications, setNotifications] = useState(true);

  return (
    <div className="space-y-8 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-white">Settings</h1>
        <p className="text-white/60 mt-1">
          Customize your dashboard experience
        </p>
      </div>

      {/* Profile */}
      <GlassCard>
        <div className="flex items-center gap-2 mb-6">
          <User className="w-5 h-5 text-primary-400" />
          <h2 className="text-xl font-semibold text-white">Profile</h2>
        </div>

        <div className="flex items-center gap-6">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-500 to-purple-500 flex items-center justify-center text-2xl font-bold text-white">
            {user?.username.slice(0, 2).toUpperCase()}
          </div>
          <div className="flex-1">
            <h3 className="text-xl font-semibold text-white">
              {user?.username}
            </h3>
            <div className="flex items-center gap-2 mt-1">
              <Shield className="w-4 h-4 text-amber-400" />
              <span className="text-white/60">{user?.role}</span>
            </div>
          </div>
          <Button variant="secondary">Edit Profile</Button>
        </div>
      </GlassCard>

      {/* Appearance */}
      <GlassCard>
        <div className="flex items-center gap-2 mb-6">
          <Palette className="w-5 h-5 text-primary-400" />
          <h2 className="text-xl font-semibold text-white">Appearance</h2>
        </div>

        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-white font-medium">Dark Mode</h3>
              <p className="text-sm text-white/60">
                Toggle between light and dark themes
              </p>
            </div>
            <motion.button
              onClick={toggleTheme}
              className={`
                w-16 h-9 rounded-full p-1 transition-colors duration-300
                ${theme === "dark" ? "bg-primary-500" : "bg-white/20"}
              `}
            >
              <motion.div
                animate={{ x: theme === "dark" ? 28 : 0 }}
                className="w-7 h-7 rounded-full bg-white flex items-center justify-center shadow-lg"
              >
                {theme === "dark" ? (
                  <Moon className="w-4 h-4 text-primary-500" />
                ) : (
                  <Sun className="w-4 h-4 text-amber-500" />
                )}
              </motion.div>
            </motion.button>
          </div>
        </div>
      </GlassCard>

      {/* Dashboard Settings */}
      <GlassCard>
        <div className="flex items-center gap-2 mb-6">
          <RefreshCw className="w-5 h-5 text-primary-400" />
          <h2 className="text-xl font-semibold text-white">Dashboard</h2>
        </div>

        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-white font-medium">Auto-Refresh Interval</h3>
              <p className="text-sm text-white/60">
                How often to refresh metrics data
              </p>
            </div>
            <select
              value={refreshInterval}
              onChange={(e) => setRefreshInterval(Number(e.target.value))}
              className="input-field w-32"
            >
              <option value={5}>5 seconds</option>
              <option value={10}>10 seconds</option>
              <option value={30}>30 seconds</option>
              <option value={60}>1 minute</option>
            </select>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-white font-medium">Notifications</h3>
              <p className="text-sm text-white/60">
                Receive alerts for system events
              </p>
            </div>
            <motion.button
              onClick={() => setNotifications(!notifications)}
              className={`
                w-16 h-9 rounded-full p-1 transition-colors duration-300
                ${notifications ? "bg-green-500" : "bg-white/20"}
              `}
            >
              <motion.div
                animate={{ x: notifications ? 28 : 0 }}
                className="w-7 h-7 rounded-full bg-white flex items-center justify-center shadow-lg"
              >
                <Bell
                  className={`w-4 h-4 ${notifications ? "text-green-500" : "text-gray-400"}`}
                />
              </motion.div>
            </motion.button>
          </div>
        </div>
      </GlassCard>

      {/* About */}
      <GlassCard>
        <h2 className="text-xl font-semibold text-white mb-4">About CloudFS</h2>
        <div className="space-y-2 text-white/60">
          <p>
            <span className="text-white">Version:</span> 1.0.0
          </p>
          <p>
            <span className="text-white">Backend:</span> Java 17 + Spring Boot
          </p>
          <p>
            <span className="text-white">Frontend:</span> React 18 + TypeScript
            + Tailwind CSS
          </p>
          <p>
            <span className="text-white">ML Model:</span> Random Forest
            (scikit-learn)
          </p>
          <p>
            <span className="text-white">Database:</span> MySQL
          </p>
        </div>
        <div className="mt-4 pt-4 border-t border-white/10">
          <p className="text-sm text-white/40">
            A portfolio-grade distributed file system with ML-powered
            auto-scaling.
          </p>
        </div>
      </GlassCard>
    </div>
  );
}
