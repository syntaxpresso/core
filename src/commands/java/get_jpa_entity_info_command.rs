use std::path::Path;

use crate::{
  commands::java::{
    responses::get_jpa_entity_info_response::GetJpaEntityInfoResponse,
    services::get_jpa_entity_info_service::run,
  },
  common::{response::Response, validators::directory_validator::validate_file_path_within_base},
};

pub fn execute(
  cwd: &Path,
  entity_file_path: Option<&Path>,
  b64_source_code: Option<&str>,
) -> Response<GetJpaEntityInfoResponse> {
  let cwd_string = cwd.display().to_string();
  let cmd_name = String::from("get-jpa-entity-info");
  // Path containment validation: ensure entity file path (if provided) is within the cwd
  if let Some(file_path) = entity_file_path {
    let file_path_str = file_path.display().to_string();
    if let Err(error_msg) = validate_file_path_within_base(&file_path_str, cwd) {
      return Response::error(
        cmd_name,
        cwd_string,
        format!("Entity file path must be within working directory: {}", error_msg),
      );
    }
  }

  match run(entity_file_path, b64_source_code, cwd) {
    Ok(response) => Response::success(cmd_name, cwd_string, response),
    Err(error_msg) => Response::error(cmd_name, cwd_string, error_msg),
  }
}
