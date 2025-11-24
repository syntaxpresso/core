use tree_sitter::Language;

/// Enum representing programming languages supported by Syntaxpresso
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SupportedLanguage {
  /// Java programming language
  Java,
  // Future languages can be added here:
  // Python,
  // TypeScript,
  // JavaScript,
}

impl SupportedLanguage {
  /// Get the tree-sitter Language object for this language
  ///
  /// This provides the Tree-sitter grammar needed to parse source code
  /// in the specified language.
  ///
  /// # Examples
  ///
  /// ```
  /// use syntaxpresso_core::common::supported_language::SupportedLanguage;
  ///
  /// let language = SupportedLanguage::Java;
  /// let ts_language = language.tree_sitter_language();
  /// ```
  pub fn tree_sitter_language(&self) -> Language {
    match self {
      SupportedLanguage::Java => tree_sitter_java::LANGUAGE.into(),
    }
  }

  /// Get the primary file extension for this language
  ///
  /// Returns the most common file extension without the leading dot.
  ///
  /// # Examples
  ///
  /// ```
  /// use syntaxpresso_core::common::supported_language::SupportedLanguage;
  ///
  /// assert_eq!(SupportedLanguage::Java.file_extension(), "java");
  /// ```
  pub fn file_extension(&self) -> &'static str {
    match self {
      SupportedLanguage::Java => "java",
    }
  }

  /// Get the human-readable name of the language
  ///
  /// # Examples
  ///
  /// ```
  /// use syntaxpresso_core::common::supported_language::SupportedLanguage;
  ///
  /// assert_eq!(SupportedLanguage::Java.name(), "Java");
  /// ```
  pub fn name(&self) -> &'static str {
    match self {
      SupportedLanguage::Java => "Java",
    }
  }
}
