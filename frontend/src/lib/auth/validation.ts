// Mirrors the backend's request validation so most mistakes are reported in Turkish before a round trip.

export const PASSWORD_MIN_LENGTH = 8;
// BCrypt reads at most 72 bytes; the backend rejects longer passwords. "ş" takes two bytes.
export const PASSWORD_MAX_BYTES = 72;
const FULL_NAME_MAX_LENGTH = 100;
const EMAIL_MAX_LENGTH = 254;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// Same rule as the backend's PhoneNumbers.TURKISH_MOBILE.
export const TURKISH_MOBILE_PATTERN = /^(?:\+90|0090|90|0)?[\s-]*\(?5\d{2}\)?(?:[\s-]*\d){7}$/;

export type LoginField = "email" | "password";
export type RegisterField = "fullName" | "email" | "phone" | "password" | "confirmPassword";
export type FieldErrors<F extends string> = Partial<Record<F, string>>;

export type AuthFormState<F extends string> =
  { message?: string; fieldErrors?: FieldErrors<F> } | undefined;

type Validation<T, F extends string> =
  { ok: true; data: T } | { ok: false; errors: FieldErrors<F> };

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

function text(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function result<T, F extends string>(data: T, errors: FieldErrors<F>): Validation<T, F> {
  return Object.keys(errors).length > 0 ? { ok: false, errors } : { ok: true, data };
}

export function validateLogin(
  input: Record<LoginField, unknown>,
): Validation<LoginInput, LoginField> {
  const email = text(input.email).trim();
  const password = text(input.password);
  const errors: FieldErrors<LoginField> = {};

  if (!email) errors.email = "E-posta adresinizi girin.";
  if (!password) errors.password = "Şifrenizi girin.";

  return result({ email, password }, errors);
}

export function validateRegistration(
  input: Record<RegisterField, unknown>,
): Validation<RegisterInput, RegisterField> {
  const fullName = text(input.fullName).trim();
  const email = text(input.email).trim();
  const phone = text(input.phone).trim();
  const password = text(input.password);
  const confirmPassword = text(input.confirmPassword);
  const errors: FieldErrors<RegisterField> = {};

  if (!fullName) {
    errors.fullName = "Adınızı ve soyadınızı girin.";
  } else if (fullName.length > FULL_NAME_MAX_LENGTH) {
    errors.fullName = `Ad soyad en fazla ${FULL_NAME_MAX_LENGTH} karakter olabilir.`;
  }

  if (!email) {
    errors.email = "E-posta adresinizi girin.";
  } else if (email.length > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.test(email)) {
    errors.email = "Geçerli bir e-posta adresi girin.";
  }

  if (!phone) {
    errors.phone = "Telefon numaranızı girin.";
  } else if (!TURKISH_MOBILE_PATTERN.test(phone)) {
    errors.phone = "05XX XXX XX XX biçiminde bir cep telefonu numarası girin.";
  }

  if (!password.trim()) {
    errors.password = "Bir şifre belirleyin.";
  } else if (password.length < PASSWORD_MIN_LENGTH) {
    errors.password = `Şifreniz en az ${PASSWORD_MIN_LENGTH} karakter olmalıdır.`;
  } else if (new TextEncoder().encode(password).length > PASSWORD_MAX_BYTES) {
    errors.password = "Şifreniz çok uzun. Daha kısa bir şifre seçin.";
  }

  if (!errors.password && confirmPassword !== password) {
    errors.confirmPassword = "Şifreler birbiriyle eşleşmiyor.";
  }

  return result({ fullName, email, phone, password }, errors);
}
