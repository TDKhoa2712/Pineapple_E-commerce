import type { Metadata } from "next";
import { CouponsContent } from "./coupons-content";

export const metadata: Metadata = { title: "Mã giảm giá" };

export default function CouponsPage() {
  return <CouponsContent />;
}