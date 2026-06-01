import type { Metadata } from "next";
import { InventoryContent } from "./inventory-content";

export const metadata: Metadata = { title: "Báo cáo kho hàng" };

export default function InventoryPage() {
  return <InventoryContent />;
}