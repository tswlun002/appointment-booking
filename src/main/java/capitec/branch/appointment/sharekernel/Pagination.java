package capitec.branch.appointment.sharekernel;

public record Pagination(
        int totalCount,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        boolean isFirstPage,
        boolean isLastPage
) {

}
