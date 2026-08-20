/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

public class SteamAuth {

    public static enum UserHasLicenseForAppResult {
        HasLicense,
        DoesNotHaveLicense,
        NoAuth;

        private static final UserHasLicenseForAppResult[] values;

        static UserHasLicenseForAppResult byOrdinal(int result) {
            return values[result];
        }

        static {
            values = UserHasLicenseForAppResult.values();
        }
    }

    public static enum AuthSessionResponse {
        OK,
        UserNotConnectedToSteam,
        NoLicenseOrExpired,
        VACBanned,
        LoggedInElseWhere,
        VACCheckTimedOut,
        AuthTicketCanceled,
        AuthTicketInvalidAlreadyUsed,
        AuthTicketInvalid,
        PublisherIssuedBan;

        private static final AuthSessionResponse[] values;

        static AuthSessionResponse byOrdinal(int authSessionResponse) {
            return values[authSessionResponse];
        }

        static {
            values = AuthSessionResponse.values();
        }
    }

    public static enum BeginAuthSessionResult {
        OK,
        InvalidTicket,
        DuplicateRequest,
        InvalidVersion,
        GameMismatch,
        ExpiredTicket;

        private static final BeginAuthSessionResult[] values;

        static BeginAuthSessionResult byOrdinal(int authSessionResponse) {
            return values[authSessionResponse];
        }

        static {
            values = BeginAuthSessionResult.values();
        }
    }
}

