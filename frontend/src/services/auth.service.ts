import apiClient from "@/lib/api-client";
import type { ApiResponse, AuthResponse, LoginRequest, UserResponse } from "@/types";

export const authService = {
  login: async (payload: LoginRequest) => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
      "/api/v1/auth/login",
      payload
    );
    return data;
  },

  logout: async () => {
    await apiClient.post("/api/v1/auth/logout");
  },

  getMe: async () => {
    const { data } = await apiClient.get<ApiResponse<UserResponse>>("/api/v1/auth/me");
    return data.data;
  },

  refresh: async () => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
      "/api/v1/auth/refresh"
    );
    return data.data;
  },
};