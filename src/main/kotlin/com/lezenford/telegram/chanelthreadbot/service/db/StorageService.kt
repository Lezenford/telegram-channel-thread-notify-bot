package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManagerFactory

abstract class StorageService {
    protected abstract val entityManagerFactory: EntityManagerFactory

    protected suspend inline fun <T> call(crossinline action: () -> T): T {
        return withContext(Dispatchers.IO) { action() }
    }

    protected suspend inline fun <T> callTransactional(crossinline action: () -> T): T? {
        return kotlin.runCatching {
            withContext(Dispatchers.IO) {
                TransactionTemplate(JpaTransactionManager(entityManagerFactory)).execute {
                    action()
                }
            }
        }.onFailure {
            log.error("Transactional database operation error", it)
        }.getOrNull()
    }

    protected companion object {
        val log by Logger()
    }
}
