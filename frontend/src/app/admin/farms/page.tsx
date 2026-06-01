import type { Metadata } from "next";
import { FarmsContent } from "./farms-content";

export const metadata: Metadata = { title: "Quản lý nông trại" };

export default function FarmsPage() {
  return <FarmsContent />;
}