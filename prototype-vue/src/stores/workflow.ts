import { acceptHMRUpdate, defineStore } from 'pinia'
import { mockAds } from '@/mock/ads'
import { mockMaterials } from '@/mock/materials'
import { mockPlans } from '@/mock/plans'
import { mockDevices } from '@/mock/devices'
import { mockBills } from '@/mock/bills'
import { mockWorkorders } from '@/mock/workorders'
import { mockFeedback } from '@/mock/feedback'
import { mockReports } from '@/mock/reports'
import type { AdDemand } from '@/types/ad'
import type { Material } from '@/types/material'
import type { DeliveryPlan } from '@/types/plan'
import type { Device } from '@/types/device'
import type { Workorder } from '@/types/workorder'

function nextId(prefix: string, size: number) {
  return `${prefix}-${String(size + 1).padStart(3, '0')}`
}

export const useWorkflowStore = defineStore('workflow', {
  state: () => ({
    ads: structuredClone(mockAds),
    materials: structuredClone(mockMaterials),
    plans: structuredClone(mockPlans),
    devices: structuredClone(mockDevices),
    bills: structuredClone(mockBills),
    workorders: structuredClone(mockWorkorders),
    feedback: structuredClone(mockFeedback),
    reports: structuredClone(mockReports)
  }),
  getters: {
    pendingMaterials: (state) =>
      state.materials.filter((item) => item.status === 'pending_review'),
    approvedMaterials: (state) =>
      state.materials.filter((item) => item.status === 'approved'),
    livePlans: (state) => state.plans.filter((item) => item.status === 'live')
  },
  actions: {
    createAd(payload: {
      title: string
      description?: string
      advertiserId: string
      agentId?: string
      type: 'image' | 'video' | 'interactive'
      objective: 'exposure' | 'conversion' | 'engagement'
      region: string
      budget: number
      status: 'draft' | 'submitted'
    }) {
      const ad: AdDemand = {
        id: nextId('ad', this.ads.length),
        title: payload.title,
        description: payload.description,
        advertiserId: payload.advertiserId,
        agentId: payload.agentId,
        type: payload.type,
        objective: payload.objective,
        region: payload.region,
        budget: payload.budget,
        status: payload.status,
        materialIds: [],
        createdAt: new Date().toISOString()
      }

      this.ads.unshift(ad)
      return ad
    },

    updateAd(
      adId: string,
      payload: Partial<
        Pick<
          AdDemand,
          'title' | 'description' | 'type' | 'objective' | 'region' | 'budget' | 'status'
        >
      >
    ) {
      const ad = this.ads.find((item) => item.id === adId)
      if (!ad) return null
      Object.assign(ad, payload)
      return ad
    },

    removeAd(adId: string) {
      const adIndex = this.ads.findIndex((item) => item.id === adId)
      if (adIndex === -1) {
        throw new Error('广告不存在，可能已被删除。')
      }

      if (this.materials.some((item) => item.adId === adId)) {
        throw new Error('该广告下仍有关联素材，请先删除素材后再删除广告。')
      }

      if (this.plans.some((item) => item.adId === adId)) {
        throw new Error('该广告下仍有关联计划，请先删除计划后再删除广告。')
      }

      const [deleted] = this.ads.splice(adIndex, 1)
      return deleted
    },

    createMaterial(payload: {
      adId: string
      name: string
      description?: string
      type: 'image' | 'video'
      url?: string
      uploaderId: string
    }) {
      const material: Material = {
        id: nextId('mat', this.materials.length),
        adId: payload.adId,
        name: payload.name,
        description: payload.description,
        type: payload.type,
        url:
          payload.url ||
          `https://placehold.co/600x400?text=${encodeURIComponent(payload.name)}`,
        uploaderId: payload.uploaderId,
        status: 'pending_review',
        createdAt: new Date().toISOString()
      }

      this.materials.unshift(material)

      const ad = this.ads.find((item) => item.id === payload.adId)
      if (ad && !ad.materialIds.includes(material.id)) {
        ad.materialIds.push(material.id)
        if (ad.status === 'draft') {
          ad.status = 'submitted'
        }
      }

      return material
    },

    updateMaterial(
      materialId: string,
      payload: Partial<
        Pick<Material, 'name' | 'description' | 'type' | 'url'>
      >
    ) {
      const material = this.materials.find((item) => item.id === materialId)
      if (!material) return null
      Object.assign(material, payload)
      return material
    },

    removeMaterial(materialId: string) {
      const materialIndex = this.materials.findIndex((item) => item.id === materialId)
      if (materialIndex === -1) {
        throw new Error('素材不存在，可能已被删除。')
      }

      if (this.plans.some((item) => item.materialIds.includes(materialId))) {
        throw new Error('该素材已被投放计划引用，请先调整计划后再删除素材。')
      }

      const [deleted] = this.materials.splice(materialIndex, 1)
      const ad = this.ads.find((item) => item.id === deleted.adId)
      if (ad) {
        ad.materialIds = ad.materialIds.filter((item) => item !== materialId)
      }

      return deleted
    },

    approveMaterial(materialId: string, reviewerId: string) {
      const material = this.materials.find((item) => item.id === materialId)
      if (!material) return
      material.status = 'approved'
      material.reviewedBy = reviewerId
      material.reviewComment = '审核通过'
    },

    rejectMaterial(materialId: string, reviewerId: string, reason: string) {
      const material = this.materials.find((item) => item.id === materialId)
      if (!material) return
      material.status = 'rejected'
      material.reviewedBy = reviewerId
      material.reviewComment = reason
    },

    createPlan(payload: {
      adId: string
      materialIds: string[]
      region: string
      buildingIds: string[]
      deviceIds: string[]
      startAt: string
      endAt: string
      targeting: {
        timeSlots: string[]
        audienceTags: string[]
        lbsEnabled: boolean
      }
      operatorId?: string
    }) {
      const plan: DeliveryPlan = {
        id: nextId('plan', this.plans.length),
        adId: payload.adId,
        materialIds: payload.materialIds,
        region: payload.region,
        buildingIds: payload.buildingIds,
        deviceIds: payload.deviceIds,
        startAt: payload.startAt,
        endAt: payload.endAt,
        targeting: payload.targeting,
        status: 'pending_schedule',
        operatorId: payload.operatorId
      }

      this.plans.unshift(plan)

      const ad = this.ads.find((item) => item.id === payload.adId)
      if (ad) {
        ad.planId = plan.id
        ad.status = 'planning'
      }

      return plan
    },

    updatePlan(
      planId: string,
      payload: Partial<
        Pick<DeliveryPlan, 'region' | 'buildingIds' | 'deviceIds' | 'startAt' | 'endAt'> & {
          targeting: DeliveryPlan['targeting']
        }
      >
    ) {
      const plan = this.plans.find((item) => item.id === planId)
      if (!plan) return null

      if (payload.targeting) {
        plan.targeting = payload.targeting
      }

      Object.assign(plan, {
        region: payload.region ?? plan.region,
        buildingIds: payload.buildingIds ?? plan.buildingIds,
        deviceIds: payload.deviceIds ?? plan.deviceIds,
        startAt: payload.startAt ?? plan.startAt,
        endAt: payload.endAt ?? plan.endAt
      })

      return plan
    },

    removePlan(planId: string) {
      const planIndex = this.plans.findIndex((item) => item.id === planId)
      if (planIndex === -1) {
        throw new Error('计划不存在，可能已被删除。')
      }

      const plan = this.plans[planIndex]
      if (plan.status === 'live') {
        throw new Error('该计划正在投放中，请先暂停或结束后再删除。')
      }

      this.devices.forEach((device) => {
        if (device.currentPlanId === plan.id) {
          device.currentPlanId = undefined
          device.currentAdId = undefined
        }
      })

      const ad = this.ads.find((item) => item.id === plan.adId)
      if (ad?.planId === plan.id) {
        ad.planId = undefined
        ad.status = ad.materialIds.length ? 'submitted' : 'draft'
      }

      const [deleted] = this.plans.splice(planIndex, 1)
      return deleted
    },

    updateDevice(
      deviceId: string,
      payload: Partial<
        Pick<Device, 'building' | 'floor' | 'region' | 'screenSize' | 'status'>
      >
    ) {
      const device = this.devices.find((item) => item.id === deviceId)
      if (!device) return null
      Object.assign(device, payload)
      return device
    },

    removeDevice(deviceId: string) {
      const deviceIndex = this.devices.findIndex((item) => item.id === deviceId)
      if (deviceIndex === -1) {
        throw new Error('设备不存在，可能已被删除。')
      }

      const device = this.devices[deviceIndex]
      if (device.currentPlanId || device.currentAdId) {
        throw new Error('该设备当前仍在计划中使用，请先解除投放关联后再删除。')
      }

      const [deleted] = this.devices.splice(deviceIndex, 1)
      this.plans.forEach((plan) => {
        plan.deviceIds = plan.deviceIds.filter((item) => item !== deleted.id)
      })

      return deleted
    },

    updateDeviceStatus(deviceId: string, status: 'online' | 'offline' | 'fault') {
      const device = this.devices.find((item) => item.id === deviceId)
      if (!device) return null
      device.status = status
      return device
    },

    updatePlanStatus(
      planId: string,
      status: 'draft' | 'pending_schedule' | 'scheduled' | 'live' | 'paused' | 'finished'
    ) {
      const plan = this.plans.find((item) => item.id === planId)
      if (!plan) return
      plan.status = status

      const ad = this.ads.find((item) => item.id === plan.adId)
      if (ad) {
        if (status === 'live') ad.status = 'live'
        if (status === 'finished') ad.status = 'finished'
        if (status === 'paused' || status === 'scheduled') ad.status = 'planning'
      }

      if (status === 'live') {
        this.devices.forEach((device) => {
          if (plan.deviceIds.includes(device.id)) {
            device.currentPlanId = plan.id
            device.currentAdId = plan.adId
          }
        })
      }

      if (status === 'paused' || status === 'finished') {
        this.devices.forEach((device) => {
          if (device.currentPlanId === plan.id) {
            device.currentPlanId = undefined
            device.currentAdId = undefined
          }
        })
      }
    },

    updateBillStatus(billId: string, status: 'pending' | 'confirmed' | 'paid') {
      const bill = this.bills.find((item) => item.id === billId)
      if (!bill) return
      bill.status = status
    },

    updateWorkorderStatus(
      workorderId: string,
      status: 'open' | 'processing' | 'resolved'
    ) {
      const workorder = this.workorders.find((item) => item.id === workorderId)
      if (!workorder) return
      workorder.status = status
    },

    updateWorkorder(
      workorderId: string,
      payload: Partial<Pick<Workorder, 'priority' | 'assigneeId' | 'status'>>
    ) {
      const workorder = this.workorders.find((item) => item.id === workorderId)
      if (!workorder) return null
      Object.assign(workorder, payload)
      return workorder
    },

    resetDemoData() {
      this.ads = structuredClone(mockAds)
      this.materials = structuredClone(mockMaterials)
      this.plans = structuredClone(mockPlans)
      this.devices = structuredClone(mockDevices)
      this.bills = structuredClone(mockBills)
      this.workorders = structuredClone(mockWorkorders)
      this.feedback = structuredClone(mockFeedback)
      this.reports = structuredClone(mockReports)
    }
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useWorkflowStore, import.meta.hot))
}
