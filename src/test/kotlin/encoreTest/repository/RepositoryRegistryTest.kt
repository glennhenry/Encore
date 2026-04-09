package encoreTest.repository

import encore.repository.Repository
import encore.repository.RepositoryRegistry
import testHelper.assertDoesNotFail
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertIs

class RepositoryRegistryTest {
    @Test
    fun `test registry register and get`() {
        val registry = RepositoryRegistry()

        // register
        assertDoesNotFail {
            registry.register {
                add(TestRepository1::class) {
                    TestRepositoryImpl1A()
                }
                add(TestRepository2::class) {
                    TestRepositoryImpl2B()
                }
            }
        }

        // get should match the registered concrete implementation
        assertIs<TestRepositoryImpl1A>(registry.get(TestRepository1::class))
        assertIs<TestRepositoryImpl2B>(registry.get(TestRepository2::class))

        // unregistered fails
        assertFails {
            registry.get(TestRepository3::class)
        }

        // get by the registered concrete class fails (unregistered)
        assertFails {
            registry.get(TestRepositoryImpl1A::class)
        }
    }
}

interface TestRepository1 : Repository
interface TestRepository2 : Repository
interface TestRepository3 : Repository

class TestRepositoryImpl1A : TestRepository1 {
    override val name: String = "Impl 1A"
}

class TestRepositoryImpl2B : TestRepository2 {
    override val name: String = "Impl 2B"
}
