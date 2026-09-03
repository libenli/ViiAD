import type { Language } from './index'

export interface CountryCodeOption {
  value: string
  label: Record<Language, string>
}

export const countryCodeOptions: CountryCodeOption[] = [
  { value: '+86', label: { 'zh-CN': '+86 中国', 'en-US': '+86 China' } },
  { value: '+852', label: { 'zh-CN': '+852 中国香港', 'en-US': '+852 Hong Kong, China' } },
  { value: '+853', label: { 'zh-CN': '+853 中国澳门', 'en-US': '+853 Macao, China' } },
  { value: '+886', label: { 'zh-CN': '+886 中国台湾', 'en-US': '+886 Taiwan, China' } },
  { value: '+1', label: { 'zh-CN': '+1 美国 / 加拿大', 'en-US': '+1 US / Canada' } },
  { value: '+44', label: { 'zh-CN': '+44 英国', 'en-US': '+44 United Kingdom' } },
  { value: '+81', label: { 'zh-CN': '+81 日本', 'en-US': '+81 Japan' } },
  { value: '+82', label: { 'zh-CN': '+82 韩国', 'en-US': '+82 South Korea' } },
  { value: '+65', label: { 'zh-CN': '+65 新加坡', 'en-US': '+65 Singapore' } }
]
