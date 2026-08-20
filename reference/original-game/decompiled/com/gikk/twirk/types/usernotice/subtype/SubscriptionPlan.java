/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice.subtype;

public enum SubscriptionPlan {
    SUB_PRIME("Prime", "Prime"),
    SUB_LOW("1000", "4.99$"),
    SUB_MID("2000", "9.99$"),
    SUB_HIGH("3000", "24.99$");

    private final String message;
    private final String value;

    private SubscriptionPlan(String msg, String val) {
        this.message = msg;
        this.value = val;
    }

    public String getValue() {
        return this.value;
    }

    public static SubscriptionPlan of(String paramSubPlan) {
        for (SubscriptionPlan s : SubscriptionPlan.values()) {
            if (!s.message.equals(paramSubPlan)) continue;
            return s;
        }
        return SUB_LOW;
    }
}

