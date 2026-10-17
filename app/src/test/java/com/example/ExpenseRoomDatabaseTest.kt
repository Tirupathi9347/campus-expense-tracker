package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CategoryBudget
import com.example.data.CategoryBudgetDao
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseDao
import com.example.data.ExpenseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Robolectric tests for Room database operations: insert, query Flow, update, and delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpenseRoomDatabaseTest {

    private lateinit var db: ExpenseDatabase
    private lateinit var dao: ExpenseDao
    private lateinit var categoryBudgetDao: CategoryBudgetDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.expenseDao()
        categoryBudgetDao = db.categoryBudgetDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveExpense() = runBlocking {
        val expense = Expense(
            title = "Campus Dorm Blanket",
            amount = 35.0,
            category = ExpenseCategory.HOUSING.name,
            dateMillis = 1000L,
            note = "Bedding supply"
        )
        val id = dao.insert(expense)
        val loaded = dao.getExpenseById(id)

        assertNotNull(loaded)
        assertEquals("Campus Dorm Blanket", loaded?.title)
        assertEquals(35.0, loaded?.amount ?: 0.0, 0.001)
        assertEquals(ExpenseCategory.HOUSING.name, loaded?.category)
    }

    @Test
    fun updateExpense() = runBlocking {
        val expense = Expense(
            title = "Dining Hall Lunch",
            amount = 12.0,
            category = ExpenseCategory.FOOD.name,
            dateMillis = 2000L
        )
        val id = dao.insert(expense)
        val inserted = dao.getExpenseById(id)!!

        val updated = inserted.copy(amount = 15.50, title = "Dining Hall Dinner")
        dao.update(updated)

        val reloaded = dao.getExpenseById(id)
        assertEquals(15.50, reloaded?.amount ?: 0.0, 0.001)
        assertEquals("Dining Hall Dinner", reloaded?.title)
    }

    @Test
    fun deleteExpense() = runBlocking {
        val expense = Expense(
            title = "Coffee",
            amount = 4.50,
            category = ExpenseCategory.FOOD.name,
            dateMillis = 3000L
        )
        val id = dao.insert(expense)
        assertNotNull(dao.getExpenseById(id))

        dao.deleteById(id)
        assertNull(dao.getExpenseById(id))
    }

    @Test
    fun getAllExpenses_orderedByDateDesc() = runBlocking {
        dao.insert(Expense(title = "Old", amount = 10.0, category = ExpenseCategory.OTHER.name, dateMillis = 100L))
        dao.insert(Expense(title = "Newest", amount = 20.0, category = ExpenseCategory.OTHER.name, dateMillis = 500L))
        dao.insert(Expense(title = "Mid", amount = 15.0, category = ExpenseCategory.OTHER.name, dateMillis = 300L))

        val list = dao.getAllExpenses().first()
        assertEquals(3, list.size)
        assertEquals("Newest", list[0].title)
        assertEquals("Mid", list[1].title)
        assertEquals("Old", list[2].title)
    }

    // --- Feature 1: CategoryBudgetDao Persistence Tests ---

    @Test
    fun insertAndRetrieveCategoryBudget() = runBlocking {
        val budget = CategoryBudget(categoryName = ExpenseCategory.FOOD.name, budgetLimit = 1500.0)
        categoryBudgetDao.setCategoryBudget(budget)

        val retrieved = categoryBudgetDao.getCategoryBudget(ExpenseCategory.FOOD.name)
        assertNotNull(retrieved)
        assertEquals(ExpenseCategory.FOOD.name, retrieved?.categoryName)
        assertEquals(1500.0, retrieved?.budgetLimit ?: 0.0, 0.001)
    }

    @Test
    fun updateCategoryBudget_onConflictReplacesLimit() = runBlocking {
        val initial = CategoryBudget(categoryName = ExpenseCategory.BOOKS.name, budgetLimit = 1000.0)
        categoryBudgetDao.setCategoryBudget(initial)

        val updated = CategoryBudget(categoryName = ExpenseCategory.BOOKS.name, budgetLimit = 1800.0)
        categoryBudgetDao.setCategoryBudget(updated)

        val retrieved = categoryBudgetDao.getCategoryBudget(ExpenseCategory.BOOKS.name)
        assertEquals(1800.0, retrieved?.budgetLimit ?: 0.0, 0.001)
    }

    @Test
    fun deleteCategoryBudget_removesEntry() = runBlocking {
        val budget = CategoryBudget(categoryName = ExpenseCategory.TRANSPORT.name, budgetLimit = 800.0)
        categoryBudgetDao.setCategoryBudget(budget)
        assertNotNull(categoryBudgetDao.getCategoryBudget(ExpenseCategory.TRANSPORT.name))

        categoryBudgetDao.deleteCategoryBudget(ExpenseCategory.TRANSPORT.name)
        assertNull(categoryBudgetDao.getCategoryBudget(ExpenseCategory.TRANSPORT.name))
    }

    @Test
    fun getAllCategoryBudgetsStream_emitsList() = runBlocking {
        categoryBudgetDao.setCategoryBudget(CategoryBudget(ExpenseCategory.FOOD.name, 1200.0))
        categoryBudgetDao.setCategoryBudget(CategoryBudget(ExpenseCategory.HOUSING.name, 4000.0))

        val list = categoryBudgetDao.getAllCategoryBudgetsStream().first()
        assertEquals(2, list.size)
        val names = list.map { it.categoryName }.toSet()
        assertEquals(setOf(ExpenseCategory.FOOD.name, ExpenseCategory.HOUSING.name), names)
    }
}
