package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.core.theme.LocalCortexDimens
import com.example.core.theme.LocalCortexSpacing

@Composable
fun CortexButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(LocalCortexDimens.current.cornerRadius),
        contentPadding = PaddingValues(
            horizontal = LocalCortexSpacing.current.medium,
            vertical = LocalCortexSpacing.current.small
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun CortexTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(LocalCortexDimens.current.cornerRadius)
    )
}

@Composable
fun CortexCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.padding(LocalCortexSpacing.current.small),
        shape = RoundedCornerShape(LocalCortexDimens.current.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortexTopBar(
    title: String,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortexBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = LocalCortexDimens.current.cornerRadius,
            topEnd = LocalCortexDimens.current.cornerRadius
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        content = content
    )
}

@Composable
fun CortexDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            CortexButton(onClick = onConfirm, text = confirmButtonText)
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Zrušit")
            }
        },
        shape = RoundedCornerShape(LocalCortexDimens.current.cornerRadius)
    )
}

@Composable
fun CortexEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(LocalCortexSpacing.current.large),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CortexLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(LocalCortexSpacing.current.large),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CortexErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(LocalCortexSpacing.current.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = LocalCortexSpacing.current.medium)
        )
        CortexButton(onClick = onRetry, text = "Opakovat")
    }
}

@Composable
fun CortexChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier,
        shape = RoundedCornerShape(LocalCortexDimens.current.cornerRadius)
    )
}
