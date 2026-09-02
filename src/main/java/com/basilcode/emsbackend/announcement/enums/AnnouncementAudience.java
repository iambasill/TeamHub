package com.basilcode.emsbackend.announcement.enums;

/** Who an announcement is aimed at. An announcement with no audiences recorded (or explicitly
 * including {@code ALL}) reaches every employee; anything else is a targeted subset — role-based
 * ({@code EMPLOYEE}/{@code HR}/{@code MANAGER}/{@code ADMIN}/{@code SUPER_ADMIN}, matching {@code
 * UserTypeEnum}) or membership-based ({@code BOARD_MEMBERS}, which isn't a role at all but a
 * separate admin-managed roster — see {@code BoardMembershipService}). An announcement can target
 * more than one audience at once. */
public enum AnnouncementAudience {
    ALL,
    EMPLOYEE,
    HR,
    MANAGER,
    ADMIN,
    SUPER_ADMIN,
    BOARD_MEMBERS
}
