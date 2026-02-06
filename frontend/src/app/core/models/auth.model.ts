export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
}

export interface LoginResponse {
  requires2FA: boolean;
  tempToken: string | null;
  token: string | null;
  userId: string | null;
  email: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface ProfileResponse {
  email: string | null;
  accountName: string | null;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
}

export interface UpdateProfileRequest {
  email?: string | null;
  accountName?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  phone?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  postalCode?: string | null;
  city?: string | null;
  country?: string | null;
}
