package com.shopmate.data.remote

import com.shopmate.data.remote.dto.ServerProductsResponse
import com.shopmate.data.remote.dto.StoreSnapshotRequest
import com.shopmate.data.remote.dto.StoreSnapshotResponse
import com.shopmate.data.remote.dto.SyncSalesRequest
import com.shopmate.data.remote.dto.SyncSalesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ShopMateApi {

    /** Batch upload sale records. */
    @POST("sync/sales/batch")
    suspend fun syncSales(
        @Body request: SyncSalesRequest,
    ): Response<SyncSalesResponse>

    /** Upload the full store snapshot (products + categories). */
    @POST("sync/snapshot")
    suspend fun syncSnapshot(
        @Body request: StoreSnapshotRequest,
    ): Response<StoreSnapshotResponse>

    /** Pull latest product metadata (server-wins conflict resolution). */
    @GET("sync/products")
    suspend fun getServerProducts(
        @Query("store_id") storeId: String,
        @Query("since") sinceTimestamp: Long? = null,
    ): Response<ServerProductsResponse>

    /** Health check — verify the API is reachable. */
    @GET("health")
    suspend fun healthCheck(): Response<Unit>
}
