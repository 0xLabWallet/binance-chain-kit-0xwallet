package com.wallet0x.binancechainkit.storage

import androidx.sqlite.db.SimpleSQLiteQuery
import com.wallet0x.binancechainkit.core.IStorage
import com.wallet0x.binancechainkit.models.Balance
import com.wallet0x.binancechainkit.models.LatestBlock
import com.wallet0x.binancechainkit.models.SyncState
import com.wallet0x.binancechainkit.models.Transaction

class Storage(private val database: KitDatabase) : IStorage {

    override fun getAllBalances(): List<Balance>?
    {
        return database.balance.getAll()
    }

    override fun removeBalances(balances: List<Balance>)
    {
        database.balance.delete(balances.map { it.symbol })
    }

    // LatestBlock

    override var latestBlock: LatestBlock?
        get() = database.latestBlock.get()
        set(value) {
            value?.let {
                database.latestBlock.update(value)
            }
        }

    // SyncState

    override var syncState: SyncState?
        get() = database.syncState.get()
        set(value) {
            value?.let {
                database.syncState.update(value)
            }
        }

    // Balance

    override fun getBalance(symbol: String): Balance? {
        return database.balance.getBalance(symbol)
    }

    override fun setBalances(balances: List<Balance>) {
        database.balance.insertAll(balances)
    }

    // Transactions

    override fun addTransactions(transactions: List<Transaction>) {
        database.transactions.insertAll(transactions)
    }

    override fun getTransaction(hash: String): Transaction? {
        return database.transactions.getByHash(hash)
    }

    override fun getTransactions(
        symbol: String,
        fromAddress: String?,
        toAddress: String?,
        fromTransactionHash: String?,
        limit: Int?
    ): List<Transaction> {
        var sql = "SELECT * FROM `Transaction` WHERE symbol = '$symbol'"

        fromAddress?.let {
            sql += " AND `from` = '$it'"
        }

        toAddress?.let {
            sql += " AND `to` = '$it'"
        }

        fromTransactionHash?.let {
            database.transactions.getByHash(it)?.let { transaction ->
                sql += " AND `blockTime` <= ${transaction.blockTime.time}"
                sql += " AND `transactionId` <> '${transaction.transactionId}'"
            }
        }

        sql += " ORDER BY `blockTime` DESC"

        limit?.let {
            sql += " LIMIT $it"
        }

        return database.transactions.getSql(SimpleSQLiteQuery(sql))
    }

}
