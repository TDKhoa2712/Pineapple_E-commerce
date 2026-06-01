import type { Metadata } from "next";
import { ReviewsContent } from "./reviews-content";

export const metadata: Metadata = { title: "Quản lý đánh giá" };

export default function ReviewsPage() {
  return <ReviewsContent />;
}