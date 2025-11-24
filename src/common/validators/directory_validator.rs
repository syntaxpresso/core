use crate::common::utils::path_security_util::PathSecurityValidator;
use std::path::PathBuf;

/// Validates that a file path is contained within the specified base directory (cwd).
/// This ensures all file operations stay within the user's chosen working directory,
/// preventing accidental writes outside the project scope.
///
/// # Arguments
/// * `file_path` - The file path string to validate
/// * `base_path` - The base directory that the file must be contained within
///
/// # Returns
/// * `Ok(PathBuf)` - The canonicalized, validated file path
/// * `Err(String)` - If the file path is invalid or outside the base directory
///
/// # Validation Rules
/// - File path must resolve to a location within the base directory
/// - Resolves symbolic links to determine the actual file location
/// - Works with both existing and non-existent file paths
/// - Rejects paths that would escape the working directory (e.g., "../../outside")
///
pub fn validate_file_path_within_base(
  file_path: &str,
  base_path: &std::path::Path,
) -> Result<PathBuf, String> {
  let path = PathBuf::from(file_path);
  // Path containment validation - ensure the file path is within the cwd
  let validator = PathSecurityValidator::new(base_path)
    .map_err(|e| format!("Path containment validation setup failed: {}", e))?;
  validator
    .validate_path_containment(&path)
    .map_err(|e| format!("File path must be within working directory: {}", e))
}

/// Validates that a directory exists and is accessible, without containment restrictions.
/// This function is designed for the --cwd parameter to allow users to work on any project
/// they have access to. Path containment validation happens WITHIN the chosen directory,
/// not on the directory selection itself.
///
/// # Arguments
/// * `s` - The directory path string to validate
///
/// # Returns
/// * `Ok(PathBuf)` - The canonicalized directory path
/// * `Err(String)` - If the directory is invalid or doesn't exist
///
/// # Design Philosophy
/// - The --cwd parameter should accept any valid directory (user's project root)
/// - Path containment restrictions apply to operations WITHIN the chosen cwd, not to the cwd selection itself
/// - Users should be able to work on projects anywhere they have filesystem access
pub fn validate_directory_unrestricted(s: &str) -> Result<PathBuf, String> {
  let path = PathBuf::from(s);
  // Basic validation - ensure directory exists and is accessible
  if !path.exists() {
    return Err(format!("Directory does not exist: {}", s));
  }
  if !path.is_dir() {
    return Err(format!("Path is not a directory: {}", s));
  }
  // Canonicalize to resolve any symbolic links and get absolute path
  path.canonicalize().map_err(|e| format!("Cannot canonicalize directory path '{}': {}", s, e))
}
