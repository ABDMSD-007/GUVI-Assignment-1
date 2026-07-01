package org.northernarc.assessment4.dto;

/**
 * Type-safe Spring Data interface projection for the "total balance per branch"
 * aggregation. Using a projection avoids returning raw {@code Object[]} rows and
 * removes the need for unchecked casts in the service layer.
 *
 * The JPQL query must alias the selected columns to match these getter names
 * (i.e. {@code AS branch} and {@code AS totalBalance}).
 */
public interface BranchBalance {

    String getBranch();

    Double getTotalBalance();
}

