package luzzr.ji.domain.repository

import kotlinx.coroutines.flow.Flow
import luzzr.ji.domain.model.Transaction

interface TransactionRepository {
    fun observeAllTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun saveTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
}
