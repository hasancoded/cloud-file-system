import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import {
  Shield,
  User,
  Files,
  Clock,
  UserPlus,
  Search,
  ChevronUp,
  ChevronDown,
  Trash2,
} from "lucide-react";
import { GlassCard, Button, Badge } from "../components/ui/Card";
import { usersApi } from "../services/api";
import { useAuthStore } from "../stores";
import type { User as UserType } from "../types";

export default function UsersPage() {
  const [users, setUsers] = useState<UserType[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const { user: currentUser } = useAuthStore();

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const data = await usersApi.list();
      setUsers(data);
    } catch (err) {
      setUsers(getMockUsers());
    } finally {
      setLoading(false);
    }
  };

  const handlePromote = async (id: number) => {
    try {
      await usersApi.promote(id);
    } catch (err) {
      // Demo mode
    }
    setUsers((prev) =>
      prev.map((u) => (u.id === id ? { ...u, role: "ADMIN" } : u)),
    );
  };

  const handleDemote = async (id: number) => {
    try {
      await usersApi.demote(id);
    } catch (err) {
      // Demo mode
    }
    setUsers((prev) =>
      prev.map((u) => (u.id === id ? { ...u, role: "USER" } : u)),
    );
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this user?")) return;
    try {
      await usersApi.delete(id);
    } catch (err) {
      // Demo mode
    }
    setUsers((prev) => prev.filter((u) => u.id !== id));
  };

  const filteredUsers = users.filter((user) =>
    user.username.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const getInitials = (username: string) => {
    return username.slice(0, 2).toUpperCase();
  };

  const getAvatarColor = (username: string) => {
    const colors = [
      "from-blue-500 to-blue-600",
      "from-green-500 to-green-600",
      "from-purple-500 to-purple-600",
      "from-amber-500 to-amber-600",
      "from-pink-500 to-pink-600",
      "from-cyan-500 to-cyan-600",
    ];
    return colors[username.charCodeAt(0) % colors.length];
  };

  const timeAgo = (timestamp: string) => {
    if (timestamp === "Never") return "Never";
    const now = new Date();
    const then = new Date(timestamp);
    const diff = Math.floor((now.getTime() - then.getTime()) / 1000);
    if (diff < 60) return "Just now";
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  if (currentUser?.role !== "ADMIN") {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <GlassCard className="text-center max-w-md">
          <Shield className="w-16 h-16 mx-auto text-amber-400 mb-4" />
          <h2 className="text-2xl font-bold text-white mb-2">
            Admin Access Required
          </h2>
          <p className="text-white/60">
            You need administrator privileges to view this page.
          </p>
        </GlassCard>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Users</h1>
          <p className="text-white/60 mt-1">{users.length} registered users</p>
        </div>
        <Button variant="primary">
          <UserPlus className="w-4 h-4 mr-2" />
          Add User
        </Button>
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
        <input
          type="text"
          placeholder="Search users..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full input-field pl-12"
        />
      </div>

      {/* User Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredUsers.map((user, index) => (
          <motion.div
            key={user.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className="glass-card rounded-2xl p-6 hover-lift"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-4">
                <div
                  className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${getAvatarColor(user.username)} flex items-center justify-center text-xl font-bold text-white shadow-lg`}
                >
                  {getInitials(user.username)}
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">
                    {user.username}
                  </h3>
                  <Badge
                    variant={user.role === "ADMIN" ? "warning" : "default"}
                  >
                    {user.role === "ADMIN" ? (
                      <>
                        <Shield className="w-3 h-3 mr-1" />
                        Admin
                      </>
                    ) : (
                      <>
                        <User className="w-3 h-3 mr-1" />
                        User
                      </>
                    )}
                  </Badge>
                </div>
              </div>
            </div>

            <div className="space-y-3 mb-6">
              <div className="flex items-center gap-2 text-white/60">
                <Files className="w-4 h-4" />
                <span className="text-sm">{user.filesOwned} files owned</span>
              </div>
              <div className="flex items-center gap-2 text-white/60">
                <Clock className="w-4 h-4" />
                <span className="text-sm">
                  Active {timeAgo(user.lastLogin)}
                </span>
              </div>
            </div>

            <div className="flex gap-2 pt-4 border-t border-white/10">
              {user.role === "ADMIN" ? (
                <Button
                  variant="secondary"
                  size="sm"
                  className="flex-1"
                  onClick={() => handleDemote(user.id)}
                  disabled={user.username === "admin"}
                >
                  <ChevronDown className="w-4 h-4 mr-1" />
                  Demote
                </Button>
              ) : (
                <Button
                  variant="secondary"
                  size="sm"
                  className="flex-1"
                  onClick={() => handlePromote(user.id)}
                >
                  <ChevronUp className="w-4 h-4 mr-1" />
                  Promote
                </Button>
              )}
              <Button
                variant="danger"
                size="sm"
                onClick={() => handleDelete(user.id)}
                disabled={
                  user.username === "admin" ||
                  user.username === currentUser?.username
                }
              >
                <Trash2 className="w-4 h-4" />
              </Button>
            </div>
          </motion.div>
        ))}
      </div>

      {filteredUsers.length === 0 && !loading && (
        <div className="text-center py-12">
          <User className="w-16 h-16 mx-auto text-white/20 mb-4" />
          <p className="text-white/60">No users found</p>
        </div>
      )}
    </div>
  );
}

function getMockUsers(): UserType[] {
  return [
    {
      id: 1,
      username: "admin",
      role: "ADMIN",
      filesOwned: 45,
      lastLogin: new Date(Date.now() - 3600000).toISOString(),
      createdAt: "2024-01-01",
    },
    {
      id: 2,
      username: "alice",
      role: "ADMIN",
      filesOwned: 128,
      lastLogin: new Date(Date.now() - 7200000).toISOString(),
      createdAt: "2024-01-05",
    },
    {
      id: 3,
      username: "bob",
      role: "USER",
      filesOwned: 32,
      lastLogin: new Date(Date.now() - 86400000).toISOString(),
      createdAt: "2024-01-10",
    },
    {
      id: 4,
      username: "charlie",
      role: "USER",
      filesOwned: 16,
      lastLogin: new Date(Date.now() - 172800000).toISOString(),
      createdAt: "2024-01-15",
    },
    {
      id: 5,
      username: "diana",
      role: "USER",
      filesOwned: 64,
      lastLogin: "Never",
      createdAt: "2024-01-20",
    },
  ];
}
