package com.flowchat.app.presentation.chat

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatScreenContractTest {
    @Test
    fun topBarDoesNotExposeOverflowModelMenu() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val topBarStart = source.indexOf("topBar = {")
        val bottomBarStart = source.indexOf("bottomBar =", topBarStart)
        val topBarBlock = source.substring(topBarStart, bottomBarStart)

        assertFalse(topBarBlock.contains("MoreVert"))
        assertFalse(topBarBlock.contains("DropdownMenu"))
        assertFalse(topBarBlock.contains("DropdownMenuItem"))
        assertFalse(topBarBlock.contains("showModelMenu"))
    }

    @Test
    fun topBarOpensConversationSettingsFromRightTuningActionOnly() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val topBarStart = source.indexOf("CenterAlignedTopAppBar")
        val topBarEnd = source.indexOf("bottomBar =", topBarStart)
        val topBarBlock = source.substring(topBarStart, topBarEnd)
        val titleStart = topBarBlock.indexOf("title = {")
        val navigationStart = topBarBlock.indexOf("navigationIcon =", titleStart)
        val titleBlock = topBarBlock.substring(titleStart, navigationStart)
        val actionsStart = topBarBlock.indexOf("actions = {")
        assertTrue(actionsStart >= 0)
        val actionsBlock = topBarBlock.substring(actionsStart)

        assertTrue(source.contains("private val OriginalSettingsIcon = Icons.Default.Settings"))
        assertFalse(titleBlock.contains(".clickable("))
        assertFalse(titleBlock.contains("showSettings = true"))
        assertTrue(titleBlock.contains("displayAssistantTitle()"))
        assertTrue(actionsBlock.contains("if (state.currentConversation != null)"))
        assertTrue(actionsBlock.contains("IconButton("))
        assertFalse(actionsBlock.contains("enabled = state.currentConversation != null"))
        assertTrue(actionsBlock.contains("showSettings = true"))
        assertTrue(actionsBlock.contains("ConversationSettingsIcon("))
        assertTrue(actionsBlock.contains("R.string.conversation_settings"))
        assertFalse(topBarBlock.contains("painterResource(R.drawable.reference_gear)"))
        assertFalse(source.contains("private fun ReferenceGearIcon("))
        assertTrue(source.contains("private fun ConversationSettingsIcon("))
        assertTrue(source.contains("drawCircle("))
        assertTrue(source.contains("drawLine("))
    }

    @Test
    fun topBarDrawerButtonUsesTwoUnevenLeftAlignedLines() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val topBarStart = source.indexOf("CenterAlignedTopAppBar")
        val topBarEnd = source.indexOf("bottomBar =", topBarStart)
        val topBarBlock = source.substring(topBarStart, topBarEnd)
        val navStart = topBarBlock.indexOf("navigationIcon = {")
        val navEnd = topBarBlock.indexOf("actions = {", navStart)
        val navBlock = topBarBlock.substring(navStart, navEnd)

        assertFalse(navBlock.contains("Icons.Default.Menu"))
        assertFalse(source.contains("import androidx.compose.material.icons.filled.Menu"))
        assertTrue(navBlock.contains("DrawerMenuIcon("))
        assertTrue(source.contains("private fun DrawerMenuIcon("))
        assertTrue(source.contains("val lineStart = size.width * 0.20f"))
        assertTrue(source.contains("val topLineEnd = size.width * 0.82f"))
        assertTrue(source.contains("val bottomLineEnd = size.width * 0.58f"))
        assertTrue(source.contains("start = Offset(lineStart, topY)"))
        assertTrue(source.contains("start = Offset(lineStart, bottomY)"))
        assertTrue(source.contains("end = Offset(topLineEnd, topY)"))
        assertTrue(source.contains("end = Offset(bottomLineEnd, bottomY)"))
    }

    @Test
    fun referenceGearPngUsesTransparentBackground() {
        val image = decodeRgbaPng(File("src/main/res/drawable/reference_gear.png"))
        val corners = listOf(
            image.alphaAt(0, 0),
            image.alphaAt(image.width - 1, 0),
            image.alphaAt(0, image.height - 1),
            image.alphaAt(image.width - 1, image.height - 1)
        )

        corners.forEach { alpha ->
            assertEquals(0, alpha)
        }
        assertTrue(
            (0 until image.width).any { x ->
                (0 until image.height).any { y ->
                    image.alphaAt(x, y) > 240
                }
            }
        )
    }

    @Test
    fun launcherIconUsesFourthVersionRasterMipmapWithoutAdaptiveOverride() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val expectedSizes = mapOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )

        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertFalse(File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml").exists())

        expectedSizes.forEach { (directory, expectedSize) ->
            val icon = File("src/main/res/$directory/ic_launcher.png")
            assertTrue("Missing launcher icon at ${icon.path}", icon.exists())
            val (width, height) = readPngDimensions(icon)
            assertEquals(expectedSize, width)
            assertEquals(expectedSize, height)
        }

        val xhdpiIcon = decodeRgbaPng(File("src/main/res/mipmap-xhdpi/ic_launcher.png"))
        val lightBackgroundPixels = xhdpiIcon.countPixels { _, _, r, g, b, alpha ->
            alpha > 220 && r > 210 && g > 210 && b > 210
        }
        val middleLineDarkPixels = xhdpiIcon.countPixels { _, y, r, g, b, alpha ->
            y > xhdpiIcon.height * 0.43 &&
                y < xhdpiIcon.height * 0.52 &&
                alpha > 220 &&
                r < 70 &&
                g < 70 &&
                b < 70
        }
        val centerCurveDarkPixels = xhdpiIcon.countPixels { x, y, r, g, b, alpha ->
            x > xhdpiIcon.width * 0.43 &&
                x < xhdpiIcon.width * 0.58 &&
                y > xhdpiIcon.height * 0.35 &&
                y < xhdpiIcon.height * 0.68 &&
                alpha > 220 &&
                r < 70 &&
                g < 70 &&
                b < 70
        }
        assertTrue("Fourth version launcher icon should keep a mostly light background", lightBackgroundPixels > 8_000)
        assertTrue("Fourth version launcher icon should keep the black horizontal stroke", middleLineDarkPixels > 80)
        assertTrue("Fourth version launcher icon should keep the black center curve", centerCurveDarkPixels > 70)
    }

    @Test
    fun conversationRowsOpenOnClickAndDeleteOnLongPress() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()

        assertFalse(source.contains("stringResource(R.string.open)"))
        assert(source.contains("combinedClickable("))
        assert(source.contains("onLongClick"))
        assert(source.contains("AlertDialog("))
        assert(source.contains("deleteConversation("))
    }

    @Test
    fun drawerNewConversationButtonIsBottomEndCircularOutline() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val drawerStart = source.indexOf("ModalDrawerSheet")
        val scaffoldStart = source.indexOf("Scaffold(", drawerStart)
        val drawerBlock = source.substring(drawerStart, scaffoldStart)
        val titleStart = drawerBlock.indexOf("R.string.app_name")
        val profileStart = drawerBlock.indexOf("UserProfileEntry(", titleStart)

        assertTrue(drawerStart >= 0)
        assertTrue(scaffoldStart > drawerStart)
        assertTrue(titleStart >= 0)
        assertTrue(profileStart > titleStart)
        val drawerHeaderBlock = drawerBlock.substring(titleStart, profileStart)

        assertFalse(drawerHeaderBlock.contains("newConversation()"))
        assertFalse(drawerHeaderBlock.contains("Icons.Default.Add"))
        assertTrue(drawerBlock.contains(".align(Alignment.BottomEnd)"))
        assertTrue(drawerBlock.contains("OutlinedIconButton("))
        assertTrue(drawerBlock.contains("CircleShape"))
        assertTrue(drawerBlock.contains(".size(32.dp)"))
        assertTrue(drawerBlock.contains("contentAlignment = Alignment.Center"))
        assertTrue(drawerBlock.contains("Canvas("))
        assertTrue(drawerBlock.contains(".size(18.dp)"))
        assertTrue(drawerBlock.contains("val center = Offset(size.width / 2f, size.height / 2f)"))
        assertTrue(drawerBlock.contains("drawLine("))
        assertTrue(drawerBlock.contains("StrokeCap.Round"))
        assertFalse(drawerBlock.contains("text = \"+\""))
        assertTrue(drawerBlock.contains("viewModel.newConversation()"))
    }

    @Test
    fun drawerMovesProviderLanguageIntoProfileSettingsAndLabelsConversationList() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()
        val drawerStart = source.indexOf("ModalDrawerSheet")
        val scaffoldStart = source.indexOf("Scaffold(", drawerStart)
        val drawerBlock = source.substring(drawerStart, scaffoldStart)
        val profileEntryStart = drawerBlock.indexOf("UserProfileEntry(")
        val conversationsLabelStart = drawerBlock.indexOf("R.string.conversation_list", profileEntryStart)
        val firstConversationLoopStart = drawerBlock.indexOf("state.conversations.forEach", conversationsLabelStart)

        assertTrue(profileEntryStart > 0)
        assertTrue(drawerBlock.contains("showUserSettings = true"))
        assertTrue(drawerBlock.contains("Icons.Default.Settings"))
        assertTrue(conversationsLabelStart > profileEntryStart)
        assertTrue(firstConversationLoopStart > conversationsLabelStart)
        assertFalse(drawerBlock.contains("TextButton(onClick = onOpenProviders"))
        assertFalse(drawerBlock.contains("LanguageSelector(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))"))
        assertTrue(source.contains("private fun UserProfileSettingsSheet("))
        assertTrue(source.contains("R.string.model_provider_configuration"))
        assertFalse(source.contains("R.string.web_search_configuration"))
        assertFalse(source.contains("onOpenWebSearch"))
        assertTrue(source.contains("LanguageSelector("))
        assertTrue(source.contains("onOpenProviders()"))
        assertTrue(strings.contains("<string name=\"user_display_name\">User</string>"))
        assertTrue(strings.contains("<string name=\"conversation_list\">Conversation list</string>"))
        assertTrue(strings.contains("<string name=\"model_provider_configuration\">Model provider configuration</string>"))
        assertTrue(zhStrings.contains("name=\"user_display_name\""))
        assertTrue(zhStrings.contains("name=\"conversation_list\""))
        assertTrue(zhStrings.contains("name=\"model_provider_configuration\""))
    }

    @Test
    fun userProfileSettingsContainsBackgroundModeButtons() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()
        val sheetStart = source.indexOf("private fun UserProfileSettingsSheet(")
        val sheetEnd = source.indexOf("@Composable\nprivate fun SettingsRow", sheetStart)
        val sheetBlock = source.substring(sheetStart, sheetEnd)

        assertTrue(source.contains("import com.flowchat.app.ui.theme.AppAppearance"))
        assertTrue(sheetBlock.contains("appAppearance: AppAppearance"))
        assertTrue(sheetBlock.contains("onAppAppearanceChange: (AppAppearance) -> Unit"))
        assertTrue(sheetBlock.contains("R.string.background_settings"))
        assertTrue(sheetBlock.contains("BackgroundModeButton("))
        assertTrue(sheetBlock.contains("AppAppearance.Light"))
        assertTrue(sheetBlock.contains("AppAppearance.Dark"))
        assertTrue(sheetBlock.contains("R.string.background_light"))
        assertTrue(sheetBlock.contains("R.string.background_dark"))
        assertTrue(source.contains("private fun BackgroundModeButton("))
        assertTrue(source.contains(".height(56.dp)"))
        assertTrue(source.contains(".background(previewBackgroundColor, RoundedCornerShape(10.dp))"))
        assertTrue(source.contains("color = previewTextColor"))
        assertTrue(strings.contains("<string name=\"background_settings\">Background settings</string>"))
        assertTrue(strings.contains("<string name=\"background_light\">Light</string>"))
        assertTrue(strings.contains("<string name=\"background_dark\">Dark</string>"))
        assertTrue(zhStrings.contains("name=\"background_settings\""))
        assertTrue(zhStrings.contains("name=\"background_light\""))
        assertTrue(zhStrings.contains("name=\"background_dark\""))
    }

    @Test
    fun userProfileEntryIsTransparentUntilPressedAndProfileSheetCanEditAvatarAndName() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()
        val entryStart = source.indexOf("private fun UserProfileEntry(")
        val sheetStart = source.indexOf("private fun UserProfileSettingsSheet(")
        val entryBlock = source.substring(entryStart, sheetStart)
        val sheetEnd = source.indexOf("@Composable\nprivate fun SettingsRow", sheetStart)
        val sheetBlock = source.substring(sheetStart, sheetEnd)

        assertTrue(entryBlock.contains("avatarPath: String?"))
        assertTrue(entryBlock.contains(".clip(RoundedCornerShape(36.dp))"))
        assertTrue(entryBlock.contains(".combinedClickable("))
        assertFalse(entryBlock.contains("Card("))
        assertFalse(entryBlock.contains(".background("))
        assertTrue(sheetBlock.contains("rememberLauncherForActivityResult(ActivityResultContracts.GetContent())"))
        assertTrue(sheetBlock.contains("copyUserAvatarToPrivateFile(context, uri)"))
        assertTrue(sheetBlock.contains("ProfileAvatar("))
        assertTrue(sheetBlock.contains("AvatarEditButton("))
        assertTrue(sheetBlock.contains("OutlinedTextField("))
        assertTrue(sheetBlock.contains("R.string.profile_name"))
        assertTrue(sheetBlock.contains("onSave(profileName, profileAvatarPath)"))
        assertTrue(source.contains("saveUserProfile(context, profileName, profileAvatarPath)"))
        assertTrue(strings.contains("<string name=\"profile_name\">Name</string>"))
        assertTrue(zhStrings.contains("name=\"profile_name\""))
    }

    @Test
    fun appDisplayNameUsesFlowChatWithoutSpaceEverywhere() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(strings.contains("<string name=\"app_name\">FlowChat</string>"))
        assertTrue(zhStrings.contains("<string name=\"app_name\">FlowChat</string>"))
        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        assertTrue(source.contains("stringResource(R.string.app_name)"))
        assertFalse(source.contains("\"Flow Chat\""))
        assertFalse(strings.contains(">Flow Chat<"))
        assertFalse(zhStrings.contains(">Flow Chat<"))
    }

    @Test
    fun chatViewModelDoesNotCreateDefaultConversationOnStartup() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val initStart = source.indexOf("init {")
        val initEnd = source.indexOf("\n    fun", initStart)
        val initBlock = source.substring(initStart, initEnd)

        assertTrue(initStart >= 0)
        assertFalse(initBlock.contains("ensureInitialConversation"))
        assertFalse(initBlock.contains("createConversation"))
        assertFalse(initBlock.contains("deleteEmptyDefaultConversations"))
    }

    @Test
    fun blankAssistantCompletionIsMarkedFailedInsteadOfDisplayedAsDots() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val screenSource = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()

        assertTrue(source.contains("Provider returned an empty response."))
        assertTrue(source.contains("content.isBlank()"))
        assertTrue(source.contains("chatRepository.markMessageFailed(assistant.id, emptyResponseMessage)"))
        assertTrue(screenSource.contains("isWaitingForAssistant"))
        assertTrue(screenSource.contains("StreamingJumpingDots("))
        assertFalse(screenSource.contains("MessageStatus.Streaming -> \"...\""))
        assertTrue(screenSource.contains("R.string.empty_response_message"))
    }

    @Test
    fun assistantWaitingPlaceholderUsesSequentialJumpingDots() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val jumpingStart = source.indexOf("private fun StreamingJumpingDots(")
        assertTrue(jumpingStart >= 0)
        val composerStart = source.indexOf("@OptIn(ExperimentalComposeUiApi::class)", jumpingStart)
        val jumpingBlock = source.substring(jumpingStart, composerStart)

        assertTrue(jumpingBlock.contains("rememberInfiniteTransition"))
        assertTrue(jumpingBlock.contains("infiniteRepeatable("))
        assertTrue(jumpingBlock.contains("tween(durationMillis = 900"))
        assertTrue(jumpingBlock.contains("phaseOffsets = listOf(0f, 0.18f, 0.36f)"))
        assertTrue(jumpingBlock.contains("val jumpHeight = dotRadius * 2.2f"))
        assertTrue(jumpingBlock.contains("val centers = listOf("))
        assertTrue(jumpingBlock.contains("center.copy(x = center.x - spacing"))
        assertTrue(jumpingBlock.contains("center.copy(x = center.x + spacing"))
        assertTrue(jumpingBlock.contains("drawCircle("))
        assertFalse(jumpingBlock.contains("maxRadius"))
    }

    @Test
    fun composerRequestsFocusAndShowsKeyboardWhenInputIsUsed() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val activitySource = File("src/main/java/com/flowchat/app/MainActivity.kt").readText()
        val composerModifierStart = source.indexOf(".focusRequester(focusRequester)")
        val focusChangedStart = source.indexOf(".onFocusChanged", composerModifierStart)
        val pointerInputStart = source.indexOf(".pointerInput", focusChangedStart)
        val focusChangedBlock = source.substring(focusChangedStart, pointerInputStart)

        assertTrue(source.contains("FocusRequester"))
        assertTrue(source.contains("LocalSoftwareKeyboardController"))
        assertTrue(source.contains("InputMethodManager"))
        assertTrue(source.contains("focusRequester(focusRequester)"))
        assertTrue(composerModifierStart >= 0)
        assertTrue(focusChangedStart >= 0)
        assertTrue(pointerInputStart > focusChangedStart)
        assertTrue(source.contains("keyboardController?.show()"))
        assertTrue(source.contains("showSoftInput(view, InputMethodManager.SHOW_FORCED)"))
        assertTrue(source.contains("requestFocusAndShowKeyboard()"))
        assertFalse(focusChangedBlock.contains("showKeyboard()"))
        assertFalse(focusChangedBlock.contains("requestFocus()"))
        assertTrue(manifest.contains("android:windowSoftInputMode=\"adjustResize\""))
        assertTrue(activitySource.contains("SOFT_INPUT_ADJUST_RESIZE"))
    }

    @Test
    fun conversationSettingsKeepsSaveButtonReachableForLongSystemPrompts() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val sheetStart = source.indexOf("private fun ConversationSettingsSheet(")
        val sheetEnd = source.indexOf("@Composable\nprivate fun LanguageSelector", sheetStart)
        val sheetBlock = source.substring(sheetStart, sheetEnd)
        val promptFieldStart = sheetBlock.indexOf("label = { Text(stringResource(R.string.system_prompt)) }")
        val temperatureStart = sheetBlock.indexOf("Text(\"${'$'}{stringResource(R.string.temperature)}", promptFieldStart)
        val promptFieldBlock = sheetBlock.substring(promptFieldStart, temperatureStart)

        assertTrue(sheetBlock.contains(".heightIn(max = 620.dp)"))
        assertTrue(sheetBlock.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(sheetBlock.contains(".weight(1f)"))
        assertFalse(sheetBlock.contains(".weight(1f, fill = false)"))
        assertTrue(promptFieldBlock.contains("maxLines = 8"))
        assertTrue(sheetBlock.contains(".navigationBarsPadding()"))
        assertTrue(sheetBlock.contains(".imePadding()"))
        assertTrue(sheetBlock.indexOf("Button(") > sheetBlock.indexOf(".verticalScroll(rememberScrollState())"))
    }

    @Test
    fun conversationModelAndEntityStoreAssistantProfile() {
        val modelSource = File("src/main/java/com/flowchat/app/domain/model/Conversation.kt").readText()
        val entitySource = File("src/main/java/com/flowchat/app/data/db/ConversationEntity.kt").readText()

        assertTrue(modelSource.contains("val assistantName: String = \"\""))
        assertTrue(modelSource.contains("val assistantAvatarPath: String? = null"))
        assertTrue(entitySource.contains("val assistantName: String"))
        assertTrue(entitySource.contains("val assistantAvatarPath: String?"))
        assertTrue(entitySource.contains("assistantName = assistantName"))
        assertTrue(entitySource.contains("assistantAvatarPath = assistantAvatarPath"))
    }

    @Test
    fun databaseMigratesAssistantProfileColumnsWithoutDestructiveMigration() {
        val databaseSource = File("src/main/java/com/flowchat/app/data/db/FlowChatDatabase.kt").readText()
        val appModuleSource = File("src/main/java/com/flowchat/app/di/AppModule.kt").readText()

        assertTrue(databaseSource.contains("version = 4"))
        assertTrue(databaseSource.contains("MIGRATION_1_2"))
        assertTrue(databaseSource.contains("ALTER TABLE conversations ADD COLUMN assistantName TEXT NOT NULL DEFAULT ''"))
        assertTrue(databaseSource.contains("ALTER TABLE conversations ADD COLUMN assistantAvatarPath TEXT"))
        assertTrue(databaseSource.contains("MIGRATION_2_3"))
        assertTrue(appModuleSource.contains(".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)"))
        assertFalse(appModuleSource.contains("fallbackToDestructiveMigration"))
    }

    @Test
    fun conversationSettingsIncludesAssistantAvatarPickerAndNickname() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val sheetStart = source.indexOf("private fun ConversationSettingsSheet(")
        val sheetEnd = source.indexOf("@Composable\nprivate fun LanguageSelector", sheetStart)
        val sheetBlock = source.substring(sheetStart, sheetEnd)

        assertTrue(source.contains("rememberLauncherForActivityResult(ActivityResultContracts.GetContent())"))
        assertTrue(source.contains("copyAssistantAvatarToPrivateFile(context, conversation.id, uri)"))
        assertTrue(sheetBlock.contains("AssistantProfileEditor("))
        assertTrue(sheetBlock.contains("assistantName"))
        assertTrue(sheetBlock.contains("assistantAvatarPath"))
        assertTrue(sheetBlock.contains("R.string.assistant_name"))
        assertTrue(sheetBlock.contains("onSave(assistantName, assistantAvatarPath, showAvatars, prompt"))
        assertTrue(sheetBlock.indexOf("AssistantProfileEditor(") < sheetBlock.indexOf("R.string.system_prompt"))
    }

    @Test
    fun chatScreenUsesAssistantProfileForTitleAndAssistantMessages() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val titleStart = source.indexOf("CenterAlignedTopAppBar")
        val titleEnd = source.indexOf("navigationIcon =", titleStart)
        val titleBlock = source.substring(titleStart, titleEnd)
        val bubbleStart = source.indexOf("private fun MessageBubble(")
        val bubbleEnd = source.indexOf("@OptIn(ExperimentalComposeUiApi::class)", bubbleStart)
        val bubbleBlock = source.substring(bubbleStart, bubbleEnd)

        assertTrue(titleBlock.contains("displayAssistantTitle()"))
        assertTrue(source.contains("MessageList(messages = state.messages, assistantAvatarPath = state.currentConversation?.assistantAvatarPath"))
        assertTrue(source.contains("private fun MessageBubble(message: Message, assistantAvatarPath: String?, userAvatarPath: String?, showAvatars: Boolean)"))
        assertTrue(bubbleBlock.contains("AssistantAvatar("))
        assertTrue(bubbleBlock.contains("if (isUser) Arrangement.End else Arrangement.Start"))
        assertTrue(source.contains("private fun Conversation.displayAssistantTitle()"))
    }

    @Test
    fun conversationSettingsControlsMessageAvatarsForBothRoles() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val modelSource = File("src/main/java/com/flowchat/app/domain/model/Conversation.kt").readText()
        val entitySource = File("src/main/java/com/flowchat/app/data/db/ConversationEntity.kt").readText()
        val databaseSource = File("src/main/java/com/flowchat/app/data/db/FlowChatDatabase.kt").readText()
        val appModuleSource = File("src/main/java/com/flowchat/app/di/AppModule.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()
        val sheetStart = source.indexOf("private fun ConversationSettingsSheet(")
        val sheetEnd = source.indexOf("@Composable\nprivate fun AssistantProfileEditor", sheetStart)
        val sheetBlock = source.substring(sheetStart, sheetEnd)
        val bubbleStart = source.indexOf("private fun MessageBubble(")
        val bubbleEnd = source.indexOf("@Composable\nprivate fun StreamingJumpingDots", bubbleStart)
        val bubbleBlock = source.substring(bubbleStart, bubbleEnd)

        assertTrue(modelSource.contains("val showAvatars: Boolean = false"))
        assertTrue(entitySource.contains("val showAvatars: Boolean"))
        assertTrue(entitySource.contains("showAvatars = showAvatars"))
        assertTrue(databaseSource.contains("version = 4"))
        assertTrue(databaseSource.contains("MIGRATION_2_3"))
        assertTrue(databaseSource.contains("ALTER TABLE conversations ADD COLUMN showAvatars INTEGER NOT NULL DEFAULT 0"))
        assertTrue(appModuleSource.contains(".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)"))
        assertTrue(sheetBlock.contains("var showAvatars by remember(conversation.id) { mutableStateOf(conversation.showAvatars) }"))
        assertTrue(sheetBlock.contains("Switch("))
        assertTrue(sheetBlock.contains("checked = showAvatars"))
        assertTrue(sheetBlock.contains("onCheckedChange = { showAvatars = it }"))
        assertTrue(sheetBlock.contains("R.string.show_avatars"))
        assertTrue(sheetBlock.contains("onSave(assistantName, assistantAvatarPath, showAvatars, prompt"))
        assertTrue(source.contains("MessageList(messages = state.messages, assistantAvatarPath = state.currentConversation?.assistantAvatarPath, userAvatarPath = userAvatarPath, showAvatars = state.currentConversation?.showAvatars == true"))
        assertTrue(source.contains("private fun MessageBubble(message: Message, assistantAvatarPath: String?, userAvatarPath: String?, showAvatars: Boolean)"))
        assertTrue(bubbleBlock.contains("if (showAvatars && !isUser)"))
        assertTrue(bubbleBlock.contains("if (showAvatars && isUser)"))
        assertTrue(bubbleBlock.contains("ProfileAvatar("))
        assertTrue(strings.contains("<string name=\"show_avatars\">Show avatars</string>"))
        assertTrue(zhStrings.contains("name=\"show_avatars\""))
    }

    @Test
    fun composerRemovesDeepThinkingControlAndMessagesRenderReasoningContent() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val viewModelSource = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val requestFactorySource = File("src/main/java/com/flowchat/app/domain/chat/ChatRequestFactory.kt").readText()
        val modelSource = File("src/main/java/com/flowchat/app/domain/model/Conversation.kt").readText()
        val entitySource = File("src/main/java/com/flowchat/app/data/db/ConversationEntity.kt").readText()
        val messageSource = File("src/main/java/com/flowchat/app/domain/model/Message.kt").readText()
        val messageEntitySource = File("src/main/java/com/flowchat/app/data/db/MessageEntity.kt").readText()
        val databaseSource = File("src/main/java/com/flowchat/app/data/db/FlowChatDatabase.kt").readText()
        val appModuleSource = File("src/main/java/com/flowchat/app/di/AppModule.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()
        val sheetStart = source.indexOf("private fun ConversationSettingsSheet(")
        val sheetEnd = source.indexOf("@Composable\nprivate fun AssistantProfileEditor", sheetStart)
        val sheetBlock = source.substring(sheetStart, sheetEnd)
        val bubbleStart = source.indexOf("private fun MessageBubble(")
        val bubbleEnd = source.indexOf("@Composable\nprivate fun StreamingJumpingDots", bubbleStart)
        val bubbleBlock = source.substring(bubbleStart, bubbleEnd)
        val bottomBarStart = source.indexOf("bottomBar = {")
        val bottomBarEnd = source.indexOf(") { padding ->", bottomBarStart)
        val bottomBarBlock = source.substring(bottomBarStart, bottomBarEnd)
        val composerStart = source.indexOf("private fun MessageComposer(")
        val composerEnd = source.indexOf("@OptIn(ExperimentalMaterial3Api::class)", composerStart)
        val composerBlock = source.substring(composerStart, composerEnd)

        assertTrue(modelSource.contains("val enableThinking: Boolean = false"))
        assertTrue(entitySource.contains("val enableThinking: Boolean"))
        assertTrue(entitySource.contains("enableThinking = enableThinking"))
        assertTrue(messageSource.contains("val reasoningContent: String = \"\""))
        assertTrue(messageEntitySource.contains("val reasoningContent: String"))
        assertTrue(messageEntitySource.contains("reasoningContent = reasoningContent"))
        assertTrue(databaseSource.contains("version = 4"))
        assertTrue(databaseSource.contains("MIGRATION_3_4"))
        assertTrue(databaseSource.contains("ALTER TABLE conversations ADD COLUMN enableThinking INTEGER NOT NULL DEFAULT 0"))
        assertTrue(databaseSource.contains("ALTER TABLE messages ADD COLUMN reasoningContent TEXT NOT NULL DEFAULT ''"))
        assertTrue(appModuleSource.contains(".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)"))
        assertFalse(sheetBlock.contains("var enableThinking"))
        assertFalse(sheetBlock.contains("R.string.deep_thinking"))
        assertFalse(sheetBlock.contains("checked = enableThinking"))
        assertFalse(sheetBlock.contains("onCheckedChange = { enableThinking = it }"))
        assertTrue(sheetBlock.contains("onSave(assistantName, assistantAvatarPath, showAvatars, prompt"))
        assertFalse(bottomBarBlock.contains("thinkingEnabled ="))
        assertFalse(bottomBarBlock.contains("onToggleThinking"))
        assertFalse(composerBlock.contains("thinkingEnabled: Boolean"))
        assertFalse(composerBlock.contains("onToggleThinking: () -> Unit"))
        assertFalse(composerBlock.contains("R.string.deep_thinking"))
        assertFalse(composerBlock.contains("onClick = onToggleThinking"))
        assertFalse(composerBlock.contains("if (thinkingEnabled)"))
        assertFalse(composerBlock.contains("DeepThinkingIcon("))
        assertFalse(composerBlock.contains("Icons.Filled.Psychology"))
        assertFalse(source.contains("private fun DeepThinkingIcon("))
        assertFalse(viewModelSource.contains("fun toggleThinking()"))
        assertTrue(requestFactorySource.contains("enableThinking = true"))
        assertTrue(bubbleBlock.contains("message.reasoningContent"))
        assertTrue(bubbleBlock.contains("ReasoningBubble("))
        assertTrue(bubbleBlock.contains("val hasReasoning = !isUser && message.reasoningContent.isNotBlank()"))
        assertTrue(bubbleBlock.contains("val isWaitingForAssistant ="))
        assertTrue(bubbleBlock.contains("message.content.isBlank() && !hasReasoning"))
        assertTrue(bubbleBlock.contains("val shouldShowContentBubble ="))
        assertTrue(bubbleBlock.contains("if (shouldShowContentBubble)"))
        assertTrue(source.contains("private fun ReasoningBubble("))
        assertTrue(source.contains("R.string.thinking_in_progress"))
        assertTrue(source.contains("R.string.thinking_done"))
        assertTrue(source.contains("R.string.thinking_stopped"))
        assertTrue(source.contains("message.status == MessageStatus.Stopped"))
        assertTrue(source.contains("R.string.thinking_expand"))
        assertTrue(source.contains("R.string.thinking_collapse"))
        assertTrue(source.contains("val elapsedSeconds ="))
        assertTrue(source.contains("clickable { expanded = !expanded }"))
        assertTrue(source.contains("text = message.reasoningContent"))
        assertFalse(source.contains("R.string.thinking_block"))
        assertTrue(strings.contains("<string name=\"thinking_in_progress\">Thinking...</string>"))
        assertTrue(strings.contains("<string name=\"thinking_done\">Thought for %1${'$'}d sec</string>"))
        assertTrue(strings.contains("<string name=\"thinking_expand\">Expand</string>"))
        assertTrue(strings.contains("<string name=\"thinking_collapse\">Collapse</string>"))
        assertTrue(zhStrings.contains("name=\"thinking_in_progress\""))
        assertTrue(zhStrings.contains("name=\"thinking_done\""))
        assertTrue(zhStrings.contains("name=\"thinking_expand\""))
        assertTrue(zhStrings.contains("name=\"thinking_collapse\""))
        assertFalse(strings.contains("name=\"thinking_block\""))
        assertFalse(zhStrings.contains("name=\"thinking_block\""))
        assertFalse(strings.contains("name=\"deep_thinking\""))
        assertFalse(zhStrings.contains("name=\"deep_thinking\""))
    }

    @Test
    fun avatarEditPencilButtonsSlightlyOverlapAvatarCornerWithGrayFillAndWhitePencil() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val userSheetStart = source.indexOf("private fun UserProfileSettingsSheet(")
        val userSheetEnd = source.indexOf("@Composable\nprivate fun SettingsRow", userSheetStart)
        val userSheetBlock = source.substring(userSheetStart, userSheetEnd)
        val assistantEditorStart = source.indexOf("private fun AssistantProfileEditor(")
        val assistantEditorEnd = source.indexOf("@Composable\nprivate fun AvatarEditButton", assistantEditorStart)
        val assistantEditorBlock = source.substring(assistantEditorStart, assistantEditorEnd)
        val editButtonStart = source.indexOf("private fun AvatarEditButton(")
        val editButtonEnd = source.indexOf("@Composable\nprivate fun ProfileAvatar", editButtonStart)
        val editButtonBlock = source.substring(editButtonStart, editButtonEnd)

        assertTrue(userSheetBlock.contains("AvatarEditButton("))
        assertTrue(userSheetBlock.contains(".align(Alignment.BottomEnd)"))
        assertTrue(userSheetBlock.contains(".offset(x = (-4).dp, y = (-4).dp)"))
        assertFalse(userSheetBlock.contains("OutlinedIconButton("))
        assertFalse(userSheetBlock.contains(".size(42.dp)"))
        assertTrue(assistantEditorBlock.contains("AvatarEditButton("))
        assertTrue(assistantEditorBlock.contains(".align(Alignment.BottomEnd)"))
        assertTrue(assistantEditorBlock.contains(".offset(x = (-4).dp, y = (-4).dp)"))
        assertFalse(assistantEditorBlock.contains("OutlinedIconButton("))
        assertFalse(assistantEditorBlock.contains(".size(42.dp)"))
        assertTrue(editButtonBlock.contains(".size(32.dp)"))
        assertTrue(editButtonBlock.contains(".background(Color(0xFF3A3A3A), CircleShape)"))
        assertTrue(editButtonBlock.contains("val pencilColor = Color.White"))
        assertTrue(editButtonBlock.contains("Canvas(modifier = Modifier.size(17.dp))"))
        assertTrue(editButtonBlock.contains("val upperBodyStart = Offset("))
        assertTrue(editButtonBlock.contains("val lowerBodyStart = Offset("))
        assertTrue(editButtonBlock.contains("val pencilTip = Offset("))
        assertTrue(editButtonBlock.contains("val tailStart = upperBodyEnd"))
        assertTrue(editButtonBlock.contains("val tailEnd = lowerBodyEnd"))
        assertTrue(editButtonBlock.contains("start = upperBodyStart"))
        assertTrue(editButtonBlock.contains("start = lowerBodyStart"))
        assertTrue(editButtonBlock.contains("end = pencilTip"))
        assertFalse(editButtonBlock.contains("val bodyStart = Offset("))
        assertFalse(editButtonBlock.contains("Icons.Default.Edit"))
    }

    @Test
    fun messageBubbleSupportsCopyAndTextSelectionActions() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()
        val bubbleStart = source.indexOf("private fun MessageBubble(")
        val bubbleEnd = source.indexOf("@Composable\nprivate fun StreamingJumpingDots", bubbleStart)
        val bubbleBlock = source.substring(bubbleStart, bubbleEnd)

        assertTrue(bubbleBlock.contains("combinedClickable("))
        assertTrue(bubbleBlock.contains("onLongClick = {"))
        assertTrue(bubbleBlock.contains("showMessageActions = true"))
        assertTrue(bubbleBlock.contains("DropdownMenu("))
        assertTrue(bubbleBlock.contains("DropdownMenuItem("))
        assertTrue(bubbleBlock.contains("clipboardManager.setText(AnnotatedString(copyText))"))
        assertTrue(bubbleBlock.contains("showTextSelection = true"))
        assertTrue(bubbleBlock.contains("SelectionContainer"))
        assertTrue(bubbleBlock.contains("R.string.copy_message"))
        assertTrue(bubbleBlock.contains("R.string.select_text"))
        assertTrue(strings.contains("<string name=\"copy_message\">Copy</string>"))
        assertTrue(strings.contains("<string name=\"select_text\">Select text</string>"))
        assertTrue(zhStrings.contains("<string name=\"copy_message\">复制</string>"))
        assertTrue(zhStrings.contains("<string name=\"select_text\">选取文字</string>"))
    }
    @Test
    fun messageBubbleShowsSystemTimeBelowEachDialogBubble() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val bubbleStart = source.indexOf("private fun MessageBubble(")
        val bubbleEnd = source.indexOf("@Composable\nprivate fun StreamingJumpingDots", bubbleStart)
        val bubbleBlock = source.substring(bubbleStart, bubbleEnd)

        assertTrue(bubbleBlock.contains("val context = LocalContext.current"))
        assertTrue(bubbleBlock.contains("val messageTime = remember(context, message.createdAt)"))
        assertTrue(bubbleBlock.contains("formatMessageTime(context, message.createdAt)"))
        assertTrue(bubbleBlock.contains("text = messageTime"))
        assertTrue(bubbleBlock.contains("style = MaterialTheme.typography.labelSmall"))
        assertTrue(bubbleBlock.contains("color = MaterialTheme.colorScheme.onSurfaceVariant"))
        assertTrue(bubbleBlock.contains(".align(if (isUser) Alignment.End else Alignment.Start)"))
        assertTrue(source.contains("private fun formatMessageTime(context: Context, timestampMillis: Long): String"))
        assertTrue(source.contains("android.text.format.DateFormat.getTimeFormat(context)"))
        assertTrue(source.contains("Date(timestampMillis)"))
    }

    @Test
    fun messageListAutoScrollsToLatestMessageWhenMessagesChange() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val listStart = source.indexOf("private fun MessageList(")
        val listEnd = source.indexOf("@OptIn(ExperimentalFoundationApi::class)", listStart)
        val listBlock = source.substring(listStart, listEnd)

        assertTrue(source.contains("import androidx.compose.runtime.LaunchedEffect"))
        assertTrue(source.contains("import androidx.compose.foundation.lazy.rememberLazyListState"))
        assertTrue(listBlock.contains("val listState = rememberLazyListState()"))
        assertTrue(listBlock.contains("val lastMessage = messages.lastOrNull()"))
        assertTrue(listBlock.contains("LaunchedEffect(messages.size, lastMessage?.id, lastMessage?.content, lastMessage?.reasoningContent)"))
        assertTrue(listBlock.contains("listState.animateScrollToItem(messages.size)"))
        assertTrue(listBlock.contains("state = listState"))
        assertTrue(listBlock.contains("item(key = BottomMessageAnchorKey)"))
        assertTrue(source.contains("private const val BottomMessageAnchorKey = \"bottom-message-anchor\""))
    }

    @Test
    fun composerShowsWebSearchToggleBelowInputWithSeparateOnAndOffIcons() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val bottomBarStart = source.indexOf("bottomBar = {")
        val bottomBarEnd = source.indexOf(") { padding ->", bottomBarStart)
        val bottomBarBlock = source.substring(bottomBarStart, bottomBarEnd)
        val composerStart = source.indexOf("private fun MessageComposer(")
        val composerEnd = source.indexOf("@OptIn(ExperimentalMaterial3Api::class)", composerStart)
        val composerBlock = source.substring(composerStart, composerEnd)
        val strings = File("src/main/res/values/strings.xml").readText()
        val zhStrings = File("src/main/res/values-zh-rCN/strings.xml").readText()

        assertTrue(bottomBarBlock.contains("webSearchEnabled = state.webSearchEnabled"))
        assertTrue(bottomBarBlock.contains("enabled = state.currentConversation != null"))
        assertTrue(bottomBarBlock.contains("onToggleWebSearch = viewModel::toggleWebSearch"))
        assertTrue(composerBlock.contains("enabled: Boolean"))
        assertTrue(composerBlock.contains("webSearchEnabled: Boolean"))
        assertTrue(composerBlock.contains("onToggleWebSearch: () -> Unit"))
        assertTrue(composerBlock.contains("R.string.create_conversation_to_chat"))
        assertTrue(composerBlock.contains("if (enabled) R.string.message_hint else R.string.create_conversation_to_chat"))
        assertTrue(composerBlock.contains("enabled = enabled"))
        assertTrue(composerBlock.contains("R.drawable.ic_web_search_on"))
        assertTrue(composerBlock.contains("R.drawable.ic_web_search_off"))
        assertTrue(composerBlock.contains("enabled = enabled && !isStreaming"))
        assertTrue(composerBlock.contains("onClick = onToggleWebSearch"))
        assertTrue(composerBlock.contains("if (webSearchEnabled)"))
        assertTrue(composerBlock.contains("R.string.web_search_on"))
        assertTrue(composerBlock.contains("R.string.web_search_off"))
        assertTrue(strings.contains("<string name=\"create_conversation_to_chat\">Create a conversation before chatting</string>"))
        assertTrue(zhStrings.contains("<string name=\"create_conversation_to_chat\">请新建会话再进行聊天</string>"))
        assertTrue(File("src/main/res/drawable/ic_web_search_on.xml").exists())
        assertTrue(File("src/main/res/drawable/ic_web_search_off.xml").exists())
        assertFalse(File("src/main/res/drawable-nodpi/web_search_on.png").exists())
        assertFalse(File("src/main/res/drawable-nodpi/web_search_off.png").exists())
    }

    @Test
    fun composerSendButtonUsesInputAwareColorAndStreamingStopIcon() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val composerStart = source.indexOf("private fun MessageComposer(")
        val composerEnd = source.indexOf("@OptIn(ExperimentalMaterial3Api::class)", composerStart)
        val composerBlock = source.substring(composerStart, composerEnd)
        val sendButtonStart = composerBlock.indexOf("IconButton(")
        val sendButtonEnd = composerBlock.indexOf("Row(", sendButtonStart)
        val sendButtonBlock = composerBlock.substring(sendButtonStart, sendButtonEnd)

        assertTrue(composerBlock.contains("val hasInput = value.isNotBlank()"))
        assertTrue(composerBlock.contains("val isDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f"))
        assertTrue(composerBlock.contains("val sendButtonContainerColor = when {"))
        assertTrue(composerBlock.contains("isStreaming || hasInput -> if (isDarkBackground) Color.White else Color.Black"))
        assertTrue(composerBlock.contains("else -> Color(0xFFBDBDBD)"))
        assertTrue(composerBlock.contains("val sendButtonContentColor = if (isDarkBackground) Color.Black else Color.White"))
        assertTrue(sendButtonBlock.contains("onClick = if (isStreaming) onStop else ::submitMessage"))
        assertTrue(sendButtonBlock.contains("enabled = enabled || isStreaming"))
        assertTrue(sendButtonBlock.contains(".background(sendButtonContainerColor, CircleShape)"))
        assertTrue(sendButtonBlock.contains("if (isStreaming) {"))
        assertTrue(sendButtonBlock.contains("StreamingStopIcon("))
        assertTrue(sendButtonBlock.contains("Icons.AutoMirrored.Filled.Send"))
        assertTrue(source.contains("private fun StreamingStopIcon("))
        assertTrue(source.contains("drawRoundRect("))
        assertTrue(source.contains("cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())"))
    }

    @Test
    fun composerClearsTextFieldAfterSubmittingMessage() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatScreen.kt").readText()
        val composerStart = source.indexOf("private fun MessageComposer(")
        val composerEnd = source.indexOf("@OptIn(ExperimentalMaterial3Api::class)", composerStart)
        val composerBlock = source.substring(composerStart, composerEnd)

        assertTrue(composerBlock.contains("fun submitMessage()"))
        assertTrue(composerBlock.contains("if (!enabled) return"))
        assertTrue(composerBlock.contains("if (value.isBlank()) return"))
        assertTrue(composerBlock.contains("onSend()"))
        assertTrue(composerBlock.contains("onValueChange(\"\")"))
        assertTrue(composerBlock.contains("keyboardActions = KeyboardActions(onSend = { submitMessage() })"))
        assertTrue(composerBlock.contains("onClick = if (isStreaming) onStop else ::submitMessage"))
    }

    @Test
    fun chatViewModelSearchesAndInjectsContextOnlyWhenWebSearchIsEnabled() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()

        assertTrue(source.contains("private val webSearchEnabled = MutableStateFlow(false)"))
        assertTrue(source.contains("webSearchEnabled = webSearchEnabledValue"))
        assertTrue(source.contains("fun toggleWebSearch()"))
        assertTrue(source.contains("if (!isStreaming.value)"))
        assertTrue(source.contains("webSearchSettingsRepository.getTavilyApiKey()"))
        assertTrue(source.contains("webSearchClient.search(text, tavilyApiKey)"))
        assertTrue(source.contains("WebSearchContextFormatter.format(searchResult)"))
        assertTrue(source.contains("promptProfileRepository.getActiveProfile()"))
        assertTrue(source.contains("memoryRepository.retrieve(text, activeProfile.memory.topN)"))
        assertTrue(source.contains("MemoryContextFormatter.format("))
        assertTrue(source.contains("ChatRequestFactory.create("))
        assertTrue(source.contains("activeProfile.thinkingFormat"))
        assertTrue(source.contains("saveMemoryIfEnabled(activeProfile, text, content)"))
        assertTrue(source.contains("memoryRepository.saveTurn(userMessage, assistantReply)"))
        assertTrue(source.contains("Tavily API key is not configured."))
        assertTrue(source.contains("No web search results found."))
    }

    @Test
    fun chatViewModelDisplaysBatchedReasoningBeforeFinalContentIncrementally() {
        val source = File("src/main/java/com/flowchat/app/presentation/chat/ChatViewModel.kt").readText()
        val reasoningBranchStart = source.indexOf("is ChatDelta.Reasoning ->")
        val fullResponseBranchStart = source.indexOf("is ChatDelta.FullResponse ->")
        val doneBranchStart = source.indexOf("ChatDelta.Done ->")
        val reasoningBranch = source.substring(reasoningBranchStart, fullResponseBranchStart)
        val fullResponseBranch = source.substring(fullResponseBranchStart, doneBranchStart)

        assertTrue(source.contains("private suspend fun appendReasoningDelta("))
        assertTrue(source.contains("private suspend fun appendContentDelta("))
        assertTrue(source.contains("private fun String.displayChunks()"))
        assertTrue(source.contains("BatchedReasoningToContentPauseMillis"))
        assertTrue(source.contains("IncrementalDisplayDelayMillis"))
        assertTrue(reasoningBranch.contains("appendReasoningDelta("))
        assertFalse(reasoningBranch.contains("reasoningContent += delta.text"))
        assertTrue(fullResponseBranch.indexOf("appendReasoningDelta(") < fullResponseBranch.indexOf("delay(BatchedReasoningToContentPauseMillis)"))
        assertTrue(fullResponseBranch.indexOf("delay(BatchedReasoningToContentPauseMillis)") < fullResponseBranch.indexOf("appendContentDelta("))
        assertTrue(source.contains("if (content.isBlank() && reasoningContent.isNotBlank())"))
    }
}

private data class RgbaPng(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
) {
    fun alphaAt(x: Int, y: Int): Int = pixels[((y * width + x) * 4) + 3].toInt() and 0xFF

    fun countPixels(predicate: (x: Int, y: Int, r: Int, g: Int, b: Int, alpha: Int) -> Boolean): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = (y * width + x) * 4
                val r = pixels[offset].toInt() and 0xFF
                val g = pixels[offset + 1].toInt() and 0xFF
                val b = pixels[offset + 2].toInt() and 0xFF
                val alpha = pixels[offset + 3].toInt() and 0xFF
                if (predicate(x, y, r, g, b, alpha)) {
                    count++
                }
            }
        }
        return count
    }
}

private fun decodeRgbaPng(file: File): RgbaPng {
    val bytes = file.readBytes()
    require(bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))

    var offset = 8
    var width = 0
    var height = 0
    var bitDepth = 0
    var colorType = 0
    val idat = ByteArrayOutputStream()

    while (offset < bytes.size) {
        val length = readPngInt(bytes, offset)
        val type = String(bytes, offset + 4, 4, StandardCharsets.US_ASCII)
        val dataOffset = offset + 8
        when (type) {
            "IHDR" -> {
                width = readPngInt(bytes, dataOffset)
                height = readPngInt(bytes, dataOffset + 4)
                bitDepth = bytes[dataOffset + 8].toInt() and 0xFF
                colorType = bytes[dataOffset + 9].toInt() and 0xFF
            }
            "IDAT" -> idat.write(bytes, dataOffset, length)
            "IEND" -> break
        }
        offset = dataOffset + length + 4
    }

    require(bitDepth == 8 && colorType == 6) {
        "reference_gear.png must be 8-bit RGBA PNG, got bitDepth=$bitDepth colorType=$colorType"
    }

    val inflated = InflaterInputStream(ByteArrayInputStream(idat.toByteArray())).readBytes()
    val stride = width * 4
    val pixels = ByteArray(height * stride)
    var sourceOffset = 0
    var previous = ByteArray(stride)

    repeat(height) { y ->
        val filter = inflated[sourceOffset++].toInt() and 0xFF
        val row = inflated.copyOfRange(sourceOffset, sourceOffset + stride)
        sourceOffset += stride

        for (index in row.indices) {
            val raw = row[index].toInt() and 0xFF
            val left = if (index >= 4) row[index - 4].toInt() and 0xFF else 0
            val up = previous[index].toInt() and 0xFF
            val upLeft = if (index >= 4) previous[index - 4].toInt() and 0xFF else 0
            val value = when (filter) {
                0 -> raw
                1 -> raw + left
                2 -> raw + up
                3 -> raw + ((left + up) / 2)
                4 -> raw + paethPredictor(left, up, upLeft)
                else -> error("Unsupported PNG filter: $filter")
            }
            row[index] = (value and 0xFF).toByte()
        }

        row.copyInto(pixels, y * stride)
        previous = row
    }

    return RgbaPng(width, height, pixels)
}

private fun readPngInt(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)

private fun readPngDimensions(file: File): Pair<Int, Int> {
    val bytes = file.readBytes()
    require(bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))
    return readPngInt(bytes, 16) to readPngInt(bytes, 20)
}

private fun paethPredictor(left: Int, up: Int, upLeft: Int): Int {
    val estimate = left + up - upLeft
    val leftDistance = kotlin.math.abs(estimate - left)
    val upDistance = kotlin.math.abs(estimate - up)
    val upLeftDistance = kotlin.math.abs(estimate - upLeft)
    return when {
        leftDistance <= upDistance && leftDistance <= upLeftDistance -> left
        upDistance <= upLeftDistance -> up
        else -> upLeft
    }
}
