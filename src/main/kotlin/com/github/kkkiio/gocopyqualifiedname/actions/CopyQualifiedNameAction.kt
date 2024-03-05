package com.github.kkkiio.gocopyqualifiedname.actions

import com.goide.GoLanguage
import com.goide.psi.GoNamedElement
import com.goide.util.GoUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import java.awt.datatransfer.StringSelection

class CopyQualifiedNameAction : AnAction() {
    private val logger = thisLogger()
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        // only for *.go files
        e.presentation.isEnabledAndVisible =
            e.dataContext.getData(PlatformDataKeys.PSI_FILE)?.language == GoLanguage.INSTANCE
    }

    override fun actionPerformed(e: AnActionEvent) {
        // for go identifier under cursor
        val psiElement = e.dataContext.getData(PlatformDataKeys.PSI_ELEMENT) ?: return
        logger.debug { "actionPerformed start, psiElement=${psiElement.debugs()}" }
        // as go reference expression
        val goNamedElement = psiElement.parentOfType<GoNamedElement>(withSelf = true) ?: return
        logger.debug { "goNamedElement=${goNamedElement.debugs()}" }
        val qualifiedName = getFqn(goNamedElement)
        // set clipboard
        CopyPasteManager.getInstance().setContents(StringSelection(qualifiedName))
    }
}

fun getFqn(goNamedElement: GoNamedElement): String? {
    val importPath = GoUtil.getImportPath(goNamedElement, goNamedElement) ?: return null
    return "$importPath.${goNamedElement.name}"
}

fun String.quoteKt() = StringUtil.wrapWithDoubleQuote(StringUtil.escapeStringCharacters(this))
fun PsiElement.debugs() = "{text=${text.quoteKt()}, elementType=$elementType, class=${javaClass}}"