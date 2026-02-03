import { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Upload,
  Search,
  Trash2,
  Share2,
  Download,
  FileText,
  Image,
  Video,
  File,
  MoreVertical,
  Plus,
  Grid3X3,
  List,
} from "lucide-react";
import { GlassCard, Button, Badge } from "../components/ui/Card";
import { filesApi } from "../services/api";
import type { FileItem } from "../types";

export default function FilesPage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [viewMode, setViewMode] = useState<"grid" | "list">("list");
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  useEffect(() => {
    fetchFiles();
  }, []);

  const fetchFiles = async () => {
    try {
      const data = await filesApi.list();
      setFiles(data);
    } catch (err) {
      // Use mock data
      setFiles(getMockFiles());
    } finally {
      setLoading(false);
    }
  };

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback(() => {
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback(async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFiles = Array.from(e.dataTransfer.files);

    if (droppedFiles.length > 0) {
      await handleUpload(droppedFiles[0]);
    }
  }, []);

  const handleUpload = async (file: File) => {
    setUploading(true);
    setUploadProgress(0);

    // Simulate upload progress
    const interval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 90) {
          clearInterval(interval);
          return prev;
        }
        return prev + 10;
      });
    }, 200);

    try {
      await filesApi.upload(file);
      setUploadProgress(100);
      setTimeout(() => {
        setUploading(false);
        setUploadProgress(0);
        fetchFiles();
      }, 500);
    } catch (err) {
      // For demo, add to local state
      const newFile: FileItem = {
        id: Date.now(),
        filename: file.name,
        owner: "You",
        size: file.size,
        uploadDate: new Date().toISOString(),
        modifiedDate: new Date().toISOString(),
        type: getFileType(file.name),
      };
      setFiles((prev) => [newFile, ...prev]);
      setUploadProgress(100);
      setTimeout(() => {
        setUploading(false);
        setUploadProgress(0);
      }, 500);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await filesApi.delete(id);
    } catch (err) {
      // Demo mode - just remove from local state
    }
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const filteredFiles = files.filter((file) =>
    file.filename.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const getIcon = (type: string) => {
    switch (type) {
      case "document":
        return <FileText className="w-8 h-8 text-blue-400" />;
      case "image":
        return <Image className="w-8 h-8 text-green-400" />;
      case "video":
        return <Video className="w-8 h-8 text-purple-400" />;
      default:
        return <File className="w-8 h-8 text-white/60" />;
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Files</h1>
          <p className="text-white/60 mt-1">{files.length} files stored</p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="primary"
            onClick={() => document.getElementById("fileInput")?.click()}
          >
            <Plus className="w-4 h-4 mr-2" />
            Upload File
          </Button>
          <input
            type="file"
            id="fileInput"
            className="hidden"
            onChange={(e) =>
              e.target.files?.[0] && handleUpload(e.target.files[0])
            }
          />
        </div>
      </div>

      {/* Upload Zone */}
      <motion.div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        animate={{
          scale: isDragging ? 1.02 : 1,
          borderColor: isDragging
            ? "rgba(59, 130, 246, 0.5)"
            : "rgba(255, 255, 255, 0.1)",
        }}
        className={`
          glass-card rounded-2xl p-8 border-2 border-dashed text-center
          ${isDragging ? "bg-primary-500/10" : ""}
        `}
      >
        {uploading ? (
          <div className="space-y-4">
            <div className="w-16 h-16 mx-auto rounded-full bg-primary-500/20 flex items-center justify-center">
              <Upload className="w-8 h-8 text-primary-400 animate-bounce" />
            </div>
            <p className="text-white">Uploading...</p>
            <div className="w-full max-w-xs mx-auto h-2 bg-white/10 rounded-full overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${uploadProgress}%` }}
                className="h-full bg-gradient-to-r from-primary-500 to-primary-400"
              />
            </div>
            <p className="text-sm text-white/60">{uploadProgress}%</p>
          </div>
        ) : (
          <>
            <div className="w-16 h-16 mx-auto rounded-full bg-white/10 flex items-center justify-center mb-4">
              <Upload className="w-8 h-8 text-white/60" />
            </div>
            <p className="text-white mb-2">
              {isDragging ? "Drop files here" : "Drag and drop files here"}
            </p>
            <p className="text-sm text-white/40">
              or click the upload button above
            </p>
          </>
        )}
      </motion.div>

      {/* Controls */}
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
          <input
            type="text"
            placeholder="Search files..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full input-field pl-12"
          />
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={viewMode === "list" ? "primary" : "ghost"}
            size="sm"
            onClick={() => setViewMode("list")}
          >
            <List className="w-4 h-4" />
          </Button>
          <Button
            variant={viewMode === "grid" ? "primary" : "ghost"}
            size="sm"
            onClick={() => setViewMode("grid")}
          >
            <Grid3X3 className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* File List */}
      <AnimatePresence mode="wait">
        {viewMode === "list" ? (
          <GlassCard>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-white/10">
                    <th className="text-left py-4 px-4 text-white/60 font-medium">
                      Name
                    </th>
                    <th className="text-left py-4 px-4 text-white/60 font-medium hidden sm:table-cell">
                      Size
                    </th>
                    <th className="text-left py-4 px-4 text-white/60 font-medium hidden md:table-cell">
                      Owner
                    </th>
                    <th className="text-left py-4 px-4 text-white/60 font-medium hidden lg:table-cell">
                      Modified
                    </th>
                    <th className="text-right py-4 px-4 text-white/60 font-medium">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filteredFiles.map((file, index) => (
                    <motion.tr
                      key={file.id}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className="border-b border-white/5 hover:bg-white/5 transition-colors"
                    >
                      <td className="py-4 px-4">
                        <div className="flex items-center gap-3">
                          {getIcon(file.type)}
                          <div>
                            <p className="text-white font-medium truncate max-w-[200px]">
                              {file.filename}
                            </p>
                            <p className="text-xs text-white/50 sm:hidden">
                              {formatSize(file.size)}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 px-4 text-white/60 hidden sm:table-cell">
                        {formatSize(file.size)}
                      </td>
                      <td className="py-4 px-4 hidden md:table-cell">
                        <Badge>{file.owner}</Badge>
                      </td>
                      <td className="py-4 px-4 text-white/60 text-sm hidden lg:table-cell">
                        {new Date(file.modifiedDate).toLocaleDateString()}
                      </td>
                      <td className="py-4 px-4">
                        <div className="flex items-center justify-end gap-2">
                          <button className="p-2 rounded-lg hover:bg-white/10 text-white/60 hover:text-white transition-colors">
                            <Download className="w-4 h-4" />
                          </button>
                          <button className="p-2 rounded-lg hover:bg-white/10 text-white/60 hover:text-white transition-colors">
                            <Share2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDelete(file.id)}
                            className="p-2 rounded-lg hover:bg-red-500/20 text-white/60 hover:text-red-400 transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </motion.tr>
                  ))}
                </tbody>
              </table>
            </div>
          </GlassCard>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filteredFiles.map((file, index) => (
              <motion.div
                key={file.id}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: index * 0.05 }}
                className="glass-card rounded-2xl p-6 hover-lift cursor-pointer group"
              >
                <div className="flex items-center justify-between mb-4">
                  {getIcon(file.type)}
                  <div className="opacity-0 group-hover:opacity-100 transition-opacity">
                    <button className="p-2 rounded-lg hover:bg-white/10 text-white/60">
                      <MoreVertical className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <h3 className="text-white font-medium truncate mb-1">
                  {file.filename}
                </h3>
                <p className="text-sm text-white/50">{formatSize(file.size)}</p>
                <div className="mt-4 pt-4 border-t border-white/10 flex items-center justify-between">
                  <Badge>{file.owner}</Badge>
                  <span className="text-xs text-white/40">
                    {new Date(file.modifiedDate).toLocaleDateString()}
                  </span>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </AnimatePresence>

      {filteredFiles.length === 0 && !loading && (
        <div className="text-center py-12">
          <File className="w-16 h-16 mx-auto text-white/20 mb-4" />
          <p className="text-white/60">No files found</p>
        </div>
      )}
    </div>
  );
}

function getFileType(
  filename: string,
): "document" | "image" | "video" | "other" {
  const lower = filename.toLowerCase();
  if (lower.match(/\.(pdf|doc|docx|txt|xlsx|pptx)$/)) return "document";
  if (lower.match(/\.(jpg|jpeg|png|gif|webp|svg)$/)) return "image";
  if (lower.match(/\.(mp4|mov|avi|mkv|webm)$/)) return "video";
  return "other";
}

function getMockFiles(): FileItem[] {
  return [
    {
      id: 1,
      filename: "annual_report_2024.pdf",
      owner: "Admin",
      size: 2456789,
      uploadDate: "2024-01-20",
      modifiedDate: "2024-01-25",
      type: "document",
    },
    {
      id: 2,
      filename: "project_mockup.png",
      owner: "Alice",
      size: 1234567,
      uploadDate: "2024-01-18",
      modifiedDate: "2024-01-24",
      type: "image",
    },
    {
      id: 3,
      filename: "presentation.pptx",
      owner: "Bob",
      size: 5678901,
      uploadDate: "2024-01-15",
      modifiedDate: "2024-01-22",
      type: "document",
    },
    {
      id: 4,
      filename: "demo_video.mp4",
      owner: "Charlie",
      size: 89012345,
      uploadDate: "2024-01-10",
      modifiedDate: "2024-01-20",
      type: "video",
    },
    {
      id: 5,
      filename: "data_export.xlsx",
      owner: "Admin",
      size: 345678,
      uploadDate: "2024-01-08",
      modifiedDate: "2024-01-18",
      type: "document",
    },
    {
      id: 6,
      filename: "logo.svg",
      owner: "Alice",
      size: 12345,
      uploadDate: "2024-01-05",
      modifiedDate: "2024-01-15",
      type: "image",
    },
  ];
}
