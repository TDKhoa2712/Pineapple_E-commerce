package backend.pineapple_ecommerce.mapper;

import backend.pineapple_ecommerce.dto.request.CreateAddressRequest;
import backend.pineapple_ecommerce.dto.response.AddressResponse;
import backend.pineapple_ecommerce.entity.Address;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "carrierMetadata", source = "carrierMetadata", qualifiedByName = "mapToJsonString")
    Address toEntity(CreateAddressRequest request);

    @Mapping(target = "carrierMetadata", source = "carrierMetadata", qualifiedByName = "jsonStringToMap")
    AddressResponse toResponse(Address address);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "carrierMetadata", source = "carrierMetadata", qualifiedByName = "mapToJsonString")
    void updateFromRequest(CreateAddressRequest request, @MappingTarget Address address);

    // === Custom conversions ===
    @Named("mapToJsonString")
    default String mapToJsonString(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Cannot convert Map to JSON string", e);
        }
    }

    @Named("jsonStringToMap")
    default Map<String, Object> jsonStringToMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Cannot convert JSON string to Map", e);
        }
    }
}