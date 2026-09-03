import { enUS } from './en-US'
import { zhCN } from './zh-CN'

export type Language = 'zh-CN' | 'en-US'

export const DEFAULT_LANGUAGE: Language = 'zh-CN'
export const LANGUAGE_STORAGE_KEY = 'viiad-language'

export const localeMessages = {
  'zh-CN': zhCN,
  'en-US': enUS
}

export type LocaleMessages = typeof zhCN

export function normalizeLanguage(value: string | null): Language {
  return value === 'en-US' || value === 'zh-CN' ? value : DEFAULT_LANGUAGE
}
