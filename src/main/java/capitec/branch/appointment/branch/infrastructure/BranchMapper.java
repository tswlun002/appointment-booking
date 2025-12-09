package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    /**
     * Converts BranchEntity  Record to Branch (Domain Model).
     */
    Branch toDomain(BranchEntity entity);

    /**
     * Converts Branch (Domain Model) to BranchEntity  Record.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BranchEntity toEntity(Branch domain);
}