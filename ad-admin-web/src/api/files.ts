import { request, type ApiResponse } from '@/utils/request'

export interface FileUploadResult {
  url: string
  objectKey: string
  originalFilename: string
  contentType?: string
  size: number
}

export function uploadFile(file: File, folder = 'materials', onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', folder)
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 10 * 60 * 1000,
    onUploadProgress: (event) => {
      if (!event.total) return
      onProgress?.(Math.round((event.loaded / event.total) * 100))
    }
  }) as Promise<ApiResponse<FileUploadResult>>
}
