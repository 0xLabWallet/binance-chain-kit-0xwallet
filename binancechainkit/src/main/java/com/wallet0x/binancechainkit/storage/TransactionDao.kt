package com.wallet0x.binancechainkit.storage

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.wallet0x.binancechainkit.models.Transaction

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM `Transaction` WHERE symbol = :symbol ORDER BY blockTime DESC")
    fun getAll(symbol: String): List<Transaction>

    @Query("DELETE FROM `Transaction`")
    fun deleteAll()

    @Query("SELECT * FROM `Transaction` WHERE transactionId = :hash LIMIT 1")
    fun getByHash(hash: String) : Transaction?

    @RawQuery
    fun getSql(query: SupportSQLiteQuery): List<Transaction>
}
