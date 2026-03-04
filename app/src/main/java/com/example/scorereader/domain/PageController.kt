package com.example.scorereader.domain

class PageController(
    private val pageCountProvider: () -> Int,
    private val onPageChanged: (Int) -> Unit
) {

    var currentPage: Int = 0
        private set

    fun handle(command: PageCommand) {
        when (command) {
            PageCommand.NEXT -> {
                if (currentPage + 1 < pageCountProvider()) {
                    currentPage++
                    onPageChanged(currentPage)
                }
            }

            PageCommand.PREVIOUS -> {
                if (currentPage > 0) {
                    currentPage--
                    onPageChanged(currentPage)
                }
            }
        }
    }
}