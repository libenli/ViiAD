import type { RoleCode } from '@/config/roles'

export interface MockUser {
  id: string
  username: string
  password: string
  name: string
  role: RoleCode
  status: 'active' | 'disabled'
  region?: string
  companyId?: string
}
