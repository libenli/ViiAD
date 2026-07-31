import type { RoleCode } from './roles'
import type { AdDemand } from '@/types/ad'
import type { Material } from '@/types/material'
import type { DeliveryPlan } from '@/types/plan'

export function canCreateByRole(role: RoleCode | null) {
  return ['advertiser', 'agent', 'business', 'super_admin'].includes(role ?? '')
}

export function canEditAd(role: RoleCode | null, ad: AdDemand, userId?: string) {
  if (!role || !userId) return false
  if (role === 'super_admin' || role === 'business') return true
  if (role === 'advertiser') return ad.advertiserId === userId
  if (role === 'agent') return ad.agentId === userId
  return false
}

export function canEditMaterial(
  role: RoleCode | null,
  material: Material,
  ads: AdDemand[],
  userId?: string
) {
  if (!role || !userId) return false
  if (role === 'super_admin' || role === 'business') return true

  const ad = ads.find((item) => item.id === material.adId)
  if (!ad) return false
  if (role === 'advertiser') return ad.advertiserId === userId
  if (role === 'agent') return ad.agentId === userId
  return false
}

export function canEditPlan(
  role: RoleCode | null,
  plan: DeliveryPlan,
  ads: AdDemand[],
  userId?: string
) {
  if (!role || !userId) return false
  if (role === 'super_admin' || role === 'business') return true

  const ad = ads.find((item) => item.id === plan.adId)
  if (!ad) return false
  if (role === 'advertiser') return ad.advertiserId === userId
  if (role === 'agent') return ad.agentId === userId
  return false
}

export function visibleAdsByRole(role: RoleCode | null, ads: AdDemand[], userId?: string) {
  if (!role) return ads
  if (role === 'advertiser') return ads.filter((item) => item.advertiserId === userId)
  if (role === 'agent') return ads.filter((item) => item.agentId === userId)
  if (role === 'audience') return ads.filter((item) => item.status === 'live')
  return ads
}

export function visibleMaterialsByRole(
  role: RoleCode | null,
  materials: Material[],
  ads: AdDemand[],
  userId?: string
) {
  if (!role) return materials
  if (role === 'advertiser') {
    const adIds = ads.filter((item) => item.advertiserId === userId).map((item) => item.id)
    return materials.filter((item) => adIds.includes(item.adId))
  }
  if (role === 'agent') {
    const adIds = ads.filter((item) => item.agentId === userId).map((item) => item.id)
    return materials.filter((item) => adIds.includes(item.adId))
  }
  return materials
}

export function visiblePlansByRole(
  role: RoleCode | null,
  plans: DeliveryPlan[],
  ads: AdDemand[],
  userId?: string
) {
  if (!role) return plans
  if (role === 'advertiser') {
    const adIds = ads.filter((item) => item.advertiserId === userId).map((item) => item.id)
    return plans.filter((item) => adIds.includes(item.adId))
  }
  if (role === 'agent') {
    const adIds = ads.filter((item) => item.agentId === userId).map((item) => item.id)
    return plans.filter((item) => adIds.includes(item.adId))
  }
  return plans
}
