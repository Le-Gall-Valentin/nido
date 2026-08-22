package com.nido.api.space.infrastructure.persistence.adapter;

import com.nido.api.shared.model.PageResult;
import com.nido.api.space.domain.model.CreateSharedSpaceCommand;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceAdminView;
import com.nido.api.space.domain.model.SpaceAppearance;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.model.UpdateSpaceCommand;
import com.nido.api.space.domain.port.out.SpaceAdminPort;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.MemberCount;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpaceRepositoryAdapter implements SpaceRepository, SpaceCommandPort, SpaceMembershipPort, SpaceAdminPort {

    private static final String PERSONAL_SPACE_NAME = "Perso";

    private final SpaceJpaRepository spaces;
    private final SpaceMemberJpaRepository members;

    public SpaceRepositoryAdapter(SpaceJpaRepository spaces, SpaceMemberJpaRepository members) {
        this.spaces = spaces;
        this.members = members;
    }

    @Override
    public Optional<Space> findById(UUID spaceId) {
        return spaces.findById(spaceId).map(SpaceRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Space> findPersonalOwnedBy(UUID userId) {
        return spaces.findByPersonalOwnerId(userId).map(SpaceRepositoryAdapter::toDomain);
    }

    @Override
    public List<SpaceSummaryView> findMySpaces(UUID userId) {
        List<SpaceMemberEntity> memberships = members.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<UUID, SpaceRole> roleBySpace = memberships.stream()
            .collect(Collectors.toMap(SpaceMemberEntity::getSpaceId, SpaceMemberEntity::getRole));
        Map<UUID, Long> counts = members.countBySpaceIds(roleBySpace.keySet()).stream()
            .collect(Collectors.toMap(MemberCount::spaceId, MemberCount::total));
        return spaces.findAllById(roleBySpace.keySet()).stream()
            .map(e -> new SpaceSummaryView(e.getId(), e.getType(), e.getName(), e.getAccent(), e.getGlyph(),
                roleBySpace.get(e.getId()), counts.getOrDefault(e.getId(), 0L)))
            // l'espace perso d'abord, puis les groupes par nom
            .sorted(Comparator.comparing((SpaceSummaryView v) -> v.type() != SpaceType.PERSONAL)
                .thenComparing(SpaceSummaryView::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public long countMembers(UUID spaceId) {
        return members.countBySpaceId(spaceId);
    }

    @Override
    public PageResult<SpaceAdminView> findAll(int page, int size) {
        Page<SpaceEntity> found = spaces.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        Map<UUID, Long> counts = found.isEmpty()
            ? Map.of()
            : members.countBySpaceIds(found.getContent().stream().map(SpaceEntity::getId).toList()).stream()
                .collect(Collectors.toMap(MemberCount::spaceId, MemberCount::total));
        List<SpaceAdminView> content = found.getContent().stream()
            .map(e -> new SpaceAdminView(e.getId(), e.getType(), e.getName(),
                counts.getOrDefault(e.getId(), 0L), e.getCreatedBy(), e.getCreatedAt()))
            .toList();
        return new PageResult<>(content, found.getTotalElements(), page, size);
    }

    @Override
    public Space createPersonal(UUID ownerUserId) {
        SpaceEntity e = new SpaceEntity();
        e.setType(SpaceType.PERSONAL);
        e.setName(PERSONAL_SPACE_NAME);
        e.setAccent(SpaceAppearance.PERSONAL_ACCENT);
        e.setGlyph(SpaceAppearance.PERSONAL_GLYPH);
        e.setPersonalOwnerId(ownerUserId);
        e.setCreatedBy(ownerUserId);
        return save(e);
    }

    @Override
    public Space createShared(CreateSharedSpaceCommand command) {
        SpaceEntity e = new SpaceEntity();
        e.setType(SpaceType.SHARED);
        e.setName(command.name());
        e.setDescription(command.description());
        e.setAccent(command.accent());
        e.setGlyph(command.glyph());
        e.setCreatedBy(command.creatorUserId());
        return save(e);
    }

    @Override
    public void update(UpdateSpaceCommand command) {
        SpaceEntity e = spaces.findById(command.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        e.setName(command.name());
        e.setDescription(command.description());
        e.setAccent(command.accent());
        e.setGlyph(command.glyph());
        save(e);
    }

    @Override
    public void delete(UUID spaceId) {
        spaces.deleteById(spaceId);
        spaces.flush();
    }

    @Override
    public Optional<SpaceMembership> find(UUID spaceId, UUID userId) {
        return members.findBySpaceIdAndUserId(spaceId, userId).map(SpaceRepositoryAdapter::toDomain);
    }

    @Override
    public List<SpaceMembership> findMemberships(UUID spaceId) {
        return members.findBySpaceIdOrderByJoinedAtAsc(spaceId).stream()
            .map(SpaceRepositoryAdapter::toDomain)
            .toList();
    }

    @Override
    public List<SpaceMembership> findByUser(UUID userId) {
        return members.findByUserId(userId).stream().map(SpaceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public SpaceMembership add(UUID spaceId, UUID userId, SpaceRole role) {
        try {
            SpaceMemberEntity e = new SpaceMemberEntity();
            e.setSpaceId(spaceId);
            e.setUserId(userId);
            e.setRole(role);
            return toDomain(members.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(ex);
        }
    }

    @Override
    public void changeRole(UUID membershipId, SpaceRole newRole) {
        try {
            if (members.updateRole(membershipId, newRole) == 0) {
                throw new SpaceException.MemberNotFound();
            }
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(ex);
        }
    }

    @Override
    public void remove(UUID membershipId) {
        members.deleteById(membershipId);
        members.flush();
    }

    @Override
    public Optional<SpaceMembership> findSuccessor(UUID spaceId, UUID excludedUserId) {
        // Un VIEWER n'hérite jamais de la propriété : ADMIN d'abord, puis MEMBER, le plus ancien gagne.
        List<SpaceMemberEntity> candidates = members.findCandidates(
            spaceId, excludedUserId, List.of(SpaceRole.ADMIN, SpaceRole.MEMBER));
        return candidates.stream()
            .sorted(Comparator.comparing((SpaceMemberEntity m) -> m.getRole() != SpaceRole.ADMIN)
                .thenComparing(SpaceMemberEntity::getJoinedAt))
            .findFirst()
            .map(SpaceRepositoryAdapter::toDomain);
    }

    private Space save(SpaceEntity e) {
        try {
            return toDomain(spaces.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(ex);
        }
    }

    private SpaceException resolveConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String c = cve.getConstraintName();
            if (c != null) {
                if (c.contains("uq_spaces_personal_owner")) return new SpaceException.PersonalSpaceAlreadyExists();
                if (c.contains("uq_space_members_space_user")) return new SpaceException.AlreadyMember();
                if (c.contains("uq_space_members_single_owner")) return new SpaceException.OwnerAlreadyExists();
            }
        }
        return new SpaceException.DataIntegrityError();
    }

    private static Space toDomain(SpaceEntity e) {
        return new Space(e.getId(), e.getType(), e.getName(), e.getDescription(),
            e.getAccent(), e.getGlyph(), e.getPersonalOwnerId(), e.getCreatedAt());
    }

    private static SpaceMembership toDomain(SpaceMemberEntity e) {
        return new SpaceMembership(e.getId(), e.getSpaceId(), e.getUserId(), e.getRole(), e.getJoinedAt());
    }
}
