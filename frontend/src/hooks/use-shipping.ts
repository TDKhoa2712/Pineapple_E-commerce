'use client'

import { useQuery, useMutation } from '@tanstack/react-query'
import { shippingApi } from '@/services/api'

export function useProvinces(carrier?: string) {
  return useQuery({
    queryKey: ['shipping-provinces', carrier],
    queryFn: () => shippingApi.getProvinces(carrier),
    staleTime: 24 * 60 * 60 * 1000,
  })
}

export function useDistricts(provinceId: string | null, carrier?: string) {
  return useQuery({
    queryKey: ['shipping-districts', provinceId, carrier],
    queryFn: () => shippingApi.getDistricts(provinceId!, carrier),
    enabled: !!provinceId,
    staleTime: 24 * 60 * 60 * 1000,
  })
}

export function useWards(districtId: string | null, carrier?: string) {
  return useQuery({
    queryKey: ['shipping-wards', districtId, carrier],
    queryFn: () => shippingApi.getWards(districtId!, carrier),
    enabled: !!districtId,
    staleTime: 24 * 60 * 60 * 1000,
  })
}

export function useCalculateShippingFee() {
  return useMutation({
    mutationFn: ({ data, carrier }: { data: import('@/types').CalculateShippingFeeRequest; carrier?: string }) =>
      shippingApi.calculateFee(data, carrier),
  })
}
