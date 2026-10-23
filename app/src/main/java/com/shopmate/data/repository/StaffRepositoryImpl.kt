package com.shopmate.data.repository

import com.shopmate.data.remote.tenancyRefusal
import com.shopmate.domain.repository.Invite
import com.shopmate.domain.repository.StaffMember
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.StaffRole
import com.shopmate.domain.repository.TenancyResult
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invites and the staff list go through the migration's functions (they read `auth.users` and
 * check limits). Role, stock rights and removal are plain table writes: the existing
 * "owners manage memberships" and "owners manage store access" policies already allow exactly
 * the owner to make them.
 */
@Singleton
class StaffRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : StaffRepository {

    override suspend fun list(): TenancyResult<List<StaffMember>> = attempt {
        postgrest.rpc("list_staff").decodeList<StaffRowDto>()
            .groupBy { it.membershipId }
            .map { (membershipId, rows) ->
                val first = rows.first()
                StaffMember(
                    membershipId = membershipId,
                    userId = first.userId,
                    email = first.email.orEmpty(),
                    role = first.role.toRole(),
                    stores = rows.mapNotNull { row ->
                        row.storeId?.let {
                            StaffMember.StaffStore(it, row.storeName.orEmpty(), row.canManageStock)
                        }
                    },
                )
            }
            .sortedWith(compareBy({ it.role.ordinal }, { it.email }))
    }

    override suspend fun invite(
        storeId: String,
        storeName: String,
        role: StaffRole,
        canManageStock: Boolean,
    ): TenancyResult<Invite> = attempt {
        val code = postgrest.rpc(
            function = "create_invite",
            parameters = buildJsonObject {
                put("p_store_id", storeId)
                put("p_role", role.wire)
                put("p_can_manage_stock", canManageStock)
            },
        ).decodeAs<String>()
        Invite(code = code, role = role, storeName = storeName)
    }

    override suspend fun changeRole(membershipId: String, role: StaffRole): TenancyResult<Unit> = attempt {
        // One owner per organisation: the app never makes anyone else an owner.
        require(role != StaffRole.OWNER)
        postgrest.from("memberships").update(buildJsonObject { put("role", role.wire) }) {
            filter { eq("id", membershipId) }
        }
        Unit
    }

    override suspend fun setStockRights(membershipId: String, storeId: String, granted: Boolean): TenancyResult<Unit> =
        attempt {
            postgrest.from("store_access").update(buildJsonObject { put("can_manage_stock", granted) }) {
                filter {
                    eq("membership_id", membershipId)
                    eq("store_id", storeId)
                }
            }
            Unit
        }

    override suspend fun remove(membershipId: String): TenancyResult<Unit> = attempt {
        postgrest.from("store_access").delete { filter { eq("membership_id", membershipId) } }
        postgrest.from("memberships").delete { filter { eq("id", membershipId) } }
        Unit
    }

    private inline fun <T> attempt(block: () -> T): TenancyResult<T> =
        runCatching(block).fold(
            onSuccess = { TenancyResult.Done(it) },
            onFailure = { TenancyResult.Refused(it.tenancyRefusal()) },
        )

    private val StaffRole.wire: String get() = name.lowercase()

    private fun String.toRole(): StaffRole = when (this) {
        "owner" -> StaffRole.OWNER
        "manager" -> StaffRole.MANAGER
        else -> StaffRole.CASHIER
    }

    @Serializable
    private data class StaffRowDto(
        @SerialName("membership_id") val membershipId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("email") val email: String? = null,
        @SerialName("role") val role: String,
        @SerialName("store_id") val storeId: String? = null,
        @SerialName("store_name") val storeName: String? = null,
        @SerialName("can_manage_stock") val canManageStock: Boolean = false,
    )
}
