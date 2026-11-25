package mx.buap.fcc.microplan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.buap.fcc.microplan.domain.model.DayStreakIndicator
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.domain.model.WeekDayStatus
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme

@Composable
fun MicroPlanCard(
  modifier: Modifier = Modifier,
  containerColor: Color = MicroPlanTheme.colors.surface,
  borderColor: Color = MicroPlanTheme.colors.border,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = if (MicroPlanTheme.colors.isDark) 0.dp else 1.dp
  ) {
    Column(modifier = Modifier.padding(contentPadding), content = content)
  }
}

@Composable
fun StatusPill(
  text: String,
  backgroundColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  onClick: (() -> Unit)? = null
) {
  val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .background(backgroundColor)
      .then(clickMod)
      .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (leadingIcon != null) {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(13.dp)
      )
    }
    Text(
      text = text,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1
    )
  }
}

@Composable
fun ModuleTopBar(
  title: String,
  leadingIcon: ImageVector,
  streakDays: Int?,
  onProfileClick: () -> Unit,
  onBellClick: (() -> Unit)? = null
) {
  val colors = MicroPlanTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 18.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(colors.primaryBlueSoft)
          .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = colors.primaryBlue,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(Modifier.width(10.dp))
      Text(
        text = title,
        color = colors.textPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (streakDays != null) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(text = "🔥", fontSize = 12.sp)
          Text(
            text = "$streakDays Días",
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      if (onBellClick != null) {
        IconButton(
          onClick = onBellClick,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.NotificationsNone,
            contentDescription = "Probar recordatorio de desconexión",
            tint = colors.textPrimary
          )
        }
      }

      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(colors.primaryBlue)
          .clickable(onClick = onProfileClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Rounded.Person,
          contentDescription = "Ir a Profile",
          tint = Color.White,
          modifier = Modifier.size(19.dp)
        )
      }
    }
  }
}

@Composable
fun WeeklyStreakRow(
  indicators: List<DayStreakIndicator>,
  modifier: Modifier = Modifier
) {
  val colors = MicroPlanTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(colors.background)
      .border(1.dp, colors.border, RoundedCornerShape(14.dp))
      .padding(horizontal = 10.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    indicators.forEach { item ->
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (item.status) {
          WeekDayStatus.COMPLETED -> {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.successGreen),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Cumplido",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          WeekDayStatus.TODAY -> {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(2.dp, colors.primaryBlue, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "HOY",
                color = colors.primaryBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          WeekDayStatus.MISSED -> {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.dangerSoft)
                .border(1.dp, colors.dangerBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(colors.dangerRed)
              )
            }
          }

          WeekDayStatus.PENDING -> {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surfaceSubtle),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(colors.textSecondary.copy(alpha = 0.45f))
              )
            }
          }
        }

        Spacer(Modifier.size(6.dp))
        Text(
          text = item.dayLetter,
          color = if (item.status == WeekDayStatus.TODAY) colors.primaryBlue else colors.textSecondary,
          fontSize = 11.sp,
          fontWeight = if (item.status == WeekDayStatus.TODAY) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun TimeMinutesPickerDialog(
  title: String,
  initialMinutes: Int,
  presetMinutes: List<Int> = listOf(21 * 60 + 30, 22 * 60, 22 * 60 + 30, 23 * 60, 23 * 60 + 30),
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit
) {
  val colors = MicroPlanTheme.colors
  var selectedMinutes by remember(initialMinutes) { mutableIntStateOf(initialMinutes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = {
      Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = TimeFormatters.formatMinutesAs12Hour(selectedMinutes),
          color = colors.primaryBlue,
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { selectedMinutes = ((selectedMinutes - 15) + 1440) % 1440 },
            modifier = Modifier.weight(1f)
          ) {
            Text("-15 min")
          }
          OutlinedButton(
            onClick = { selectedMinutes = (selectedMinutes + 15) % 1440 },
            modifier = Modifier.weight(1f)
          ) {
            Text("+15 min")
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          presetMinutes.take(3).forEach { preset ->
            FilterChip(
              selected = selectedMinutes == preset,
              onClick = { selectedMinutes = preset },
              label = { Text(TimeFormatters.formatMinutesAs12Hour(preset), fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }
        if (presetMinutes.size > 3) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            presetMinutes.drop(3).forEach { preset ->
              FilterChip(
                selected = selectedMinutes == preset,
                onClick = { selectedMinutes = preset },
                label = { Text(TimeFormatters.formatMinutesAs12Hour(preset), fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedMinutes) },
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
      ) {
        Text("Guardar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar", color = colors.textSecondary)
      }
    }
  )
}
