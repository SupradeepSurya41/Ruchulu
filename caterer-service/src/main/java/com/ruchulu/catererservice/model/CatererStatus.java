package com.ruchulu.catererservice.model;

public enum CatererStatus {
    PENDING_APPROVAL,   // submitted, awaiting admin approval
    ACTIVE,             // approved and live
    SUSPENDED,          // suspended by admin
    DEACTIVATED,        // caterer self-deactivated
    REJECTED            // admin rejected with reason
}
