package com.github.kkkiio.gocopyqualifiedname

import com.github.kkkiio.gocopyqualifiedname.actions.CopyQualifiedNameAction
import com.goide.project.GoApplicationLibrariesService
import com.goide.project.GoProjectLibrariesService
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkImpl
import com.goide.sdk.GoSdkService
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import java.io.File

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class CopyQualifiedNameActionTest : BasePlatformTestCase() {
    fun testCopyQualifiedName() {
        myFixture.configureByFile("CopyQualifiedNameActionTest/main.go")
        myFixture.testAction(CopyQualifiedNameAction())
        assertEquals(
            "encoding/json.Marshal",
            CopyPasteManager.getInstance().contents?.getTransferData(DataFlavor.stringFlavor)
        )
    }

    override fun getTestDataPath() = "src/test/testData/src"

    override fun setUp() {
        super.setUp()
        GoProjectLibrariesService.getInstance(project).isUseGoPathFromSystemEnvironment = false
        GoApplicationLibrariesService.getInstance().setLibraryRootUrls("temp:///src/")
        setUpProjectSdk()
    }


    override fun tearDown() {
        GoApplicationLibrariesService.getInstance().setLibraryRootUrls()
        super.tearDown()
    }

    private fun setUpProjectSdk() {
        GoSdkService.getInstance(project).setSdk(createMockSdk())
    }

    private fun createMockSdk(): GoSdk {
        val version = "1.21.0"
        val homePath: String = localFileUrl("src/test/testData/mockSdk-$version/")
        return GoSdkImpl(homePath, version, null)
    }

    private fun localFileUrl(relPath: String): String {
        return File(relPath).also { check(it.exists()) }.let { "file://" + it.absolutePath }
    }
}
