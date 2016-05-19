package org.jbei.ice.lib.dto.entry;

/**
 * Type of visibility for entries in the system.
 * DELETED     -> Entry has been deleted
 * DRAFT       -> Entry is part of a bulk upload that is still being recorded
 * PENDING     -> Entry is part of a bulk upload that has been submitted and is pending approval by an admin
 * TRANSFERRED -> Entry was transferred from another registry pending approval from admin
 * OK          -> Entry is available to be viewed by those with adequate permissions
 *
 * @author Hector Plahar
 */
public enum Visibility {

    DELETED(-1), DRAFT(0), PENDING(1), TRANSFERRED(2), OK(9);

    private final int value;

    Visibility(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static Visibility valueToEnum(Integer value) {
        // this is for legacy reasons. Visibility was abandoned for a while
        if (value == null)
            return OK;

        switch (value) {
            case -1:
                return DELETED;

            case 0:
                return DRAFT;

            case 1:
                return PENDING;

            case 2:
                return TRANSFERRED;

            case 9:
            default:
                return OK;
        }
    }
}
