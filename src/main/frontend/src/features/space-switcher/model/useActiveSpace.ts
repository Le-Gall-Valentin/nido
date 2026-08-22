import { useParams } from 'react-router-dom'
import type { SpaceSummary } from '@/entities/space'
import { useMySpaces } from './useMySpaces'

interface ActiveSpace {
  spaceId: string | undefined
  space: SpaceSummary | undefined
  isLoading: boolean
}

/**
 * Le contexte actif vient de l'URL, jamais du store : deux onglets ouverts sur
 * deux contextes doivent afficher deux contextes. Le store ne sert qu'à se
 * souvenir du dernier choix pour restaurer la navigation au démarrage.
 */
export function useActiveSpace(): ActiveSpace {
  const { spaceId } = useParams<{ spaceId: string }>()
  const { data: spaces, isLoading } = useMySpaces()
  const space = spaceId ? spaces?.find((s) => s.id === spaceId) : undefined
  return { spaceId, space, isLoading }
}
