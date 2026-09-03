const SAFE_TEXT_PATTERN = /[^\u4e00-\u9fa5A-Za-z0-9\s\-_（）()，。,.、：:；;！!？?]/g

export function sanitizeBusinessText(value?: string) {
  return (value || '').replace(SAFE_TEXT_PATTERN, '')
}
