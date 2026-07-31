export interface Bill {
  id: string
  advertiserId: string
  agentId?: string
  planId: string
  amount: number
  commission?: number
  invoiceRequested: boolean
  status: 'pending' | 'confirmed' | 'paid'
  createdAt: string
}
