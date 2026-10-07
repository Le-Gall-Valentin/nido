package com.nido.api.space.infrastructure.persistence.adapter;

import com.nido.api.infrastructure.config.SpaceKeyCache;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.shared.model.NameOrdering;
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
import com.nido.api.space.infrastructure.persistence.SpaceSalt;
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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpaceRepositoryAdapter implements SpaceRepository, SpaceCommandPort, SpaceMembershipPort, SpaceAdminPort {

    private static final String PERSONAL_SPACE_NAME = "Perso";

    private final SpaceJpaRepository spaces;
    private final SpaceMemberJpaRepository members;
    private final SpaceKeyCache keys;

    public SpaceRepositoryAdapter(SpaceJpaRepository spaces, SpaceMemberJpaRepository members, SpaceKeyCache keys) {
        this.spaces = spaces;
        this.members = members;
        this.keys = keys;
    }

    @Override
    public Optional<Space> findById(UUID spaceId) {
        return spaces.findById(spaceId).map(this::toSpace);
    }

    @Override
    public Optional<Space> findPersonalOwnedBy(UUID userId) {
        return spaces.findByPersonalOwnerId(userId).map(this::toSpace);
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
            .map(e -> new SpaceSummaryView(e.getId(), e.getType(), nameOf(e), e.getAccent(), e.getGlyph(),
                ZoneId.of(e.getTimezone()),
                roleBySpace.get(e.getId()), counts.getOrDefault(e.getId(), 0L)))
            // l'espace perso d'abord, puis les groupes par nom
            .sorted(Comparator.comparing((SpaceSummaryView v) -> v.type() != SpaceType.PERSONAL)
                .thenComparing(SpaceSummaryView::name, NameOrdering.comparator()))
            .toList();
    }

    @Override
    public long countMembers(UUID spaceId) {
        return members.countBySpaceId(spaceId);
    }

    @Override
    public List<Space> findByIds(Collection<UUID> spaceIds) {
        return spaces.findAllById(spaceIds).stream()
            .map(this::toSpace)
            .toList();
    }

    @Override
    public String findEncryptionSaltById(UUID spaceId) {
        return spaces.findById(spaceId).map(SpaceEntity::getEncryptionSalt).orElseThrow(SpaceException.SpaceNotFound::new);
    }

    @Override
    public PageResult<SpaceAdminView> findAll(int page, int size) {
        Page<SpaceEntity> found = spaces.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        Map<UUID, Long> counts = found.isEmpty()
            ? Map.of()
            : members.countBySpaceIds(found.getContent().stream().map(SpaceEntity::getId).toList()).stream()
                .collect(Collectors.toMap(MemberCount::spaceId, MemberCount::total));
        List<SpaceAdminView> content = found.getContent().stream()
            .map(e -> new SpaceAdminView(e.getId(), e.getType(), nameOf(e),
                counts.getOrDefault(e.getId(), 0L), e.getCreatedBy(), e.getCreatedAt()))
            .toList();
        return new PageResult<>(content, found.getTotalElements(), page, size);
    }

    @Override
    public Space createPersonal(UUID ownerUserId) {
        SpaceEntity e = new SpaceEntity();
        e.setType(SpaceType.PERSONAL);
        e.setEncryptionSalt(SpaceSalt.random());
        e.setNameEncrypted(sealerOf(e).seal(SpaceEntity.NAME, e.getId(), PERSONAL_SPACE_NAME));
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
        // The salt before the insert: the name is sealed with it.
        e.setEncryptionSalt(SpaceSalt.random());
        SpaceSealer sealer = sealerOf(e);
        e.setNameEncrypted(sealer.seal(SpaceEntity.NAME, e.getId(), command.name()));
        e.setDescriptionEncrypted(sealer.sealNullable(SpaceEntity.DESCRIPTION, e.getId(), command.description()));
        e.setAccent(command.accent());
        e.setGlyph(command.glyph());
        e.setCreatedBy(command.creatorUserId());
        e.setTimezone(command.timezone().getId());
        return save(e);
    }

    @Override
    public void update(UpdateSpaceCommand command) {
        SpaceEntity e = spaces.findById(command.spaceId())
            .orElseThrow(SpaceException.SpaceNotFound::new);
        // Modification partielle : seuls les champs fournis sont appliqués. Une description
        // vide est un effacement explicite, à distinguer d'une absence.
        SpaceSealer sealer = sealerOf(e);
        if (command.name() != null) e.setNameEncrypted(sealer.seal(SpaceEntity.NAME, e.getId(), command.name()));
        if (command.description() != null) {
            e.setDescriptionEncrypted(command.description().isEmpty() ? null
                : sealer.seal(SpaceEntity.DESCRIPTION, e.getId(), command.description()));
        }
        if (command.accent() != null) e.setAccent(command.accent());
        if (command.glyph() != null) e.setGlyph(command.glyph());
        if (command.timezone() != null) e.setTimezone(command.timezone().getId());
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
        // Trois paliers : le plus haut rôle restant hérite, le plus ancien d'abord. Un lecteur
        // hérite en dernier recours, plutôt que de détruire un contenu que d'autres consultent.
        List<SpaceMemberEntity> candidates = members.findCandidates(
            spaceId, excludedUserId, List.of(SpaceRole.ADMIN, SpaceRole.MEMBER, SpaceRole.VIEWER));
        return candidates.stream()
            .min(Comparator.comparingInt((SpaceMemberEntity m) -> -m.getRole().rank())
                .thenComparing(SpaceMemberEntity::getJoinedAt))
            .map(SpaceRepositoryAdapter::toDomain);
    }

    private Space save(SpaceEntity e) {
        try {
            return toSpace(spaces.saveAndFlush(e));
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

    // Built on the key cache with the row's own salt rather than through SpaceSealers, which asks this module for the
    // salt: the space module would depend on itself through a bean.
    private SpaceSealer sealerOf(SpaceEntity e) {
        return SpaceSealer.of(keys.forSpace(e.getId(), e::getEncryptionSalt));
    }

    private String nameOf(SpaceEntity e) {
        return sealerOf(e).open(SpaceEntity.NAME, e.getId(), e.getNameEncrypted());
    }

    private Space toSpace(SpaceEntity e) {
        SpaceSealer sealer = sealerOf(e);
        return new Space(e.getId(), e.getType(), sealer.open(SpaceEntity.NAME, e.getId(), e.getNameEncrypted()),
            sealer.openNullable(SpaceEntity.DESCRIPTION, e.getId(), e.getDescriptionEncrypted()),
            e.getAccent(), e.getGlyph(), e.getPersonalOwnerId(),
            ZoneId.of(e.getTimezone()), e.getCreatedAt());
    }

    private static SpaceMembership toDomain(SpaceMemberEntity e) {
        return new SpaceMembership(e.getId(), e.getSpaceId(), e.getUserId(), e.getRole(), e.getJoinedAt());
    }
}
