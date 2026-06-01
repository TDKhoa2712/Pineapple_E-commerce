import type { Metadata } from "next";
import { OrdersContent } from "./orders-content";

export const metadata: Metadata = { title: "Quản lý đơn hàng" };

export default function OrdersPage() {
  return <OrdersContent />;
}