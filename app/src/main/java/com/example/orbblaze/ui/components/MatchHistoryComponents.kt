package com.example.orbblaze.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbblaze.domain.model.DuelMatch
import com.example.orbblaze.ui.theme.NavyDark
import com.example.orbblaze.ui.theme.SageGreen

@Composable
fun MatchHistoryMiniItem(match: DuelMatch) {
    val isWin = match.result == "WIN"
    Surface(
        color = (if(isWin) SageGreen else Color.Red).copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, (if(isWin) SageGreen else Color.Red).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if(isWin) "🏆" else "💀", fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "vs ${match.opponentName}", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    color = NavyDark
                )
                Text(
                    text = if(match.eloChange >= 0) "+${match.eloChange} ELO" else "${match.eloChange} ELO", 
                    fontSize = 10.sp, 
                    color = if(isWin) SageGreen else Color.Red, 
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = "${match.score}-${match.opponentScore}", 
                fontWeight = FontWeight.Black, 
                fontSize = 12.sp, 
                color = NavyDark
            )
        }
    }
}
