package com.mocharealm.accompanist.lyrics.phonetics

/** Share immutable storage across engines, opening each registered pack at most once. */
class SharedPackLoader(delegate: PackLoader, packNames: Set<String> = ReadingLocale.entries.map { it.packName }.toSet()) : PackLoader {
    private val sources = packNames.associateWith { name -> lazy { delegate.load(name) } }
    override fun load(packName: String): ByteSource = requireNotNull(sources[packName]) { "Unregistered pack: $packName" }.value
}

/** Defers opening a pack until this language is parsed. Lazy loading never changes scoring. */
class LazyPackResolver(override val locale: ReadingLocale, loader: PackLoader,
    decorate: (ReadingLexicon) -> ReadingLexicon = { it },
) : LanguageResolver {
    private val delegate by lazy {
        val dictionary = CompiledDictionary(loader.load(locale.packName))
        require(dictionary.locale == locale) { "Wrong language pack" }
        LexiconResolver(decorate(dictionary))
    }
    override fun resolve(text: String, range: TextRange, options: ParseOptions, workspace: ParseWorkspace): LanguageResult =
        delegate.resolve(text, range, options, workspace)
}
