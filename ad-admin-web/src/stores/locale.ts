import { defineStore } from 'pinia'
import { DEFAULT_LANGUAGE, LANGUAGE_STORAGE_KEY, localeMessages, normalizeLanguage, type Language } from '@/locales'

export const useLocaleStore = defineStore('locale', {
  state: () => ({
    language: normalizeLanguage(localStorage.getItem(LANGUAGE_STORAGE_KEY))
  }),
  getters: {
    messages: (state) => localeMessages[state.language],
    t: (state) => (path: string, fallback = '') => {
      const segments = path.split('.')
      let value: unknown = localeMessages[state.language]
      for (const segment of segments) {
        if (!value || typeof value !== 'object' || !(segment in value)) {
          return fallback || path
        }
        value = (value as Record<string, unknown>)[segment]
      }
      return typeof value === 'string' ? value : fallback || path
    }
  },
  actions: {
    setLanguage(language: Language) {
      this.language = language
      localStorage.setItem(LANGUAGE_STORAGE_KEY, language)
      document.documentElement.lang = language
    },
    initLanguage() {
      this.setLanguage(normalizeLanguage(localStorage.getItem(LANGUAGE_STORAGE_KEY) || DEFAULT_LANGUAGE))
    }
  }
})
