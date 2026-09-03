export function isValidEmail(value?: string) {
  if (!value) {
    return true
  }
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value)
}

export function isValidPhone(value?: string) {
  if (!value) {
    return true
  }
  return /^\+?[0-9][0-9\s-]{5,19}$/.test(value)
}
