package com.io.media.util

data class PaginationState(
    var currentPage: Int = 0,
    var isLoading: Boolean = false,
    var isLastPage: Boolean = false,
    val itemsPerPage: Int = 30,
    var totalItem: Int = 30
)