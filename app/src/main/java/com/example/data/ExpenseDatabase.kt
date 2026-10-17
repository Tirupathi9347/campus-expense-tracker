package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Main Room Database for the Campus Expense Tracker.
 * RoomDatabase abstract class with singleton instance pattern.
 */
@Database(entities = [Expense::class, CategoryBudget::class], version = 2, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao

    companion object {
        @Volatile
        private var Instance: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "campus_expense_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabasePrepopulateCallback(context))
                    .build()
                    .also { Instance = it }
            }
        }

        /**
         * Initial seed data illustrating typical university student expenses
         * across current and recent dates.
         */
        private class DatabasePrepopulateCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = getDatabase(context).expenseDao()
                    val calendar = Calendar.getInstance()

                    // Today
                    val now = calendar.timeInMillis

                    // 1 day ago
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterday = calendar.timeInMillis

                    // 3 days ago
                    calendar.add(Calendar.DAY_OF_YEAR, -2)
                    val threeDaysAgo = calendar.timeInMillis

                    // 6 days ago
                    calendar.add(Calendar.DAY_OF_YEAR, -3)
                    val sixDaysAgo = calendar.timeInMillis

                    // 12 days ago
                    calendar.add(Calendar.DAY_OF_YEAR, -6)
                    val twelveDaysAgo = calendar.timeInMillis

                    val sampleExpenses = listOf(
                        Expense(
                            title = "Campus Bookstore Textbooks",
                            amount = 124.50,
                            category = ExpenseCategory.BOOKS.name,
                            dateMillis = now,
                            note = "Data Structures & Algorithms textbook"
                        ),
                        Expense(
                            title = "Student Union Cafe & Lunch",
                            amount = 14.80,
                            category = ExpenseCategory.FOOD.name,
                            dateMillis = now,
                            note = "Meal with study group"
                        ),
                        Expense(
                            title = "Monthly Campus Transit Pass",
                            amount = 45.00,
                            category = ExpenseCategory.TRANSPORT.name,
                            dateMillis = yesterday,
                            note = "Subsidized university bus pass"
                        ),
                        Expense(
                            title = "Campus Printing & Lab Supplies",
                            amount = 18.25,
                            category = ExpenseCategory.BOOKS.name,
                            dateMillis = threeDaysAgo,
                            note = "Final project poster printing"
                        ),
                        Expense(
                            title = "Campus Dorm Laundry Card Refill",
                            amount = 25.00,
                            category = ExpenseCategory.HOUSING.name,
                            dateMillis = sixDaysAgo,
                            note = "Quarterly laundry top-up"
                        ),
                        Expense(
                            title = "University Coding Society Hackathon Snack",
                            amount = 12.50,
                            category = ExpenseCategory.FOOD.name,
                            dateMillis = twelveDaysAgo,
                            note = "Late night study snacks"
                        )
                    )
                    dao.insertAll(sampleExpenses)
                }
            }
        }
    }
}
