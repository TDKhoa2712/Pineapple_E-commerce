import type { Metadata } from "next";
import { UsersContent } from "./users-content";

export const metadata: Metadata = { title: "Quản lý người dùng" };

export default function UsersPage() {
  return <UsersContent />;
}