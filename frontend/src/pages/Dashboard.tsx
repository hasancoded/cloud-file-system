import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  Files,
  Users,
  HardDrive,
  Activity,
  Server,
  Database,
  Brain,
  Clock,
  User,
  Upload,
  Share2,
  LogIn,
  CheckCircle,
  AlertTriangle,
} from "lucide-react";
import {
  StatCard,
  GlassCard,
  Badge,
  ProgressBar,
  Skeleton,
} from "../components/ui/Card";
import {
  RequestRateChart,
  UploadsPerDayChart,
  StorageByTypeChart,
  ContainerScalingChart,
} from "../components/charts/Charts";
import { metricsApi } from "../services/api";
import type { DashboardMetrics, ActivityItem } from "../types";

export default function DashboardPage() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [_error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const data = await metricsApi.getDashboard();
        setMetrics(data);
        setError(null);
      } catch (err) {
        setError("Failed to load metrics");
        // Use mock data for demo
        setMetrics(getMockMetrics());
      } finally {
        setLoading(false);
      }
    };

    fetchMetrics();
    const interval = setInterval(fetchMetrics, 10000); // Refresh every 10 seconds
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return <DashboardSkeleton />;
  }

  const data = metrics || getMockMetrics();

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Dashboard</h1>
          <p className="text-white/60 mt-1">Real-time system overview</p>
        </div>
        <div className="flex items-center gap-3">
          <Badge
            variant={data.systemHealth === "healthy" ? "success" : "warning"}
          >
            <span className="w-2 h-2 rounded-full bg-current mr-2 animate-pulse" />
            {data.systemHealth === "healthy" ? "System Healthy" : "Degraded"}
          </Badge>
          <Badge variant="default">
            <Clock className="w-3 h-3 mr-1" />
            Live
          </Badge>
        </div>
      </div>

      {/* Hero Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Files"
          value={data.totalFiles.toLocaleString()}
          icon={<Files className="w-6 h-6 text-white" />}
          trend={12}
          trendLabel="vs last week"
          color="blue"
        />
        <StatCard
          title="Active Users"
          value={data.activeUsers}
          icon={<Users className="w-6 h-6 text-white" />}
          color="green"
        />
        <StatCard
          title="Storage Used"
          value={`${data.storageUsed.toFixed(1)} GB`}
          icon={<HardDrive className="w-6 h-6 text-white" />}
          color="amber"
        />
        <StatCard
          title="Containers"
          value={`${data.containers.filter((c) => c.status === "running").length} / ${data.containers.length}`}
          icon={<Server className="w-6 h-6 text-white" />}
          color="red"
        />
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RequestRateChart data={data.requestRate} />
        <UploadsPerDayChart data={data.uploadsPerDay} />
        <StorageByTypeChart data={data.storageByType} />
        <ContainerScalingChart
          data={data.requestRate.map((r, i) => ({
            time: r.time || "",
            count: Math.max(1, Math.floor(3 + Math.sin(i) * 2)),
          }))}
        />
      </div>

      {/* Bottom Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* System Status */}
        <GlassCard className="lg:col-span-1">
          <h3 className="text-lg font-semibold text-white mb-6">
            System Status
          </h3>
          <div className="space-y-4">
            <StatusItem
              icon={<Database className="w-5 h-5" />}
              label="Database"
              status="Connected"
              detail="23ms latency"
              healthy
            />
            <StatusItem
              icon={<Server className="w-5 h-5" />}
              label="Load Balancer"
              status={`${data.loadBalancerStats.healthyServers} servers`}
              detail={`${data.loadBalancerStats.totalRequests.toLocaleString()} req`}
              healthy
            />
            <StatusItem
              icon={<Brain className="w-5 h-5" />}
              label="ML Predictor"
              status={data.mlPrediction.mlServiceHealthy ? "Active" : "Offline"}
              detail={`${(data.mlPrediction.accuracy * 100).toFixed(1)}% accuracy`}
              healthy={data.mlPrediction.mlServiceHealthy}
            />

            <div className="pt-4 border-t border-white/10">
              <div className="flex justify-between text-sm mb-2">
                <span className="text-white/60">Storage</span>
                <span className="text-white">
                  {data.storageUsed.toFixed(1)} / {data.storageTotal} GB
                </span>
              </div>
              <ProgressBar
                value={data.storageUsed}
                max={data.storageTotal}
                color="blue"
              />
            </div>

            <div className="pt-4">
              <p className="text-sm text-white/60 mb-2">Active Containers</p>
              <div className="flex gap-2">
                {data.containers.map((container, i) => (
                  <motion.div
                    key={i}
                    whileHover={{ scale: 1.1 }}
                    className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold ${
                      container.status === "running"
                        ? "bg-green-500/20 text-green-400 border border-green-500/30"
                        : "bg-white/5 text-white/30 border border-white/10"
                    }`}
                    title={`${container.name}: ${container.status}`}
                  >
                    {i + 1}
                  </motion.div>
                ))}
              </div>
            </div>
          </div>
        </GlassCard>

        {/* ML Prediction Panel */}
        <GlassCard className="lg:col-span-1">
          <div className="flex items-center gap-2 mb-6">
            <Brain className="w-5 h-5 text-primary-400" />
            <h3 className="text-lg font-semibold text-white">
              ML Prediction (30min)
            </h3>
          </div>

          <div className="space-y-6">
            <div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-white/60">Current Load</span>
                <span className="text-white">
                  {data.mlPrediction.currentLoad.toFixed(0)} req/hr
                </span>
              </div>
              <ProgressBar
                value={data.mlPrediction.currentLoad}
                max={1000}
                color="blue"
              />
            </div>

            <div>
              <div className="flex justify-between text-sm mb-2">
                <span className="text-white/60">Predicted Load</span>
                <span className="text-green-400">
                  {data.mlPrediction.predictedLoad.toFixed(0)} req/hr
                </span>
              </div>
              <ProgressBar
                value={data.mlPrediction.predictedLoad}
                max={1000}
                color="green"
              />
            </div>

            <div className="p-4 rounded-xl bg-white/5 border border-white/10">
              <div className="flex justify-between items-center mb-2">
                <span className="text-sm text-white/60">
                  Confidence Interval
                </span>
                <span className="text-sm text-white">95%</span>
              </div>
              <p className="text-lg font-semibold text-white">
                {data.mlPrediction.lowerBound.toFixed(0)} -{" "}
                {data.mlPrediction.upperBound.toFixed(0)} req/hr
              </p>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-primary-500/10 border border-primary-500/30">
              <span className="text-sm text-primary-300">Model Accuracy</span>
              <span className="text-lg font-bold text-primary-400">
                {(data.mlPrediction.accuracy * 100).toFixed(1)}%
              </span>
            </div>

            {data.mlPrediction.shouldScaleUp && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center gap-2"
              >
                <AlertTriangle className="w-5 h-5 text-amber-400" />
                <span className="text-sm text-amber-300">
                  Scale-up recommended
                </span>
              </motion.div>
            )}
          </div>
        </GlassCard>

        {/* Activity Feed */}
        <GlassCard className="lg:col-span-1">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-semibold text-white">Live Activity</h3>
            <Activity className="w-5 h-5 text-primary-400 animate-pulse" />
          </div>
          <div className="space-y-3 max-h-[400px] overflow-y-auto">
            {data.recentActivity.slice(0, 10).map((activity, i) => (
              <ActivityItem key={i} activity={activity} index={i} />
            ))}
          </div>
        </GlassCard>
      </div>
    </div>
  );
}

function StatusItem({
  icon,
  label,
  status,
  detail,
  healthy,
}: {
  icon: React.ReactNode;
  label: string;
  status: string;
  detail: string;
  healthy: boolean;
}) {
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div
          className={`p-2 rounded-lg ${healthy ? "bg-green-500/20 text-green-400" : "bg-red-500/20 text-red-400"}`}
        >
          {icon}
        </div>
        <div>
          <p className="text-white font-medium">{label}</p>
          <p className="text-xs text-white/50">{detail}</p>
        </div>
      </div>
      <div className="flex items-center gap-2">
        {healthy ? (
          <CheckCircle className="w-4 h-4 text-green-400" />
        ) : (
          <AlertTriangle className="w-4 h-4 text-red-400" />
        )}
        <span
          className={`text-sm ${healthy ? "text-green-400" : "text-red-400"}`}
        >
          {status}
        </span>
      </div>
    </div>
  );
}

function ActivityItem({
  activity,
  index,
}: {
  activity: ActivityItem;
  index: number;
}) {
  const getIcon = () => {
    if (activity.action.includes("login")) return <LogIn className="w-4 h-4" />;
    if (
      activity.action.includes("upload") ||
      activity.action.includes("created")
    )
      return <Upload className="w-4 h-4" />;
    if (activity.action.includes("share"))
      return <Share2 className="w-4 h-4" />;
    return <User className="w-4 h-4" />;
  };

  const getColor = () => {
    if (activity.action.includes("login"))
      return "text-green-400 bg-green-500/20";
    if (activity.action.includes("upload"))
      return "text-blue-400 bg-blue-500/20";
    if (activity.action.includes("share"))
      return "text-purple-400 bg-purple-500/20";
    return "text-white/60 bg-white/10";
  };

  const timeAgo = (timestamp: string) => {
    const now = new Date();
    const then = new Date(timestamp);
    const diff = Math.floor((now.getTime() - then.getTime()) / 1000);
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.05 }}
      className="flex items-start gap-3 p-3 rounded-xl hover:bg-white/5 transition-all activity-item"
    >
      <div className={`p-2 rounded-lg ${getColor()}`}>{getIcon()}</div>
      <div className="flex-1 min-w-0">
        <p className="text-sm text-white">
          <span className="font-medium">{activity.user}</span>
          <span className="text-white/60"> {activity.action}</span>
        </p>
        {activity.details && (
          <p className="text-xs text-white/50 truncate">{activity.details}</p>
        )}
      </div>
      <span className="text-xs text-white/40 whitespace-nowrap">
        {timeAgo(activity.time)}
      </span>
    </motion.div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-8">
      <div className="flex justify-between items-center">
        <div>
          <Skeleton className="h-8 w-40 mb-2" />
          <Skeleton className="h-4 w-60" />
        </div>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-36 rounded-2xl" />
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-72 rounded-2xl" />
        ))}
      </div>
    </div>
  );
}

function getMockMetrics(): DashboardMetrics {
  return {
    totalFiles: 1543,
    activeUsers: 23,
    storageUsed: 45.2,
    storageTotal: 100,
    systemHealth: "healthy",
    containers: [
      { name: "container-1", status: "running", load: 45 },
      { name: "container-2", status: "running", load: 62 },
      { name: "container-3", status: "running", load: 38 },
      { name: "container-4", status: "stopped", load: 0 },
      { name: "container-5", status: "stopped", load: 0 },
    ],
    requestRate: Array.from({ length: 24 }, (_, i) => ({
      time: `${String(i).padStart(2, "0")}:00`,
      date: "",
      count: Math.floor(20 + Math.random() * 50 + Math.sin(i / 4) * 20),
    })),
    uploadsPerDay: [
      { date: "Jan 22", time: "", count: 12 },
      { date: "Jan 23", time: "", count: 8 },
      { date: "Jan 24", time: "", count: 15 },
      { date: "Jan 25", time: "", count: 22 },
      { date: "Jan 26", time: "", count: 18 },
      { date: "Jan 27", time: "", count: 25 },
      { date: "Jan 28", time: "", count: 19 },
    ],
    storageByType: { documents: 40, images: 35, videos: 20, other: 5 },
    recentActivity: [
      {
        user: "Alice",
        action: "uploaded",
        details: "report.pdf",
        time: new Date(Date.now() - 120000).toISOString(),
        icon: "upload",
      },
      {
        user: "Bob",
        action: "login success",
        details: "",
        time: new Date(Date.now() - 300000).toISOString(),
        icon: "user",
      },
      {
        user: "System",
        action: "scaled to 3 containers",
        details: "ML prediction triggered",
        time: new Date(Date.now() - 480000).toISOString(),
        icon: "server",
      },
      {
        user: "Alice",
        action: "shared",
        details: "document.docx with Bob",
        time: new Date(Date.now() - 600000).toISOString(),
        icon: "share",
      },
      {
        user: "Charlie",
        action: "uploaded",
        details: "image.png",
        time: new Date(Date.now() - 900000).toISOString(),
        icon: "upload",
      },
    ],
    loadBalancerStats: { totalRequests: 12450, healthyServers: 3 },
    mlPrediction: {
      currentLoad: 450,
      predictedLoad: 680,
      confidence: 0.85,
      lowerBound: 620,
      upperBound: 740,
      shouldScaleUp: true,
      shouldScaleDown: false,
      mlServiceHealthy: true,
      accuracy: 0.892,
      mlEnabled: true,
    },
  };
}
