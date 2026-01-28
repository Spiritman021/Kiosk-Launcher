package com.kv.kiosklauncher.presentation.emergency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Emergency exit code setup dialog
 */
@Composable
fun EmergencyCodeDialog(
    onDismiss: () -> Unit,
    onCodeSet: () -> Unit,
    viewModel: EmergencyExitViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    val hasEmergencyCode by viewModel.hasEmergencyCode.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is EmergencyExitUiState.Success) {
            onCodeSet()
            viewModel.resetState()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null)
        },
        title = {
            Text(if (hasEmergencyCode) "Update Emergency Code" else "Set Emergency Code")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Set a code to exit kiosk mode in emergencies. This code should be different from your admin password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                // Code field
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Emergency Code") },
                    placeholder = { Text("Enter 4+ characters") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Confirm code field
                OutlinedTextField(
                    value = confirmCode,
                    onValueChange = { confirmCode = it },
                    label = { Text("Confirm Code") },
                    placeholder = { Text("Re-enter code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.setEmergencyCode(code, confirmCode)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Error message
                if (uiState is EmergencyExitUiState.Error) {
                    Text(
                        text = (uiState as EmergencyExitUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Loading indicator
                if (uiState is EmergencyExitUiState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Clear code option if code exists
                if (hasEmergencyCode) {
                    TextButton(
                        onClick = { viewModel.clearEmergencyCode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Emergency Code", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.setEmergencyCode(code, confirmCode) },
                enabled = code.isNotBlank() && 
                         confirmCode.isNotBlank() && 
                         uiState !is EmergencyExitUiState.Loading
            ) {
                Text(if (hasEmergencyCode) "Update" else "Set Code")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Emergency exit verification dialog
 */
@Composable
fun EmergencyExitVerificationDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    viewModel: EmergencyExitViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null)
        },
        title = {
            Text("Emergency Exit")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter your emergency exit code to exit kiosk mode.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it
                        errorMessage = null
                    },
                    label = { Text("Emergency Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (viewModel.verifyEmergencyCode(code)) {
                                onVerified()
                            } else {
                                errorMessage = "Invalid emergency code"
                            }
                        }
                    ),
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (viewModel.verifyEmergencyCode(code)) {
                        onVerified()
                    } else {
                        errorMessage = "Invalid emergency code"
                    }
                },
                enabled = code.isNotBlank()
            ) {
                Text("Exit Kiosk Mode")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
