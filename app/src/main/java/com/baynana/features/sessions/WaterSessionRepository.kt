package com.baynana.features.sessions

import com.baynana.features.customers.Customer
import com.baynana.features.customers.CustomerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class WaterSessionWithCustomer(
    val session: WaterSession,
    val customer: Customer?
)

class WaterSessionRepository(
    private val sessionDao: WaterSessionDao,
    private val customerDao: CustomerDao
) {
    val allSessions: Flow<List<WaterSession>> = sessionDao.getAllSessions()

    val sessionsWithCustomer: Flow<List<WaterSessionWithCustomer>> =
        combine(sessionDao.getAllSessions(), customerDao.getAllCustomers()) { sessions, customers ->
            val customerMap = customers.associateBy { it.id }
            sessions.map { session ->
                WaterSessionWithCustomer(
                    session = session,
                    customer = customerMap[session.customerId]
                )
            }
        }

    fun getSessionsForCustomer(customerId: Long): Flow<List<WaterSession>> =
        sessionDao.getSessionsForCustomer(customerId)

    fun getSessionsBetween(fromTime: Long, toTime: Long): Flow<List<WaterSession>> =
        sessionDao.getSessionsBetween(fromTime, toTime)

    suspend fun getSessionById(id: Long): WaterSession? = sessionDao.getSessionById(id)

    suspend fun insertSession(session: WaterSession): Long = sessionDao.insertSession(session)

    suspend fun updateSession(session: WaterSession) = sessionDao.updateSession(session)

    suspend fun deleteSession(session: WaterSession) = sessionDao.deleteSession(session)

    suspend fun deleteSessionById(id: Long) = sessionDao.deleteSessionById(id)
}
