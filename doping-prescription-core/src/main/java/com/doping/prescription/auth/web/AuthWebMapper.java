package com.doping.prescription.auth.web;

import com.doping.prescription.auth.persistence.UserAccount;
import java.time.LocalDate;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps users to API responses; entities never leave the module (docs/kod-standartlari.md §2). */
@Mapper(componentModel = "spring")
interface AuthWebMapper {

    @Mapping(target = "userId", source = "id")
    AuthDtos.MeResponse toMe(UserAccount user);

    AuthDtos.DemoUserResponse toDemoUser(UserAccount user);

    default LocalDate unwrap(Optional<LocalDate> date) {
        return date.orElse(null);
    }
}
