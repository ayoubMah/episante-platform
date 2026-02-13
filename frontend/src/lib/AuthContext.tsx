import {
  createContext,
  useContext,
  useState,
  type ReactNode,
  useEffect,
} from "react";
import { jwtDecode } from "jwt-decode";

interface UserPayload {
  sub: string; // The UUID
  role: "ADMIN" | "DOCTOR" | "PATIENT"; // The Role
  exp: number;
}

interface AuthContextType {
  isAuthenticated: boolean;
  userRole: "ADMIN" | "DOCTOR" | "PATIENT" | null;
  userId: string | null;
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userRole, setUserRole] = useState<AuthContextType["userRole"]>(null);
  const [userId, setUserId] = useState<string | null>(null);

  // Helper to decode token safely
  const processToken = (token: string) => {
    try {
      const decoded = jwtDecode<UserPayload>(token);
      setIsAuthenticated(true);
      setUserRole(decoded.role);
      setUserId(decoded.sub);
    } catch (e) {
      console.error("Invalid Token", e);
      logout();
    }
  };

  // On Mount: Check storage
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      processToken(token);
    }
  }, []);

  const login = (token: string) => {
    localStorage.setItem("accessToken", token);
    processToken(token);
  };

  const logout = () => {
    localStorage.removeItem("accessToken");
    setIsAuthenticated(false);
    setUserRole(null);
    setUserId(null);
  };

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, userRole, userId, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
}
