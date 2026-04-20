package foatto.compose.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import foatto.compose.colorTabActive
import foatto.compose.colorTabInactive
import foatto.compose.colorTabSelected
import foatto.compose.colorTabUnselected
import foatto.compose.model.TabInfo

@Composable
internal fun TabPanel(
    isWideScreen: Boolean,
    isTabPanelVisible: Boolean,
    selectedTabIndex: Int,
    tabInfos: List<TabInfo>,
    onTabClick: (index: Int) -> Unit,
    onTabCloseClick: (index: Int) -> Unit,
) {
    val density = LocalDensity.current

    if (isTabPanelVisible) {
        if (isWideScreen) {
            TabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = selectedTabIndex,
                indicator =
                    @Composable { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = colorTabSelected ?: TabRowDefaults.primaryContentColor,
                            )
                        }
                    },
            ) {
                tabInfos.forEachIndexed { index, tabInfo ->
                    Tab(
                        modifier = Modifier
                            .wrapContentWidth()
                            .background(
                                if (index == selectedTabIndex) {
                                    colorTabActive
                                } else {
                                    colorTabInactive
                                }
                            ),
                        selectedContentColor = colorTabSelected ?: LocalContentColor.current,
                        unselectedContentColor = colorTabUnselected ?: LocalContentColor.current,
                        selected = index == selectedTabIndex,
                        onClick = { onTabClick(index) },
                    ) {
                        Row(
                            //horizontalArrangement = Arrangement.SpaceBetween, - don't work properly, use Spacer
                        ) {
//                        Spacer(modifier = Modifier.weight(1.0f))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .weight(1.0f)
                                    .wrapContentWidth(),
                                textAlign = TextAlign.Center,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                text = tabInfo.texts.joinToString("\n"),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledIconButton(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                shape = RoundedCornerShape(0.dp),               // чтобы не срезало углы иконок
                                colors = IconButtonDefaults.iconButtonColors(), // иконки должны быть на прозрачном фоне
                                enabled = tabInfos.size > 1,
                                onClick = {
                                    onTabCloseClick(index)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            var isExpanded by remember { mutableStateOf(false) }
            var textFieldSize by remember { mutableStateOf(Size.Zero) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(top = 2.dp)
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        },
                    value = tabInfos[selectedTabIndex].texts.joinToString("\n"),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            imageVector = if (isExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = null,
                            modifier = Modifier.clickable {
                                isExpanded = !isExpanded
                            }
                        )
                    },
                )
                DropdownMenu(
                    modifier = Modifier
                        .width(
                            with(density) {
                                textFieldSize.width.toDp()
                            }
                        ),
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                ) {
                    tabInfos.forEachIndexed { index, tabInfo ->
                        DropdownMenuItem(
                            text = {
                                Text(text = tabInfo.texts.joinToString("\n"))
                            },
                            onClick = {
                                isExpanded = false
                                onTabClick(index)
                            },
                        )
                    }
                }
                FilledIconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    colors = IconButtonDefaults.iconButtonColors(), // иконки должны быть на прозрачном фоне
                    enabled = tabInfos.size > 1,
                    onClick = {
                        onTabCloseClick(selectedTabIndex)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
