'use client'

import { useQuery } from '@tanstack/react-query'
import { categoryApi } from '@/services/api'
import { queryKeys } from './query-keys'

export function useCategories() {
  return useQuery({
    queryKey: queryKeys.categories,
    queryFn: () => categoryApi.getTree(),
    staleTime: Infinity,
  })
}
