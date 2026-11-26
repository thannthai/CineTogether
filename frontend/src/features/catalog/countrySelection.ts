// Same limit as the backend (docs/design/api.md, GET /movies).
export const MAX_COUNTRIES = 5

// The URL and the API carry the selection as one comma-separated value.
export function parseCountries(value: string | undefined): string[] {
  const unique = new Set((value ?? '').split(',').map((slug) => slug.trim()).filter(Boolean))
  return [...unique].slice(0, MAX_COUNTRIES)
}

export function toggleCountry(selected: string[], slug: string): string[] {
  if (selected.includes(slug)) return selected.filter((item) => item !== slug)
  return selected.length >= MAX_COUNTRIES ? selected : [...selected, slug]
}

// An empty selection means "all countries": no parameter at all.
export function countriesParam(selected: string[]): string | undefined {
  return selected.length > 0 ? selected.join(',') : undefined
}
