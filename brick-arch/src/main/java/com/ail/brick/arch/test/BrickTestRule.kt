package com.ail.brick.arch.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 用于 ViewModel 单元测试的 JUnit 4 TestRule。
 *
 * 自动在每个测试前将 [Dispatchers.Main] 替换为 [TestDispatcher]，测试后重置。
 * 消除每个测试类中重复的 `@Before/@After` 样板代码。
 *
 * ```kotlin
 * class MyViewModelTest {
 *     @get:Rule
 *     val brickTestRule = BrickTestRule()
 *
 *     @Test
 *     fun `test state update`() = runTest {
 *         val vm = MyViewModel()
 *         vm.dispatch(MyIntent.Load)
 *         advanceUntilIdle()
 *         assertEquals(expected, vm.state.value)
 *     }
 * }
 * ```
 *
 * @param testDispatcher 测试用调度器，默认 [UnconfinedTestDispatcher]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrickTestRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
