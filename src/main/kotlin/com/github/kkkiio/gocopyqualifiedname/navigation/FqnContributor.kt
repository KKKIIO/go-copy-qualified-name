package com.github.kkkiio.gocopyqualifiedname.navigation

import com.github.kkkiio.gocopyqualifiedname.actions.getFqn
import com.goide.go.GoGotoContributorBase
import com.goide.psi.GoNamedElement
import com.goide.sdk.GoPackageUtil
import com.goide.stubs.index.GoAllPrivateNamesIndex
import com.goide.stubs.index.GoAllPublicNamesIndex
import com.goide.stubs.index.GoNonPackageLevelNamesIndex
import com.intellij.navigation.ChooseByNameContributorEx2
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.ResolveState
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter

class FqnGoNamedElement(private val delegate: GoNamedElement) : GoNamedElement by delegate {
    override fun getQualifiedName(): String? {
        return getFqn(delegate)
    }
}

data class FqnPattern(val importPath: String)

class FqnContributor : GoGotoContributorBase<GoNamedElement>(
    GoNamedElement::class.java,
    GoAllPublicNamesIndex.ALL_PUBLIC_NAMES,
    GoAllPrivateNamesIndex.ALL_PRIVATE_NAMES,
    GoNonPackageLevelNamesIndex.KEY,
), ChooseByNameContributorEx2 {
    private val logger = thisLogger()
    override fun processNames(processor: Processor<in String>, parameters: FindSymbolParameters) {
        val pattern = parsePattern(parameters) ?: return
        val packages =
            GoPackageUtil.findByImportPath(pattern.importPath, parameters.project, null, ResolveState.initial())
        logger.debug { "processNames parameters=${parameters.debugs()}, packages.size=${packages.size}" }
        if (packages.isEmpty()) return
        val packageScope = GoPackageUtil.packagesScope(packages)
        val scope = parameters.searchScope.intersectWith(packageScope)
        processNames(processor, scope, parameters.idFilter)
    }

    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        super.processNames({
            // distinguish from the result of super
            processor.process(Char.MIN_VALUE + it)
        }, scope, filter)
    }

    override fun processElementsWithName(
        rawName: String, processor: Processor<in NavigationItem>, parameters: FindSymbolParameters
    ) {
        if (!rawName.startsWith(Char.MIN_VALUE)) return
        val name = rawName.drop(1)
        if (!parameters.completePattern.contains(name)) return
        val pattern = parsePattern(parameters) ?: return
        val packages =
            GoPackageUtil.findByImportPath(pattern.importPath, parameters.project, null, ResolveState.initial())
        logger.debug { "processElementsWithName name=${name}, parameters=${parameters.debugs()}, packages.size=${packages.size}" }
        if (packages.isEmpty()) return
        val packageScope = GoPackageUtil.packagesScope(packages)
        val scope = parameters.searchScope.intersectWith(packageScope)
        super.processElementsWithName(name, { elem: NavigationItem ->
            val namedElement = elem as GoNamedElement
            logger.debug {
                "processElementsWithName.p elem=${namedElement.name}, elem.id=${
                    System.identityHashCode(
                        namedElement
                    )
                }"
            }
            processor.process(FqnGoNamedElement(namedElement))
        }, parameters.withScope(scope))
    }

    private fun parsePattern(parameters: FindSymbolParameters): FqnPattern? {
        val completePattern = parameters.completePattern
        // remove line number if any
        val fqn = completePattern.substringBefore(':')
        val iLastPathSep = fqn.lastIndexOf('/')
        if (iLastPathSep <= 0) return null
        val packageDot = fqn.indexOf('.', iLastPathSep + 1)
        if (packageDot < 0) return null
        return FqnPattern(fqn.substring(0, packageDot))
    }
}

fun FindSymbolParameters.debugs() =
    "{completePattern=$completePattern, localPatternName=$localPatternName, searchScope=$searchScope, idFilter=$idFilter}"