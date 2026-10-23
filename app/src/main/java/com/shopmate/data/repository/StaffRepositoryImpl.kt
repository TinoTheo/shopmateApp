package com.shopmate.data.repository

import com.shopmate.data.auth.AuthManager
import com.shopmate.data.auth.StoreContext
import com.shopmate.data.local.dao.StaffDao
import com.shopmate.data.local.entity.StaffEntity
import com.shopmate.data.remote.tenancyRefusal
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.StaffRole
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TenancyResult
import com.shopmate.domain.repository.TillStaff
import com.shopmate.domain.util.PinHasher
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The owner's writes go straight to the `staff` table — "owners manage staff" RLS allows exactly
 * the owner — and the till then pulls the list back, so every till reads the same rows.
 *
 * Who has unlocked the till lives in memory only: a restarted app is locked again.
 */
@Singleton
class StaffRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val staffDao: StaffDao,
    private val storeContext: StoreContext,
    private val authManager: AuthManager,
) : StaffRepository {

    override val staff: Flow<List<TillStaff>> =
        staffDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    private val _current = MutableStateFlow<TillStaff?>(null)
    override val current: StateFlow<TillStaff?> = _current.asStateFlow()

    override val locked: Flow<Boolean> =
        combine(staff, _current) { list, current -> list.isNotEmpty() && current == null }

    override suspend fun unlock(staffId: String, pin: String): Boolean {
        val row = staffDao.getById(staffId)?.takeIf { it.active } ?: return false
        // PBKDF2 is deliberately slow; keep it off the main thread.
        val ok = withContext(Dispatchers.Default) { PinHasher.matches(pin, row.pinSalt, row.pinHash) }
        if (ok) _current.value = row.toDomain()
        return ok
    }

    override fun lock() {
        _current.value = null
    }

    override suspend fun pull(storeId: String) {
        val rows = runCatching {
            postgrest.from("staff").select {
                filter {
                    eq("store_id", storeId)
                    eq("active", true)
                }
            }.decodeList<StaffRowDto>()
        }.getOrElse { return } // offline: the till keeps its copy and stays unlockable
        // Narrow, never widen: only this store's rows reach the till.
        staffDao.replaceAll(rows.filter { it.storeId == storeId }.map { it.toEntity() })
        // Someone removed or demoted while unlocked takes the till's new view of them.
        _current.value = _current.value?.let { me -> staffDao.getById(me.id)?.takeIf { it.active }?.toDomain() }
    }

    override suspend fun setOwnerPin(ownerName: String, pin: String): TenancyResult<Unit> = write { storeId ->
        val ownerRow = staff.first().firstOrNull { it.role == StaffRole.OWNER }
        if (ownerRow != null) {
            updatePin(ownerRow.id, pin)
        } else {
            val salt = PinHasher.newSalt()
            postgrest.from("staff").insert(
                NewStaffDto(
                    storeId = storeId,
                    name = ownerName.trim(),
                    role = "owner",
                    pinHash = hash(pin, salt),
                    pinSalt = salt,
                    canManageStock = true,
                    userId = authManager.currentUserId,
                ),
            )
        }
    }

    override suspend fun add(name: String, role: StaffRole, pin: String, canManageStock: Boolean) = write { storeId ->
        require(role != StaffRole.OWNER)
        val salt = PinHasher.newSalt()
        postgrest.from("staff").insert(
            NewStaffDto(
                storeId = storeId,
                name = name.trim(),
                role = role.wire,
                pinHash = hash(pin, salt),
                pinSalt = salt,
                canManageStock = role == StaffRole.CASHIER && canManageStock,
                userId = null,
            ),
        )
    }

    override suspend fun update(staffId: String, role: StaffRole, canManageStock: Boolean) = write {
        require(role != StaffRole.OWNER)
        postgrest.from("staff").update(
            buildJsonObject {
                put("role", role.wire)
                put("can_manage_stock", role == StaffRole.CASHIER && canManageStock)
            },
        ) { filter { eq("id", staffId) } }
    }

    override suspend fun changePin(staffId: String, pin: String) = write { updatePin(staffId, pin) }

    override suspend fun remove(staffId: String) = write {
        postgrest.from("staff").update(buildJsonObject { put("active", false) }) {
            filter { eq("id", staffId) }
        }
    }

    private suspend fun updatePin(staffId: String, pin: String) {
        val salt = PinHasher.newSalt()
        postgrest.from("staff").update(
            buildJsonObject {
                put("pin_hash", hash(pin, salt))
                put("pin_salt", salt)
            },
        ) { filter { eq("id", staffId) } }
    }

    /** Every owner write, then a pull so this till shows what the server now holds. */
    private suspend fun write(block: suspend (storeId: String) -> Unit): TenancyResult<Unit> {
        val storeId = storeContext.getActiveStoreId() ?: return TenancyResult.Refused(TenancyRefusal.OTHER)
        return runCatching { block(storeId) }.fold(
            onSuccess = {
                pull(storeId)
                TenancyResult.Done(Unit)
            },
            onFailure = { TenancyResult.Refused(it.tenancyRefusal()) },
        )
    }

    private suspend fun hash(pin: String, salt: String): String {
        require(PinHasher.isValidPin(pin))
        return withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
    }

    private val StaffRole.wire: String get() = name.lowercase()

    private fun StaffEntity.toDomain() = TillStaff(
        id = id,
        name = name,
        role = when (role) {
            "owner" -> StaffRole.OWNER
            "manager" -> StaffRole.MANAGER
            else -> StaffRole.CASHIER
        },
        canManageStock = canManageStock,
    )

    @Serializable
    private data class StaffRowDto(
        @SerialName("id") val id: String,
        @SerialName("store_id") val storeId: String,
        @SerialName("name") val name: String,
        @SerialName("role") val role: String,
        @SerialName("pin_hash") val pinHash: String,
        @SerialName("pin_salt") val pinSalt: String,
        @SerialName("can_manage_stock") val canManageStock: Boolean = false,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("active") val active: Boolean = true,
    ) {
        fun toEntity() = StaffEntity(id, name, role, pinHash, pinSalt, canManageStock, userId, active)
    }

    @Serializable
    private data class NewStaffDto(
        @SerialName("store_id") val storeId: String,
        @SerialName("name") val name: String,
        @SerialName("role") val role: String,
        @SerialName("pin_hash") val pinHash: String,
        @SerialName("pin_salt") val pinSalt: String,
        @SerialName("can_manage_stock") val canManageStock: Boolean,
        @SerialName("user_id") val userId: String?,
    )
}
