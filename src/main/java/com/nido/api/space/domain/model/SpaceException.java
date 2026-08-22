package com.nido.api.space.domain.model;

public abstract sealed class SpaceException extends RuntimeException
    permits SpaceException.SpaceNotFound,
            SpaceException.NotAMember,
            SpaceException.MemberNotFound,
            SpaceException.InsufficientRole,
            SpaceException.OwnerRequired,
            SpaceException.PersonalSpaceImmutable,
            SpaceException.InvalidAppearance,
            SpaceException.InvalidSpaceName,
            SpaceException.OwnerRoleNotAssignable,
            SpaceException.SelfManagementForbidden,
            SpaceException.RoleAlreadyAssigned,
            SpaceException.LastOwnerCannotLeave,
            SpaceException.SpaceNotEmpty,
            SpaceException.AlreadyMember,
            SpaceException.PersonalSpaceAlreadyExists,
            SpaceException.DataIntegrityError {

    private SpaceException(String message) { super(message); }

    public static final class SpaceNotFound extends SpaceException {
        public SpaceNotFound() { super("Space not found"); }
    }
    /** Non-membre : jamais 403, pour ne pas révéler l'existence du contexte. */
    public static final class NotAMember extends SpaceException {
        public NotAMember() { super("Space not found"); }
    }
    public static final class MemberNotFound extends SpaceException {
        public MemberNotFound() { super("Member not found in this space"); }
    }
    public static final class InsufficientRole extends SpaceException {
        public InsufficientRole() { super("Insufficient role in this space"); }
    }
    public static final class OwnerRequired extends SpaceException {
        public OwnerRequired() { super("Only the owner can perform this action"); }
    }
    public static final class PersonalSpaceImmutable extends SpaceException {
        public PersonalSpaceImmutable() { super("The personal space cannot be modified, shared or deleted"); }
    }
    public static final class InvalidAppearance extends SpaceException {
        public InvalidAppearance() { super("Accent or glyph outside the allowed palette"); }
    }
    public static final class InvalidSpaceName extends SpaceException {
        public InvalidSpaceName() { super("Space name must be between 1 and 80 characters"); }
    }
    public static final class OwnerRoleNotAssignable extends SpaceException {
        public OwnerRoleNotAssignable() { super("The OWNER role is only reachable through an ownership transfer"); }
    }
    public static final class SelfManagementForbidden extends SpaceException {
        public SelfManagementForbidden() { super("You cannot manage your own membership"); }
    }
    public static final class RoleAlreadyAssigned extends SpaceException {
        public RoleAlreadyAssigned() { super("Member already has this role"); }
    }
    public static final class LastOwnerCannotLeave extends SpaceException {
        public LastOwnerCannotLeave() { super("Transfer ownership before leaving this space"); }
    }
    public static final class SpaceNotEmpty extends SpaceException {
        public SpaceNotEmpty() { super("Space still has members"); }
    }
    public static final class AlreadyMember extends SpaceException {
        public AlreadyMember() { super("User is already a member of this space"); }
    }
    public static final class PersonalSpaceAlreadyExists extends SpaceException {
        public PersonalSpaceAlreadyExists() { super("User already has a personal space"); }
    }
    public static final class DataIntegrityError extends SpaceException {
        public DataIntegrityError() { super("Unexpected data integrity violation"); }
    }
}
