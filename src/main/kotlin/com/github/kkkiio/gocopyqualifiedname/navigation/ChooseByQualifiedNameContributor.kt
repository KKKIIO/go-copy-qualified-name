package com.github.kkkiio.gocopyqualifiedname.navigation

import com.goide.psi.GoNamedElement
import com.goide.sdk.GoPackageUtil
import com.goide.stubs.index.GoAllPrivateNamesIndex
import com.goide.stubs.index.GoAllPublicNamesIndex
import com.goide.stubs.index.GoNonPackageLevelNamesIndex
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.ResolveState
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter

class ChooseByQualifiedNameContributor : ChooseByNameContributorEx {
    private val myIndexKeys = arrayOf(
        GoAllPublicNamesIndex.ALL_PUBLIC_NAMES,
        GoAllPrivateNamesIndex.ALL_PRIVATE_NAMES,
        GoNonPackageLevelNamesIndex.KEY
    )
    override fun processNames(p0: Processor<in String>, p1: GlobalSearchScope, p2: IdFilter?) = Unit

    override fun processElementsWithName(name: String, processor: Processor<in NavigationItem>, parameters: FindSymbolParameters) {
        // remove line number if any
        val fqn = name.substringBeforeLast(':')
        // split import path and name by first dot
        val parts = fqn.split('.', limit = 2)
        val ( importPath, shortName ) = if (parts.size == 2)  {
            parts[0] to parts[1]
        } else {
            "" to parts[0]
        }
        val packageScope = GoPackageUtil.findByImportPath(importPath, parameters.project, null, ResolveState.initial())
            .let { GoPackageUtil.packagesScope(it) }
        val scope = parameters.searchScope.intersectWith(packageScope)
        myIndexKeys.forEach { key->
            ProgressManager.checkCanceled()
            StubIndex.getInstance().processElements(
                key,
                shortName,
                parameters.project,
                scope,
                parameters.idFilter,
                GoNamedElement::class.java, processor
            )
        }
    }
}