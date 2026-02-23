package capitec.branch.appointment.sharekernel;

/**
 * Pagination metadata for paginated responses.
 * Calculates derived pagination fields from page, limit, and totalCount.
 */
public record Pagination(
        int totalCount,
        int page,
        int limit,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        boolean isFirstPage,
        boolean isLastPage
) {

    /**
     * Creates Pagination with calculated derived fields.
     *
     * @param page       current page number (0-based)
     * @param limit      number of items per page
     * @param totalCount total number of items across all pages
     * @return Pagination with all fields calculated
     */
    public static Pagination of(int page, int limit, int totalCount) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (totalCount < 0) {
            throw new IllegalArgumentException("Total count must be non-negative");
        }

        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / limit);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        boolean isFirstPage = page == 0;
        boolean isLastPage = totalCount == 0 || page >= totalPages - 1;

        return new Pagination(
                totalCount,
                page,
                limit,
                totalPages,
                hasNext,
                hasPrevious,
                isFirstPage,
                isLastPage
        );
    }

    /**
     * Creates Pagination using offset-based pagination.
     * Converts offset to page number internally.
     *
     * @param offset     number of items to skip
     * @param limit      number of items per page
     * @param totalCount total number of items across all pages
     * @return Pagination with all fields calculated
     */
    public static Pagination ofOffset(int offset, int limit, int totalCount) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        int page = offset / limit;
        return of(page, limit, totalCount);
    }
}
