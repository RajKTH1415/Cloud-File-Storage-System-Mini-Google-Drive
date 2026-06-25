package com.cloudFileStorageSystem.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long deletedUsers;
}