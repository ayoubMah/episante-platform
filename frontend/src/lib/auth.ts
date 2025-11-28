import api from "./api";

export const authApi = {
  login: async (email: string, password: string) => {
    const response = await api.post("/auth/login", { email, password });
    localStorage.setItem("accessToken", response.data.accessToken);
    return response.data;
  },

  logout: () => {
    localStorage.removeItem("accessToken");
  },
};
