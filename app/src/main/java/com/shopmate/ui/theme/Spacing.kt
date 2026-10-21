package com.shopmate.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 4px-based scale, leaning roomy. Crisp does not mean cramped — when unsure, choose MORE
 * space. Confident layouts leave room empty.
 *
 * - [md] between related items (list rows, cards in a group)
 * - [sm] between a label and its content
 * - [xl] between sections on a screen
 * - [xxl] between major groups / above a screen's first element
 * - [lg] screen edge padding on phones, [xl] on tablets
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}
