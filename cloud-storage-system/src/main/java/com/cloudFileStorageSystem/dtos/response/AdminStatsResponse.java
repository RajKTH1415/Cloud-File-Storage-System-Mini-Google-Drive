package com.cloudFileStorageSystem.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
        "totalUsers",
        "activeUsers",
        "lockedUsers",
        "deletedUsers"
})
public class AdminStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long deletedUsers;
}