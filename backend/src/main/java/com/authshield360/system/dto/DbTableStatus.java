package com.authshield360.system.dto;

/** Row count for one table. rows = -1 means the table could not be read. */
public record DbTableStatus(String table, long rows) {
}
