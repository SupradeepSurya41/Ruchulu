package com.ruchulu.userservice.model;

public enum AccountStatus {
    PENDING_VERIFICATION,   // registered but email not yet verified
    ACTIVE,                 // fully active account
    SUSPENDED,              // disabled by admin
    DEACTIVATED,            // user self-deactivated
    DELETED                 // soft-deleted
}
