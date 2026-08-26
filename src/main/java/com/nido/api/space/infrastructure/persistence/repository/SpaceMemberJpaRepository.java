package com.nido.api.space.infrastructure.persistence.repository;

import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceMemberJpaRepository extends JpaRepository<SpaceMemberEntity, UUID> {

    Optional<SpaceMemberEntity> findBySpaceIdAndUserId(UUID spaceId, UUID userId);

    List<SpaceMemberEntity> findBySpaceIdOrderByJoinedAtAsc(UUID spaceId);

    List<SpaceMemberEntity> findByUserId(UUID userId);

    long countBySpaceId(UUID spaceId);

    @Query("""
        select new com.nido.api.space.infrastructure.persistence.repository.MemberCount(m.spaceId, count(m))
        from SpaceMemberEntity m
        where m.spaceId in :spaceIds
        group by m.spaceId
        """)
    List<MemberCount> countBySpaceIds(@Param("spaceIds") Collection<UUID> spaceIds);

    @Query("""
        select m from SpaceMemberEntity m
        where m.spaceId = :spaceId and m.userId <> :excludedUserId and m.role in :roles
        order by m.role asc, m.joinedAt asc
        """)
    List<SpaceMemberEntity> findCandidates(@Param("spaceId") UUID spaceId,
                                           @Param("excludedUserId") UUID excludedUserId,
                                           @Param("roles") Collection<SpaceRole> roles);

    @Modifying(clearAutomatically = true)
    @Query("update SpaceMemberEntity m set m.role = :role where m.id = :id")
    int updateRole(@Param("id") UUID id, @Param("role") SpaceRole role);
}
