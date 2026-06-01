import { create } from "zustand";
import { devtools } from "zustand/middleware";

interface UIState {
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  adminProfileOpen: boolean;
  setAdminProfileOpen: (open: boolean) => void;
}

export const useUIStore = create<UIState>()(
  devtools(
    (set) => ({
      sidebarOpen: true,
      adminProfileOpen: false,
      toggleSidebar: () =>
        set((state) => ({ sidebarOpen: !state.sidebarOpen }), false, "toggleSidebar"),
      setSidebarOpen: (open) =>
        set({ sidebarOpen: open }, false, "setSidebarOpen"),
      setAdminProfileOpen: (open) =>
        set({ adminProfileOpen: open }, false, "setAdminProfileOpen"),
    }),
    { name: "UIStore" }
  )
);
