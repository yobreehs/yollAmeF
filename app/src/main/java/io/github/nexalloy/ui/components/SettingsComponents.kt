package io.github.nexalloy.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(content = content)
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
fun SettingsRow(
    title: String,
    summary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
fun SwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.5f,
                    ),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
fun CheckRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Spacer(Modifier.size(20.dp))
        }
    }
}

/**
 * Square rounded app icon with a letter fallback, in the same visual language as the
 * detector's icon tiles.
 */
@Composable
fun AppIcon(
    packageName: String,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) { loadAppIcon(context, packageName) }
    val shape = RoundedCornerShape(12.dp)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayName.take(1).uppercase(),
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun loadAppIcon(context: Context, packageName: String) = runCatching {
    val pm = context.packageManager
    val icon = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        pm.getApplicationIcon(packageName)
    } else {
        @Suppress("DEPRECATION")
        pm.getApplicationInfo(packageName, 0).loadIcon(pm)
    }
    icon.toBitmap(128, 128).asImageBitmap()
}.getOrNull()

fun isPackageInstalled(context: Context, packageName: String): Boolean = runCatching {
    context.packageManager.getPackageInfo(packageName, 0)
}.isSuccess
