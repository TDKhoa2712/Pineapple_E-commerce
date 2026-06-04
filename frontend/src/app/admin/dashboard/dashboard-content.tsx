"use client";

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { useQuery } from "@tanstack/react-query";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend,
} from "recharts";
import {
  DollarSign,
  ShoppingCart,
  Users,
  Store,
  Package,
  Clock,
} from "lucide-react";
import { StatCard } from "@/components/admin/stat-card";
import { StatusBadge } from "@/components/shared/status-badge";
import { adminOrderService } from "@/services/admin.service";
import { adminFarmService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { formatCurrency, formatDateTime, cn } from "@/lib/utils";

const statusLabels: Record<string, { label: string; color: string }> = {
  PENDING: { label: "Chờ xử lý", color: "#fbbf24" },
  CONFIRMED: { label: "Đã xác nhận", color: "#60a5fa" },
  PROCESSING: { label: "Đang chuẩn bị", color: "#34d399" },
  SHIPPING: { label: "Đang giao", color: "#22d3ee" },
  DELIVERED: { label: "Đã giao", color: "#a78bfa" },
  CANCELLED: { label: "Đã hủy", color: "#f87171" },
  REFUND_REQUESTED: { label: "Chờ hoàn tiền", color: "#f472b6" },
  REFUNDED: { label: "Đã hoàn tiền", color: "#e2e8f0" },
  RETURNED: { label: "Đã trả hàng", color: "#94a3b8" },
};

export function DashboardContent() {
  const [selectedMonthA, setSelectedMonthA] = useState<string>("");
  const [selectedMonthB, setSelectedMonthB] = useState<string>("");

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: queryKeys.dashboardStats,
    queryFn: () => adminOrderService.getStatistics(),
  });

  const { data: recentOrders, isLoading: ordersLoading } = useQuery({
    queryKey: queryKeys.orders({ page: 0, size: 5 }),
    queryFn: () => adminOrderService.getAll({ page: 0, size: 5 }),
  });

  const { data: pendingFarms, isLoading: farmsLoading } = useQuery({
    queryKey: queryKeys.farms({ status: "PENDING_APPROVAL", page: 0, size: 5 }),
    queryFn: () => adminFarmService.getAll({ status: "PENDING_APPROVAL", page: 0, size: 5 }),
  });

  // Initialize comparison months once stats data is available
  useEffect(() => {
    if (stats?.monthlyRevenueList && stats.monthlyRevenueList.length >= 2) {
      if (!selectedMonthA) {
        setSelectedMonthA(stats.monthlyRevenueList[stats.monthlyRevenueList.length - 1].month);
      }
      if (!selectedMonthB) {
        setSelectedMonthB(stats.monthlyRevenueList[stats.monthlyRevenueList.length - 2].month);
      }
    }
  }, [stats, selectedMonthA, selectedMonthB]);

  if (ordersLoading || farmsLoading || statsLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <Clock className="h-8 w-8 animate-spin text-slate-400" />
        <span className="ml-3 text-slate-400 text-sm">Đang tải thông tin thống kê...</span>
      </div>
    );
  }

  // Format month list for the chart
  const chartData = stats?.monthlyRevenueList.map((item) => {
    const [year, month] = item.month.split("-");
    return {
      ...item,
      displayMonth: `Tháng ${month}/${year.substring(2)}`,
    };
  }) ?? [];

  // Parse pie chart order status distribution
  const pieData = Object.entries(stats?.orderStatusDistribution ?? {})
    .map(([status, count]) => {
      const config = statusLabels[status] || { label: status, color: "#cbd5e1" };
      return {
        name: config.label,
        value: count,
        color: config.color,
      };
    })
    .filter((item) => item.value > 0);

  // Month comparison calculations
  const monthAData = stats?.monthlyRevenueList.find((m) => m.month === selectedMonthA);
  const monthBData = stats?.monthlyRevenueList.find((m) => m.month === selectedMonthB);

  const revenueDiff = (monthAData?.revenue ?? 0) - (monthBData?.revenue ?? 0);
  const revenueDiffPercent = (monthBData?.revenue ?? 0) > 0 
    ? (revenueDiff / monthBData!.revenue) * 100 
    : (monthAData?.revenue ?? 0) > 0 ? 100 : 0;

  const orderDiff = (monthAData?.orderCount ?? 0) - (monthBData?.orderCount ?? 0);
  const orderDiffPercent = (monthBData?.orderCount ?? 0) > 0
    ? (orderDiff / monthBData!.orderCount) * 100
    : (monthAData?.orderCount ?? 0) > 0 ? 100 : 0;

  const aovA = (monthAData?.orderCount ?? 0) > 0 ? (monthAData!.revenue / monthAData!.orderCount) : 0;
  const aovB = (monthBData?.orderCount ?? 0) > 0 ? (monthBData!.revenue / monthBData!.orderCount) : 0;
  const aovDiff = aovA - aovB;
  const aovDiffPercent = aovB > 0 ? (aovDiff / aovB) * 100 : aovA > 0 ? 100 : 0;

  const formatMonthLabel = (mStr: string) => {
    if (!mStr) return "";
    const [year, month] = mStr.split("-");
    return `${month}/${year}`;
  };

  const comparisonChartData = [
    {
      name: `Tháng ${formatMonthLabel(selectedMonthB)} (Mốc)`,
      "Doanh thu": monthBData?.revenue ?? 0,
      "Số đơn hàng": monthBData?.orderCount ?? 0,
    },
    {
      name: `Tháng ${formatMonthLabel(selectedMonthA)} (So sánh)`,
      "Doanh thu": monthAData?.revenue ?? 0,
      "Số đơn hàng": monthAData?.orderCount ?? 0,
    },
  ];

  return (
    <div className="space-y-8">
      {/* Page header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <h1 className="font-display text-2xl font-bold text-slate-100">
          Tổng quan hệ thống
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Phân tích hoạt động, doanh thu và báo cáo tăng trưởng của Pineapple
        </p>
      </motion.div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Doanh thu hệ thống"
          value={formatCurrency(stats?.totalRevenue ?? 0)}
          change={stats?.revenueChangePercentage}
          icon={DollarSign}
          iconColor="text-pine-400"
          delay={0}
        />
        <StatCard
          title="Tổng đơn hàng"
          value={(stats?.totalOrders ?? 0).toLocaleString("vi-VN")}
          change={stats?.orderChangePercentage}
          icon={ShoppingCart}
          iconColor="text-cyan-400"
          delay={0.05}
        />
        <StatCard
          title="Người dùng đăng ký"
          value={(stats?.totalUsers ?? 0).toLocaleString("vi-VN")}
          icon={Users}
          iconColor="text-violet-400"
          delay={0.1}
        />
        <StatCard
          title="Nông trại chờ duyệt"
          value={(stats?.pendingFarms ?? 0).toString()}
          icon={Store}
          iconColor="text-amber-400"
          delay={0.15}
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        {/* Revenue area chart */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="xl:col-span-2 rounded-2xl border border-slate-800 bg-slate-900 p-5"
        >
          <h2 className="font-display text-base font-semibold text-slate-200">
            Biểu đồ doanh thu 12 tháng gần nhất
          </h2>
          <div className="mt-4 h-56">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#22c55e" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis
                  dataKey="displayMonth"
                  tick={{ fill: "#475569", fontSize: 11 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fill: "#475569", fontSize: 11 }}
                  axisLine={false}
                  tickLine={false}
                  tickFormatter={(v) => `${(v / 1e6).toFixed(0)}M`}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#1e293b",
                    border: "1px solid #334155",
                    borderRadius: "10px",
                    color: "#e2e8f0",
                    fontSize: "12px",
                  }}
                  formatter={(v: number) => [formatCurrency(v), "Doanh thu"]}
                />
                <Area
                  type="monotone"
                  dataKey="revenue"
                  stroke="#22c55e"
                  strokeWidth={2}
                  fill="url(#revenueGrad)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </motion.div>

        {/* Order status pie */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
          className="rounded-2xl border border-slate-800 bg-slate-900 p-5 flex flex-col justify-between"
        >
          <h2 className="font-display text-base font-semibold text-slate-200">
            Trạng thái đơn hàng
          </h2>
          {pieData.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-500 py-8">
              <Package className="mb-2 h-10 w-10 opacity-30" />
              <p className="text-xs">Chưa có dữ liệu trạng thái</p>
            </div>
          ) : (
            <>
              <div className="mt-4 flex items-center justify-center">
                <PieChart width={160} height={160}>
                  <Pie
                    data={pieData}
                    cx={76}
                    cy={76}
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                </PieChart>
              </div>
              <ul className="mt-4 space-y-1.5 max-h-48 overflow-y-auto pr-1">
                {pieData.map((item) => (
                  <li key={item.name} className="flex items-center justify-between text-[11px]">
                    <div className="flex items-center gap-2">
                      <span
                        className="h-2 w-2 rounded-full"
                        style={{ backgroundColor: item.color }}
                      />
                      <span className="text-slate-400">{item.name}</span>
                    </div>
                    <span className="font-medium text-slate-300">
                      {item.value} đơn ({((item.value / (stats?.totalOrders || 1)) * 100).toFixed(1)}%)
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </motion.div>
      </div>

      {/* Month Comparison Card */}
      {stats?.monthlyRevenueList && stats.monthlyRevenueList.length >= 2 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.28 }}
          className="rounded-2xl border border-slate-800 bg-slate-900 p-5"
        >
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <div>
              <h2 className="font-display text-base font-semibold text-slate-200">
                Công cụ đối chiếu & so sánh tăng trưởng giữa các tháng
              </h2>
              <p className="text-xs text-slate-500 mt-1">Chọn 2 tháng bất kỳ để xem phân tích số liệu chênh lệch</p>
            </div>
            
            <div className="flex flex-wrap items-center gap-3">
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Tháng phân tích (A)</span>
                <select
                  value={selectedMonthA}
                  onChange={(e) => setSelectedMonthA(e.target.value)}
                  className="h-9 px-3 bg-slate-800 border border-slate-700 rounded-xl text-xs focus:outline-none focus:border-pine-500 text-slate-200 cursor-pointer"
                >
                  {stats.monthlyRevenueList.map((m) => (
                    <option key={m.month} value={m.month}>Tháng {formatMonthLabel(m.month)}</option>
                  ))}
                </select>
              </div>
              <div className="text-slate-600 self-end mb-2">vs</div>
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Tháng mốc đối chiếu (B)</span>
                <select
                  value={selectedMonthB}
                  onChange={(e) => setSelectedMonthB(e.target.value)}
                  className="h-9 px-3 bg-slate-800 border border-slate-700 rounded-xl text-xs focus:outline-none focus:border-pine-500 text-slate-200 cursor-pointer"
                >
                  {stats.monthlyRevenueList.map((m) => (
                    <option key={m.month} value={m.month}>Tháng {formatMonthLabel(m.month)}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {monthAData && monthBData && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-center">
              {/* Metrics cards */}
              <div className="lg:col-span-1 space-y-3">
                {/* Revenue compare */}
                <div className="p-4 bg-slate-800/30 rounded-xl border border-slate-800/80">
                  <p className="text-xs text-slate-500 font-semibold">Doanh thu đạt được</p>
                  <div className="flex justify-between items-baseline mt-1">
                    <span className="text-sm font-bold text-slate-200">
                      {formatCurrency(monthAData.revenue)}
                    </span>
                    <span className="text-xs text-slate-500">
                      (mốc: {formatCurrency(monthBData.revenue)})
                    </span>
                  </div>
                  <div className="mt-2.5 flex items-center gap-2">
                    <span className={cn(
                      "text-[10px] px-2 py-0.5 rounded-full font-bold",
                      revenueDiff >= 0 ? "bg-green-500/10 text-green-400" : "bg-red-500/10 text-red-400"
                    )}>
                      {revenueDiff >= 0 ? "▲ +" : "▼ "}{revenueDiffPercent.toFixed(1)}%
                    </span>
                    <span className="text-[11px] text-slate-400 font-medium">
                      {revenueDiff >= 0 ? "Tăng" : "Giảm"} {formatCurrency(Math.abs(revenueDiff))}
                    </span>
                  </div>
                </div>

                {/* Orders compare */}
                <div className="p-4 bg-slate-800/30 rounded-xl border border-slate-800/80">
                  <p className="text-xs text-slate-500 font-semibold">Số lượng đơn hàng</p>
                  <div className="flex justify-between items-baseline mt-1">
                    <span className="text-sm font-bold text-slate-200">
                      {monthAData.orderCount} đơn
                    </span>
                    <span className="text-xs text-slate-500">
                      (mốc: {monthBData.orderCount} đơn)
                    </span>
                  </div>
                  <div className="mt-2.5 flex items-center gap-2">
                    <span className={cn(
                      "text-[10px] px-2 py-0.5 rounded-full font-bold",
                      orderDiff >= 0 ? "bg-green-500/10 text-green-400" : "bg-red-500/10 text-red-400"
                    )}>
                      {orderDiff >= 0 ? "▲ +" : "▼ "}{orderDiffPercent.toFixed(1)}%
                    </span>
                    <span className="text-[11px] text-slate-400 font-medium">
                      {orderDiff >= 0 ? "Tăng" : "Giảm"} {Math.abs(orderDiff)} đơn
                    </span>
                  </div>
                </div>

                {/* AOV compare */}
                <div className="p-4 bg-slate-800/30 rounded-xl border border-slate-800/80">
                  <p className="text-xs text-slate-500 font-semibold">Giá trị trung bình mỗi đơn (AOV)</p>
                  <div className="flex justify-between items-baseline mt-1">
                    <span className="text-sm font-bold text-slate-200">
                      {formatCurrency(aovA)}
                    </span>
                    <span className="text-xs text-slate-500">
                      (mốc: {formatCurrency(aovB)})
                    </span>
                  </div>
                  <div className="mt-2.5 flex items-center gap-2">
                    <span className={cn(
                      "text-[10px] px-2 py-0.5 rounded-full font-bold",
                      aovDiff >= 0 ? "bg-green-500/10 text-green-400" : "bg-red-500/10 text-red-400"
                    )}>
                      {aovDiff >= 0 ? "▲ +" : "▼ "}{aovDiffPercent.toFixed(1)}%
                    </span>
                    <span className="text-[11px] text-slate-400 font-medium">
                      {aovDiff >= 0 ? "Tăng" : "Giảm"} {formatCurrency(Math.abs(aovDiff))}
                    </span>
                  </div>
                </div>
              </div>

              {/* Compare visual chart */}
              <div className="lg:col-span-2 h-64 bg-slate-800/10 rounded-xl p-4 border border-slate-800/40">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={comparisonChartData} margin={{ top: 20, right: 10, left: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                    <XAxis dataKey="name" tick={{ fill: "#475569", fontSize: 11 }} />
                    <YAxis yAxisId="left" tick={{ fill: "#475569", fontSize: 11 }} tickFormatter={(v) => `${(v / 1e6).toFixed(1)}M`} />
                    <YAxis yAxisId="right" orientation="right" tick={{ fill: "#475569", fontSize: 11 }} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#1e293b",
                        border: "1px solid #334155",
                        borderRadius: "10px",
                        color: "#e2e8f0",
                        fontSize: "12px",
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: 11, paddingTop: 10 }} />
                    <Bar yAxisId="left" dataKey="Doanh thu" fill="#22c55e" radius={[4, 4, 0, 0]} barSize={40} />
                    <Bar yAxisId="right" dataKey="Số đơn hàng" fill="#3b82f6" radius={[4, 4, 0, 0]} barSize={40} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </motion.div>
      )}

      {/* Recent orders + Pending farms */}
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        {/* Recent orders */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="rounded-2xl border border-slate-800 bg-slate-900 p-5"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-display text-base font-semibold text-slate-200">
              Đơn hàng mới nhất
            </h2>
            <ShoppingCart className="h-4 w-4 text-slate-600" />
          </div>
          <div className="mt-4 space-y-3">
            {ordersLoading
              ? Array.from({ length: 4 }).map((_, i) => (
                  <div
                    key={i}
                    className="h-12 animate-pulse rounded-lg bg-slate-800"
                  />
                ))
              : (recentOrders?.content ?? []).map((order) => (
                  <div
                    key={order.id}
                    className="flex items-center justify-between rounded-lg bg-slate-800/50 px-3 py-2.5"
                  >
                    <div>
                      <p className="text-xs font-medium text-slate-200">
                        #{order.id} · {order.userFullName ?? order.userEmail ?? "—"}
                      </p>
                      <p className="text-xs text-slate-500">
                        {formatDateTime(order.createdAt)}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <StatusBadge status={order.status} />
                      <span className="text-xs font-semibold text-pine-400">
                        {formatCurrency(order.totalAmount)}
                      </span>
                    </div>
                  </div>
                ))}
          </div>
        </motion.div>

        {/* Pending farms */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35 }}
          className="rounded-2xl border border-slate-800 bg-slate-900 p-5"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-display text-base font-semibold text-slate-200">
              Nông trại chờ phê duyệt
            </h2>
            <Clock className="h-4 w-4 text-amber-500" />
          </div>
          <div className="mt-4 space-y-3">
            {farmsLoading
              ? Array.from({ length: 4 }).map((_, i) => (
                  <div
                    key={i}
                    className="h-12 animate-pulse rounded-lg bg-slate-800"
                  />
                ))
              : (pendingFarms?.content ?? []).length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-slate-500">
                  <Package className="mb-2 h-8 w-8 opacity-40" />
                  <p className="text-sm">Không có nông trại nào chờ duyệt</p>
                </div>
              ) : (
                (pendingFarms?.content ?? []).map((farm) => (
                  <div
                    key={farm.id}
                    className="flex items-center justify-between rounded-lg bg-slate-800/50 px-3 py-2.5"
                  >
                    <div>
                      <p className="text-xs font-medium text-slate-200">
                        {farm.name}
                      </p>
                      <p className="text-xs text-slate-500">
                        {farm.ownerName} · {farm.location}
                      </p>
                    </div>
                    <StatusBadge status={farm.status} />
                  </div>
                ))
              )}
          </div>
        </motion.div>
      </div>
    </div>
  );
}