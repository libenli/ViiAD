export interface AuditLog {
  id: string
  time: string
  actorId: string
  actorRole: string
  action: string
  targetType: string
  targetId: string
  detail?: string
}
