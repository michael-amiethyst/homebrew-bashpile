package com.bashpile.engine

import org.junit.jupiter.api.*

@Order(3)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UnwinderTest {
    @Test
    @Order(10)
    fun unwindAllWithFunctionCallWorks() {
        // kotlin stub for tests
    }
}