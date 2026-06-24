package luzzr.ji

import android.content.Context
import luzzr.ji.core.database.AppDatabase
import luzzr.ji.core.payment.PaymentRecognitionManager
import luzzr.ji.data.repositoryImpl.BudgetRepositoryImpl
import luzzr.ji.data.repositoryImpl.TransactionRepositoryImpl
import luzzr.ji.domain.repository.BudgetRepository
import luzzr.ji.domain.repository.TransactionRepository
import luzzr.ji.domain.usecase.*

interface AppContainer {
    val transactionRepository: TransactionRepository
    val budgetRepository: BudgetRepository
    val paymentRecognitionManager: PaymentRecognitionManager
    val secureStorage: luzzr.ji.core.common.SecureStorage
    val observeTransactionsUseCase: ObserveTransactionsUseCase
    val createTransactionUseCase: CreateTransactionUseCase
    val updateTransactionUseCase: UpdateTransactionUseCase
    val deleteTransactionUseCase: DeleteTransactionUseCase
    val migrateTransactionUseCase: MigrateTransactionUseCase
    val observeBudgetUseCase: ObserveBudgetUseCase
    val saveBudgetUseCase: SaveBudgetUseCase
    val getExtraBillOverviewUseCase: GetExtraBillOverviewUseCase
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database.budgetDao())
    }

    override val secureStorage: luzzr.ji.core.common.SecureStorage by lazy {
        luzzr.ji.core.common.SecureStorageImpl(context)
    }

    override val paymentRecognitionManager: PaymentRecognitionManager by lazy {
        PaymentRecognitionManager(
            context = context,
            database = database,
            secureStorage = secureStorage,
            sharedPreferences = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
        )
    }

    override val observeTransactionsUseCase: ObserveTransactionsUseCase by lazy {
        ObserveTransactionsUseCase(transactionRepository)
    }

    override val createTransactionUseCase: CreateTransactionUseCase by lazy {
        CreateTransactionUseCase(transactionRepository)
    }

    override val updateTransactionUseCase: UpdateTransactionUseCase by lazy {
        UpdateTransactionUseCase(transactionRepository)
    }

    override val deleteTransactionUseCase: DeleteTransactionUseCase by lazy {
        DeleteTransactionUseCase(transactionRepository)
    }

    override val migrateTransactionUseCase: MigrateTransactionUseCase by lazy {
        MigrateTransactionUseCase(transactionRepository)
    }

    override val observeBudgetUseCase: ObserveBudgetUseCase by lazy {
        ObserveBudgetUseCase(budgetRepository)
    }

    override val saveBudgetUseCase: SaveBudgetUseCase by lazy {
        SaveBudgetUseCase(budgetRepository)
    }

    override val getExtraBillOverviewUseCase: GetExtraBillOverviewUseCase by lazy {
        GetExtraBillOverviewUseCase(transactionRepository, budgetRepository)
    }
}
