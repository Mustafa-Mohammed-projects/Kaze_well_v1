package com.kazewell.app.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazewell.app.AppEntry
import com.kazewell.app.Filter
import com.kazewell.app.IconCache
import com.kazewell.app.MainViewModel
import com.kazewell.app.R
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val running by vm.running.collectAsStateWithLifecycle()
    val dropped by vm.dropped.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val allowed by vm.allowed.collectAsStateWithLifecycle()
    val visible by vm.visible.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val autoStart by vm.autoStart.collectAsStateWithLifecycle()

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.startFirewall()
    }
    val toggle: () -> Unit = {
        if (running) {
            vm.stopFirewall()
        } else {
            val intent = VpnService.prepare(context)
            if (intent != null) vpnLauncher.launch(intent) else vm.startFirewall()
        }
    }

    val allowedCount = apps.count { it.pkg in allowed }
    val blockedCount = apps.size - allowedCount

    Box(Modifier.fillMaxSize().background(KW.Bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { HeaderCard(running, blockedCount, allowedCount, toggle) }
            item { StatsRow(blockedCount, allowedCount, dropped) }
            item {
                AutoStartCard(autoStart, vm::setAutoStart)
            }
            item { TipCard() }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionButton(stringResource(R.string.allow_shown), KW.Green, Modifier.weight(1f)) { vm.allowAll(visible) }
                    ActionButton(stringResource(R.string.block_shown), KW.Red, Modifier.weight(1f)) { vm.blockAll(visible) }
                }
            }
            stickyHeader {
                Column(Modifier.fillMaxWidth().background(KW.Bg).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SearchField(query) { vm.query.value = it }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(stringResource(R.string.filter_all), filter == Filter.ALL) { vm.filter.value = Filter.ALL }
                        FilterChip(stringResource(R.string.filter_user), filter == Filter.USER) { vm.filter.value = Filter.USER }
                        FilterChip(stringResource(R.string.filter_system), filter == Filter.SYSTEM) { vm.filter.value = Filter.SYSTEM }
                        FilterChip(stringResource(R.string.filter_allowed), filter == Filter.ALLOWED) { vm.filter.value = Filter.ALLOWED }
                        FilterChip(stringResource(R.string.filter_blocked), filter == Filter.BLOCKED) { vm.filter.value = Filter.BLOCKED }
                    }
                }
            }
            if (loading) {
                item { CenterText(stringResource(R.string.loading), null) }
            } else if (visible.isEmpty()) {
                item { CenterText(stringResource(R.string.empty_title), stringResource(R.string.empty_body)) }
            } else {
                items(visible, key = { it.pkg }) { app ->
                    AppRow(app, app.pkg in allowed) { vm.setAllowed(app.pkg, it) }
                }
            }
            item {
                Text(
                    stringResource(R.string.vpn_note),
                    color = KW.TextFaint,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(running: Boolean, blocked: Int, allowed: Int, onToggle: () -> Unit) {
    Panel(Modifier.fillMaxWidth().padding(16.dp), corner = 28.dp) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("kaze_well", color = KW.TextDim, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Shield(running, Modifier.size(150.dp).clip(CircleShape).clickable(onClick = onToggle))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(if (running) R.string.status_on else R.string.status_off),
                color = if (running) KW.Teal else KW.Red,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (running) stringResource(R.string.sub_on, blocked, allowed) else stringResource(R.string.sub_off),
                color = KW.TextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            val bg by animateColorAsState(if (running) KW.Surface3 else KW.Teal, label = "btn")
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(bg)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(if (running) R.string.btn_off else R.string.btn_on),
                    color = if (running) KW.Text else Color(0xFF04201C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatsRow(blocked: Int, allowed: Int, dropped: Long) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Stat(blocked.toString(), stringResource(R.string.stat_blocked), KW.Red, Modifier.weight(1f))
        Stat(allowed.toString(), stringResource(R.string.stat_allowed), KW.Green, Modifier.weight(1f))
        Stat(NumberFormat.getInstance().format(dropped), stringResource(R.string.stat_packets), KW.Amber, Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier) {
    Panel(modifier, corner = 18.dp) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(label, color = KW.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AutoStartCard(on: Boolean, onChange: (Boolean) -> Unit) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), corner = 18.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.auto_start), color = KW.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(stringResource(R.string.auto_start_desc), color = KW.TextDim, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            KazeSwitch(on, onChange)
        }
    }
}

@Composable
private fun TipCard() {
    Panel(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        corner = 18.dp,
        color = KW.Surface2
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.tip), color = KW.Text, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.tip_system), color = KW.Amber, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KW.Surface2)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(stringResource(R.string.search_hint), color = KW.TextFaint, fontSize = 15.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = KW.Text, fontSize = 15.sp),
            cursorBrush = SolidColor(KW.Teal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, allowed: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = IconCache.peek(app.pkg), key1 = app.pkg) {
        if (value == null) {
            value = withContext(Dispatchers.Default) { IconCache.load(context, app.pkg) }
        }
    }
    val accent = if (allowed) KW.Green else KW.Red

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!allowed) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(44.dp))
        } else {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(KW.Surface3),
                contentAlignment = Alignment.Center
            ) {
                Text(app.label.take(1).uppercase(), color = KW.TextDim, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.label,
                    color = KW.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (app.isSystem) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.system_badge),
                        color = KW.Amber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(app.pkg, color = KW.TextFaint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(if (allowed) R.string.internet_allowed else R.string.internet_blocked),
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(10.dp))
        KazeSwitch(allowed, onChange)
    }
}

@Composable
private fun KazeSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = KW.Green,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = KW.Surface3,
            uncheckedThumbColor = KW.TextDim,
            uncheckedBorderColor = KW.Outline
        )
    )
}

@Composable
private fun CenterText(title: String, body: String?) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = KW.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(body, color = KW.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
