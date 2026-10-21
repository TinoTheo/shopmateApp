package com.shopmate.domain.model

/**
 * Why a stock level changed. Every movement is a signed delta, never an absolute overwrite.
 * - [SALE]: stock removed by a POS sale (always negative)
 * - [RESTOCK]: replenishment, e.g. supplier delivery (positive)
 * - [MANUAL_ADJUSTMENT]: ad-hoc correction the merchant makes
 * - [CORRECTION]: fixing a counting/data error
 * - [PURCHASE]: initial stock recorded when a product is first created
 * - [COUNT]: a line from a committed stocktake — the signed variance between counted and
 *   expected, carrying its session id (CLAUDE.md architecture rule 5)
 */
enum class DeltaReason { SALE, RESTOCK, MANUAL_ADJUSTMENT, CORRECTION, PURCHASE, COUNT }
