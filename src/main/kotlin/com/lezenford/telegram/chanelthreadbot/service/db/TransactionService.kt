package com.lezenford.telegram.chanelthreadbot.service.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManagerFactory

abstract class TransactionService {
    protected abstract val entityManagerFactory: EntityManagerFactory

    protected suspend inline fun <T> call(crossinline action: () -> T): T {
        return withContext(Dispatchers.IO) { action() }
    }

    protected suspend inline fun <T> callTransactional(crossinline action: () -> T): T? {
        return withContext(Dispatchers.IO) {
            TransactionTemplate(JpaTransactionManager(entityManagerFactory)).execute {
                action()
            }
        }
    }
}