import { ApiError, errorMessage } from '../../../shared/lib/apiError'

// Why a source could not be resolved, in words for the viewer.
export function playbackMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) return 'Tập này chưa có nguồn phát. Hãy thử tập khác hoặc quay lại sau.'
    if (error.status === 401) return 'Bạn cần đăng nhập để xem phim.'
  }
  return errorMessage(error)
}
