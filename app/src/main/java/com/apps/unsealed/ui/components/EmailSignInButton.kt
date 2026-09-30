package com.apps.unsealed.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.PaperCream
import com.apps.unsealed.ui.theme.SurfaceBorderLight

/**
 * "Continue with Email" button — same [OutlinedButton] shape/height as
 * [GoogleSignInButton] so the two read as a matched pair when placed
 * side-by-side (see [com.apps.unsealed.ui.screens.welcome.WelcomeCarouselScreen]).
 */
@Composable
fun EmailSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.login_email_label),
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PaperCream,
            contentColor = BrandInk,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = SurfaceBorderLight,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = BrandInk,
                modifier = Modifier.height(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandInk,
                maxLines = 1,
            )
        }
    }
}
