use std::path::Path;

use crate::{
  commands::java::command_services::create_jpa_entity_service::run,
  commands::java::responses::{file_response::FileResponse},
  common::response::Response,
};

pub fn execute(
  cwd: &Path,
  package_name: &str,
  file_name: &str,
  superclass_type: Option<&str>,
  superclass_package_name: Option<&str>,
) -> Response<FileResponse> {
  let cwd_string = cwd.display().to_string();
  let cmd_name = String::from("create-jpa-entity");
  match run(cwd, package_name, file_name, superclass_type, superclass_package_name) {
    Ok(response) => Response::success(cmd_name, cwd_string, response),
    Err(error_msg) => Response::error(cmd_name, cwd_string, error_msg),
  }
}
