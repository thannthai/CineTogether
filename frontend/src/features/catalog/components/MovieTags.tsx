import type { EpisodeProgress, MovieType } from '../types'
import { languageLabel, progressLabel } from '../movieLabels'

type Props = {
  quality?: string | null
  language?: string | null
  progress?: EpisodeProgress | null
  type: MovieType
  // 'badges' = quality and language only, 'progress' = the episode sticker only; default shows all.
  part?: 'badges' | 'progress'
  className?: string
}

// Small stickers: quality (FHD or HD) and language (Vietsub) are neutral; only the progress has a color. The progress ("Tập 8/24", "Hoàn tất 36/36", "Full").
export function MovieTags({ quality, language, progress, type, part, className = '' }: Props) {
  const label = part === 'badges' ? null : progressLabel(progress, type)
  language = languageLabel(language)
  if (part === 'progress') quality = language = null
  if (!quality && !language && !label) return null
  return (
    <ul className={`movie-tags ${className}`}>
      {quality && <li className="tag-quality">{quality}</li>}
      {language && <li className="tag-language">{language}</li>}
      {label && <li className={progress?.completed ? 'tag-done' : 'tag-progress'}>{label}</li>}
    </ul>
  )
}
