use std::path::Path;

use crate::{
  commands::java::{
    services::create_java_file_service::run,
    responses::file_response::FileResponse,
    treesitter::types::{java_file_type::JavaFileType, java_source_directory_type::JavaSourceDirectoryType},
  },
  common::{response::Response, utils::case_util},
};

pub fn execute(
  cwd: &Path,
  package_name: &str,
  file_name: &str,
  file_type: &JavaFileType,
  source_directory: &JavaSourceDirectoryType,
) -> Response<FileResponse> {
  let normalized_file_name = case_util::to_pascal_case(file_name);
  let cwd_string = cwd.display().to_string();
  let cmd_name = String::from("create-java-file");
  match run(cwd, package_name, &normalized_file_name, file_type, source_directory) {
    Ok(response) => Response::success(cmd_name, cwd_string, response),
    Err(error_msg) => Response::error(cmd_name, cwd_string, error_msg),
  }
}
